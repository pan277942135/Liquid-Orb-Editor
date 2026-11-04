package com.example.aiorb.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.aiorb.gl.AIOrbRenderer
import com.example.aiorb.gl.OrbCoordinateMapper
import com.example.aiorb.model.*
import com.example.aiorb.ribbon.DualRimRibbonConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.sin

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

    // Dual-Rim Specular Ribbons (水草与流体 3D 实体带状双侧高光)
    val useDualRimRibbons: Boolean = true,
    val ribbonConfig: DualRimRibbonConfig = DualRimRibbonConfig(),

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

    // R06 Diagnostics & Inspection
    val diagnosticMode: R06DiagnosticMode = R06DiagnosticMode.RUNTIME,
    val enabledPathIds: Set<String> = setOf("G01", "G02", "A01", "A02", "C01"),
    val isLowPowerMode: Boolean = false,
    val coreVisibleRadiusUv: Float = OrbCoordinateMapper.CORE_VISIBLE_RADIUS_UV,
    val orbDiameterRenderPx: Float = 0f,
    val coreShaderCompiled: Boolean = true,
    val filamentShaderCompiled: Boolean = true,

    // Tuning Parameters
    val sunlightIntensity: Float = 1.0f,
    val refractionIntensity: Float = 1.0f,
    val causticsIntensity: Float = 1.0f,
    val grassMotion: Float = 1.0f,
    val dropletVisibility: Float = 1.0f,
    val filamentAlpha: Float = 1.0f,
    val filamentWidth: Float = 1.0f,

    // 3D Sphere Self-Rotation & HDR Glow Optics
    val rotationSpeed: Float = 0.45f,
    val isAutoRotate: Boolean = true,
    val glowIntensity: Float = 1.8f,

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
        renderer.enableGlFilaments = false // Use Compose Dual-Rim Ribbons by default for high fidelity
        // Start directly in RELEASE PREVIEW loop mode by default
        clock.startReleasePreview(loop = true)
    }

    fun toggleRibbonPipeline(useDualRim: Boolean) {
        renderer.enableGlFilaments = !useDualRim
        _uiState.value = _uiState.value.copy(useDualRimRibbons = useDualRim)
    }

    fun updateRibbonConfig(newConfig: DualRimRibbonConfig) {
        _uiState.value = _uiState.value.copy(ribbonConfig = newConfig)
    }

    fun updateSeagrassWidth(v: Float) {
        updateRibbonConfig(_uiState.value.ribbonConfig.copy(seagrassRibbonWidth = v))
    }

    fun updateSeagrassWave(v: Float) {
        updateRibbonConfig(_uiState.value.ribbonConfig.copy(seagrassWaveAmount = v))
    }

    fun updateFilamentTwist(v: Float) {
        updateRibbonConfig(_uiState.value.ribbonConfig.copy(filamentTwist = v))
    }

    fun updateFilamentRadiusRatio(v: Float) {
        updateRibbonConfig(_uiState.value.ribbonConfig.copy(filamentRadiusRatio = v))
    }

    fun updateFilamentThickness(v: Float) {
        updateRibbonConfig(_uiState.value.ribbonConfig.copy(filamentThickness = v))
    }

    fun updateFilamentOrbitSpeed(v: Float) {
        updateRibbonConfig(_uiState.value.ribbonConfig.copy(filamentOrbitSpeed = v))
    }

    fun updateFilamentCount(count: Int) {
        updateRibbonConfig(_uiState.value.ribbonConfig.copy(filamentCount = count))
    }

    private val _uiState = MutableStateFlow(AIOrbUiState())
    val uiState: StateFlow<AIOrbUiState> = _uiState.asStateFlow()

    fun updateFromFrame(frame: OrbSceneFrame) {
        val breathAmp = if (frame.lifeState == OrbLifeState.S2_ACTIVE) 0.018f else 0.008f
        val breathPeriod = if (frame.lifeState == OrbLifeState.S2_ACTIVE) 4800.0 else 6000.0
        val breathPhase = 2.0 * PI * (frame.sceneElapsedMs.toDouble() / breathPeriod)
        val currentScale = (1.0f + breathAmp * sin(breathPhase).toFloat()) * renderer.orbSizeScale
        val renderPx = OrbCoordinateMapper.computeOrbDiameterRenderPx(
            renderer.viewWidth,
            renderer.viewHeight,
            currentScale
        )

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
            isHold = frame.isHold,
            orbDiameterRenderPx = renderPx,
            coreShaderCompiled = renderer.coreShaderCompiled,
            filamentShaderCompiled = renderer.filamentShaderCompiled
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

    // === R06 Diagnostic Controls ===

    fun setDiagnosticMode(mode: R06DiagnosticMode) {
        renderer.diagnosticMode = mode
        _uiState.value = _uiState.value.copy(diagnosticMode = mode)
    }

    fun togglePath(pathId: String) {
        val currentSet = _uiState.value.enabledPathIds.toMutableSet()
        if (currentSet.contains(pathId)) {
            if (currentSet.size > 1) { // Keep at least one enabled
                currentSet.remove(pathId)
            }
        } else {
            currentSet.add(pathId)
        }
        renderer.enabledPathIds.clear()
        renderer.enabledPathIds.addAll(currentSet)
        _uiState.value = _uiState.value.copy(enabledPathIds = currentSet)
    }

    fun toggleLowPowerMode() {
        val next = !_uiState.value.isLowPowerMode
        renderer.isLowPowerMode = next
        _uiState.value = _uiState.value.copy(isLowPowerMode = next)
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
            clock.replayRelease()
        } else {
            clock.reset()
            clock.triggerWake()
        }
        _uiState.value = _uiState.value.copy(isPlaying = true, isPaused = false)
    }

    fun reset() {
        clock.reset()
        _uiState.value = _uiState.value.copy(
            isPlaying = true,
            isPaused = false
        )
    }

    // === Size & Layout Controls ===

    fun toggleSize() {
        // Toggle between 0.5 (40dp) and 1.0 (80dp)
        val newScale = if (_uiState.value.orbSizeScale <= 0.6f) 1.0f else 0.5f
        updateOrbSizeScale(newScale)
    }

    fun updateOrbSizeScale(scale: Float) {
        renderer.orbSizeScale = scale
        _uiState.value = _uiState.value.copy(
            orbSizeScale = scale,
            is80dp = scale >= 0.9f
        )
    }

    fun toggleBottomPanel() {
        _uiState.value = _uiState.value.copy(
            isBottomPanelExpanded = !_uiState.value.isBottomPanelExpanded
        )
    }

    fun setBottomPanelExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isBottomPanelExpanded = expanded)
    }

    // === Parameter Tuning Handlers ===

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

    fun updateRotationSpeed(speed: Float) {
        renderer.rotationSpeed = speed
        _uiState.value = _uiState.value.copy(rotationSpeed = speed)
    }

    fun toggleAutoRotate() {
        val next = !_uiState.value.isAutoRotate
        renderer.isAutoRotate = next
        _uiState.value = _uiState.value.copy(isAutoRotate = next)
    }

    fun updateGlowIntensity(v: Float) {
        renderer.glowIntensityMultiplier = v
        _uiState.value = _uiState.value.copy(glowIntensity = v)
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
        setDiagnosticMode(R06DiagnosticMode.RUNTIME)
        renderer.enabledPathIds.clear()
        renderer.enabledPathIds.addAll(listOf("G01", "G02", "A01", "A02", "C01"))
        _uiState.value = _uiState.value.copy(
            enabledPathIds = setOf("G01", "G02", "A01", "A02", "C01"),
            isLowPowerMode = false
        )
    }

    fun exportCalibrationJson() {
        val s = _uiState.value
        val json = JSONObject().apply {
            put("contractId", "YUJIAN_AI_ORB_WAKE_BREATH_RELEASE_VISUAL_PROFILE_V1_1")
            put("status", "CALIBRATION_CANDIDATE")
            put("labMode", s.labMode.name)
            put("releaseLoopEnabled", s.releaseLoopEnabled)
            put("releaseCycleIndex", s.releaseCycleIndex)
            put("diagnosticMode", s.diagnosticMode.name)
            put("coreVisibleRadiusUv", s.coreVisibleRadiusUv)
            put("orbDiameterDp", s.orbSizeScale * 80.0)
            put("orbDiameterRenderPx", s.orbDiameterRenderPx)
            put("sunlightIntensityMultiplier", s.sunlightIntensity)
            put("refractionIntensityMultiplier", s.refractionIntensity)
            put("causticsIntensityMultiplier", s.causticsIntensity)
            put("grassMotionMultiplier", s.grassMotion)
            put("dropletVisibilityMultiplier", s.dropletVisibility)
            put("filamentAlphaMultiplier", s.filamentAlpha)
            put("filamentWidthMultiplier", s.filamentWidth)
            put("orbSizeScale", s.orbSizeScale)
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
