package com.example.aiorb.model

import android.graphics.Color
import kotlin.math.PI
import kotlin.math.sin

data class WaterDroplet(
    val id: String,
    val baseUv: Point2D,
    val diameterDp: Float,
    val bodyAlpha: Float,
    val rimAlpha: Float,
    val periodMs: Long,
    val phase: Float
) {
    /**
     * Compute current drifted UV position inside core water body
     * maxHorizontalDrift: 0.007D, maxVerticalDrift: 0.012D
     */
    fun computeCurrentUv(sceneElapsedMs: Long): Point2D {
        val t = (phase + sceneElapsedMs.toDouble() / periodMs) * 2.0 * PI
        val dx = 0.007f * sin(t).toFloat()
        val dy = 0.012f * sin(t * 0.85 + 0.5).toFloat()
        return Point2D(baseUv.x + dx, baseUv.y + dy)
    }

    fun computeCurrentAlpha(wakeProgress: Float, isS2: Boolean): Float {
        val base = if (isS2) rimAlpha else rimAlpha * 0.4f
        return (base * (0.3f + 0.7f * wakeProgress)).coerceIn(0f, 1f)
    }

    companion object {
        val DROBLET_LIST = listOf(
            WaterDroplet("D1", Point2D(0.32f, 0.36f), 1.8f, 0.23f, 0.38f, 5200L, 0.12f),
            WaterDroplet("D2", Point2D(0.68f, 0.56f), 2.2f, 0.26f, 0.44f, 6100L, 0.49f),
            WaterDroplet("D3", Point2D(0.49f, 0.73f), 1.4f, 0.19f, 0.34f, 4800L, 0.83f)
        )
    }
}

object SunlightContract {
    val SOURCE_UV = Point2D(0.24f, 0.20f)
    const val MAIN_COLOR_HEX = "#D8BC87"
    val MAIN_COLOR = Color.parseColor(MAIN_COLOR_HEX)
    const val SPECULAR_COLOR_HEX = "#FFF4D7"
    val SPECULAR_COLOR = Color.parseColor(SPECULAR_COLOR_HEX)

    const val GLASS_SPECULAR_ALPHA_MAX = 0.11f
    const val CORE_REFRACTION_ALPHA_MAX = 0.11f
    const val CAUSTIC_RESPONSE_ALPHA_MAX = 0.14f
    const val GRASS_RESPONSE_ALPHA_MAX = 0.06f
    const val DROPLET_SPECULAR_ALPHA_MAX = 0.32f
    const val TOTAL_ADDITIONAL_ADDITIVE_ALPHA_MAX = 0.22f

    const val GLASS_SPECULAR_PERIOD_MS = 6400L
    const val CORE_REFRACTION_PERIOD_MS = 5200L

    fun computeGlassSpecularAlpha(sceneElapsedMs: Long, wakeProgress: Float): Float {
        val t = (sceneElapsedMs.toDouble() / GLASS_SPECULAR_PERIOD_MS) * 2.0 * PI
        val sineVal = 0.5f + 0.5f * sin(t).toFloat()
        return (GLASS_SPECULAR_ALPHA_MAX * sineVal * (0.2f + 0.8f * wakeProgress)).coerceIn(0f, GLASS_SPECULAR_ALPHA_MAX)
    }

    fun computeCoreRefractionAlpha(sceneElapsedMs: Long, wakeProgress: Float): Float {
        val t = (sceneElapsedMs.toDouble() / CORE_REFRACTION_PERIOD_MS) * 2.0 * PI
        val sineVal = 0.5f + 0.5f * sin(t + 1.2).toFloat()
        return (CORE_REFRACTION_ALPHA_MAX * sineVal * (0.2f + 0.8f * wakeProgress)).coerceIn(0f, CORE_REFRACTION_ALPHA_MAX)
    }
}

object CausticsContract {
    const val DEEP_PERIOD_MS = 6500L
    const val DEEP_FREQUENCY = 1.65f
    const val DEEP_ALPHA_MAX = 0.07f

    const val SURFACE_PERIOD_MS = 4100L
    const val SURFACE_FREQUENCY = 3.05f
    const val SURFACE_ALPHA_MAX = 0.12f

    const val INNER_RADIUS_D = 0.385f
    const val EDGE_FADE_D = 0.035f

    fun computeCausticsAlpha(sceneElapsedMs: Long, wakeProgress: Float): Float {
        val tDeep = (sceneElapsedMs.toDouble() / DEEP_PERIOD_MS) * 2.0 * PI
        val tSurf = (sceneElapsedMs.toDouble() / SURFACE_PERIOD_MS) * 2.0 * PI
        val dAlpha = DEEP_ALPHA_MAX * (0.5f + 0.5f * sin(tDeep).toFloat())
        val sAlpha = SURFACE_ALPHA_MAX * (0.5f + 0.5f * sin(tSurf).toFloat())
        val combined = (dAlpha + sAlpha) * (0.2f + 0.8f * wakeProgress)
        return combined.coerceIn(0f, 0.19f)
    }
}
