package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * 3-component RGB float vector (0.0 to 1.0)
 */
data class Vec3Color(val r: Float, val g: Float, val b: Float) {
    fun toColor(): Color = Color(r, g, b, 1f)
    
    fun toHex(): String {
        val ri = (r.coerceIn(0f, 1f) * 255).toInt()
        val gi = (g.coerceIn(0f, 1f) * 255).toInt()
        val bi = (b.coerceIn(0f, 1f) * 255).toInt()
        return String.format("#%02X%02X%02X", ri, gi, bi)
    }

    companion object {
        fun fromHex(hex: String): Vec3Color {
            val cleanHex = hex.removePrefix("#")
            return try {
                val rgb = cleanHex.toLong(16)
                if (cleanHex.length == 6) {
                    val r = ((rgb shr 16) and 0xFF) / 255f
                    val g = ((rgb shr 8) and 0xFF) / 255f
                    val b = (rgb and 0xFF) / 255f
                    Vec3Color(r, g, b)
                } else if (cleanHex.length == 8) {
                    val r = ((rgb shr 16) and 0xFF) / 255f
                    val g = ((rgb shr 8) and 0xFF) / 255f
                    val b = (rgb and 0xFF) / 255f
                    Vec3Color(r, g, b)
                } else {
                    Vec3Color(0.5f, 0.5f, 0.5f)
                }
            } catch (e: Exception) {
                Vec3Color(0.5f, 0.5f, 0.5f)
            }
        }

        fun fromColor(color: Color): Vec3Color {
            return Vec3Color(color.red, color.green, color.blue)
        }
    }
}

/**
 * Complete parameter state for the Liquid Orb,
 * mapping 1:1 to LerSent001/orb WebGPU/Metal parameters.
 */
data class OrbConfig(
    val presetId: String = "siri_wave",
    val nameEn: String = "Siri Wave",
    val nameZh: String = "Siri 动态光环",
    
    // Geometry & Motion
    val speed: Float = 1.0f,          // 0.1 .. 3.0
    val radius: Float = 0.65f,        // 0.2 .. 1.2
    val zoom: Float = 1.35f,          // 0.5 .. 3.0
    val warp: Float = 0.85f,          // 0.0 .. 3.0
    val ridgeAmt: Float = 1.2f,       // 0.0 .. 4.0
    val sharp: Float = 2.4f,          // 0.5 .. 6.0
    
    // Material Optics
    val shade: Float = 0.8f,          // 0.0 .. 2.0
    val sheen: Float = 1.4f,          // 0.0 .. 3.0
    val gloss: Float = 3.8f,          // 0.1 .. 10.0
    val refraction: Float = 1.2f,     // 0.0 .. 2.5
    val dispersion: Float = 0.65f,    // 0.0 .. 1.5
    val roughness: Float = 0.15f,     // 0.0 .. 1.0
    
    // Outer Glow & Edge
    val glow: Float = 1.6f,           // 0.0 .. 3.0
    val edgeSoftness: Float = 0.08f,  // 0.01 .. 0.5
    
    // Palette Colors
    val color1: Vec3Color = Vec3Color(0.02f, 0.71f, 0.83f),     // Cyan
    val color2: Vec3Color = Vec3Color(0.92f, 0.28f, 0.60f),     // Magenta
    val accentColor: Vec3Color = Vec3Color(0.54f, 0.17f, 0.89f), // Purple
    val coreColor: Vec3Color = Vec3Color(0.05f, 0.02f, 0.18f),   // Deep Indigo
    val glowColor: Vec3Color = Vec3Color(0.24f, 0.60f, 0.96f),   // Blue/Violet glow
    
    // Audio Reactivity
    val audioReactive: Boolean = false,
    val audioSensitivity: Float = 1.0f,
    val bassInfluence: Float = 1.2f,
    val midInfluence: Float = 1.0f,
    val trebleInfluence: Float = 0.8f,
    
    // Interactive drag displacement
    val dragTurbulence: Float = 0.0f
)

/**
 * Preview environment scene backgrounds
 */
enum class SceneBackground(val labelEn: String, val labelZh: String) {
    COSMIC_DEEP("Cosmic Deep", "深空暗夜"),
    OLED_BLACK("OLED Black", "纯黑无界"),
    CYBER_GRID("Cyber Grid", "赛博网格"),
    STUDIO_SPOT("Studio Light", "摄影棚光"),
    AURORA_SKY("Aurora Sky", "极光微芒")
}
