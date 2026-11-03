package com.example.glyph.model

import com.example.model.Vec3Color
import org.json.JSONObject

/**
 * Optical and material configuration profile for the Yujian Water Ink Chinese species glyph system.
 */
data class YujianGlyphMaterialProfile(
    val speciesName: String = "黑鱼",
    val scientificName: String = "Channa argus",
    val fontOption: GlyphFontOption = GlyphFontOption.MA_SHAN_ZHENG,
    
    // Geometry & Stretch
    val maxStretchX: Float = 1.58f,
    val maxStretchY: Float = 0.84f,
    val finalStretchX: Float = 1.28f,
    val finalStretchY: Float = 0.95f,
    
    // Optical & Fluid Physics
    val ribbonIntensity: Float = 1.0f,
    val fluidTurbulence: Float = 1.25f,
    val crystalRefraction: Float = 1.62f,
    val specularGloss: Float = 4.5f,
    val inkOpacity: Float = 0.90f,
    val goldFilamentAlpha: Float = 0.70f,
    val edgeRefractionWidth: Float = 0.08f,
    val outerGlow: Float = 1.45f,
    
    // Palette Colors (Yujian Water Ink standard)
    val lakeInkColor: Vec3Color = Vec3Color.fromHex("#173E49"),     // Deep Lake Ink (深湖蓝/水墨)
    val lakeTealColor: Vec3Color = Vec3Color.fromHex("#2E8B83"),    // Lake Teal (湖水蓝绿)
    val waterCyanColor: Vec3Color = Vec3Color.fromHex("#67C8D2"),   // Water Cyan (水光青蓝)
    val deepWaterColor: Vec3Color = Vec3Color.fromHex("#174F5D"),  // Deep Water (深潭底色)
    val warmGoldColor: Vec3Color = Vec3Color.fromHex("#D8B36A"),    // Warm Gold (暖金光丝)
    val sunGlintColor: Vec3Color = Vec3Color.fromHex("#F6ECC9")     // Soft Sunlight Glint
) {
    /**
     * Serializes this profile to JSON format matching the deliverable contract.
     */
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("profileVersion", "YUJIAN_GLYPH_V2_FROZEN")
        root.put("speciesName", speciesName)
        root.put("scientificName", scientificName)
        root.put("fontFamily", fontOption.name)
        root.put("fontLabel", fontOption.labelZh)

        val geometry = JSONObject()
        geometry.put("maxStretchX", maxStretchX)
        geometry.put("maxStretchY", maxStretchY)
        geometry.put("finalStretchX", finalStretchX)
        geometry.put("finalStretchY", finalStretchY)
        root.put("geometry", geometry)

        val optics = JSONObject()
        optics.put("ribbonIntensity", ribbonIntensity)
        optics.put("fluidTurbulence", fluidTurbulence)
        optics.put("crystalRefraction", crystalRefraction)
        optics.put("specularGloss", specularGloss)
        optics.put("inkOpacity", inkOpacity)
        optics.put("goldFilamentAlpha", goldFilamentAlpha)
        optics.put("edgeRefractionWidth", edgeRefractionWidth)
        optics.put("outerGlow", outerGlow)
        root.put("optics", optics)

        val palette = JSONObject()
        palette.put("lakeInk", lakeInkColor.toHex())
        palette.put("lakeTeal", lakeTealColor.toHex())
        palette.put("waterCyan", waterCyanColor.toHex())
        palette.put("deepWater", deepWaterColor.toHex())
        palette.put("warmGold", warmGoldColor.toHex())
        palette.put("sunGlint", sunGlintColor.toHex())
        root.put("palette", palette)

        val keyframes = JSONObject()
        for (kf in GlyphKeyframe.entries) {
            val kfObj = JSONObject()
            kfObj.put("timeMs", kf.targetTimeMs)
            kfObj.put("progress", kf.normalizedProgress)
            kfObj.put("nameZh", kf.nameZh)
            kfObj.put("nameEn", kf.nameEn)
            keyframes.put(kf.id, kfObj)
        }
        root.put("keyframes", keyframes)

        return root.toString(2)
    }

    companion object {
        fun createDefault(species: String = "黑鱼", scientific: String = "Channa argus"): YujianGlyphMaterialProfile {
            return YujianGlyphMaterialProfile(
                speciesName = species,
                scientificName = scientific
            )
        }

        fun forLargemouthBass(): YujianGlyphMaterialProfile {
            return YujianGlyphMaterialProfile(
                speciesName = "大口黑鲈",
                scientificName = "Micropterus salmoides",
                fontOption = GlyphFontOption.MA_SHAN_ZHENG,
                maxStretchX = 1.62f,
                finalStretchX = 1.34f,
                crystalRefraction = 1.68f,
                inkOpacity = 0.92f,
                goldFilamentAlpha = 0.75f
            )
        }
    }
}
