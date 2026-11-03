package com.example.aiorb.gl

import com.example.aiorb.model.Point2D
import kotlin.math.sqrt

/**
 * Coordinate Mapper for AI Orb & R06 Filaments.
 *
 * Establishes the exact geometric contract between:
 * 1. OrbLocal Units (D): Center at (0, 0), sphere core radius = 0.5D, max envelope = 0.675D
 * 2. Texture UV Space: 1254x1254 PNG with measured transparent padding (core visible radius = 0.3971 UV)
 * 3. Screen Pixels: Dependent on screen resolution, minDim, and orbSizeScale
 * 4. OpenGL NDC: [-1, 1] clip space with aspect-ratio correction
 */
object OrbCoordinateMapper {

    /**
     * Exact empirically measured visible core radius in UV space of 透明湖水玻璃球.png.
     * Alpha threshold >= 128: width = 996px, height = 994px.
     * Visible Radius = 498.0px / 1254.0px = 0.397129f (rounded to 0.3971f).
     */
    const val CORE_VISIBLE_RADIUS_UV: Float = 0.3971f

    /** Core radius in OrbLocal diameter units (D) */
    const val CORE_RADIUS_D: Float = 0.5f

    /** Maximum extent of R06 filament envelope in OrbLocal units (D) */
    const val MAX_RADIUS_D: Float = 0.675f

    /** Base diameter of the orb design in DP */
    const val BASE_ORB_DIAMETER_DP: Float = 80.0f

    /**
     * Position scale factor that maps 0.5D in OrbLocal units to match
     * the visible rim of the core glass sphere in NDC clip space.
     *
     * Position in NDC = p * positionScale * (minDim / resolution)
     * positionScale = 4.0 * CORE_VISIBLE_RADIUS_UV * currentScale
     */
    fun computePositionScale(currentScale: Float): Float {
        return 4.0f * CORE_VISIBLE_RADIUS_UV * currentScale
    }

    /**
     * Compute actual rendered diameter of the core glass sphere in screen pixels.
     */
    fun computeOrbDiameterRenderPx(width: Int, height: Int, currentScale: Float): Float {
        val minDim = minOf(width, height).toFloat()
        return 2.0f * CORE_VISIBLE_RADIUS_UV * currentScale * minDim
    }

    /**
     * Compute stroke width in OrbLocal D units to maintain constant DP width on screen,
     * independent of breath oscillation scale.
     *
     * @param profileWidthDp Profile stroke width from R06 geometry / profile (e.g., 2.5dp)
     * @param materialMultiplier Multiplier from material config (e.g., 1.75 for green)
     * @param userMultiplier User tuning multiplier from debug panel
     * @param orbSizeScale Overall orb size scale (0.5 for 40dp, 1.0 for 80dp)
     * @param breathFactor Current breath oscillation factor (approx 1.0 +- 0.018)
     */
    fun computeStrokeWidthD(
        profileWidthDp: Float,
        materialMultiplier: Float,
        userMultiplier: Float,
        orbSizeScale: Float,
        breathFactor: Float = 1.0f
    ): Float {
        val strokeWidthDp = profileWidthDp * materialMultiplier * userMultiplier
        val orbDiameterDp = (orbSizeScale * BASE_ORB_DIAMETER_DP).coerceAtLeast(1.0f)
        // Divide by breathFactor so the physical width does not pulse with sphere breathing
        return strokeWidthDp / (orbDiameterDp * breathFactor)
    }

    /**
     * Map a point in OrbLocal D units to OpenGL NDC coordinates.
     */
    fun orbLocalToNdc(
        point: Point2D,
        currentScale: Float,
        width: Int,
        height: Int
    ): Point2D {
        val minDim = minOf(width, height).toFloat()
        val posScale = computePositionScale(currentScale)
        val ndcX = point.x * posScale * (minDim / width.toFloat().coerceAtLeast(1f))
        // Y-axis is inverted: SVG/OrbLocal y-down -> OpenGL NDC y-up
        val ndcY = -point.y * posScale * (minDim / height.toFloat().coerceAtLeast(1f))
        return Point2D(ndcX, ndcY)
    }

    /**
     * Map a point in OrbLocal D units to screen pixel coordinates (origin at top-left).
     */
    fun orbLocalToScreenPx(
        point: Point2D,
        currentScale: Float,
        width: Int,
        height: Int
    ): Point2D {
        val minDim = minOf(width, height).toFloat()
        val radiusPx = CORE_VISIBLE_RADIUS_UV * currentScale * minDim
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        // 1.0D corresponds to 2 * radiusPx
        val pxX = centerX + point.x * (2.0f * radiusPx)
        val pxY = centerY + point.y * (2.0f * radiusPx)
        return Point2D(pxX, pxY)
    }

    /**
     * Compute distance from center in OrbLocal D units.
     */
    fun computeRadiusD(x: Float, y: Float): Float {
        return sqrt(x * x + y * y)
    }
}
