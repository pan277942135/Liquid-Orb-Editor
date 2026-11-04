package com.example.aiorb.ribbon

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color

/**
 * Visual and material specification for a single 3D ribbon.
 */
data class RibbonStyle(
    val id: String,
    val nameZh: String,
    val bodyColor: Color,
    val edgeHighlight: Color,
    val widthMultiplier: Float,
    val isSunRibbon: Boolean,
    val baseTilt: Float,
    val basePhase: Float
) {
    val blendMode: BlendMode
        get() = if (isSunRibbon) BlendMode.Screen else BlendMode.SrcOver
}

/**
 * Complete parameter state for the Dual-Rim Specular Ribbons (水草与流体 3D 实体带状双侧高光).
 */
data class DualRimRibbonConfig(
    // 实体带基准宽度 (与倍率结合决定水草叶片最厚处的物理尺寸)
    val seagrassRibbonWidth: Float = 14.0f,      // 4.0 .. 30.0
    // 水流柔顺摆动幅度 (控制纬度方向沿正弦波起伏的剧烈程度)
    val seagrassWaveAmount: Float = 0.92f,        // 0.0 .. 2.0
    // 丝带数量 (1 .. 5)
    val filamentCount: Int = 5,
    // 丝带总可见度 (0.0 .. 1.0)
    val filamentVisibility: Float = 0.98f,
    // 丝带公转与水流涌动速度 (0.1 .. 3.0)
    val filamentOrbitSpeed: Float = 0.95f,
    // 三维螺旋扭曲度 (0.5 .. 5.0)
    val filamentTwist: Float = 2.8f,
    // 丝带轨道半径比 (1.05 .. 1.45)
    val filamentRadiusRatio: Float = 1.22f,
    // 中央纯白发光核细线的基础粗细 (1.0 .. 8.0)
    val filamentThickness: Float = 4.0f,
    // 声音振幅扩张系数 (0.0 .. 1.0)
    val filamentAudioExpand: Float = 0.35f,
    // 11 点钟日光强度 (0.0 .. 1.0)
    val sunlightIntensity: Float = 1.0f,
    // 轨道空间对角倾角 (原画高保真 -30 deg)
    val orbitInclinationDeg: Float = -30.0f
) {
    companion object {
        /**
         * 5 条多光谱与多材质丝带定义 (1:1 对齐高保真参考工程)
         */
        val DEFAULT_STYLES: List<RibbonStyle> = listOf(
            // 0: 暖金耀光能量带 (绕球轨道金光环，受光面爆发星芒)
            RibbonStyle(
                id = "sun_corona_gold",
                nameZh = "暖金耀光能量带",
                bodyColor = Color(0xFFFBBF24),      // 琥珀金
                edgeHighlight = Color(0xFFFEF08A),  // 太阳浅金
                widthMultiplier = 1.00f,
                isSunRibbon = true,
                baseTilt = -0.5235988f,             // -30 deg
                basePhase = 0.0f
            ),
            // 1: 翠绿水草阔叶片 (最宽阔的水草叶，宽达 18.9dp，浸水植物质感)
            RibbonStyle(
                id = "emerald_seagrass_broad",
                nameZh = "翠绿水草阔叶片",
                bodyColor = Color(0xFF15803D),      // 深林苔绿
                edgeHighlight = Color(0xFFD9F99D),  // 嫩芽微光
                widthMultiplier = 1.35f,            // 1.35x 达到 18.9dp
                isSunRibbon = false,
                baseTilt = -0.38f,
                basePhase = 1.2566f                 // 2 * PI / 5
            ),
            // 2: 晶莹透流水体环 (如同流动水银玻璃环，纯白超高反光边)
            RibbonStyle(
                id = "crystalline_liquid_water",
                nameZh = "晶莹透流水体环",
                bodyColor = Color(0xFF38BDF8),      // 冰清水蓝
                edgeHighlight = Color.White,
                widthMultiplier = 0.95f,
                isSunRibbon = false,
                baseTilt = -0.62f,
                basePhase = 2.5132f                 // 4 * PI / 5
            ),
            // 3: 柔美回旋嫩绿水草 (左下角自然回卷的水草尾部)
            RibbonStyle(
                id = "tender_swirling_seagrass",
                nameZh = "柔美回旋嫩绿水草",
                bodyColor = Color(0xFF84CC16),      // 鲜嫩草绿
                edgeHighlight = Color(0xFFBEF264),  // 荧光黄绿
                widthMultiplier = 1.15f,
                isSunRibbon = false,
                baseTilt = -0.22f,
                basePhase = 3.7699f                 // 6 * PI / 5
            ),
            // 4: 太阳等离子金光弧 (连接 11 点钟日照耀斑的高能细弧带)
            RibbonStyle(
                id = "solar_plasma_arc",
                nameZh = "太阳等离子金光弧",
                bodyColor = Color(0xFFF59E0B),      // 暖阳橙金
                edgeHighlight = Color.White,
                widthMultiplier = 0.85f,
                isSunRibbon = true,
                baseTilt = -0.50f,
                basePhase = 5.0265f                 // 8 * PI / 5
            )
        )
    }
}
