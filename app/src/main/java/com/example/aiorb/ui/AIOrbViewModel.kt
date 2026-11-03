package com.example.aiorb.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.aiorb.gl.AIOrbRenderer
import com.example.aiorb.model.OrbLifeState
import com.example.aiorb.model.OrbPhaseEvent
import com.example.aiorb.model.OrbSceneFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class AIOrbLabMode {
    RELEASE_PREVIEW,
    FULL_LIFECYCLE
}

data class AIOrbUiState(
    val labMode: AIOrbLabMode = AIOrbLabMode.RELEASE_PREVIEW,
    val lifeState: OrbLifeState = OrbLifeState.S2_ACTIVE,
    val currentPhaseEvent: OrbPhaseEvent = OrbPhaseEvent.RELEASE,
    val isPaused: Boolean = false,
    val isPlaying: Boolean = true,
    val orbSizeScale: Float = 0.5f, // Default 0.5f (half of previous 80dp = 40dp)
    val is80dp: Boolean = false, // Backwards compatibility

    // Current metrics
    val sceneElapsedMs: Long = 0L,
    val opticalReferenceElapsedMs: Long = 0L,
    val wakeProgress: Float = 1f,
    val filamentAlphaProgress: Float = 1f,
    val releaseProgress: Float = 0f,

    // RELEASE Loop metrics
    val releaseLoopEnabled: Boolean = true,
    val releaseCycleIndex: Int = 0,
    val releaseCycleElapsedMs: Long = 0L,
    val isHold: Boolean = false,

    // Tuning Parameters
    val sunlightIntensity: Float = 1.0f,
    val refractionIntensity: Float = 1.0f,
    val causticsIntensity: Float = 1.0f,
    val grassMotion: Float = 1.0f,
    val dropletVisibility: Float = 1.0f,
    val filamentAlpha: Float = 1.0f,
    val filamentWidth: Float = 1.0f,

    // UI Panel state
    val isBottomPanelExpanded: Boolean = true,
    val showTuningPanel: Boolean = true,
    val showCalibrationDialog: Boolean = false,
    val exportedJsonText: String? = null
)

class AIOrbViewModel(application: Application) : AndroidViewModel(application) {

    val renderer = AIOrbRenderer(application)
    private val clock get() = renderer.sceneClock

    init {
        renderer.orbSizeScale = 0.5f
        // Start directly in RELEASE PREVIEW loop mode by default
        clock.startReleasePreview(loop = true)
    }

    private val _uiState = MutableStateFlow(AIOrbUiState())
    val uiState: StateFlow<AIOrbUiState> = _uiState.asStateFlow()

    fun updateFromFrame(frame: OrbSceneFrame) {
        _uiState.value = _uiState.value.copy(
            lifeState = frame.lifeState,
            currentPhaseEvent = frame.currentPhaseEvent,
            isPaused = frame.isPaused,
            sceneElapsedMs = frame.sceneElapsedMs,
            opticalReferenceElapsedMs = frame.opticalReferenceElapsedMs,
            wakeProgress = frame.wakeProgress,
            filamentAlphaProgress = frame.filamentAlphaProgress,
            releaseProgress = frame.releaseProgress,
            releaseCycleIndex = frame.releaseCycleIndex,
            releaseCycleElapsedMs = frame.releaseCycleElapsedMs,
            releaseLoopEnabled = frame.releaseLoopEnabled,
            isHold = frame.isHold
        )
    }

    // === Mode Switching ===

    fun setLabMode(mode: AIOrbLabMode) {
        if (_uiState.value.labMode == mode) return
        _uiState.value = _uiState.value.copy(labMode = mode)
        if (mode == AIOrbLabMode.RELEASE_PREVIEW) {
            clock.startReleasePreview(loop = _uiState.value.releaseLoopEnabled)
        } else {
            clock.setReleasePreviewMode(false)
            clock.reset()
            clock.triggerWake()
        }
    }

    fun toggleReleaseLoop() {
        val nextLoop = !_uiState.value.releaseLoopEnabled
        clock.setReleaseLoop(nextLoop)
        _uiState.value = _uiState.value.copy(releaseLoopEnabled = nextLoop)
    }

    // === Core Visual Actions ===

    fun triggerWake() {
        _uiState.value = _uiState.value.copy(labMode = AIOrbLabMode.FULL_LIFECYCLE, isPlaying = true)
        clock.triggerWake()
    }

    fun triggerBreath() {
        _uiState.value = _uiState.value.copy(isPlaying = true)
        clock.triggerBreath()
    }

    fun triggerRelease() {
        _uiState.value = _uiState.value.copy(isPlaying = true)
        clock.triggerRelease()
    }

    // === Playback Controls ===

    fun togglePlayPause() {
        if (clock.isPaused) {
            clock.resume()
            _uiState.value = _uiState.value.copy(isPaused = false, isPlaying = true)
        } else {
            clock.pause()
            _uiState.value = _uiState.value.copy(isPaused = true, isPlaying = false)
        }
    }

    fun replay() {
        if (_uiState.value.labMode == AIOrbLabMode.RELEASE_PREVIEW) {
            // Replay release without executing wake
            clock.replayRelease()
            _uiState.value = _uiState.value.copy(isPlaying = true, isPaused = false)
        } else {
            // Full lifecycle replay wake
            clock.reset()
            clock.triggerWake()
            _uiState.value = _uiState.value.copy(isPlaying = true, isPaused = false)
        }
    }

    fun reset() {
        if (_uiState.value.labMode == AIOrbLabMode.RELEASE_PREVIEW) {
            clock.replayRelease()
            _uiState.value = _uiState.value.copy(isPlaying = true, isPaused = false)
        } else {
            clock.reset()
            _uiState.value = _uiState.value.copy(
                isPlaying = true,
                isPaused = false,
                lifeState = OrbLifeState.S1_IDLE,
                currentPhaseEvent = OrbPhaseEvent.NONE
            )
        }
    }

    fun updateOrbSizeScale(scale: Float) {
        val clamped = scale.coerceIn(0.2f, 2.0f)
        renderer.orbSizeScale = clamped
        _uiState.value = _uiState.value.copy(
            orbSizeScale = clamped,
            is80dp = (clamped >= 0.95f)
        )
    }

    fun setSizePreset(scale: Float) {
        updateOrbSizeScale(scale)
    }

    fun toggleSize() {
        val cur = _uiState.value.orbSizeScale
        val next = when {
            cur < 0.65f -> 0.85f // to 68dp
            cur < 0.95f -> 1.0f  // to 80dp
            cur < 1.15f -> 1.25f // to 100dp
            else -> 0.5f         // to default 40dp (half size)
        }
        updateOrbSizeScale(next)
    }

    fun toggleBottomPanel() {
        _uiState.value = _uiState.value.copy(isBottomPanelExpanded = !_uiState.value.isBottomPanelExpanded)
    }

    fun setBottomPanelExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isBottomPanelExpanded = expanded)
    }

    // === Parameter Tuning Sliders ===

    fun updateSunlightIntensity(v: Float) {
        renderer.sunlightIntensityMultiplier = v
        _uiState.value = _uiState.value.copy(sunlightIntensity = v)
    }

    fun updateRefractionIntensity(v: Float) {
        renderer.refractionIntensityMultiplier = v
        _uiState.value = _uiState.value.copy(refractionIntensity = v)
    }

    fun updateCausticsIntensity(v: Float) {
        renderer.causticsIntensityMultiplier = v
        _uiState.value = _uiState.value.copy(causticsIntensity = v)
    }

    fun updateGrassMotion(v: Float) {
        renderer.grassMotionMultiplier = v
        _uiState.value = _uiState.value.copy(grassMotion = v)
    }

    fun updateDropletVisibility(v: Float) {
        renderer.dropletVisibilityMultiplier = v
        _uiState.value = _uiState.value.copy(dropletVisibility = v)
    }

    fun updateFilamentAlpha(v: Float) {
        renderer.filamentAlphaMultiplier = v
        _uiState.value = _uiState.value.copy(filamentAlpha = v)
    }

    fun updateFilamentWidth(v: Float) {
        renderer.filamentWidthMultiplier = v
        _uiState.value = _uiState.value.copy(filamentWidth = v)
    }

    fun toggleTuningPanel() {
        _uiState.value = _uiState.value.copy(showTuningPanel = !_uiState.value.showTuningPanel)
    }

    fun resetToFrozenDefaults() {
        updateSunlightIntensity(1.0f)
        updateRefractionIntensity(1.0f)
        updateCausticsIntensity(1.0f)
        updateGrassMotion(1.0f)
        updateDropletVisibility(1.0f)
        updateFilamentAlpha(1.0f)
        updateFilamentWidth(1.0f)
        updateOrbSizeScale(0.5f) // Default is half size (40dp)
    }

    fun exportCalibrationJson() {
        val s = _uiState.value
        val json = JSONObject().apply {
            put("contractId", "YUJIAN_AI_ORB_WAKE_BREATH_RELEASE_VISUAL_PROFILE_V1_1")
            put("status", "CALIBRATION_CANDIDATE")
            put("labMode", s.labMode.name)
            put("releaseLoopEnabled", s.releaseLoopEnabled)
            put("releaseCycleIndex", s.releaseCycleIndex)
            put("sunlightIntensityMultiplier", s.sunlightIntensity)
            put("refractionIntensityMultiplier", s.refractionIntensity)
            put("causticsIntensityMultiplier", s.causticsIntensity)
            put("grassMotionMultiplier", s.grassMotion)
            put("dropletVisibilityMultiplier", s.dropletVisibility)
            put("filamentAlphaMultiplier", s.filamentAlpha)
            put("filamentWidthMultiplier", s.filamentWidth)
            put("orbSizeScale", s.orbSizeScale)
            put("orbDiameterDp", s.orbSizeScale * 80.0)
            put("r06FilamentFirstPassGain", 1.28)
            put("r06FilamentAlphaCap", 0.82)
            put("r06SunGlintGain", 1.15)
            put("r06ReleaseHeadGain", 1.18)
            put("r06ReturnHeadGain", 1.07)
        }
        _uiState.value = _uiState.value.copy(
            showCalibrationDialog = true,
            exportedJsonText = json.toString(2)
        )
    }

    fun dismissCalibrationDialog() {
        _uiState.value = _uiState.value.copy(
            showCalibrationDialog = false,
            exportedJsonText = null
        )
    }
}
