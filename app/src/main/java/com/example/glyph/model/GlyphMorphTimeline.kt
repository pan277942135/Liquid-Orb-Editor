package com.example.glyph.model

import kotlin.math.sin

/**
 * Playback mode of the morphing timeline
 */
enum class TimelinePlayMode {
    AUTO,        // Runs automatically from 0 to 2400ms, then holds at KF06 or loops
    MANUAL,      // Strictly frozen at the selected keyframe checkpoint
    SCRUB,       // Scrubbed continuously via slider (0.0 to 1.0)
    LOOP         // Repeated cyclic animation
}

/**
 * Continuous optical and morphing state at any instant in time.
 */
data class GlyphTimelineState(
    val elapsedMs: Long,
    val progress: Float,                     // 0.0 to 1.0
    val activeKeyframe: GlyphKeyframe,       // Nearest or pinned checkpoint
    val playMode: TimelinePlayMode,
    
    // Core geometry stretch & deformation
    val coreStretchX: Float,                 // 1.0 -> 1.58 -> 1.25
    val coreStretchY: Float,                 // 1.0 -> 0.85 -> 0.95
    val coreWarpDistortion: Float,           // Nonlinear FBM warp intensity (0.2 -> 1.4 -> 0.4)
    val coreAlpha: Float,                    // Sphere core presence
    
    // Water ribbon release parameters
    val ribbonReleaseProgress: Float,        // 0.0 -> 1.0 (release outward)
    val ribbonIntensity: Float,              // Brightness & visibility of Bezier flow ribbons
    val ribbonDispersion: Float,             // Color separation along ribbons
    
    // Chinese glyph crystallization parameters
    val glyphAlpha: Float,                   // 0.0 -> 0.4 -> 0.85 -> 1.0
    val glyphFluidErosion: Float,            // Fluid disturbance on strokes (1.0 -> 0.0)
    val glyphRefraction: Float,              // Crystal index of refraction on stroke normals
    val glyphGloss: Float,                   // Specular highlight gloss
    val inkOpacity: Float,                   // Deep lake ink density inside strokes
    val goldFilamentAlpha: Float,            // Golden light threads on calligraphy contours
    val ambientBreathScale: Float            // Subtle breathing modulation
)

/**
 * Pure evaluation engine for the glyph morphing timeline.
 */
object GlyphMorphTimeline {

    const val TOTAL_DURATION_MS = 2400L

    /**
     * Smooth Hermite interpolation (smoothstep)
     */
    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3.0f - 2.0f * t)
    }

    /**
     * Linear interpolation
     */
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction.coerceIn(0f, 1f)
    }

    /**
     * Evaluates the timeline at any given time.
     */
    fun evaluate(
        elapsedMs: Long,
        totalDurationMs: Long = TOTAL_DURATION_MS,
        mode: TimelinePlayMode = TimelinePlayMode.AUTO,
        manualKeyframe: GlyphKeyframe? = null,
        scrubProgress: Float = 0f
    ): GlyphTimelineState {
        val effectiveProgress: Float
        val effectiveElapsedMs: Long

        when (mode) {
            TimelinePlayMode.MANUAL -> {
                val kf = manualKeyframe ?: GlyphKeyframe.KF01_ORB_SPHERE
                effectiveProgress = kf.normalizedProgress
                effectiveElapsedMs = kf.targetTimeMs
            }
            TimelinePlayMode.SCRUB -> {
                effectiveProgress = scrubProgress.coerceIn(0f, 1f)
                effectiveElapsedMs = (effectiveProgress * totalDurationMs).toLong()
            }
            TimelinePlayMode.LOOP -> {
                val cycleTime = if (totalDurationMs > 0) elapsedMs % totalDurationMs else 0L
                effectiveProgress = cycleTime.toFloat() / totalDurationMs.toFloat()
                effectiveElapsedMs = cycleTime
            }
            TimelinePlayMode.AUTO -> {
                effectiveElapsedMs = elapsedMs.coerceIn(0L, totalDurationMs)
                effectiveProgress = (effectiveElapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            }
        }

        val p = effectiveProgress
        val nearestKeyframe = manualKeyframe ?: GlyphKeyframe.fromProgress(p)

        // 1. Core Stretch: 1.0 at 0.0 -> peaks at 1.58 around p=0.20~0.35 -> settles to 1.25 at p=1.0
        val stretchX: Float = when {
            p < 0.20f -> {
                val t = smoothstep(0.00f, 0.20f, p)
                lerp(1.00f, 1.58f, t)
            }
            p < 0.50f -> {
                val t = smoothstep(0.20f, 0.50f, p)
                lerp(1.58f, 1.42f, t)
            }
            else -> {
                val t = smoothstep(0.50f, 1.00f, p)
                lerp(1.42f, 1.28f, t)
            }
        }

        val stretchY: Float = when {
            p < 0.20f -> {
                val t = smoothstep(0.00f, 0.20f, p)
                lerp(1.00f, 0.84f, t)
            }
            p < 0.50f -> {
                val t = smoothstep(0.20f, 0.50f, p)
                lerp(0.84f, 0.90f, t)
            }
            else -> {
                val t = smoothstep(0.50f, 1.00f, p)
                lerp(0.90f, 0.95f, t)
            }
        }

        // 2. Nonlinear Warp Distortion: peaks in KF02/KF03
        val coreWarp: Float = when {
            p < 0.25f -> {
                val t = smoothstep(0.0f, 0.25f, p)
                lerp(0.35f, 1.50f, t)
            }
            p < 0.60f -> {
                val t = smoothstep(0.25f, 0.60f, p)
                lerp(1.50f, 0.85f, t)
            }
            else -> {
                val t = smoothstep(0.60f, 1.00f, p)
                lerp(0.85f, 0.42f, t)
            }
        }

        // 3. Water Ribbon Release (emerges from KF02 to KF04, peaks around KF03)
        val ribbonProg = smoothstep(0.18f, 0.42f, p)
        val ribbonIntensity: Float = when {
            p < 0.20f -> 0.0f
            p < 0.40f -> smoothstep(0.20f, 0.40f, p) * 1.0f
            p < 0.70f -> lerp(1.0f, 0.55f, smoothstep(0.40f, 0.70f, p))
            else -> lerp(0.55f, 0.25f, smoothstep(0.70f, 1.00f, p))
        }
        val ribbonDispersion = lerp(0.2f, 0.85f, smoothstep(0.25f, 0.60f, p))

        // 4. Glyph Appearance & Crystallization
        // KF01-KF03 (p < 0.40): glyphAlpha = 0
        // KF04 (p ~ 0.60): glyph initial condensation (alpha ~ 0.45, high erosion)
        // KF05 (p ~ 0.80): crystallization (alpha ~ 0.85, high refraction & gloss)
        // KF06 (p = 1.00): settled stable deep lake ink (alpha = 1.0, gold filaments)
        val glyphAlpha: Float = when {
            p < 0.38f -> 0.0f
            p < 0.60f -> smoothstep(0.38f, 0.60f, p) * 0.48f
            p < 0.80f -> lerp(0.48f, 0.88f, smoothstep(0.60f, 0.80f, p))
            else -> lerp(0.88f, 1.00f, smoothstep(0.80f, 1.00f, p))
        }

        val fluidErosion: Float = when {
            p < 0.40f -> 1.0f
            p < 0.75f -> lerp(1.0f, 0.25f, smoothstep(0.40f, 0.75f, p))
            else -> lerp(0.25f, 0.00f, smoothstep(0.75f, 1.00f, p))
        }

        val refraction: Float = when {
            p < 0.45f -> 1.0f
            p < 0.80f -> lerp(1.0f, 1.85f, smoothstep(0.45f, 0.80f, p))
            else -> lerp(1.85f, 1.55f, smoothstep(0.80f, 1.00f, p))
        }

        val gloss: Float = when {
            p < 0.45f -> 2.0f
            p < 0.80f -> lerp(2.0f, 5.2f, smoothstep(0.45f, 0.80f, p))
            else -> lerp(5.2f, 4.4f, smoothstep(0.80f, 1.00f, p))
        }

        val inkDensity: Float = when {
            p < 0.40f -> 0.0f
            p < 0.65f -> smoothstep(0.40f, 0.65f, p) * 0.55f
            p < 0.85f -> lerp(0.55f, 0.85f, smoothstep(0.65f, 0.85f, p))
            else -> lerp(0.85f, 0.94f, smoothstep(0.85f, 1.00f, p))
        }

        val goldFilament: Float = when {
            p < 0.50f -> 0.0f
            p < 0.80f -> smoothstep(0.50f, 0.80f, p) * 0.85f
            else -> lerp(0.85f, 0.65f, smoothstep(0.80f, 1.00f, p))
        }

        // Subtle ambient breath scale for life presence
        val breathTime = (effectiveElapsedMs % 4800L).toFloat() / 4800f
        val breathScale = 1.0f + 0.035f * sin(breathTime * 6.2831853f)

        return GlyphTimelineState(
            elapsedMs = effectiveElapsedMs,
            progress = p,
            activeKeyframe = nearestKeyframe,
            playMode = mode,
            coreStretchX = stretchX,
            coreStretchY = stretchY,
            coreWarpDistortion = coreWarp,
            coreAlpha = 1.0f,
            ribbonReleaseProgress = ribbonProg,
            ribbonIntensity = ribbonIntensity,
            ribbonDispersion = ribbonDispersion,
            glyphAlpha = glyphAlpha,
            glyphFluidErosion = fluidErosion,
            glyphRefraction = refraction,
            glyphGloss = gloss,
            inkOpacity = inkDensity,
            goldFilamentAlpha = goldFilament,
            ambientBreathScale = breathScale
        )
    }
}
