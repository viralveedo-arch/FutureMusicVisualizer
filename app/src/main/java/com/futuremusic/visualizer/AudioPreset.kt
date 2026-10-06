package com.futuremusic.visualizer

import android.graphics.Color

data class AudioPreset(
    val name: String,
    val bass: Float,
    val treble: Float,
    val glow: Float,
    val warp: Float
)

object PresetLibrary {
    val defaultPresets = listOf(
        AudioPreset("Neon Pulse", 1.1f, 0.7f, 0.9f, 0.8f),
        AudioPreset("Bass Rush", 1.6f, 0.4f, 0.7f, 1.1f),
        AudioPreset("Midnight Echo", 0.9f, 1.0f, 1.2f, 0.6f),
        AudioPreset("Aurora Drift", 1.2f, 1.4f, 1.5f, 1.0f),
        AudioPreset("Cyber Wave", 1.5f, 1.2f, 1.1f, 1.4f)
    )
}
