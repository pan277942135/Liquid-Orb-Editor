package com.example.aiorb.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.aiorb.ribbon.DualRimRibbonCanvas
import com.example.aiorb.ui.components.DualRimRibbonControlSection
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
import com.example.aiorb.gl.AIOrbTextureView
import com.example.aiorb.model.OrbLifeState
import com.example.aiorb.model.OrbPhaseEvent
import com.example.ui.components.OrbSliderRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun AIOrbVisualLabScreen(
    viewModel: AIOrbViewModel,
    onSwitchMode: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val currentFrameState = remember { mutableStateOf(viewModel.renderer.sceneClock.produceFrame()) }

    // Realtime frame update loop to synchronize UI State
    LaunchedEffect(Unit) {
        while (isActive) {
            val frame = viewModel.renderer.sceneClock.produceFrame()
            currentFrameState.value = frame
            viewModel.updateFromFrame(frame)
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070913))
    ) {

        // 1. Back Pass 3D Dual-Rim Specular Ribbons (behind the glass sphere)
        if (uiState.useDualRimRibbons) {
            DualRimRibbonCanvas(
                isBackPass = true,
                frame = currentFrameState.value,
                config = uiState.ribbonConfig,
                orbSizeScale = uiState.orbSizeScale,
                touchRotationOffset = viewModel.renderer.touchRotationOffset,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Core Visualizer Host (TextureView - draws transparent 3D glass core sphere)
        AndroidView(
            factory = { ctx ->
                AIOrbTextureView(ctx, viewModel.renderer)
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("ai_orb_texture_view")
        )

        // 3. Front Pass 3D Dual-Rim Specular Ribbons (in front of glass sphere + Light Knot Starburst)
        if (uiState.useDualRimRibbons) {
            DualRimRibbonCanvas(
                isBackPass = false,
                frame = currentFrameState.value,
                config = uiState.ribbonConfig,
                orbSizeScale = uiState.orbSizeScale,
                touchRotationOffset = viewModel.renderer.touchRotationOffset,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Overlay Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar
            TopLabHeader(
                uiState = uiState,
                onPlayPause = { viewModel.togglePlayPause() },
                onReplay = { viewModel.replay() },
                onReset = { viewModel.reset() },
                onToggleSize = { viewModel.toggleSize() },
                onTogglePanel = { viewModel.toggleBottomPanel() },
                onExport = { viewModel.exportCalibrationJson() },
                onSwitchMode = onSwitchMode,
                onSetLabMode = { viewModel.setLabMode(it) },
                onToggleLoop = { viewModel.toggleReleaseLoop() }
            )

            // Real-time HUD info
            MetricsHudBar(
                uiState = uiState,
                onToggleRibbonPipeline = { viewModel.toggleRibbonPipeline(!uiState.useDualRimRibbons) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 3. Collapsible / Expandable Bottom Control Panel
            BottomControlPanel(
                uiState = uiState,
                viewModel = viewModel
            )
        }

        // 4. Calibration Export Modal Bottom Sheet
        if (uiState.showCalibrationDialog && uiState.exportedJsonText != null) {
            CalibrationExportDialog(
                jsonText = uiState.exportedJsonText!!,
                onDismiss = { viewModel.dismissCalibrationDialog() }
            )
        }
    }
}

@Composable
private fun TopLabHeader(
    uiState: AIOrbUiState,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onReset: () -> Unit,
    onToggleSize: () -> Unit,
    onTogglePanel: () -> Unit,
    onExport: () -> Unit,
    onSwitchMode: (String) -> Unit,
    onSetLabMode: (AIOrbLabMode) -> Unit,
    onToggleLoop: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xD90D111E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Row 1: Title, Mode Tabs, and Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (uiState.lifeState == OrbLifeState.S2_ACTIVE) Color(0xFF10B981) else Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI ORB VISUAL LAB",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "R06 Frozen Profile + Kinematics",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Switch to 06 凝字实验室
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4467C8D2)),
                        modifier = Modifier
                            .clickable { onSwitchMode("glyph_lab") }
                            .testTag("switch_to_glyph_lab_button")
                    ) {
                        Text(
                            text = "06凝字",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF67C8D2),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Size toggle badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E2338),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4438BDF8)),
                        modifier = Modifier
                            .clickable { onToggleSize() }
                            .testTag("size_toggle_button")
                    ) {
                        Text(
                            text = "${(uiState.orbSizeScale * 80).toInt()}dp",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF67E8F9),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onTogglePanel, modifier = Modifier.size(32.dp).testTag("tuning_toggle_button")) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Toggle Control Panel",
                            tint = if (uiState.isBottomPanelExpanded) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onExport, modifier = Modifier.size(32.dp).testTag("export_calibration_button")) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export JSON",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Mode Selector Segmented Tabs & Playback Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Segmented Mode Tabs: [RELEASE 专项 | 完整生命流程]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF121828))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val isReleaseMode = uiState.labMode == AIOrbLabMode.RELEASE_PREVIEW
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isReleaseMode) Color(0xFFEC4899) else Color.Transparent,
                        modifier = Modifier
                            .clickable { onSetLabMode(AIOrbLabMode.RELEASE_PREVIEW) }
                            .testTag("mode_release_preview_tab")
                    ) {
                        Text(
                            text = "RELEASE 专项 (循环)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReleaseMode) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (!isReleaseMode) Color(0xFF38BDF8) else Color.Transparent,
                        modifier = Modifier
                            .clickable { onSetLabMode(AIOrbLabMode.FULL_LIFECYCLE) }
                            .testTag("mode_full_lifecycle_tab")
                    ) {
                        Text(
                            text = "完整生命流程",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isReleaseMode) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Quick Play/Pause, Replay, and Loop Toggle
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Loop Toggle (active in RELEASE mode)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (uiState.releaseLoopEnabled) Color(0xFF065F46) else Color(0xFF334155),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.releaseLoopEnabled) Color(0xFF10B981) else Color(0xFF64748B)),
                        modifier = Modifier
                            .clickable { onToggleLoop() }
                            .testTag("loop_toggle_button")
                    ) {
                        Text(
                            text = if (uiState.releaseLoopEnabled) "LOOP: ON" else "LOOP: OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.releaseLoopEnabled) Color(0xFFA7F3D0) else Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(onClick = onPlayPause, modifier = Modifier.size(32.dp).testTag("play_pause_button")) {
                        Icon(
                            imageVector = if (!uiState.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = if (!uiState.isPaused) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onReplay, modifier = Modifier.size(32.dp).testTag("replay_button")) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsHudBar(
    uiState: AIOrbUiState,
    onToggleRibbonPipeline: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xB3090C16),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Row 1: State Chip, Cycle, R_uv, Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stateColor = when {
                    uiState.isPaused -> Color(0xFFEAB308)
                    uiState.isHold -> Color(0xFFF59E0B)
                    uiState.currentPhaseEvent == OrbPhaseEvent.RELEASE -> Color(0xFFEC4899)
                    uiState.currentPhaseEvent == OrbPhaseEvent.RETURN -> Color(0xFFA855F7)
                    uiState.currentPhaseEvent == OrbPhaseEvent.WAKE -> Color(0xFF06B6D4)
                    uiState.lifeState == OrbLifeState.S2_ACTIVE -> Color(0xFF10B981)
                    else -> Color(0xFF64748B)
                }
                val stateText = when {
                    uiState.isPaused -> "PAUSED"
                    uiState.isHold -> "HOLD (350–690ms)"
                    uiState.currentPhaseEvent == OrbPhaseEvent.RELEASE -> "RELEASE (0–350ms)"
                    uiState.currentPhaseEvent == OrbPhaseEvent.RETURN -> "RETURN (690–950ms)"
                    uiState.currentPhaseEvent == OrbPhaseEvent.WAKE -> "WAKE"
                    uiState.lifeState == OrbLifeState.S2_ACTIVE -> "S2 BREATH"
                    else -> "S1 IDLE"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(stateColor.copy(alpha = 0.2f))
                        .border(1.dp, stateColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stateText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                }

                Text(
                    text = "Cycle #${uiState.releaseCycleIndex} (${uiState.releaseCycleElapsedMs}ms/950ms)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF67E8F9)
                )

                Text(
                    text = "R_uv:${String.format(Locale.US, "%.4f", uiState.coreVisibleRadiusUv)}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFA7F3D0)
                )

                Text(
                    text = "${(uiState.orbSizeScale * 80).toInt()}dp (${uiState.orbDiameterRenderPx.toInt()}px)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Row 2: Diagnostic Mode, Active Paths (G01, G02, A01, A02, C01), Alpha, GL Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Diag Mode Badge
                val diagModeColor = when (uiState.diagnosticMode) {
                    com.example.aiorb.model.R06DiagnosticMode.RUNTIME -> Color(0xFF10B981)
                    com.example.aiorb.model.R06DiagnosticMode.GEOMETRY_ONLY -> Color(0xFFF59E0B)
                    com.example.aiorb.model.R06DiagnosticMode.MATERIAL -> Color(0xFFEC4899)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(diagModeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "MODE: ${uiState.diagnosticMode.label}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = diagModeColor
                    )
                }

                // 5 Paths mini indicators
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val paths = listOf("G01", "G02", "A01", "A02", "C01")
                    for (pid in paths) {
                        val active = pid in uiState.enabledPathIds
                        val pColor = when {
                            pid.startsWith("G") -> Color(0xFF34D399)
                            pid.startsWith("A") -> Color(0xFFFBBF24)
                            else -> Color(0xFF38BDF8)
                        }
                        Text(
                            text = pid,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (active) pColor else Color(0xFF475569)
                        )
                    }
                }

                Text(
                    text = "Rot:${String.format(Locale.US, "%.1f", uiState.rotationSpeed)}x",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFDE047)
                )

                Text(
                    text = "Alpha:${(uiState.filamentAlphaProgress * 100).toInt()}%",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0)
                )

                Text(
                    text = if (uiState.useDualRimRibbons) "3D DUAL-RIM" else (if (uiState.coreShaderCompiled && uiState.filamentShaderCompiled) "GL: OK" else "GL: ERR"),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (uiState.useDualRimRibbons) Color(0xFF34D399) else (if (uiState.coreShaderCompiled && uiState.filamentShaderCompiled) Color(0xFF4ADE80) else Color(0xFFEF4444)),
                    modifier = Modifier.clickable { onToggleRibbonPipeline?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun BottomControlPanel(
    uiState: AIOrbUiState,
    viewModel: AIOrbViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        color = Color(0xEB0C101D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            // Drag handle & Collapse/Expand Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleBottomPanel() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("bottom_panel_toggle_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isBottomPanelExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (uiState.isBottomPanelExpanded) "收起控制面板" else "展开控制面板",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isBottomPanelExpanded) "收起控制面板" else "展开控制面板",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Collapsed state quick indicators & triggers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "尺寸: ${(uiState.orbSizeScale * 80).toInt()}dp (${String.format(Locale.US, "%.2fx", uiState.orbSizeScale)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF67E8F9)
                        )
                    }

                    if (!uiState.isBottomPanelExpanded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (uiState.labMode == AIOrbLabMode.RELEASE_PREVIEW) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (!uiState.isPaused) Color(0xFF0284C7) else Color(0xFFEAB308),
                                    modifier = Modifier.clickable { viewModel.togglePlayPause() }
                                ) {
                                    Text(if (!uiState.isPaused) "PAUSE" else "PLAY", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF7C3AED),
                                    modifier = Modifier.clickable { viewModel.replay() }
                                ) {
                                    Text("REPLAY", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (uiState.releaseLoopEnabled) Color(0xFF059669) else Color(0xFF475569),
                                    modifier = Modifier.clickable { viewModel.toggleReleaseLoop() }
                                ) {
                                    Text(if (uiState.releaseLoopEnabled) "LOOP:ON" else "LOOP:OFF", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (uiState.currentPhaseEvent == OrbPhaseEvent.WAKE) Color(0xFF06B6D4) else Color(0xFF1E293B),
                                    modifier = Modifier.clickable { viewModel.triggerWake() }
                                ) {
                                    Text("WAKE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (uiState.lifeState == OrbLifeState.S2_ACTIVE && uiState.currentPhaseEvent == OrbPhaseEvent.NONE) Color(0xFF10B981) else Color(0xFF1E293B),
                                    modifier = Modifier.clickable { viewModel.triggerBreath() }
                                ) {
                                    Text("BREATH", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (uiState.currentPhaseEvent == OrbPhaseEvent.RELEASE || uiState.currentPhaseEvent == OrbPhaseEvent.RETURN) Color(0xFFEC4899) else Color(0xFF1E293B),
                                    modifier = Modifier.clickable { viewModel.triggerRelease() }
                                ) {
                                    Text("REL", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Expanded Content with smooth animation
            AnimatedVisibility(
                visible = uiState.isBottomPanelExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Actions Bar (RELEASE Special or Full Lifecycle)
                    if (uiState.labMode == AIOrbLabMode.RELEASE_PREVIEW) {
                        ReleaseDedicatedActionsBar(
                            uiState = uiState,
                            onPlayPause = { viewModel.togglePlayPause() },
                            onReplay = { viewModel.replay() },
                            onToggleLoop = { viewModel.toggleReleaseLoop() },
                            onCollapse = { viewModel.setBottomPanelExpanded(false) }
                        )
                    } else {
                        PrimaryThreeStateBar(
                            uiState = uiState,
                            onWake = { viewModel.triggerWake() },
                            onBreath = { viewModel.triggerBreath() },
                            onRelease = { viewModel.triggerRelease() }
                        )
                    }

                    // 2. Orb Size Controls (球体大小控制参数)
                    OrbSizeControlSection(
                        currentScale = uiState.orbSizeScale,
                        onScaleChange = { viewModel.updateOrbSizeScale(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Dual-Rim Specular Ribbons (3D 实体带状双侧高光水草生态)
                    DualRimRibbonControlSection(
                        uiState = uiState,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. R06 Diagnostic Controls (光丝诊断、几何模式与材质分离)
                    R06DiagnosticSection(
                        uiState = uiState,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Optical Parameters Tuning
                    TuningParametersSection(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun OrbSizeControlSection(
    currentScale: Float,
    onScaleChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9913192B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
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

            Spacer(modifier = Modifier.height(6.dp))

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
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
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

            Spacer(modifier = Modifier.height(6.dp))

            // Continuous size slider
            OrbSliderRow(
                title = "缩放比例 (Scale Slider)",
                value = currentScale,
                range = 0.2f..1.5f,
                onValueChange = onScaleChange,
                testTag = "slider_orb_size",
                displayFormat = "%.2fx",
                unit = " (${(currentScale * 80).toInt()}dp)"
            )
        }
    }
}

@Composable
private fun R06DiagnosticSection(
    uiState: AIOrbUiState,
    viewModel: AIOrbViewModel
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9913192B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "R06 外部光丝诊断 (R06 Diagnostics)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24)
                )
                Text(
                    text = "96细分 / 0.3971 UV",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Diagnostic Mode Selector (Segmented)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (mode in com.example.aiorb.model.R06DiagnosticMode.values()) {
                    val isSelected = uiState.diagnosticMode == mode
                    val selBg = when (mode) {
                        com.example.aiorb.model.R06DiagnosticMode.RUNTIME -> Color(0xFF059669)
                        com.example.aiorb.model.R06DiagnosticMode.GEOMETRY_ONLY -> Color(0xFFD97706)
                        com.example.aiorb.model.R06DiagnosticMode.MATERIAL -> Color(0xFFDB2777)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) selBg else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setDiagnosticMode(mode) }
                            .testTag("diag_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = when (mode) {
                                com.example.aiorb.model.R06DiagnosticMode.RUNTIME -> "正常光学 (Runtime)"
                                com.example.aiorb.model.R06DiagnosticMode.GEOMETRY_ONLY -> "纯几何 (Geometry)"
                                com.example.aiorb.model.R06DiagnosticMode.MATERIAL -> "材质层 (Material)"
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Individual 5 Paths Toggles
            Text(
                text = "五条冻结光丝独立可见性开关 (Path Isolation):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCBD5E1)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val pathConfigs = listOf(
                    Triple("G01", "G01 (绿/后)", Color(0xFF34D399)),
                    Triple("G02", "G02 (绿/前)", Color(0xFF10B981)),
                    Triple("A01", "A01 (金/后)", Color(0xFFFBBF24)),
                    Triple("A02", "A02 (金/前)", Color(0xFFF59E0B)),
                    Triple("C01", "C01 (青/前)", Color(0xFF38BDF8))
                )
                for ((pid, _, col) in pathConfigs) {
                    val isEnabled = pid in uiState.enabledPathIds
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEnabled) col.copy(alpha = 0.25f) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isEnabled) col else Color(0x33FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.togglePath(pid) }
                            .testTag("toggle_path_$pid")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = pid,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) col else Color(0xFF64748B)
                            )
                            Text(
                                text = if (isEnabled) "ON" else "OFF",
                                fontSize = 9.sp,
                                color = if (isEnabled) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Low Power Mode Toggle (48 vs 96 Subdivisions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "网格细分精度: ${if (uiState.isLowPowerMode) "48段 (低功耗)" else "96段 (高精平滑)"}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (uiState.isLowPowerMode) Color(0xFF475569) else Color(0xFF0284C7),
                    modifier = Modifier
                        .clickable { viewModel.toggleLowPowerMode() }
                        .testTag("low_power_toggle_button")
                ) {
                    Text(
                        text = if (uiState.isLowPowerMode) "切换96段" else "切换48段",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseDedicatedActionsBar(
    uiState: AIOrbUiState,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onToggleLoop: () -> Unit,
    onCollapse: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xCC0D111E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44EC4899))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play/Pause
            Button(
                onClick = onPlayPause,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("release_play_pause_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!uiState.isPaused) Color(0xFF0284C7) else Color(0xFFEAB308)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (!uiState.isPaused) "PAUSE" else "PLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(if (!uiState.isPaused) "暂停播放" else "继续播放", fontSize = 8.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            // Replay
            Button(
                onClick = onReplay,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("release_replay_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("REPLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("重播RELEASE", fontSize = 8.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            // Loop Toggle
            Button(
                onClick = onToggleLoop,
                modifier = Modifier
                    .weight(1.1f)
                    .height(44.dp)
                    .testTag("release_loop_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.releaseLoopEnabled) Color(0xFF059669) else Color(0xFF334155)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (uiState.releaseLoopEnabled) "LOOP: ON" else "LOOP: OFF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(if (uiState.releaseLoopEnabled) "连续循环" else "单次播放", fontSize = 8.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            // Fullscreen / Collapse Panel
            Button(
                onClick = onCollapse,
                modifier = Modifier
                    .weight(0.9f)
                    .height(44.dp)
                    .testTag("release_fullscreen_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FULL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Text("全屏预览", fontSize = 8.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
private fun PrimaryThreeStateBar(
    uiState: AIOrbUiState,
    onWake: () -> Unit,
    onBreath: () -> Unit,
    onRelease: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xCC0D111E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // WAKE Button
            Button(
                onClick = onWake,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("action_wake_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.currentPhaseEvent == OrbPhaseEvent.WAKE) Color(0xFF06B6D4) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WAKE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("苏醒 (350ms)", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            // BREATH Button
            Button(
                onClick = onBreath,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("action_breath_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.lifeState == OrbLifeState.S2_ACTIVE && uiState.currentPhaseEvent == OrbPhaseEvent.NONE) Color(0xFF10B981) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BREATH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("持续呼吸 (4.8s)", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            // RELEASE Button
            Button(
                onClick = onRelease,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("action_release_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.currentPhaseEvent == OrbPhaseEvent.RELEASE || uiState.currentPhaseEvent == OrbPhaseEvent.RETURN) Color(0xFFEC4899) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RELEASE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("释放/回收 (950ms)", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun TuningParametersSection(
    uiState: AIOrbUiState,
    viewModel: AIOrbViewModel
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC0D111E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("tuning_drawer_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "参数标定调测 (Tuning Lab)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "恢复冻结默认",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF43F5E),
                    modifier = Modifier
                        .clickable { viewModel.resetToFrozenDefaults() }
                        .padding(4.dp)
                        .testTag("reset_defaults_text")
                )
            }

            OrbSliderRow(
                title = "Sunlight Intensity (阳光强度)",
                value = uiState.sunlightIntensity,
                range = 0.0f..2.0f,
                onValueChange = { viewModel.updateSunlightIntensity(it) },
                testTag = "slider_sunlight"
            )

            OrbSliderRow(
                title = "Refraction Intensity (折射光强)",
                value = uiState.refractionIntensity,
                range = 0.0f..2.0f,
                onValueChange = { viewModel.updateRefractionIntensity(it) },
                testTag = "slider_refraction"
            )

            OrbSliderRow(
                title = "Caustics Intensity (内部焦散)",
                value = uiState.causticsIntensity,
                range = 0.0f..2.0f,
                onValueChange = { viewModel.updateCausticsIntensity(it) },
                testTag = "slider_caustics"
            )

            OrbSliderRow(
                title = "Grass Motion (水草动态权重)",
                value = uiState.grassMotion,
                range = 0.0f..2.5f,
                onValueChange = { viewModel.updateGrassMotion(it) },
                testTag = "slider_grass_motion"
            )

            OrbSliderRow(
                title = "Droplet Visibility (水滴透镜可见度)",
                value = uiState.dropletVisibility,
                range = 0.0f..2.0f,
                onValueChange = { viewModel.updateDropletVisibility(it) },
                testTag = "slider_droplets"
            )

            OrbSliderRow(
                title = "Sphere Rotation Speed (球体自转速度)",
                value = uiState.rotationSpeed,
                range = 0.0f..2.0f,
                onValueChange = { viewModel.updateRotationSpeed(it) },
                testTag = "slider_rotation_speed",
                displayFormat = "%.2fx"
            )

            OrbSliderRow(
                title = "Filament HDR Glow (光丝高保真辉光/泛光)",
                value = uiState.glowIntensity,
                range = 0.5f..3.5f,
                onValueChange = { viewModel.updateGlowIntensity(it) },
                testTag = "slider_glow_intensity",
                displayFormat = "%.2fx"
            )

            OrbSliderRow(
                title = "Filament Alpha (R06 光丝透明度)",
                value = uiState.filamentAlpha,
                range = 0.1f..2.0f,
                onValueChange = { viewModel.updateFilamentAlpha(it) },
                testTag = "slider_filament_alpha"
            )

            OrbSliderRow(
                title = "Filament Width (R06 光丝宽度)",
                value = uiState.filamentWidth,
                range = 0.5f..2.5f,
                onValueChange = { viewModel.updateFilamentWidth(it) },
                testTag = "slider_filament_width"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalibrationExportDialog(
    jsonText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1322)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "CALIBRATION_CANDIDATE 标定配置导出",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF060810))
                    .padding(12.dp)
            ) {
                Text(
                    text = jsonText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF86EFAC),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("AI Orb Calibration", jsonText))
                    Toast.makeText(context, "已复制标定参数到剪贴板！", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("copy_calibration_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("复制完整 JSON", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
