package com.example.glyph.model

import androidx.annotation.FontRes
import com.example.R

/**
 * Font options for dynamic Chinese species glyph rendering
 */
enum class GlyphFontOption(
    val labelZh: String,
    val labelEn: String,
    @param:FontRes val fontResId: Int?
) {
    MA_SHAN_ZHENG("马善政楷书", "Ma Shan Zheng (Calligraphy)", R.font.ma_shan_zheng),
    ZHI_MANG_XING("志芒星草书", "Zhi Mang Xing (Cursive)", R.font.zhi_mang_xing),
    SYSTEM_DEFAULT("系统黑体", "System Default (Sans-serif)", null)
}

/**
 * Configuration specification for a fish species name and its dynamic glyph rendering.
 * Supports any arbitrary Chinese text entered at runtime.
 */
data class SpeciesGlyphSpec(
    val speciesName: String = "黑鱼",
    val scientificName: String = "Channa argus",
    val englishName: String = "Northern Snakehead",
    val fontOption: GlyphFontOption = GlyphFontOption.MA_SHAN_ZHENG,
    val fontSizeSp: Float = 84f,
    val letterSpacing: Float = 0.08f,
    val isVertical: Boolean = false
) {
    companion object {
        val PRESET_SPECIES = listOf(
            SpeciesGlyphSpec("黑鱼", "Channa argus", "Northern Snakehead"),
            SpeciesGlyphSpec("大口黑鲈", "Micropterus salmoides", "Largemouth Bass"),
            SpeciesGlyphSpec("草鱼", "Ctenopharyngodon idella", "Grass Carp"),
            SpeciesGlyphSpec("鲫鱼", "Carassius auratus", "Crucian Carp"),
            SpeciesGlyphSpec("加州鲈", "Micropterus salmoides", "California Bass"),
            SpeciesGlyphSpec("罗非鱼", "Oreochromis niloticus", "Nile Tilapia"),
            SpeciesGlyphSpec("鲤鱼", "Cyprinus carpio", "Common Carp"),
            SpeciesGlyphSpec("鳜鱼", "Siniperca chuatsi", "Mandarin Fish")
        )
    }
}
