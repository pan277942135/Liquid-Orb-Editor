package com.example.aiorb.model

import android.content.Context
import android.graphics.Color
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class FilamentDepth {
    BACK,
    FRONT
}

enum class FilamentMaterialType(
    val baseColor: Int,
    val highlightColor: Int,
    val refractionColor: Int,
    val widthMultiplier: Float,
    val tailLengthRatio: Float
) {
    GREEN_RIBBON(
        baseColor = Color.parseColor("#5B8F78"),
        highlightColor = Color.parseColor("#A9D9C5"),
        refractionColor = Color.parseColor("#326F67"),
        widthMultiplier = 1.75f,
        tailLengthRatio = 0.23f
    ),
    WARM_GOLD_FILAMENT(
        baseColor = Color.parseColor("#D8BC87"),
        highlightColor = Color.parseColor("#FFE6B0"),
        refractionColor = Color.parseColor("#8D754B"),
        widthMultiplier = 1.0f,
        tailLengthRatio = 0.15f
    ),
    CYAN_WATER_RIBBON(
        baseColor = Color.parseColor("#348CA6"),
        highlightColor = Color.parseColor("#79D7E4"),
        refractionColor = Color.parseColor("#B4F1E8"),
        widthMultiplier = 1.5f,
        tailLengthRatio = 0.20f
    )
}

data class Point2D(val x: Float, val y: Float) {
    fun distanceTo(other: Point2D): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}

data class BezierSegment(
    val c1: Point2D,
    val c2: Point2D,
    val end: Point2D
)

/**
 * Single R06 filament path backed by actual geometry from R06_orb_filament_path_geometry.json
 */
class FilamentPath(
    val id: String,
    val materialType: FilamentMaterialType,
    val depth: FilamentDepth,
    val start: Point2D,
    val segments: List<BezierSegment>,
    val periodMs: Long,
    val phase: Float,
    val widthMinDp: Float,
    val widthMaxDp: Float,
    val alphaMin: Float,
    val alphaMax: Float
) {
    // 128-sample arc-length LUT computed once at load time
    val lutSamples = 128
    val lutPoints = ArrayList<Point2D>(lutSamples)
    val lutCumulativeLengths = FloatArray(lutSamples)
    val totalArcLength: Float

    init {
        // Pre-compute 128 equidistant samples along the 2-segment cubic Bezier
        var accum = 0f
        var prevPt = start
        lutPoints.add(start)
        lutCumulativeLengths[0] = 0f

        val totalSubdivisions = lutSamples - 1
        for (i in 1..totalSubdivisions) {
            val globalT = i.toFloat() / totalSubdivisions
            val pt = evaluateCubic(globalT)
            accum += prevPt.distanceTo(pt)
            lutPoints.add(pt)
            lutCumulativeLengths[i] = accum
            prevPt = pt
        }
        totalArcLength = accum
    }

    private fun evaluateCubic(globalT: Float): Point2D {
        val numSegments = segments.size
        if (numSegments == 1) {
            return evalBezier(start, segments[0].c1, segments[0].c2, segments[0].end, globalT)
        }
        // Segment 1 or 2
        val segT = (globalT * numSegments).coerceIn(0f, numSegments.toFloat() - 0.0001f)
        val segIdx = segT.toInt()
        val localT = segT - segIdx
        val p0 = if (segIdx == 0) start else segments[segIdx - 1].end
        val seg = segments[segIdx]
        return evalBezier(p0, seg.c1, seg.c2, seg.end, localT)
    }

    private fun evalBezier(p0: Point2D, p1: Point2D, p2: Point2D, p3: Point2D, t: Float): Point2D {
        val u = 1f - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        val x = uuu * p0.x + 3f * uu * t * p1.x + 3f * u * tt * p2.x + ttt * p3.x
        val y = uuu * p0.y + 3f * uu * t * p1.y + 3f * u * tt * p2.y + ttt * p3.y
        return Point2D(x, y)
    }

    /**
     * Get position along the arc-length (0.0 to 1.0)
     */
    fun getPointAtArcFraction(s: Float): Point2D {
        val clampedS = s.coerceIn(0f, 1f)
        val targetDist = clampedS * totalArcLength

        // Binary search or linear search on the 128-sample LUT
        var idx = 0
        while (idx < lutSamples - 1 && lutCumulativeLengths[idx + 1] < targetDist) {
            idx++
        }
        if (idx >= lutSamples - 1) return lutPoints.last()

        val l0 = lutCumulativeLengths[idx]
        val l1 = lutCumulativeLengths[idx + 1]
        val ratio = if (l1 > l0) (targetDist - l0) / (l1 - l0) else 0f
        val p0 = lutPoints[idx]
        val p1 = lutPoints[idx + 1]
        return Point2D(
            p0.x + (p1.x - p0.x) * ratio,
            p0.y + (p1.y - p0.y) * ratio
        )
    }

    /**
     * Compute current head and tail arc fractions according to frozen motion formula:
     * p = fract(phase + elapsedMs / periodMs)
     * headDistance = 0.5 - 0.5 * cos(2π * p)
     */
    fun computeHeadAndTail(elapsedMs: Long, releaseProgress: Float, isReturn: Boolean): Pair<Float, Float> {
        val p = ((phase + elapsedMs.toDouble() / periodMs) % 1.0).toFloat()
        var headDistance = 0.5f - 0.5f * cos(2f * PI.toFloat() * p)

        if (releaseProgress > 0f) {
            // Outward extension multiplier during RELEASE, returning during RETURN
            val ext = if (!isReturn) releaseProgress * 0.25f else (1f - releaseProgress) * 0.25f
            headDistance = (headDistance * (1f + ext)).coerceIn(0f, 1f)
        }

        val tailDist = (headDistance - materialType.tailLengthRatio).coerceAtLeast(0f)
        return Pair(headDistance, tailDist)
    }

    companion object {
        fun loadAllFromAssets(context: Context): List<FilamentPath> {
            val paths = mutableListOf<FilamentPath>()
            try {
                // Try supplement path first, then fallback to packaged assets
                val filename = "ai_orb/r06_v1/assets/paths/R06_orb_filament_path_geometry.json"
                val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val jsonPaths = root.getJSONArray("paths")

                for (i in 0 until jsonPaths.length()) {
                    val pObj = jsonPaths.getJSONObject(i)
                    val id = pObj.getString("id")
                    val typeStr = pObj.getString("type")
                    val depthStr = pObj.getString("depth")

                    val mat = when (typeStr.lowercase()) {
                        "green" -> FilamentMaterialType.GREEN_RIBBON
                        "gold" -> FilamentMaterialType.WARM_GOLD_FILAMENT
                        else -> FilamentMaterialType.CYAN_WATER_RIBBON
                    }
                    val depth = if (depthStr.equals("BACK", ignoreCase = true)) FilamentDepth.BACK else FilamentDepth.FRONT

                    val sArr = pObj.getJSONArray("start")
                    val startPt = Point2D(sArr.getDouble(0).toFloat(), sArr.getDouble(1).toFloat())

                    val segArr = pObj.getJSONArray("segments")
                    val segmentsList = mutableListOf<BezierSegment>()
                    for (j in 0 until segArr.length()) {
                        val sObj = segArr.getJSONObject(j)
                        val c1Arr = sObj.getJSONArray("c1")
                        val c2Arr = sObj.getJSONArray("c2")
                        val eArr = sObj.getJSONArray("end")
                        segmentsList.add(
                            BezierSegment(
                                c1 = Point2D(c1Arr.getDouble(0).toFloat(), c1Arr.getDouble(1).toFloat()),
                                c2 = Point2D(c2Arr.getDouble(0).toFloat(), c2Arr.getDouble(1).toFloat()),
                                end = Point2D(eArr.getDouble(0).toFloat(), eArr.getDouble(1).toFloat())
                            )
                        )
                    }

                    val period = pObj.optLong("period_ms", 4800L)
                    val ph = pObj.optDouble("phase", 0.0).toFloat()

                    val wArr = pObj.getJSONArray("width_dp_min_max")
                    val wMin = wArr.getDouble(0).toFloat()
                    val wMax = wArr.getDouble(1).toFloat()

                    val aArr = pObj.getJSONArray("alpha_min_max")
                    val aMin = aArr.getDouble(0).toFloat()
                    val aMax = aArr.getDouble(1).toFloat()

                    paths.add(
                        FilamentPath(
                            id = id,
                            materialType = mat,
                            depth = depth,
                            start = startPt,
                            segments = segmentsList,
                            periodMs = period,
                            phase = ph,
                            widthMinDp = wMin,
                            widthMaxDp = wMax,
                            alphaMin = aMin,
                            alphaMax = aMax
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return paths
        }
    }
}
