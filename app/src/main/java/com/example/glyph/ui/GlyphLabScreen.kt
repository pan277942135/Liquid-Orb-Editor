package com.example.glyph.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.glyph.gl.GlyphOrbTextureView
import com.example.glyph.model.GlyphFontOption
import com.example.glyph.model.GlyphKeyframe
import com.example.glyph.model.GlyphMorphTimeline
import com.example.glyph.model.SpeciesGlyphSpec
import com.example.glyph.model.TimelinePlayMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun GlyphLabScreen(
    viewModel: GlyphLabViewModel,
    onSwitchMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Realtime frame update loop to sync UI
    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.syncFromRenderer()
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070913))
    ) {

        // 1. OpenGL ES 2.0 TextureView Host
        AndroidView(
            factory = { ctx ->
                GlyphOrbTextureView(ctx, viewModel.renderer)
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("glyph_orb_texture_view")
        )

        // 2. Overlays
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar
            GlyphTopHeader(
                uiState = uiState,
                onPlayPause = { viewModel.togglePlayPause() },
                onReplay = { viewModel.replay() },
                onReset = { viewModel.reset() },
                onToggleSize = { viewModel.toggleSize() },
                onTogglePanel = { viewModel.toggleBottomPanel() },
                onExport = { viewModel.exportProfileJson() },
                onSwitchMode = onSwitchMode
            )

            // Real-time HUD info
            GlyphMetricsHudBar(uiState = uiState)

            Spacer(modifier = Modifier.weight(1f))

            // 3. Collapsible / Expandable Bottom Control Panel
            GlyphBottomControlPanel(
                uiState = uiState,
                viewModel = viewModel
            )
        }

        // 4. Export JSON Dialog
        if (uiState.showExportDialog && uiState.exportedJsonText != null) {
            GlyphExportDialog(
                speciesName = uiState.spec.speciesName,
                jsonText = uiState.exportedJsonText!!,
                onDismiss = { viewModel.dismissExportDialog() }
            )
        }
    }
}

@Composable
private fun GlyphTopHeader(
    uiState: GlyphLabUiState,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onReset: () -> Unit,
    onToggleSize: () -> Unit,
    onTogglePanel: () -> Unit,
    onExport: () -> Unit,
    onSwitchMode: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xD90D111E),
        border = BorderStroke(1.dp, Color(0x3338BDF8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF67C8D2))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "06 凝字实验室",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "AI Orb 动态鱼种中文凝字 V2",
                        fontSize = 10.sp,
                        color = Color(0xFF67C8D2)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mode switcher button (to switch between Labs)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0x4467C8D2)),
                    modifier = Modifier
                        .clickable { onSwitchMode("ai_orb_lab") }
                        .testTag("switch_to_ai_orb_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Mode",
                            tint = Color(0xFF67C8D2),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "三态",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF67C8D2)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))

                // Size toggle badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E2338),
                    border = BorderStroke(1.dp, Color(0x4438BDF8)),
                    modifier = Modifier
                        .clickable { onToggleSize() }
                        .testTag("size_toggle_button")
                ) {
                    Text(
                        text = "${(uiState.orbSizeScale * 80).toInt()}dp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF67E8F9),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onPlayPause, modifier = Modifier.size(34.dp).testTag("play_pause_button")) {
                    Icon(
                        imageVector = if (!uiState.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (!uiState.isPaused) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onReplay, modifier = Modifier.size(34.dp).testTag("replay_button")) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onReset, modifier = Modifier.size(34.dp).testTag("reset_button")) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onTogglePanel, modifier = Modifier.size(34.dp).testTag("tuning_toggle_button")) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Toggle Panel",
                        tint = if (uiState.isBottomPanelExpanded) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onExport, modifier = Modifier.size(34.dp).testTag("export_profile_button")) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export Profile",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlyphMetricsHudBar(uiState: GlyphLabUiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0x99090C16)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Species & Keyframe chip
            val kfColor = when (uiState.activeKeyframe) {
                GlyphKeyframe.KF01_ORB_SPHERE -> Color(0xFF38BDF8)
                GlyphKeyframe.KF02_WATER_CORE_STRETCH -> Color(0xFF06B6D4)
                GlyphKeyframe.KF03_WATER_RIBBON_RELEASE -> Color(0xFF2DD4BF)
                GlyphKeyframe.KF04_GLYPH_INITIAL -> Color(0xFFA78BFA)
                GlyphKeyframe.KF05_GLYPH_CRYSTALLIZE -> Color(0xFFF472B6)
                GlyphKeyframe.KF06_GLYPH_SETTLED -> Color(0xFFFBBF24)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF173E49))
                        .border(1.dp, Color(0xFF2E8B83), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = uiState.spec.speciesName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF67C8D2)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(kfColor.copy(alpha = 0.2f))
                        .border(1.dp, kfColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${uiState.activeKeyframe.id} ${uiState.activeKeyframe.nameZh}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = kfColor
                    )
                }
            }

            // Monotonic Clock metrics
            Text(
                text = "t:${uiState.elapsedMs}ms",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )

            // Progress percentage
            Text(
                text = "${(uiState.progress * 100).toInt()}% [${uiState.playMode.name}]",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF67E8F9)
            )
        }
    }
}

@Composable
private fun GlyphBottomControlPanel(
    uiState: GlyphLabUiState,
    viewModel: GlyphLabViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        color = Color(0xEB0C101D),
        border = BorderStroke(1.dp, Color(0x3338BDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleBottomPanel() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("glyph_panel_toggle_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isBottomPanelExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (uiState.isBottomPanelExpanded) "收起控制面板" else "展开控制面板",
                        tint = Color(0xFF67C8D2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isBottomPanelExpanded) "收起凝字控制台" else "展开凝字控制台",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Collapsed quick state
                if (!uiState.isBottomPanelExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (kf in GlyphKeyframe.entries) {
                            val isSel = uiState.activeKeyframe == kf
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) Color(0xFF67C8D2) else Color(0xFF1E293B),
                                modifier = Modifier.clickable { viewModel.stepToKeyframe(kf) }
                            ) {
                                Text(
                                    text = kf.id,
                                    fontSize = 10.sp,
                                    color = if (isSel) Color(0xFF0F172A) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expanded panel content
            AnimatedVisibility(
                visible = uiState.isBottomPanelExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Section 1: Dynamic Species Input & Preset Chips
                    DynamicSpeciesSection(
                        currentSpec = uiState.spec,
                        onCustomInput = { viewModel.updateCustomSpecies(it) },
                        onSelectPreset = { viewModel.selectPresetSpecies(it) },
                        onSelectFont = { viewModel.updateFontOption(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section 2: Six Keyframe Stepping Checkpoints
                    KeyframeCheckpointsSection(
                        activeKeyframe = uiState.activeKeyframe,
                        playMode = uiState.playMode,
                        onStep = { viewModel.stepToKeyframe(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section 3: Scrubbing Slider & Play Mode
                    ScrubbingTimelineSection(
                        progress = uiState.progress,
                        elapsedMs = uiState.elapsedMs,
                        playMode = uiState.playMode,
                        onScrub = { viewModel.scrubTo(it) },
                        onModeSelect = { viewModel.setPlayMode(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section 4: Orb Size Controls (球体尺寸控制参数)
                    OrbSizeSection(
                        currentScale = uiState.orbSizeScale,
                        onScaleChange = { viewModel.updateOrbSizeScale(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section 5: Optical & Calibration Parameters
                    OpticalTuningSection(
                        uiState = uiState,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DynamicSpeciesSection(
    currentSpec: SpeciesGlyphSpec,
    onCustomInput: (String) -> Unit,
    onSelectPreset: (SpeciesGlyphSpec) -> Unit,
    onSelectFont: (GlyphFontOption) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9913192B),
        border = BorderStroke(1.dp, Color(0x3367C8D2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "动态鱼种输入 (Dynamic Species Text)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF67C8D2)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Text input field
            var inputText by remember(currentSpec.speciesName) { mutableStateOf(currentSpec.speciesName) }
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    onCustomInput(it)
                },
                label = { Text("输入任意鱼种名称 (如: 黑鱼, 大口黑鲈, 鳜鱼)", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF67C8D2),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFCBD5E1),
                    focusedLabelColor = Color(0xFF67C8D2),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("species_name_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick preset chips
            Text(
                text = "常用鱼种快捷预设:",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (preset in SpeciesGlyphSpec.PRESET_SPECIES) {
                    val isSelected = currentSpec.speciesName == preset.speciesName
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF173E49) else Color(0xFF1E293B),
                        border = if (isSelected) BorderStroke(1.dp, Color(0xFF67C8D2)) else null,
                        modifier = Modifier
                            .clickable { onSelectPreset(preset) }
                            .testTag("species_preset_${preset.speciesName}")
                    ) {
                        Text(
                            text = preset.speciesName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF67C8D2) else Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Font options selector
            Text(
                text = "书法字体选择:",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (font in GlyphFontOption.entries) {
                    val isSelected = currentSpec.fontOption == font
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF2E8B83) else Color(0xFF1E293B),
                        border = if (isSelected) BorderStroke(1.dp, Color(0xFFD8B36A)) else null,
                        modifier = Modifier
                            .clickable { onSelectFont(font) }
                            .testTag("font_option_${font.name}")
                    ) {
                        Text(
                            text = font.labelZh,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyframeCheckpointsSection(
    activeKeyframe: GlyphKeyframe,
    playMode: TimelinePlayMode,
    onStep: (GlyphKeyframe) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9913192B),
        border = BorderStroke(1.dp, Color(0x3338BDF8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "六帧视觉检查点 (KF01 ~ KF06)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = if (playMode == TimelinePlayMode.MANUAL) "定格状态 [MANUAL]" else "连续状态",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (playMode == TimelinePlayMode.MANUAL) Color(0xFFFBBF24) else Color(0xFF34D399)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 6 Frame Stepping buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (kf in GlyphKeyframe.entries) {
                    val isSelected = activeKeyframe == kf && playMode == TimelinePlayMode.MANUAL
                    val btnBg = when (kf) {
                        GlyphKeyframe.KF01_ORB_SPHERE -> Color(0xFF0369A1)
                        GlyphKeyframe.KF02_WATER_CORE_STRETCH -> Color(0xFF0E7490)
                        GlyphKeyframe.KF03_WATER_RIBBON_RELEASE -> Color(0xFF0F766E)
                        GlyphKeyframe.KF04_GLYPH_INITIAL -> Color(0xFF6D28D9)
                        GlyphKeyframe.KF05_GLYPH_CRYSTALLIZE -> Color(0xFFBE185D)
                        GlyphKeyframe.KF06_GLYPH_SETTLED -> Color(0xFFB45309)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) btnBg else Color(0xFF1E293B),
                        border = if (isSelected) BorderStroke(1.5.dp, Color.White) else BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .clickable { onStep(kf) }
                            .testTag("step_${kf.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = kf.id,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else Color(0xFF67C8D2)
                            )
                            Text(
                                text = kf.nameZh,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrubbingTimelineSection(
    progress: Float,
    elapsedMs: Long,
    playMode: TimelinePlayMode,
    onScrub: (Float) -> Unit,
    onModeSelect: (TimelinePlayMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9913192B),
        border = BorderStroke(1.dp, Color(0x3338BDF8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "时间线连续 Scrub (0 ~ 2400ms)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF67E8F9)
                )
                Text(
                    text = "${elapsedMs}ms (${(progress * 100).toInt()}%)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF86EFAC)
                )
            }

            Slider(
                value = progress,
                onValueChange = { onScrub(it) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF67C8D2),
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("glyph_scrub_slider")
            )

            // Playback Mode selection chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf(
                    TimelinePlayMode.AUTO to "AUTO (自动)",
                    TimelinePlayMode.MANUAL to "MANUAL (定格)",
                    TimelinePlayMode.SCRUB to "SCRUB (拖动)",
                    TimelinePlayMode.LOOP to "LOOP (循环)"
                )
                for ((mode, label) in modes) {
                    val isSelected = playMode == mode
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onModeSelect(mode) }
                            .testTag("mode_${mode.name}")
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White,
                            modifier = Modifier.padding(vertical = 5.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbSizeSection(
    currentScale: Float,
    onScaleChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9913192B),
        border = BorderStroke(1.dp, Color(0x3338BDF8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "球体尺寸控制 (Orb Size)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF67E8F9)
                )
                Text(
                    text = "${(currentScale * 80).toInt()}dp (${String.format(Locale.US, "%.2fx", currentScale)})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF86EFAC)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    0.5f to "40dp (0.5x 默认)",
                    0.65f to "52dp (0.65x)",
                    0.75f to "60dp (0.75x)",
                    0.85f to "68dp (0.85x)",
                    1.0f to "80dp (1.0x 原大)",
                    1.25f to "100dp (1.25x)"
                )
                for ((scaleVal, label) in presets) {
                    val isSelected = kotlin.math.abs(currentScale - scaleVal) < 0.04f
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier
                            .clickable { onScaleChange(scaleVal) }
                            .testTag("size_preset_${(scaleVal * 100).toInt()}")
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Slider(
                value = currentScale,
                onValueChange = onScaleChange,
                valueRange = 0.2f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF67C8D2),
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier.fillMaxWidth().testTag("size_slider")
            )
        }
    }
}

@Composable
private fun OpticalTuningSection(
    uiState: GlyphLabUiState,
    viewModel: GlyphLabViewModel
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9913192B),
        border = BorderStroke(1.dp, Color(0x3338BDF8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "光学与材质校准参数 (Calibration Optics)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF67C8D2)
            )

            Spacer(modifier = Modifier.height(4.dp))

            val p = uiState.profile

            GlyphSliderRow(
                label = "横向舒展 (Stretch X)",
                value = p.maxStretchX,
                range = 1.0f..2.0f,
                format = "%.2fx",
                onValueChange = { v -> viewModel.updateProfile { it.copy(maxStretchX = v) } }
            )

            GlyphSliderRow(
                label = "水流带强度 (Ribbon Intensity)",
                value = p.ribbonIntensity,
                range = 0.0f..2.0f,
                format = "%.2f",
                onValueChange = { v -> viewModel.updateProfile { it.copy(ribbonIntensity = v) } }
            )

            GlyphSliderRow(
                label = "水晶折射率 (Refraction IOR)",
                value = p.crystalRefraction,
                range = 1.0f..2.5f,
                format = "%.2f",
                onValueChange = { v -> viewModel.updateProfile { it.copy(crystalRefraction = v) } }
            )

            GlyphSliderRow(
                label = "高光光泽 (Specular Gloss)",
                value = p.specularGloss,
                range = 1.0f..8.0f,
                format = "%.1f",
                onValueChange = { v -> viewModel.updateProfile { it.copy(specularGloss = v) } }
            )

            GlyphSliderRow(
                label = "深潭墨韵浓度 (Ink Opacity)",
                value = p.inkOpacity,
                range = 0.0f..1.0f,
                format = "%.2f",
                onValueChange = { v -> viewModel.updateProfile { it.copy(inkOpacity = v) } }
            )

            GlyphSliderRow(
                label = "金色光丝强度 (Gold Filament)",
                value = p.goldFilamentAlpha,
                range = 0.0f..1.0f,
                format = "%.2f",
                onValueChange = { v -> viewModel.updateProfile { it.copy(goldFilamentAlpha = v) } }
            )
        }
    }
}

@Composable
private fun GlyphSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = Color(0xFFCBD5E1))
            Text(
                text = String.format(Locale.US, format, value),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF38BDF8),
                inactiveTrackColor = Color(0xFF334155)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun GlyphExportDialog(
    speciesName: String,
    jsonText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable { onDismiss() },
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF67C8D2))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "导出鱼种凝字配置: $speciesName",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF67C8D2)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "V2 动态鱼种中文凝字标准 Profile JSON",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF070B14),
                        border = BorderStroke(1.dp, Color(0x3338BDF8))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = jsonText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF67E8F9)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Glyph Profile JSON", jsonText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "配置已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制 JSON", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("关闭", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
