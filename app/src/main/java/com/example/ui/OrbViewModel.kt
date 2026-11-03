package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.audio.AudioMode
import com.example.audio.OrbAudioEngine
import com.example.model.OrbConfig
import com.example.model.OrbPresets
import com.example.model.SceneBackground
import com.example.model.Vec3Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class OrbUiState(
    val config: OrbConfig = OrbPresets.all.first(),
    val selectedPresetId: String = "siri_wave",
    val sceneBg: SceneBackground = SceneBackground.COSMIC_DEEP,
    val isPlaying: Boolean = true,
    val isChinese: Boolean = true, // Default to Chinese as user requested in Chinese
    val isFullscreen: Boolean = false,
    val activeTab: Int = 0, // 0: Shape, 1: Optics, 2: Colors, 3: Audio & Scene
    val audioMode: AudioMode = AudioMode.OFF,
    val showExportDialog: Boolean = false,
    val showColorPickerFor: String? = null // "color1", "color2", "accent", "core", "glow"
)

class OrbViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OrbUiState())
    val uiState: StateFlow<OrbUiState> = _uiState.asStateFlow()

    val audioEngine = OrbAudioEngine(application)

    fun selectPreset(preset: OrbConfig) {
        _uiState.value = _uiState.value.copy(
            config = preset,
            selectedPresetId = preset.presetId
        )
        if (preset.audioReactive && _uiState.value.audioMode == AudioMode.OFF) {
            setAudioMode(AudioMode.SIMULATOR)
        }
    }

    fun setAudioMode(mode: AudioMode) {
        audioEngine.setMode(mode)
        _uiState.value = _uiState.value.copy(
            audioMode = audioEngine.currentMode,
            config = _uiState.value.config.copy(audioReactive = (mode != AudioMode.OFF))
        )
    }

    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun toggleLanguage() {
        _uiState.value = _uiState.value.copy(isChinese = !_uiState.value.isChinese)
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
    }

    fun setSceneBackground(bg: SceneBackground) {
        _uiState.value = _uiState.value.copy(sceneBg = bg)
    }

    fun setShowExportDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExportDialog = show)
    }

    fun setShowColorPicker(param: String?) {
        _uiState.value = _uiState.value.copy(showColorPickerFor = param)
    }

    fun resetToPreset() {
        val currentPreset = OrbPresets.getById(_uiState.value.selectedPresetId)
        _uiState.value = _uiState.value.copy(config = currentPreset)
    }

    fun randomize() {
        val rand = Random.Default
        val palettes = listOf(
            // Cyberpunk
            Triple(Vec3Color(0.05f, 0.95f, 0.95f), Vec3Color(0.95f, 0.15f, 0.65f), Vec3Color(0.65f, 0.15f, 0.95f)),
            // Sunset Ember
            Triple(Vec3Color(0.98f, 0.35f, 0.12f), Vec3Color(0.95f, 0.85f, 0.15f), Vec3Color(0.85f, 0.15f, 0.45f)),
            // Deep Ocean
            Triple(Vec3Color(0.08f, 0.85f, 0.75f), Vec3Color(0.15f, 0.45f, 0.98f), Vec3Color(0.65f, 0.85f, 1.00f)),
            // Emerald Aurora
            Triple(Vec3Color(0.15f, 0.95f, 0.55f), Vec3Color(0.12f, 0.75f, 0.95f), Vec3Color(0.85f, 0.25f, 0.85f)),
            // Royal Quartz
            Triple(Vec3Color(0.65f, 0.25f, 0.98f), Vec3Color(0.95f, 0.45f, 0.85f), Vec3Color(0.35f, 0.65f, 1.00f))
        )
        val palette = palettes[rand.nextInt(palettes.size)]

        val newConfig = _uiState.value.config.copy(
            speed = (rand.nextFloat() * 1.8f + 0.3f),
            radius = (rand.nextFloat() * 0.3f + 0.55f),
            zoom = (rand.nextFloat() * 0.8f + 0.9f),
            warp = (rand.nextFloat() * 1.8f + 0.4f),
            ridgeAmt = (rand.nextFloat() * 2.5f + 0.5f),
            sharp = (rand.nextFloat() * 3.5f + 1.5f),
            refraction = (rand.nextFloat() * 1.8f + 0.5f),
            dispersion = (rand.nextFloat() * 1.0f + 0.2f),
            sheen = (rand.nextFloat() * 1.8f + 0.8f),
            gloss = (rand.nextFloat() * 6.0f + 2.0f),
            glow = (rand.nextFloat() * 1.5f + 0.8f),
            color1 = palette.first,
            color2 = palette.second,
            accentColor = palette.third,
            glowColor = palette.first
        )
        _uiState.value = _uiState.value.copy(config = newConfig)
    }

    // Parameter updates
    fun updateSpeed(v: Float) = updateConfig { it.copy(speed = v) }
    fun updateRadius(v: Float) = updateConfig { it.copy(radius = v) }
    fun updateZoom(v: Float) = updateConfig { it.copy(zoom = v) }
    fun updateWarp(v: Float) = updateConfig { it.copy(warp = v) }
    fun updateRidgeAmt(v: Float) = updateConfig { it.copy(ridgeAmt = v) }
    fun updateSharp(v: Float) = updateConfig { it.copy(sharp = v) }

    fun updateShade(v: Float) = updateConfig { it.copy(shade = v) }
    fun updateSheen(v: Float) = updateConfig { it.copy(sheen = v) }
    fun updateGloss(v: Float) = updateConfig { it.copy(gloss = v) }
    fun updateRefraction(v: Float) = updateConfig { it.copy(refraction = v) }
    fun updateDispersion(v: Float) = updateConfig { it.copy(dispersion = v) }
    fun updateRoughness(v: Float) = updateConfig { it.copy(roughness = v) }

    fun updateGlow(v: Float) = updateConfig { it.copy(glow = v) }
    fun updateEdgeSoftness(v: Float) = updateConfig { it.copy(edgeSoftness = v) }

    fun updateAudioSensitivity(v: Float) = updateConfig { it.copy(audioSensitivity = v) }
    fun updateBassInfluence(v: Float) = updateConfig { it.copy(bassInfluence = v) }
    fun updateMidInfluence(v: Float) = updateConfig { it.copy(midInfluence = v) }
    fun updateTrebleInfluence(v: Float) = updateConfig { it.copy(trebleInfluence = v) }

    fun updateColor(paramName: String, color: Vec3Color) {
        updateConfig { cfg ->
            when (paramName) {
                "color1" -> cfg.copy(color1 = color)
                "color2" -> cfg.copy(color2 = color)
                "accent" -> cfg.copy(accentColor = color)
                "core" -> cfg.copy(coreColor = color)
                "glow" -> cfg.copy(glowColor = color)
                else -> cfg
            }
        }
    }

    private fun updateConfig(transform: (OrbConfig) -> OrbConfig) {
        _uiState.value = _uiState.value.copy(config = transform(_uiState.value.config))
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
