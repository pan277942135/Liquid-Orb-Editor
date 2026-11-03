package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Vec3Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorPickerSheet(
    targetName: String,
    initialColor: Vec3Color,
    isChinese: Boolean,
    onDismiss: () -> Unit,
    onColorSelected: (Vec3Color) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var r by remember(initialColor) { mutableFloatStateOf(initialColor.r) }
    var g by remember(initialColor) { mutableFloatStateOf(initialColor.g) }
    var b by remember(initialColor) { mutableFloatStateOf(initialColor.b) }

    val currentColor = Vec3Color(r, g, b)

    val swatches = listOf(
        Vec3Color(0.04f, 0.76f, 0.95f), // Cyan
        Vec3Color(0.96f, 0.22f, 0.58f), // Pink
        Vec3Color(0.62f, 0.20f, 0.95f), // Purple
        Vec3Color(0.12f, 0.92f, 0.58f), // Aurora Green
        Vec3Color(0.98f, 0.35f, 0.10f), // Amber Orange
        Vec3Color(0.95f, 0.85f, 0.15f), // Gold
        Vec3Color(0.24f, 0.45f, 0.95f), // Electric Blue
        Vec3Color(0.85f, 0.88f, 0.95f), // Silver
        Vec3Color(0.98f, 0.98f, 1.00f), // Pure Bright
        Vec3Color(0.03f, 0.02f, 0.14f), // Dark Midnight
        Vec3Color(0.08f, 0.01f, 0.20f), // Dark Violet
        Vec3Color(0.01f, 0.03f, 0.10f)  // Abyss
    )

    val titleMapEn = mapOf(
        "color1" to "Primary Gradient Color 1",
        "color2" to "Secondary Gradient Color 2",
        "accent" to "Accent & Highlight Color",
        "core" to "Internal Core & Shadow",
        "glow" to "Outer Rim & Bloom Glow"
    )

    val titleMapZh = mapOf(
        "color1" to "主要渐变色 1",
        "color2" to "次要渐变色 2",
        "accent" to "高光强调色",
        "core" to "内核心与暗部色",
        "glow" to "外轮廓发光色"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131525)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (isChinese) titleMapZh[targetName] ?: "颜色微调" else titleMapEn[targetName] ?: "Color Tuning",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preview box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1E35))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(currentColor.toColor())
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = currentColor.toHex(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RGB: (${(r * 255).toInt()}, ${(g * 255).toInt()}, ${(b * 255).toInt()})",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick swatches
            Text(
                text = if (isChinese) "快速推荐色卡" else "Quick Swatches",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                swatches.forEach { s ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(s.toColor())
                            .border(
                                width = if (s.toHex() == currentColor.toHex()) 2.dp else 1.dp,
                                color = if (s.toHex() == currentColor.toHex()) Color.White else Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                            .clickable {
                                r = s.r
                                g = s.g
                                b = s.b
                                onColorSelected(s)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Red slider
            ColorChannelSlider(
                label = "Red (R)",
                value = r,
                color = Color(0xFFEF4444),
                onValueChange = {
                    r = it
                    onColorSelected(Vec3Color(r, g, b))
                }
            )

            // Green slider
            ColorChannelSlider(
                label = "Green (G)",
                value = g,
                color = Color(0xFF10B981),
                onValueChange = {
                    g = it
                    onColorSelected(Vec3Color(r, g, b))
                }
            )

            // Blue slider
            ColorChannelSlider(
                label = "Blue (B)",
                value = b,
                color = Color(0xFF3B82F6),
                onValueChange = {
                    b = it
                    onColorSelected(Vec3Color(r, g, b))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_color_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text(
                    text = if (isChinese) "完成" else "Done",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier.width(68.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF1E2238)
            )
        )
        Text(
            text = (value * 255).toInt().toString(),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp)
        )
    }
}
