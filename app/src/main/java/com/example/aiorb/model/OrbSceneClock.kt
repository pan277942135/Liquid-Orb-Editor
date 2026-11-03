package com.example.aiorb.model

import android.os.SystemClock

enum class OrbLifeState {
    S1_IDLE,
    S2_ACTIVE
}

enum class OrbPhaseEvent {
    NONE,
    WAKE,
    RELEASE,
    RETURN
}

/**
 * Immutable per-frame scene snapshot derived from unified monotonic clock.
 * Conforms to contract: one monotonic source, two time projections:
 * - sceneElapsedMs: continuous life (breath, grass, droplets, caustics)
 * - opticalReferenceElapsedMs: stage-local wake, release, filament optical flow
 */
data class OrbSceneFrame(
    val sceneElapsedMs: Long,
    val opticalReferenceElapsedMs: Long,
    val lifeState: OrbLifeState,
    val wakeStartMs: Long?,
    val releaseStartMs: Long?,
    val isPaused: Boolean,
    val wakeProgress: Float,
    val filamentAlphaProgress: Float,
    val releaseProgress: Float,
    val currentPhaseEvent: OrbPhaseEvent
)

/**
 * Unified monotonic scene clock implementation.
 * Guarantees:
 * 1. Single monotonic time source (SystemClock.uptimeMillis())
 * 2. Pause freezes both projections together
 * 3. Resume preserves exact phases
 * 4. Replay Wake does not reconstruct clock
 * 5. Release does not reset Breath phase
 * 6. UI recomposition never resets life animation
 */
class OrbSceneClock {

    private var startRealtimeMs: Long = SystemClock.uptimeMillis()
    private var pausedAtRealtimeMs: Long? = null
    private var accumulatedPausedDurationMs: Long = 0L

    var isPaused: Boolean = false
        private set

    var lifeState: OrbLifeState = OrbLifeState.S1_IDLE
        private set

    var wakeStartMs: Long? = null
        private set

    var releaseStartMs: Long? = null
        private set

    private var stateEntryMs: Long = 0L

    fun start() {
        startRealtimeMs = SystemClock.uptimeMillis()
        pausedAtRealtimeMs = null
        accumulatedPausedDurationMs = 0L
        isPaused = false
        lifeState = OrbLifeState.S1_IDLE
        wakeStartMs = null
        releaseStartMs = null
        stateEntryMs = 0L
    }

    fun pause() {
        if (!isPaused) {
            isPaused = true
            pausedAtRealtimeMs = SystemClock.uptimeMillis()
        }
    }

    fun resume() {
        if (isPaused) {
            pausedAtRealtimeMs?.let { pausedAt ->
                accumulatedPausedDurationMs += (SystemClock.uptimeMillis() - pausedAt)
            }
            pausedAtRealtimeMs = null
            isPaused = false
        }
    }

    fun triggerWake() {
        val currentOptical = getOpticalElapsedMs()
        wakeStartMs = currentOptical
        lifeState = OrbLifeState.S2_ACTIVE
        stateEntryMs = getSceneElapsedMs()
    }

    fun triggerBreath() {
        if (lifeState != OrbLifeState.S2_ACTIVE) {
            lifeState = OrbLifeState.S2_ACTIVE
            stateEntryMs = getSceneElapsedMs()
        }
    }

    fun triggerRelease() {
        if (lifeState != OrbLifeState.S2_ACTIVE) {
            lifeState = OrbLifeState.S2_ACTIVE
            stateEntryMs = getSceneElapsedMs()
        }
        val currentOptical = getOpticalElapsedMs()
        releaseStartMs = currentOptical
    }

    fun reset() {
        start()
    }

    fun setLifeStateDirect(state: OrbLifeState) {
        lifeState = state
        if (state == OrbLifeState.S2_ACTIVE && stateEntryMs == 0L) {
            stateEntryMs = getSceneElapsedMs()
        }
    }

    private fun getSceneElapsedMs(): Long {
        val now = if (isPaused && pausedAtRealtimeMs != null) pausedAtRealtimeMs!! else SystemClock.uptimeMillis()
        return (now - startRealtimeMs - accumulatedPausedDurationMs).coerceAtLeast(0L)
    }

    private fun getOpticalElapsedMs(): Long {
        return getSceneElapsedMs()
    }

    fun produceFrame(): OrbSceneFrame {
        val sceneElapsed = getSceneElapsedMs()
        val opticalElapsed = getOpticalElapsedMs()

        // Compute Wake Progress: 350ms duration, easing cubicBezier(0.16, 1.0, 0.30, 1.0)
        val wakeProg: Float
        val filamentAlpha: Float
        if (wakeStartMs != null) {
            val dt = (opticalElapsed - wakeStartMs!!).coerceAtLeast(0L)
            val linearWake = (dt / 350f).coerceIn(0f, 1f)
            wakeProg = cubicBezier(0.16f, 1.0f, 0.30f, 1.0f, linearWake)
            // S1->S2 filament alpha: 450ms smoothstep concurrent
            val linearAlpha = (dt / 450f).coerceIn(0f, 1f)
            filamentAlpha = smoothStep(0f, 1f, linearAlpha)
        } else if (lifeState == OrbLifeState.S2_ACTIVE) {
            wakeProg = 1f
            filamentAlpha = 1f
        } else {
            wakeProg = 0f
            filamentAlpha = 0f
        }

        // Compute Release Progress: 350ms to 950ms local loop
        val releaseProg: Float
        val phaseEvent: OrbPhaseEvent
        if (releaseStartMs != null) {
            val dt = (opticalElapsed - releaseStartMs!!).coerceAtLeast(0L)
            if (dt < 350L) {
                releaseProg = (dt / 350f).coerceIn(0f, 1f)
                phaseEvent = OrbPhaseEvent.RELEASE
            } else if (dt < 690L) {
                releaseProg = 1f
                phaseEvent = OrbPhaseEvent.RELEASE
            } else if (dt < 950L) {
                val retProg = ((dt - 690L) / 260f).coerceIn(0f, 1f)
                releaseProg = 1f - retProg
                phaseEvent = OrbPhaseEvent.RETURN
            } else {
                releaseProg = 0f
                phaseEvent = OrbPhaseEvent.NONE
            }
        } else if (wakeStartMs != null && (opticalElapsed - wakeStartMs!!) in 0..350) {
            releaseProg = 0f
            phaseEvent = OrbPhaseEvent.WAKE
        } else {
            releaseProg = 0f
            phaseEvent = OrbPhaseEvent.NONE
        }

        return OrbSceneFrame(
            sceneElapsedMs = sceneElapsed,
            opticalReferenceElapsedMs = opticalElapsed,
            lifeState = lifeState,
            wakeStartMs = wakeStartMs,
            releaseStartMs = releaseStartMs,
            isPaused = isPaused,
            wakeProgress = wakeProg,
            filamentAlphaProgress = filamentAlpha,
            releaseProgress = releaseProg,
            currentPhaseEvent = phaseEvent
        )
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * Approximate cubic Bezier curve evaluation y(x) with P0=(0,0), P1=(x1,y1), P2=(x2,y2), P3=(1,1)
     */
    private fun cubicBezier(x1: Float, y1: Float, x2: Float, y2: Float, t: Float): Float {
        // Simple bisection or direct polynomial
        var low = 0f
        var high = 1f
        var mid = t
        for (i in 0..7) {
            mid = (low + high) * 0.5f
            val x = 3f * (1f - mid) * (1f - mid) * mid * x1 + 3f * (1f - mid) * mid * mid * x2 + mid * mid * mid
            if (x < t) {
                low = mid
            } else {
                high = mid
            }
        }
        val y = 3f * (1f - mid) * (1f - mid) * mid * y1 + 3f * (1f - mid) * mid * mid * y2 + mid * mid * mid
        return y.coerceIn(0f, 1f)
    }
}
