package com.example.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.audio.AudioMode
import com.example.gl.OrbRenderer
import com.example.gl.OrbTextureView
import com.example.model.OrbConfig
import com.example.model.OrbPresets
import com.example.model.SceneBackground
import com.example.ui.components.ColorPickerSheet
import com.example.ui.components.ExportDialog
import com.example.ui.components.OrbSliderRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun OrbScreen(
    viewModel: OrbViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isZh = uiState.isChinese

    // Renderer instance maintained across recompositions
    val orbRenderer = remember { OrbRenderer() }

    // Sync state into renderer
    LaunchedEffect(uiState.config, uiState.sceneBg, uiState.isPlaying) {
        orbRenderer.config = uiState.config
        orbRenderer.sceneBg = uiState.sceneBg
        orbRenderer.isPlaying = uiState.isPlaying
    }

    // Audio frequency stream loop - only active when audio reactivity is enabled
    LaunchedEffect(uiState.audioMode) {
        if (uiState.audioMode != AudioMode.OFF) {
            while (isActive) {
                orbRenderer.audioBass = viewModel.audioEngine.bass
                orbRenderer.audioMid = viewModel.audioEngine.mid
                orbRenderer.audioTreble = viewModel.audioEngine.treble
                delay(16)
            }
        } else {
            orbRenderer.audioBass = 0f
            orbRenderer.audioMid = 0f
            orbRenderer.audioTreble = 0f
        }
    }

    // Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setAudioMode(AudioMode.MICROPHONE)
            Toast.makeText(
                context,
                if (isZh) "麦克风已开启，对着手机说话试试！" else "Microphone active, speak to test!",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.setAudioMode(AudioMode.SIMULATOR)
            Toast.makeText(
                context,
                if (isZh) "权限未开启，已切换为自动节奏模式" else "Permission denied, switched to Auto Rhythm Beat",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF070913))) {

        // 1. OpenGL liquid orb canvas (TextureView avoids SurfaceSyncGroup timeout)
        AndroidView(
            factory = { ctx ->
                OrbTextureView(ctx, orbRenderer)
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("orb_surface_view")
        )

        // 2. Editor UI overlay (Hidden in pure fullscreen mode)
        AnimatedVisibility(
            visible = !uiState.isFullscreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top App Bar
                TopActionBar(
                    isChinese = isZh,
                    isPlaying = uiState.isPlaying,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onRandomize = { viewModel.randomize() },
                    onReset = { viewModel.resetToPreset() },
                    onExport = { viewModel.setShowExportDialog(true) },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleFullscreen = { viewModel.toggleFullscreen() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Presets Carousel
                PresetsCarousel(
                    presets = OrbPresets.all,
                    selectedPresetId = uiState.selectedPresetId,
                    isChinese = isZh,
                    onSelect = { viewModel.selectPreset(it) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Controls Panel
                BottomControlsCard(
                    viewModel = viewModel,
                    uiState = uiState,
                    isChinese = isZh,
                    onMicRequest = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }
        }

        // 3. Floating return button in fullscreen mode
        if (uiState.isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0x99111424),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable { viewModel.toggleFullscreen() }
                        .testTag("exit_fullscreen_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isZh) "退出全屏" else "Exit",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Interaction hint
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x66000000),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = if (isZh) "滑动旋转光球 • 点击右上方恢复面板" else "Drag to rotate • Tap top right for controls",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 4. Color Picker Modal
        if (uiState.showColorPickerFor != null) {
            val paramName = uiState.showColorPickerFor!!
            val initialColor = when (paramName) {
                "color1" -> uiState.config.color1
                "color2" -> uiState.config.color2
                "accent" -> uiState.config.accentColor
                "core" -> uiState.config.coreColor
                "glow" -> uiState.config.glowColor
                else -> uiState.config.color1
            }
            ColorPickerSheet(
                targetName = paramName,
                initialColor = initialColor,
                isChinese = isZh,
                onDismiss = { viewModel.setShowColorPicker(null) },
                onColorSelected = { newColor ->
                    viewModel.updateColor(paramName, newColor)
                }
            )
        }

        // 5. Export Code Modal
        if (uiState.showExportDialog) {
            ExportDialog(
                config = uiState.config,
                isChinese = isZh,
                onDismiss = { viewModel.setShowExportDialog(false) }
            )
        }
    }
}

@Composable
private fun TopActionBar(
    isChinese: Boolean,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRandomize: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xB3101326),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Title & Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFFEC4899)))
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isChinese) "Liquid Orb 编辑器" else "Liquid Orb Editor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }

            // Quick Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play / Pause
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp).testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Pause",
                        tint = if (isPlaying) Color(0xFF67E8F9) else Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Randomize
                IconButton(
                    onClick = onRandomize,
                    modifier = Modifier.size(36.dp).testTag("randomize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Randomize",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Reset
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(36.dp).testTag("reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Export code
                IconButton(
                    onClick = onExport,
                    modifier = Modifier.size(36.dp).testTag("export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Export Code",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Language toggle
                IconButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.size(36.dp).testTag("language_toggle_button")
                ) {
                    Text(
                        text = if (isChinese) "EN" else "中",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Fullscreen
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(36.dp).testTag("fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetsCarousel(
    presets: List<OrbConfig>,
    selectedPresetId: String,
    isChinese: Boolean,
    onSelect: (OrbConfig) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("presets_carousel"),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { preset ->
            val isSelected = preset.presetId == selectedPresetId
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Color(0xD9222642) else Color(0x8013172E),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
                modifier = Modifier
                    .clickable { onSelect(preset) }
                    .testTag("preset_${preset.presetId}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Two-tone mini orb preview dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(preset.color1.toColor(), preset.color2.toColor())
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isChinese) preset.nameZh else preset.nameEn,
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomControlsCard(
    viewModel: OrbViewModel,
    uiState: OrbUiState,
    isChinese: Boolean,
    onMicRequest: () -> Unit
) {
    val cfg = uiState.config

    val tabTitlesEn = listOf("Shape", "Optics", "Colors", "Sound & Scene")
    val tabTitlesZh = listOf("形状与动态", "玻璃与光学", "色彩与辉光", "声控与场景")
    val tabTitles = if (isChinese) tabTitlesZh else tabTitlesEn

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE6101326)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 340.dp)
            .testTag("bottom_controls_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.activeTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF38BDF8),
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab]),
                        color = Color(0xFF38BDF8),
                        height = 3.dp
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.activeTab == index,
                        onClick = { viewModel.setActiveTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (uiState.activeTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.activeTab == index) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }

            // Tab Content
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .verticalScroll(scrollState)
            ) {
                when (uiState.activeTab) {
                    0 -> ShapeTabContent(cfg, viewModel, isChinese)
                    1 -> OpticsTabContent(cfg, viewModel, isChinese)
                    2 -> ColorsTabContent(cfg, viewModel, isChinese)
                    3 -> AudioSceneTabContent(cfg, uiState, viewModel, isChinese, onMicRequest)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ShapeTabContent(
    cfg: OrbConfig,
    viewModel: OrbViewModel,
    isZh: Boolean
) {
    OrbSliderRow(
        title = if (isZh) "流体流动速度" else "Animation Speed",
        value = cfg.speed,
        range = 0.1f..3.0f,
        onValueChange = { viewModel.updateSpeed(it) },
        testTag = "slider_speed"
    )

    OrbSliderRow(
        title = if (isZh) "球体基础半径" else "Base Radius",
        value = cfg.radius,
        range = 0.3f..1.1f,
        onValueChange = { viewModel.updateRadius(it) },
        testTag = "slider_radius"
    )

    OrbSliderRow(
        title = if (isZh) "内部噪波缩放" else "Noise Zoom",
        value = cfg.zoom,
        range = 0.5f..2.5f,
        onValueChange = { viewModel.updateZoom(it) },
        testTag = "slider_zoom"
    )

    OrbSliderRow(
        title = if (isZh) "液态扭曲扰动" else "Liquid Warp",
        value = cfg.warp,
        range = 0.0f..3.0f,
        onValueChange = { viewModel.updateWarp(it) },
        testTag = "slider_warp"
    )

    OrbSliderRow(
        title = if (isZh) "折叠波纹脊线" else "Ridge Amount",
        value = cfg.ridgeAmt,
        range = 0.0f..4.0f,
        onValueChange = { viewModel.updateRidgeAmt(it) },
        testTag = "slider_ridge"
    )

    OrbSliderRow(
        title = if (isZh) "光弧锐度指数" else "Rim Sharpness",
        value = cfg.sharp,
        range = 0.5f..6.0f,
        onValueChange = { viewModel.updateSharp(it) },
        testTag = "slider_sharp"
    )
}

@Composable
private fun OpticsTabContent(
    cfg: OrbConfig,
    viewModel: OrbViewModel,
    isZh: Boolean
) {
    OrbSliderRow(
        title = if (isZh) "玻璃折射率 (IOR)" else "Glass Refraction (IOR)",
        value = cfg.refraction,
        range = 0.0f..2.5f,
        onValueChange = { viewModel.updateRefraction(it) },
        testTag = "slider_refraction"
    )

    OrbSliderRow(
        title = if (isZh) "色散色彩分离 (Dispersion)" else "Chromatic Dispersion",
        value = cfg.dispersion,
        range = 0.0f..1.5f,
        onValueChange = { viewModel.updateDispersion(it) },
        testTag = "slider_dispersion"
    )

    OrbSliderRow(
        title = if (isZh) "外轮廓发光强度 (Glow)" else "Outer Glow Intensity",
        value = cfg.glow,
        range = 0.0f..3.0f,
        onValueChange = { viewModel.updateGlow(it) },
        testTag = "slider_glow"
    )

    OrbSliderRow(
        title = if (isZh) "边缘羽化平滑 (Softness)" else "Edge Softness",
        value = cfg.edgeSoftness,
        range = 0.01f..0.25f,
        onValueChange = { viewModel.updateEdgeSoftness(it) },
        testTag = "slider_edge_softness"
    )

    OrbSliderRow(
        title = if (isZh) "表面镜面光泽 (Gloss)" else "Specular Gloss",
        value = cfg.gloss,
        range = 0.5f..10.0f,
        onValueChange = { viewModel.updateGloss(it) },
        testTag = "slider_gloss"
    )

    OrbSliderRow(
        title = if (isZh) "高光辉光能量 (Sheen)" else "Highlight Sheen",
        value = cfg.sheen,
        range = 0.0f..3.0f,
        onValueChange = { viewModel.updateSheen(it) },
        testTag = "slider_sheen"
    )

    OrbSliderRow(
        title = if (isZh) "菲涅尔阴影遮罩 (Shade)" else "Fresnel Shade",
        value = cfg.shade,
        range = 0.0f..2.0f,
        onValueChange = { viewModel.updateShade(it) },
        testTag = "slider_shade"
    )
}

@Composable
private fun ColorsTabContent(
    cfg: OrbConfig,
    viewModel: OrbViewModel,
    isZh: Boolean
) {
    Text(
        text = if (isZh) "点击色彩卡片进行精确调色" else "Tap a color chip to fine-tune RGB",
        color = Color(0xFF94A3B8),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ColorItem(
            name = if (isZh) "渐变色 1" else "Color 1",
            color = cfg.color1.toColor(),
            hex = cfg.color1.toHex(),
            onClick = { viewModel.setShowColorPicker("color1") }
        )
        ColorItem(
            name = if (isZh) "渐变色 2" else "Color 2",
            color = cfg.color2.toColor(),
            hex = cfg.color2.toHex(),
            onClick = { viewModel.setShowColorPicker("color2") }
        )
        ColorItem(
            name = if (isZh) "高光色" else "Accent",
            color = cfg.accentColor.toColor(),
            hex = cfg.accentColor.toHex(),
            onClick = { viewModel.setShowColorPicker("accent") }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ColorItem(
            name = if (isZh) "暗部核心" else "Core",
            color = cfg.coreColor.toColor(),
            hex = cfg.coreColor.toHex(),
            onClick = { viewModel.setShowColorPicker("core") }
        )
        ColorItem(
            name = if (isZh) "外发光色" else "Glow",
            color = cfg.glowColor.toColor(),
            hex = cfg.glowColor.toHex(),
            onClick = { viewModel.setShowColorPicker("glow") }
        )
        Box(modifier = Modifier.weight(1f)) // placeholder for 3-column balance
    }
}

@Composable
private fun ColorItem(
    name: String,
    color: Color,
    hex: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = hex,
            color = Color(0xFF67E8F9),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun AudioSceneTabContent(
    cfg: OrbConfig,
    uiState: OrbUiState,
    viewModel: OrbViewModel,
    isZh: Boolean,
    onMicRequest: () -> Unit
) {
    Text(
        text = if (isZh) "音频响应驱动 (Audio Reactivity)" else "Audio Reactivity Engine",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Audio Mode Selection Chips
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AudioModeChip(
            label = if (isZh) "关闭" else "Off",
            icon = Icons.Default.MicOff,
            isSelected = uiState.audioMode == AudioMode.OFF,
            onClick = { viewModel.setAudioMode(AudioMode.OFF) }
        )
        AudioModeChip(
            label = if (isZh) "节奏律动 (自动)" else "Beat (Auto)",
            icon = Icons.Default.MusicNote,
            isSelected = uiState.audioMode == AudioMode.SIMULATOR,
            onClick = { viewModel.setAudioMode(AudioMode.SIMULATOR) }
        )
        AudioModeChip(
            label = if (isZh) "麦克风 (实时)" else "Mic (Live)",
            icon = Icons.Default.Mic,
            isSelected = uiState.audioMode == AudioMode.MICROPHONE,
            onClick = onMicRequest
        )
    }

    if (uiState.audioMode != AudioMode.OFF) {
        Spacer(modifier = Modifier.height(14.dp))

        // Real-time audio band energy monitors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16192E))
                .padding(12.dp)
        ) {
            AudioBandMeter(
                label = if (isZh) "低频 (重音/膨胀)" else "Bass (Pulsate)",
                energy = viewModel.audioEngine.bass,
                color = Color(0xFFEC4899)
            )
            Spacer(modifier = Modifier.height(6.dp))
            AudioBandMeter(
                label = if (isZh) "中频 (人声/纹理)" else "Mid (Ridges)",
                energy = viewModel.audioEngine.mid,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            AudioBandMeter(
                label = if (isZh) "高频 (泛光/闪烁)" else "Treble (Sparkle)",
                energy = viewModel.audioEngine.treble,
                color = Color(0xFFA855F7)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OrbSliderRow(
            title = if (isZh) "音频总灵敏度" else "Sensitivity",
            value = cfg.audioSensitivity,
            range = 0.2f..2.5f,
            onValueChange = { viewModel.updateAudioSensitivity(it) }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Scene Environment Selector
    Text(
        text = if (isZh) "预览背景环境 (Scene Context)" else "Preview Scene Context",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SceneBackground.values().forEach { bg ->
            val isSelected = uiState.sceneBg == bg
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color(0xFF16192E),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                modifier = Modifier.clickable { viewModel.setSceneBackground(bg) }
            ) {
                Text(
                    text = if (isZh) bg.labelZh else bg.labelEn,
                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF16192E),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AudioBandMeter(
    label: String,
    energy: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            modifier = Modifier.width(110.dp)
        )
        LinearProgressIndicator(
            progress = { energy.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF0B0D18),
        )
    }
}
