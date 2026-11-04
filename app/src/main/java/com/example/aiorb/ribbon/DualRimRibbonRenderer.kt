package com.example.aiorb.ribbon

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * 3D 实体带状双侧高光 (Dual-Rim Specular Ribbons) 渲染管线
 *
 * 在 Jetpack Compose 原生 Canvas 上实时达到电影级渲染的水草厚度感、透水感与浸水反光质感：
 * 1. 3D 环形参数化曲线 (Parametric Spline) + 正弦水流波动 + 轨道倾角与视线旋转
 * 2. 前后景深度分流 (Z-Buffer Simulation)
 * 3. 沿法向量展开构造 3D 四边形面片 (Ribbon Mesh Strip)
 * 4. 四重分层着色：
 *    ① 实体半透色体面片 (Body Fill)
 *    ② 左右双侧物理高光折射线 (Dual-Rim Specular Lines, BlendMode.Plus)
 *    ③ 中央能量发光核纤 (Core Thread, BlendMode.Plus)
 *    ④ 强光位星芒节点 (Light Knot Starburst, BlendMode.Plus / Screen)
 */
object DualRimRibbonRenderer {

    private const val SAMPLE_STEPS = 100

    fun renderRibbons(
        drawScope: DrawScope,
        isBackPass: Boolean,
        center: Offset,
        orbRadiusPx: Float,
        elapsedSec: Float,
        config: DualRimRibbonConfig,
        yawRad: Float = 0f,
        pitchRad: Float = 0f,
        alphaProgress: Float = 1.0f
    ) {
        if (alphaProgress <= 0.01f || config.filamentVisibility <= 0.01f) return

        val baseVisibility = (config.filamentVisibility * alphaProgress).coerceIn(0f, 1f)
        val styles = DualRimRibbonConfig.DEFAULT_STYLES
        val count = config.filamentCount.coerceIn(1, styles.size)

        // 空间倾角变换矩阵参数 (-30 deg)
        val incRad = config.orbitInclinationDeg * (PI.toFloat() / 180f)
        val cosInc = cos(incRad)
        val sinInc = sin(incRad)

        // Yaw & Pitch 旋转参数
        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)

        for (i in 0 until count) {
            val style = styles[i]

            var prevPoint: Offset? = null
            var prevNormal: Offset? = null

            for (step in 0..SAMPLE_STEPS) {
                val s = step.toFloat() / SAMPLE_STEPS.toFloat()

                // 1. 3D 环形参数化拓扑 + 正弦水流波动
                val azimuth = style.basePhase +
                        2.0f * PI.toFloat() * config.filamentTwist * s +
                        elapsedSec * config.filamentOrbitSpeed

                val waveOffset = sin(3.0f * PI.toFloat() * s + elapsedSec * 1.85f + i * 0.85f)
                val latitude = style.baseTilt + config.seagrassWaveAmount * 0.28f * waveOffset

                // 径向半径：带有自然呼吸与细微螺旋膨胀
                val radiusFactor = 1.0f + 0.05f * sin(2.0f * PI.toFloat() * s + i.toFloat())
                val r3d = orbRadiusPx * config.filamentRadiusRatio * radiusFactor

                // 球坐标 -> 直角坐标
                val cosLat = cos(latitude)
                val sinLat = sin(latitude)
                val cosAz = cos(azimuth)
                val sinAz = sin(azimuth)

                val x0 = r3d * cosLat * cosAz
                val y0 = r3d * sinLat
                val z0 = r3d * cosLat * sinAz

                // 注入 -30° 空间轨道倾角
                val x1 = x0 * cosInc - y0 * sinInc
                val y1 = x0 * sinInc + y0 * cosInc
                val z1 = z0

                // 注入 Yaw (绕 Y 轴) 与 Pitch (绕 X 轴) 旋转
                val xRot = x1 * cosYaw + z1 * sinYaw
                val zTemp = -x1 * sinYaw + z1 * cosYaw
                val yRot = y1 * cosPitch - zTemp * sinPitch
                val zRot = y1 * sinPitch + zTemp * cosPitch

                // 投影至屏幕 2D 坐标
                val currentPoint = center + Offset(xRot, yRot)

                // 2. 前后景深度分流 (Z-Buffer Simulation)
                val segmentIsBack = (zRot < 0f)

                if (prevPoint != null) {
                    val p0 = prevPoint
                    val p1 = currentPoint
                    val dx = p1.x - p0.x
                    val dy = p1.y - p0.y
                    val segLen = hypot(dx, dy)

                    if (segLen > 0.05f) {
                        val nx = -dy / segLen
                        val ny = dx / segLen
                        val currNormal = Offset(nx, ny)
                        val pNorm = prevNormal ?: currNormal

                        if (segmentIsBack == isBackPass) {
                            // 叶片形态渐缩：中间宽两端尖 (sin(pi * s)^1.15)
                            val taper = sin(PI.toFloat() * s).pow(1.15f)
                            val distFromCenter = hypot(xRot, yRot)
                            // 穿过球体后方的半透明衰减
                            val occlusion = if (isBackPass && distFromCenter < orbRadiusPx) 0.35f else 1.0f
                            val segmentAlpha = (baseVisibility * taper * occlusion).coerceIn(0f, 1f)

                            if (segmentAlpha > 0.015f) {
                                // 带有叶片宽度倍率的半宽
                                val backScale = if (isBackPass) 0.82f else 1.15f
                                val halfWidth = (config.seagrassRibbonWidth * style.widthMultiplier * taper * 0.5f * backScale)
                                    .coerceAtLeast(1.4f)

                                // 向左右法线两侧外推 4 个面片顶点
                                val p1Left = p0 + pNorm * halfWidth
                                val p1Right = p0 - pNorm * halfWidth
                                val p2Left = p1 + currNormal * halfWidth
                                val p2Right = p1 - currNormal * halfWidth

                                // ① 绘制 3D 实体面片条带 (Ribbon Mesh Strip)
                                val ribbonPath = Path().apply {
                                    moveTo(p1Left.x, p1Left.y)
                                    lineTo(p2Left.x, p2Left.y)
                                    lineTo(p2Right.x, p2Right.y)
                                    lineTo(p1Right.x, p1Right.y)
                                    close()
                                }
                                drawScope.drawPath(
                                    path = ribbonPath,
                                    color = style.bodyColor.copy(alpha = segmentAlpha * 0.65f),
                                    style = Fill,
                                    blendMode = style.blendMode
                                )

                                // ② 绘制双侧物理高光边缘线 (Dual-Rim Specular Edge Lines)
                                val edgeAlpha = (segmentAlpha * 0.85f).coerceIn(0f, 1f)
                                // 左边缘高光反射
                                drawScope.drawLine(
                                    color = style.edgeHighlight.copy(alpha = edgeAlpha),
                                    start = p1Left,
                                    end = p2Left,
                                    strokeWidth = 1.35f,
                                    cap = StrokeCap.Round,
                                    blendMode = BlendMode.Plus
                                )
                                // 右边缘高光反射
                                drawScope.drawLine(
                                    color = style.edgeHighlight.copy(alpha = edgeAlpha),
                                    start = p1Right,
                                    end = p2Right,
                                    strokeWidth = 1.35f,
                                    cap = StrokeCap.Round,
                                    blendMode = BlendMode.Plus
                                )

                                // ③ 绘制中央高光细核 (Core Thread)
                                val coreWidth = (config.filamentThickness * taper * 0.35f).coerceAtLeast(0.9f)
                                drawScope.drawLine(
                                    color = Color.White.copy(alpha = segmentAlpha * 0.80f),
                                    start = p0,
                                    end = p1,
                                    strokeWidth = coreWidth,
                                    cap = StrokeCap.Round,
                                    blendMode = BlendMode.Plus
                                )

                                // ④ 强光位星芒节点 (Light Knot Starburst)
                                if (!isBackPass && style.isSunRibbon && s in 0.42f..0.48f) {
                                    drawLightKnot(
                                        drawScope = drawScope,
                                        center = p1,
                                        size = orbRadiusPx * 0.22f * config.sunlightIntensity,
                                        color = style.bodyColor
                                    )
                                }
                            }
                        }
                        prevNormal = currNormal
                    }
                }

                prevPoint = currentPoint
            }
        }
    }

    /**
     * 强光位爆发星芒节点 (Light Knot Starburst)
     */
    private fun drawLightKnot(
        drawScope: DrawScope,
        center: Offset,
        size: Float,
        color: Color
    ) {
        if (size <= 1.0f) return

        // 1. 柔和星光日晕 (Screen Blend)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, color.copy(alpha = 0.55f), Color.Transparent),
                center = center,
                radius = size * 1.6f
            ),
            radius = size * 1.6f,
            center = center,
            blendMode = BlendMode.Screen
        )

        // 2. 纯白高能主十字星芒 (BlendMode.Plus)
        val spikeWhite = Color.White.copy(alpha = 0.95f)
        val mainLen = size * 1.65f
        drawScope.drawLine(
            color = spikeWhite,
            start = center - Offset(mainLen, 0f),
            end = center + Offset(mainLen, 0f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
        drawScope.drawLine(
            color = spikeWhite,
            start = center - Offset(0f, mainLen),
            end = center + Offset(0f, mainLen),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )

        // 3. 对角金色副星芒
        val diagLen = size * 1.05f
        val diagCol = color.copy(alpha = 0.80f)
        drawScope.drawLine(
            color = diagCol,
            start = center - Offset(diagLen, diagLen),
            end = center + Offset(diagLen, diagLen),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
        drawScope.drawLine(
            color = diagCol,
            start = center - Offset(-diagLen, diagLen),
            end = center + Offset(-diagLen, diagLen),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
    }
}
