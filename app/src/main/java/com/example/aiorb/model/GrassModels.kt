package com.example.aiorb.model

import android.content.Context
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.sin

data class GrassLeafPath(
    val id: String,
    val depth: FilamentDepth,
    val periodMs: Long,
    val phaseDeg: Float,
    val amplitudeD: Float,
    val rootMaxD: Float,
    val rootUV: Point2D,
    val tipUV: Point2D,
    val curvePointsUV: List<Point2D>,
    val maskFileName: String
) {
    /**
     * Compute current tip displacement vector (dx, dy) in UV space
     * Normal to the centerline direction from root to tip.
     */
    fun computeDisplacementUV(sceneElapsedMs: Long): Point2D {
        val rad = (phaseDeg * PI / 180.0 + 2.0 * PI * sceneElapsedMs.toDouble() / periodMs).toFloat()
        val sineVal = sin(rad)
        val bendMagnitude = amplitudeD * sineVal

        // Direction normal to chord (root -> tip)
        val dx = tipUV.x - rootUV.x
        val dy = tipUV.y - rootUV.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
        // Normal vector (-dy / len, dx / len)
        val nx = -dy / len
        val ny = dx / len

        return Point2D(nx * bendMagnitude, ny * bendMagnitude)
    }

    companion object {
        fun loadFromAssets(context: Context): Map<String, GrassLeafPath> {
            val leaves = mutableMapOf<String, GrassLeafPath>()
            try {
                val jsonStr = context.assets.open("ai_orb/frozen_v1_1/ORB_GRASS_DERIVED_MASK_PATH_V1_1.json")
                    .bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val leavesObj = root.getJSONObject("leaves")

                val ids = listOf("BACK_B1", "BACK_B2", "FRONT_F1", "FRONT_F2")
                for (id in ids) {
                    if (leavesObj.has(id)) {
                        val leaf = leavesObj.getJSONObject(id)
                        val depthStr = leaf.getString("depth")
                        val depth = if (depthStr.equals("BACK", ignoreCase = true)) FilamentDepth.BACK else FilamentDepth.FRONT
                        val period = leaf.getLong("periodMs")
                        val phase = leaf.getDouble("phaseDeg").toFloat()
                        val amp = leaf.getDouble("amplitudeD").toFloat()
                        val rootMax = leaf.getDouble("rootMaxD").toFloat()

                        val rArr = leaf.getJSONArray("rootUV")
                        val rootPt = Point2D(rArr.getDouble(0).toFloat(), rArr.getDouble(1).toFloat())

                        val tArr = leaf.getJSONArray("tipUV")
                        val tipPt = Point2D(tArr.getDouble(0).toFloat(), tArr.getDouble(1).toFloat())

                        val cArr = leaf.getJSONArray("curveUV")
                        val pts = mutableListOf<Point2D>()
                        for (i in 0 until cArr.length()) {
                            val ptArr = cArr.getJSONArray(i)
                            pts.add(Point2D(ptArr.getDouble(0).toFloat(), ptArr.getDouble(1).toFloat()))
                        }
                        val maskFile = leaf.getString("maskFile")

                        leaves[id] = GrassLeafPath(
                            id = id,
                            depth = depth,
                            periodMs = period,
                            phaseDeg = phase,
                            amplitudeD = amp,
                            rootMaxD = rootMax,
                            rootUV = rootPt,
                            tipUV = tipPt,
                            curvePointsUV = pts,
                            maskFileName = maskFile
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return leaves
        }
    }
}
