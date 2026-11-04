package com.example.aiorb.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiorb.ribbon.DualRimRibbonConfig
import com.example.aiorb.ui.AIOrbUiState
import com.example.aiorb.ui.AIOrbViewModel
import java.util.Locale

@Composable
fun DualRimRibbonControlSection(
    uiState: AIOrbUiState,
    viewModel: AIOrbViewModel,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.ribbonConfig

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9913192B),
        border = BorderStroke(1.dp, Color(0x4410B981)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Title & Pipeline Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "3D 实体带状双侧高光水草生态",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7)
                    )
                }

                // Pipeline mode switcher toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.useDualRimRibbons) Color(0xFF059669) else Color(0xFF334155),
                    border = BorderStroke(1.dp, if (uiState.useDualRimRibbons) Color(0xFF34D399) else Color(0x44FFFFFF)),
                    modifier = Modifier
                        .clickable { viewModel.toggleRibbonPipeline(!uiState.useDualRimRibbons) }
                        .testTag("toggle_ribbon_pipeline_button")
                ) {
                    Text(
                        text = if (uiState.useDualRimRibbons) "管线: 3D实体双侧高光" else "管线: GL细线着色器",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5 Multi-spectral Ribbons visual indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val styles = DualRimRibbonConfig.DEFAULT_STYLES
                for ((idx, s) in styles.withIndex()) {
                    val isActive = idx < cfg.filamentCount
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isActive) Color(0x33000000) else Color(0x221E293B),
                        border = BorderStroke(1.dp, if (isActive) s.edgeHighlight.copy(alpha = 0.6f) else Color(0x22FFFFFF)),
                        modifier = Modifier.clickable {
                            viewModel.updateFilamentCount(if (cfg.filamentCount == idx + 1) idx else idx + 1)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(s.bodyColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${idx}: ${s.nameZh}",
                                fontSize = 9.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider 1: 水草实体带基准宽度 (seagrassRibbonWidth: 4.0 .. 30.0)
            RibbonSliderRow(
                label = "水草实体带宽度 (Ribbon Width)",
                value = cfg.seagrassRibbonWidth,
                range = 4.0f..30.0f,
                display = "${String.format(Locale.US, "%.1f", cfg.seagrassRibbonWidth)}dp",
                tag = "seagrass_width",
                onValueChange = { viewModel.updateSeagrassWidth(it) }
            )

            // Slider 2: 水流柔顺摆动幅度 (seagrassWaveAmount: 0.0 .. 2.0)
            RibbonSliderRow(
                label = "水流柔顺摆动幅度 (Wave Oscillation)",
                value = cfg.seagrassWaveAmount,
                range = 0.0f..2.0f,
                display = String.format(Locale.US, "%.2f", cfg.seagrassWaveAmount),
                tag = "seagrass_wave",
                onValueChange = { viewModel.updateSeagrassWave(it) }
            )

            // Slider 3: 三维螺旋扭曲缠绕 (filamentTwist: 0.5 .. 5.0)
            RibbonSliderRow(
                label = "螺旋缠绕扭曲度 (Twist & Spin)",
                value = cfg.filamentTwist,
                range = 0.5f..5.0f,
                display = "${String.format(Locale.US, "%.1f", cfg.filamentTwist)}圈",
                tag = "filament_twist",
                onValueChange = { viewModel.updateFilamentTwist(it) }
            )

            // Slider 4: 外围轨道半径比 (filamentRadiusRatio: 1.05 .. 1.45)
            RibbonSliderRow(
                label = "丝带轨道半径比 (Orbit Radius Ratio)",
                value = cfg.filamentRadiusRatio,
                range = 1.05f..1.45f,
                display = "${String.format(Locale.US, "%.2f", cfg.filamentRadiusRatio)}x",
                tag = "filament_radius",
                onValueChange = { viewModel.updateFilamentRadiusRatio(it) }
            )

            // Slider 5: 中央纯白发光核粗细 (filamentThickness: 1.0 .. 8.0)
            RibbonSliderRow(
                label = "中央纯白发光核细线粗细 (Core Thread)",
                value = cfg.filamentThickness,
                range = 1.0f..8.0f,
                display = "${String.format(Locale.US, "%.1f", cfg.filamentThickness)}px",
                tag = "filament_thickness",
                onValueChange = { viewModel.updateFilamentThickness(it) }
            )

            // Slider 6: 涌动流速 (filamentOrbitSpeed: 0.1 .. 3.0)
            RibbonSliderRow(
                label = "丝带公转与水流涌动速度 (Speed)",
                value = cfg.filamentOrbitSpeed,
                range = 0.1f..3.0f,
                display = "${String.format(Locale.US, "%.2f", cfg.filamentOrbitSpeed)}x",
                tag = "filament_speed",
                onValueChange = { viewModel.updateFilamentOrbitSpeed(it) }
            )
        }
    }
}

@Composable
private fun RibbonSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    tag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1)
            )
            Text(
                text = display,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF34D399)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF34D399),
                activeTrackColor = Color(0xFF10B981),
                inactiveTrackColor = Color(0x33334155)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag("slider_$tag")
        )
    }
}
