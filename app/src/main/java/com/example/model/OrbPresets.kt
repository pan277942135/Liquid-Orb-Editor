package com.example.model

/**
 * 13 authentic Liquid Orb presets ported from LerSent001/orb.
 */
object OrbPresets {

    val all: List<OrbConfig> = listOf(
        // 1. Siri Wave
        OrbConfig(
            presetId = "siri_wave",
            nameEn = "Siri Wave",
            nameZh = "Siri 动态光环",
            speed = 1.15f,
            radius = 0.64f,
            zoom = 1.30f,
            warp = 0.95f,
            ridgeAmt = 1.45f,
            sharp = 2.6f,
            shade = 0.85f,
            sheen = 1.6f,
            gloss = 4.2f,
            refraction = 1.35f,
            dispersion = 0.72f,
            roughness = 0.12f,
            glow = 1.8f,
            edgeSoftness = 0.07f,
            color1 = Vec3Color(0.04f, 0.76f, 0.95f),     // Bright Cyan
            color2 = Vec3Color(0.96f, 0.22f, 0.58f),     // Neon Pink
            accentColor = Vec3Color(0.62f, 0.20f, 0.95f), // Electric Violet
            coreColor = Vec3Color(0.03f, 0.02f, 0.14f),   // Dark Midnight
            glowColor = Vec3Color(0.35f, 0.55f, 0.98f),   // Siri Blue
            audioReactive = true,
            audioSensitivity = 1.2f,
            bassInfluence = 1.4f,
            midInfluence = 1.2f,
            trebleInfluence = 0.9f
        ),

        // 2. Voice Membrane
        OrbConfig(
            presetId = "voice_membrane",
            nameEn = "Voice Membrane",
            nameZh = "语音共振膜",
            speed = 0.85f,
            radius = 0.68f,
            zoom = 1.20f,
            warp = 1.35f,
            ridgeAmt = 2.1f,
            sharp = 3.2f,
            shade = 0.75f,
            sheen = 1.1f,
            gloss = 2.8f,
            refraction = 0.95f,
            dispersion = 0.45f,
            roughness = 0.22f,
            glow = 1.4f,
            edgeSoftness = 0.09f,
            color1 = Vec3Color(0.02f, 0.85f, 0.72f),     // Turquoise
            color2 = Vec3Color(0.24f, 0.45f, 0.95f),     // Indigo Blue
            accentColor = Vec3Color(0.75f, 0.15f, 0.85f), // Magenta
            coreColor = Vec3Color(0.01f, 0.03f, 0.10f),   // Deep Abyss
            glowColor = Vec3Color(0.05f, 0.80f, 0.82f),   // Cyan glow
            audioReactive = true,
            audioSensitivity = 1.5f,
            bassInfluence = 1.6f,
            midInfluence = 1.5f,
            trebleInfluence = 1.1f
        ),

        // 3. Aurora Veil
        OrbConfig(
            presetId = "aurora_veil",
            nameEn = "Aurora Veil",
            nameZh = "极光之幔",
            speed = 0.65f,
            radius = 0.66f,
            zoom = 1.25f,
            warp = 1.1f,
            ridgeAmt = 1.6f,
            sharp = 2.1f,
            shade = 0.9f,
            sheen = 1.8f,
            gloss = 3.5f,
            refraction = 1.2f,
            dispersion = 0.85f,
            roughness = 0.10f,
            glow = 1.9f,
            edgeSoftness = 0.11f,
            color1 = Vec3Color(0.12f, 0.92f, 0.58f),     // Aurora Green
            color2 = Vec3Color(0.18f, 0.75f, 0.95f),     // Arctic Sky
            accentColor = Vec3Color(0.85f, 0.25f, 0.80f), // Violet Curtain
            coreColor = Vec3Color(0.02f, 0.05f, 0.12f),   // Polar Night
            glowColor = Vec3Color(0.15f, 0.90f, 0.65f),   // Emerald Aura
            audioReactive = false
        ),

        // 4. Neural Plasma
        OrbConfig(
            presetId = "neural_plasma",
            nameEn = "Neural Plasma",
            nameZh = "神经等离子体",
            speed = 1.85f,
            radius = 0.62f,
            zoom = 1.45f,
            warp = 1.75f,
            ridgeAmt = 2.8f,
            sharp = 4.2f,
            shade = 0.70f,
            sheen = 2.2f,
            gloss = 5.0f,
            refraction = 1.5f,
            dispersion = 0.95f,
            roughness = 0.05f,
            glow = 2.4f,
            edgeSoftness = 0.06f,
            color1 = Vec3Color(0.78f, 0.12f, 0.95f),     // Electric Purple
            color2 = Vec3Color(0.05f, 0.92f, 0.95f),     // Neon Cyan
            accentColor = Vec3Color(1.00f, 0.20f, 0.55f), // Plasma Rose
            coreColor = Vec3Color(0.08f, 0.01f, 0.20f),   // Dark Matrix
            glowColor = Vec3Color(0.85f, 0.15f, 0.95f),   // High-voltage glow
            audioReactive = true,
            audioSensitivity = 1.3f
        ),

        // 5. Prismatic Field
        OrbConfig(
            presetId = "prismatic_field",
            nameEn = "Prismatic Field",
            nameZh = "棱镜场",
            speed = 0.75f,
            radius = 0.65f,
            zoom = 1.15f,
            warp = 0.55f,
            ridgeAmt = 0.85f,
            sharp = 4.8f,
            shade = 1.10f,
            sheen = 2.6f,
            gloss = 8.0f,
            refraction = 2.1f,
            dispersion = 1.45f,     // Extreme dispersion
            roughness = 0.02f,
            glow = 1.5f,
            edgeSoftness = 0.04f,
            color1 = Vec3Color(0.95f, 0.35f, 0.25f),     // Spectral Red
            color2 = Vec3Color(0.20f, 0.90f, 0.70f),     // Spectral Emerald
            accentColor = Vec3Color(0.35f, 0.50f, 1.00f), // Spectral Indigo
            coreColor = Vec3Color(0.04f, 0.04f, 0.06f),   // Obsidian Glass
            glowColor = Vec3Color(0.70f, 0.85f, 1.00f),   // Diamond Rim
            audioReactive = false
        ),

        // 6. Violet Core
        OrbConfig(
            presetId = "violet_core",
            nameEn = "Violet Core",
            nameZh = "紫晶核心",
            speed = 0.90f,
            radius = 0.67f,
            zoom = 1.35f,
            warp = 0.75f,
            ridgeAmt = 1.1f,
            sharp = 2.8f,
            shade = 0.95f,
            sheen = 1.7f,
            gloss = 4.0f,
            refraction = 1.4f,
            dispersion = 0.60f,
            roughness = 0.08f,
            glow = 2.1f,
            edgeSoftness = 0.08f,
            color1 = Vec3Color(0.55f, 0.08f, 0.90f),     // Royal Violet
            color2 = Vec3Color(0.95f, 0.15f, 0.65f),     // Core Magenta
            accentColor = Vec3Color(0.20f, 0.10f, 0.50f), // Deep Amethyst
            coreColor = Vec3Color(0.02f, 0.00f, 0.08f),   // Space Void
            glowColor = Vec3Color(0.65f, 0.12f, 0.95f),   // Violet Radiance
            audioReactive = true
        ),

        // 7. Crystal Drop
        OrbConfig(
            presetId = "crystal_drop",
            nameEn = "Crystal Drop",
            nameZh = "纯净水晶水滴",
            speed = 0.50f,
            radius = 0.63f,
            zoom = 1.10f,
            warp = 0.40f,
            ridgeAmt = 0.60f,
            sharp = 3.5f,
            shade = 1.25f,
            sheen = 2.0f,
            gloss = 7.5f,
            refraction = 1.85f,
            dispersion = 0.50f,
            roughness = 0.03f,
            glow = 1.1f,
            edgeSoftness = 0.04f,
            color1 = Vec3Color(0.85f, 0.95f, 1.00f),     // Pure Ice
            color2 = Vec3Color(0.55f, 0.85f, 0.98f),     // Cerulean Tint
            accentColor = Vec3Color(0.92f, 0.96f, 1.00f), // Caustic Bright
            coreColor = Vec3Color(0.02f, 0.05f, 0.08f),   // Deep Water
            glowColor = Vec3Color(0.70f, 0.90f, 1.00f),   // Crystal Sparkle
            audioReactive = false
        ),

        // 8. Violet Ember
        OrbConfig(
            presetId = "violet_ember",
            nameEn = "Violet Ember",
            nameZh = "紫烬熔岩",
            speed = 1.20f,
            radius = 0.65f,
            zoom = 1.40f,
            warp = 1.40f,
            ridgeAmt = 2.2f,
            sharp = 2.9f,
            shade = 0.80f,
            sheen = 1.5f,
            gloss = 3.6f,
            refraction = 1.1f,
            dispersion = 0.40f,
            roughness = 0.18f,
            glow = 2.2f,
            edgeSoftness = 0.09f,
            color1 = Vec3Color(0.98f, 0.35f, 0.10f),     // Fiery Amber
            color2 = Vec3Color(0.75f, 0.10f, 0.55f),     // Smoldering Purple
            accentColor = Vec3Color(1.00f, 0.65f, 0.20f), // Molten Gold
            coreColor = Vec3Color(0.08f, 0.01f, 0.04f),   // Volcanic Charcoal
            glowColor = Vec3Color(0.95f, 0.25f, 0.20f),   // Ember Glow
            audioReactive = false
        ),

        // 9. Refractive Gel
        OrbConfig(
            presetId = "refractive_gel",
            nameEn = "Refractive Gel",
            nameZh = "折射凝胶",
            speed = 0.70f,
            radius = 0.70f,
            zoom = 1.18f,
            warp = 1.80f,     // Heavy wobbly warp
            ridgeAmt = 1.3f,
            sharp = 1.8f,
            shade = 0.85f,
            sheen = 1.9f,
            gloss = 3.2f,
            refraction = 2.3f, // Super strong refraction
            dispersion = 0.80f,
            roughness = 0.10f,
            glow = 1.6f,
            edgeSoftness = 0.12f,
            color1 = Vec3Color(0.10f, 0.88f, 0.85f),     // Jelly Cyan
            color2 = Vec3Color(0.40f, 0.98f, 0.45f),     // Lime Gel
            accentColor = Vec3Color(0.95f, 0.90f, 0.30f), // Yellow Shimmer
            coreColor = Vec3Color(0.02f, 0.08f, 0.07f),   // Dark Emerald
            glowColor = Vec3Color(0.20f, 0.95f, 0.75f),   // Bio-luminescence
            audioReactive = false
        ),

        // 10. Chromatic Metal
        OrbConfig(
            presetId = "chromatic_metal",
            nameEn = "Chromatic Metal",
            nameZh = "炫彩液态金属",
            speed = 1.10f,
            radius = 0.65f,
            zoom = 1.30f,
            warp = 1.25f,
            ridgeAmt = 1.9f,
            sharp = 4.5f,
            shade = 0.60f,
            sheen = 2.8f,     // High sheen
            gloss = 9.0f,     // Mirror gloss
            refraction = 0.5f,
            dispersion = 1.1f,
            roughness = 0.02f,
            glow = 1.7f,
            edgeSoftness = 0.05f,
            color1 = Vec3Color(0.85f, 0.88f, 0.95f),     // Silver Mirror
            color2 = Vec3Color(0.95f, 0.40f, 0.70f),     // Anodized Pink
            accentColor = Vec3Color(0.30f, 0.70f, 1.00f), // Titanium Blue
            coreColor = Vec3Color(0.12f, 0.14f, 0.20f),   // Gunmetal
            glowColor = Vec3Color(0.70f, 0.80f, 0.98f),   // Chrome Edge
            audioReactive = false
        ),

        // 11. Frost Flow
        OrbConfig(
            presetId = "frost_flow",
            nameEn = "Frost Flow",
            nameZh = "极寒冰流",
            speed = 0.45f,
            radius = 0.65f,
            zoom = 1.25f,
            warp = 0.90f,
            ridgeAmt = 2.5f,
            sharp = 3.8f,
            shade = 1.05f,
            sheen = 1.8f,
            gloss = 5.2f,
            refraction = 1.6f,
            dispersion = 0.65f,
            roughness = 0.25f,
            glow = 1.5f,
            edgeSoftness = 0.08f,
            color1 = Vec3Color(0.70f, 0.92f, 0.98f),     // Glacial Blue
            color2 = Vec3Color(0.40f, 0.65f, 0.92f),     // Deep Fjord
            accentColor = Vec3Color(0.95f, 0.98f, 1.00f), // Frost Frosting
            coreColor = Vec3Color(0.04f, 0.08f, 0.16f),   // Frozen Trench
            glowColor = Vec3Color(0.55f, 0.85f, 1.00f),   // Subzero Glow
            audioReactive = false
        ),

        // 12. Liquid Chrome
        OrbConfig(
            presetId = "liquid_chrome",
            nameEn = "Liquid Chrome",
            nameZh = "液态水银",
            speed = 1.00f,
            radius = 0.63f,
            zoom = 1.22f,
            warp = 1.10f,
            ridgeAmt = 1.5f,
            sharp = 5.0f,
            shade = 0.50f,
            sheen = 3.0f,
            gloss = 10.0f,
            refraction = 0.35f,
            dispersion = 0.40f,
            roughness = 0.01f,
            glow = 1.4f,
            edgeSoftness = 0.04f,
            color1 = Vec3Color(0.92f, 0.94f, 0.96f),     // Liquid Silver
            color2 = Vec3Color(0.60f, 0.65f, 0.75f),     // Molten Platinum
            accentColor = Vec3Color(1.00f, 1.00f, 1.00f), // Pure Specular Reflection
            coreColor = Vec3Color(0.08f, 0.09f, 0.12f),   // Dark Chrome Shadow
            glowColor = Vec3Color(0.80f, 0.85f, 0.92f),   // Mercury Halo
            audioReactive = false
        ),

        // 13. Iridescent Opal
        OrbConfig(
            presetId = "iridescent_opal",
            nameEn = "Iridescent Opal",
            nameZh = "变彩欧泊宝石",
            speed = 0.80f,
            radius = 0.66f,
            zoom = 1.28f,
            warp = 0.85f,
            ridgeAmt = 1.7f,
            sharp = 2.5f,
            shade = 0.90f,
            sheen = 2.1f,
            gloss = 4.8f,
            refraction = 1.7f,
            dispersion = 1.25f,
            roughness = 0.07f,
            glow = 2.0f,
            edgeSoftness = 0.09f,
            color1 = Vec3Color(0.98f, 0.72f, 0.80f),     // Opal Rose
            color2 = Vec3Color(0.55f, 0.95f, 0.82f),     // Mint Pearlescence
            accentColor = Vec3Color(0.96f, 0.85f, 0.45f), // Golden Spark
            coreColor = Vec3Color(0.06f, 0.04f, 0.10f),   // Opal Dark Bed
            glowColor = Vec3Color(0.85f, 0.75f, 0.98f),   // Pastel Opal Halo
            audioReactive = false
        )
    )

    fun getById(id: String): OrbConfig {
        return all.firstOrNull { it.presetId == id } ?: all.first()
    }
}
