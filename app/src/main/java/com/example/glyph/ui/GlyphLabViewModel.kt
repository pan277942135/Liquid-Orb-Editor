package com.example.glyph.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glyph.gl.GlyphOrbRenderer
import com.example.glyph.model.GlyphFontOption
import com.example.glyph.model.GlyphKeyframe
import com.example.glyph.model.GlyphMorphTimeline
import com.example.glyph.model.SpeciesGlyphSpec
import com.example.glyph.model.TimelinePlayMode
import com.example.glyph.model.YujianGlyphMaterialProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GlyphLabUiState(
    val spec: SpeciesGlyphSpec = SpeciesGlyphSpec("黑鱼", "Channa argus", "Northern Snakehead"),
    val playMode: TimelinePlayMode = TimelinePlayMode.AUTO,
    val activeKeyframe: GlyphKeyframe = GlyphKeyframe.KF01_ORB_SPHERE,
    val progress: Float = 0f,
    val elapsedMs: Long = 0L,
    val isPaused: Boolean = false,
    val orbSizeScale: Float = 0.5f, // Default half size 40dp
    val isBottomPanelExpanded: Boolean = true,
    val profile: YujianGlyphMaterialProfile = YujianGlyphMaterialProfile.createDefault(),
    val showExportDialog: Boolean = false,
    val exportedJsonText: String? = null
)

class GlyphLabViewModel(application: Application) : AndroidViewModel(application) {

    val renderer = GlyphOrbRenderer(application)

    private val _uiState = MutableStateFlow(GlyphLabUiState())
    val uiState: StateFlow<GlyphLabUiState> = _uiState.asStateFlow()

    init {
        renderer.orbSizeScale = 0.5f
    }

    fun syncFromRenderer() {
        val state = renderer.evaluateCurrentState()
        _uiState.value = _uiState.value.copy(
            progress = state.progress,
            elapsedMs = state.elapsedMs,
            activeKeyframe = state.activeKeyframe,
            playMode = state.playMode,
            isPaused = renderer.isPaused,
            orbSizeScale = renderer.orbSizeScale,
            profile = renderer.currentProfile
        )
    }

    fun selectPresetSpecies(spec: SpeciesGlyphSpec) {
        viewModelScope.launch {
            renderer.updateSpecies(spec)
            _uiState.value = _uiState.value.copy(
                spec = spec,
                profile = renderer.currentProfile
            )
            renderer.replay()
        }
    }

    fun updateCustomSpecies(name: String) {
        val trimmed = name.trim()
        val current = _uiState.value.spec
        val updated = current.copy(speciesName = trimmed.ifEmpty { "鱼" })
        renderer.updateSpecies(updated)
        _uiState.value = _uiState.value.copy(spec = updated, profile = renderer.currentProfile)
    }

    fun updateFontOption(font: GlyphFontOption) {
        val updated = _uiState.value.spec.copy(fontOption = font)
        renderer.updateSpecies(updated)
        _uiState.value = _uiState.value.copy(spec = updated, profile = renderer.currentProfile)
    }

    fun togglePlayPause() {
        if (renderer.isPaused) {
            renderer.play()
        } else {
            renderer.pause()
        }
        syncFromRenderer()
    }

    fun replay() {
        renderer.replay()
        syncFromRenderer()
    }

    fun reset() {
        renderer.reset()
        syncFromRenderer()
    }

    fun stepToKeyframe(kf: GlyphKeyframe) {
        renderer.stepToKeyframe(kf)
        _uiState.value = _uiState.value.copy(
            activeKeyframe = kf,
            progress = kf.normalizedProgress,
            elapsedMs = kf.targetTimeMs,
            playMode = TimelinePlayMode.MANUAL,
            isPaused = true
        )
    }

    fun scrubTo(progress: Float) {
        renderer.setScrub(progress)
        val kf = GlyphKeyframe.fromProgress(progress)
        _uiState.value = _uiState.value.copy(
            progress = progress,
            elapsedMs = (progress * GlyphMorphTimeline.TOTAL_DURATION_MS).toLong(),
            activeKeyframe = kf,
            playMode = TimelinePlayMode.SCRUB
        )
    }

    fun setPlayMode(mode: TimelinePlayMode) {
        renderer.playMode = mode
        if (mode == TimelinePlayMode.AUTO || mode == TimelinePlayMode.LOOP) {
            renderer.play()
        }
        syncFromRenderer()
    }

    fun updateOrbSizeScale(scale: Float) {
        val clamped = scale.coerceIn(0.2f, 1.5f)
        renderer.orbSizeScale = clamped
        _uiState.value = _uiState.value.copy(orbSizeScale = clamped)
    }

    fun toggleSize() {
        val newScale = if (_uiState.value.orbSizeScale < 0.75f) 1.0f else 0.5f
        updateOrbSizeScale(newScale)
    }

    fun toggleBottomPanel() {
        _uiState.value = _uiState.value.copy(
            isBottomPanelExpanded = !_uiState.value.isBottomPanelExpanded
        )
    }

    fun updateProfile(transform: (YujianGlyphMaterialProfile) -> YujianGlyphMaterialProfile) {
        val newProfile = transform(renderer.currentProfile)
        renderer.currentProfile = newProfile
        _uiState.value = _uiState.value.copy(profile = newProfile)
    }

    fun exportProfileJson() {
        val json = renderer.currentProfile.toJsonString()
        _uiState.value = _uiState.value.copy(
            showExportDialog = true,
            exportedJsonText = json
        )
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(
            showExportDialog = false,
            exportedJsonText = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        renderer.release()
    }
}
