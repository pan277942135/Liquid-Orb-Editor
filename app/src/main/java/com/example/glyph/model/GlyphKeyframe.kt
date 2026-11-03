package com.example.glyph.model

/**
 * The 6 visual keyframe checkpoints for the dynamic species glyph condensation pipeline.
 *
 * Sequence:
 * AI Orb 水晶球 → 水核横向舒展 → 水光流带释放 → 中文 Glyph 初凝 → 水晶书法凝结 → 深湖蓝文字稳定
 */
enum class GlyphKeyframe(
    val id: String,
    val targetTimeMs: Long,
    val normalizedProgress: Float,
    val nameZh: String,
    val nameEn: String,
    val descriptionZh: String
) {
    KF01_ORB_SPHERE(
        id = "KF01",
        targetTimeMs = 0L,
        normalizedProgress = 0.00f,
        nameZh = "水晶球体",
        nameEn = "Orb Sphere",
        descriptionZh = "AI Orb 初始球体，水核常态呼吸，玻璃微反光"
    ),
    KF02_WATER_CORE_STRETCH(
        id = "KF02",
        targetTimeMs = 480L,
        normalizedProgress = 0.20f,
        nameZh = "水核横向舒展",
        nameEn = "Water Core Stretch",
        descriptionZh = "水核横向非线性拉伸 (1.45~1.65:1)，内部微湍流激荡"
    ),
    KF03_WATER_RIBBON_RELEASE(
        id = "KF03",
        targetTimeMs = 960L,
        normalizedProgress = 0.40f,
        nameZh = "水光流带释放",
        nameEn = "Water Ribbon Release",
        descriptionZh = "水核向两侧舒展释放 2~3 条贝塞尔水光流带，外层弧光发散"
    ),
    KF04_GLYPH_INITIAL(
        id = "KF04",
        targetTimeMs = 1440L,
        normalizedProgress = 0.60f,
        nameZh = "中文初凝",
        nameEn = "Glyph Initial",
        descriptionZh = "鱼种书法笔画骨架初现，水光流体渗入笔画轮廓"
    ),
    KF05_GLYPH_CRYSTALLIZE(
        id = "KF05",
        targetTimeMs = 1920L,
        normalizedProgress = 0.80f,
        nameZh = "水晶书法凝结",
        nameEn = "Glyph Crystallize",
        descriptionZh = "文字结晶折射增强，笔画边缘高光聚焦，金色光丝缠绕"
    ),
    KF06_GLYPH_SETTLED(
        id = "KF06",
        targetTimeMs = 2400L,
        normalizedProgress = 1.00f,
        nameZh = "深湖蓝文字稳定",
        nameEn = "Glyph Settled",
        descriptionZh = "文字稳定呈现，具有水晶玻璃、墨韵与金色光丝呼吸特征"
    );

    companion object {
        fun fromProgress(progress: Float): GlyphKeyframe {
            val clamped = progress.coerceIn(0f, 1f)
            return entries.minByOrNull { kotlin.math.abs(it.normalizedProgress - clamped) } ?: KF01_ORB_SPHERE
        }
    }
}
