package com.futuremusic.visualizer

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.TypedValue
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var visualizerView: VisualizerView
    private lateinit var playButton: MaterialButton
    private lateinit var equalizerContainer: LinearLayout
    private lateinit var presetContainer: LinearLayout
    private var audioService: AudioPlaybackService? = null
    private var bound = false
    private val presetButtons = mutableListOf<MaterialButton>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlaybackService.LocalBinder
            audioService = binder.getService()
            bound = true
            audioService?.registerListener { levels ->
                visualizerView.updateLevels(levels)
                updateEqualizerFromLevels(levels)
            }
            audioService?.startPlayback()
            updatePlayButton()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            audioService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        visualizerView = findViewById(R.id.visualizerView)
        playButton = findViewById(R.id.playButton)
        equalizerContainer = findViewById(R.id.equalizerContainer)
        presetContainer = findViewById(R.id.presetContainer)

        setupEqualizerBars()
        setupPresetButtons()
        bindService(Intent(this, AudioPlaybackService::class.java), connection, BIND_AUTO_CREATE)

        playButton.setOnClickListener {
            val service = audioService ?: return@setOnClickListener
            if (service.isPlaying()) {
                service.pausePlayback()
            } else {
                service.resumePlayback()
            }
            updatePlayButton()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            audioService?.unregisterListener { }
            unbindService(connection)
        }
    }

    private fun setupEqualizerBars() {
        equalizerContainer.removeAllViews()
        for (band in 0 until 12) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
            }

            val label = TextView(this).apply {
                text = "B${band + 1}"
                setTextColor(android.graphics.Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                layoutParams = LinearLayout.LayoutParams(52, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val slider = SeekBar(this).apply {
                max = 100
                progress = 70
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        val value = seekBar.progress / 100f
                        visualizerView.setAutoPulse(value)
                    }
                })
            }

            row.addView(label)
            row.addView(slider)
            equalizerContainer.addView(row)
        }
    }

    private fun setupPresetButtons() {
        presetContainer.removeAllViews()
        val presets = PresetLibrary.defaultPresets
        for (preset in presets) {
            val button = MaterialButton(this).apply {
                text = preset.name
                setBackgroundColor(android.graphics.Color.parseColor("#171C32"))
                setTextColor(android.graphics.Color.WHITE)
                setOnClickListener {
                    audioService?.setPreset(preset)
                    presetButtons.forEach { it.isSelected = false }
                    isSelected = true
                }
            }
            presetButtons.add(button)
            presetContainer.addView(button)
        }
        presetButtons.firstOrNull()?.isSelected = true
    }

    private fun updatePlayButton() {
        if (audioService?.isPlaying() == true) {
            playButton.text = "Pause"
            playButton.setBackgroundColor(android.graphics.Color.parseColor("#FF4ECD"))
        } else {
            playButton.text = "Play"
            playButton.setBackgroundColor(android.graphics.Color.parseColor("#7C4DFF"))
        }
    }

    private fun updateEqualizerFromLevels(levels: FloatArray) {
        val children = equalizerContainer.childCount
        for (index in 0 until children) {
            val row = equalizerContainer.getChildAt(index) as? LinearLayout ?: continue
            val slider = row.getChildAt(1) as? SeekBar ?: continue
            val level = (levels.getOrNull(index) ?: 0f) * 100f
            slider.progress = level.toInt().coerceIn(0, 100)
        }
    }
}
