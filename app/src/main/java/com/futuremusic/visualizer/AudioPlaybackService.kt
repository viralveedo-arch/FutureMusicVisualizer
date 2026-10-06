package com.futuremusic.visualizer

import android.os.Build
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Binder
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.media.audiofx.Visualizer
import androidx.core.app.NotificationCompat
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.sin

class AudioPlaybackService : Service() {

    private val binder = LocalBinder()
    private var audioTrack: AudioTrack? = null
    private var visualizer: Visualizer? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var running = false
    private var lastLevel = FloatArray(12) { 0f }
    private var currentPreset = PresetLibrary.defaultPresets[0]
    private val listeners = CopyOnWriteArrayList<(FloatArray) -> Unit>()

    fun registerListener(listener: (FloatArray) -> Unit) {
        listeners += listener
    }

    fun unregisterListener(listener: (FloatArray) -> Unit) {
        listeners.remove(listener)
    }

    fun setPreset(preset: AudioPreset) {
        currentPreset = preset
    }

    fun startPlayback() {
        if (running) return
        running = true
        initializeAudioTrack()
        startSynthLoop()
        startForeground(1001, buildNotification())
    }

    fun pausePlayback() {
        running = false
        audioTrack?.pause()
        updateVisualizerLevels(FloatArray(12) { 0f })
    }

    fun resumePlayback() {
        startPlayback()
        audioTrack?.play()
    }

    fun isPlaying(): Boolean = running

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        running = false
        thread?.quitSafely()
        visualizer?.release()
        audioTrack?.stop()
        audioTrack?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun initializeAudioTrack() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioTrack = AudioTrack(
            android.media.MediaRecorder.AudioSource.DEFAULT,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2,
            AudioTrack.MODE_STREAM
        )
        audioTrack?.play()

        visualizer = Visualizer(audioTrack!!.audioSessionId).apply {
            enabled = true
            captureSize = 256
            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) = Unit

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                    if (fft == null) return
                    val levels = FloatArray(12) { index ->
                        val start = index * 2
                        val re = fft.getOrNull(start)?.toInt()?.and(0xFF)?.toFloat() ?: 0f
                        val im = fft.getOrNull(start + 1)?.toInt()?.and(0xFF)?.toFloat() ?: 0f
                        val magnitude = kotlin.math.sqrt(re * re + im * im) / 255f
                        magnitude.coerceIn(0f, 1f)
                    }
                    updateVisualizerLevels(levels)
                }
            }, Visualizer.getMaxCaptureSize(), false, true)
        }
    }

    private fun startSynthLoop() {
        thread = HandlerThread("future-audio-thread", android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        thread?.start()
        handler = Handler(thread!!.looper)

        val sampleRate = 44100
        val baseFreq = 110f
        val phaseStep = (2 * PI * baseFreq / sampleRate).toFloat()
        var angle = 0f

        handler?.post(object : Runnable {
            override fun run() {
                if (!running) return
                val bufferSize = 2048
                val buffer = ShortArray(bufferSize)
                for (i in buffer.indices) {
                    val drive = (sin(angle * 1.0f) + sin(angle * 2.35f) * currentPreset.treble + sin(angle * 0.75f) * currentPreset.bass)
                    val shimmer = sin(angle * 7.2f) * currentPreset.glow
                    val pulse = (1f + sin(angle * 0.15f) * currentPreset.warp)
                    val sample = (drive * 0.25f + shimmer * 0.35f) * pulse * 18000f
                    buffer[i] = sample.coerceIn(-32768f, 32767f).toInt().toShort()
                    angle += phaseStep
                }
                audioTrack?.write(buffer, 0, buffer.size)
                handler?.post(this)
            }
        })
    }

    private fun updateVisualizerLevels(levels: FloatArray) {
        val transformed = FloatArray(12) { index ->
            levels.getOrNull(index)?.coerceIn(0f, 1f) ?: 0f
        }
        listeners.forEach { it(transformed) }
        lastLevel = transformed
    }

    private fun buildNotification(): Notification {
        val channelId = "future_music_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Future Music Visualizer")
            .setContentText("Neon synth engine running in the background")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        return notificationBuilder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            val channel = NotificationChannel(
                "future_music_channel",
                "Future Music",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }
}
