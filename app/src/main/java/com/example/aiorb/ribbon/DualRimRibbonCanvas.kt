package com.example.aiorb.ribbon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.aiorb.gl.OrbCoordinateMapper
import com.example.aiorb.model.OrbLifeState
import com.example.aiorb.model.OrbSceneFrame
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Compose Canvas Host for 3D Dual-Rim Specular Ribbons (水草与流体 3D 实体带状双侧高光).
 *
 * Can be rendered either as Back Pass (behind the glass sphere) or Front Pass (in front of the glass sphere).
 */
@Composable
fun DualRimRibbonCanvas(
    isBackPass: Boolean,
    frame: OrbSceneFrame,
    config: DualRimRibbonConfig,
    orbSizeScale: Float = 0.5f,
    touchRotationOffset: Float = 0.0f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val minDim = min(size.width, size.height)
        if (minDim <= 0f) return@Canvas

        // 呼吸震荡与尺寸计算
        val breathAmp = if (frame.lifeState == OrbLifeState.S2_ACTIVE) 0.018f else 0.008f
        val breathPeriod = if (frame.lifeState == OrbLifeState.S2_ACTIVE) 4800.0 else 6000.0
        val breathPhase = 2.0 * PI * (frame.sceneElapsedMs.toDouble() / breathPeriod)
        val currentScale = (1.0f + breathAmp * sin(breathPhase).toFloat()) * orbSizeScale

        val orbRadiusPx = minDim * OrbCoordinateMapper.CORE_VISIBLE_RADIUS_UV * currentScale
        val center = Offset(size.width / 2f, size.height / 2f)
        val elapsedSec = frame.sceneElapsedMs / 1000f

        // 计算总可见度：在 S1 空闲阶段渐隐，在 S2 唤醒阶段渐显
        val alphaProg = if (frame.lifeState == OrbLifeState.S1_IDLE) {
            frame.filamentAlphaProgress.coerceIn(0f, 1f)
        } else {
            1.0f
        }

        DualRimRibbonRenderer.renderRibbons(
            drawScope = this,
            isBackPass = isBackPass,
            center = center,
            orbRadiusPx = orbRadiusPx,
            elapsedSec = elapsedSec,
            config = config,
            yawRad = touchRotationOffset,
            pitchRad = 0f,
            alphaProgress = alphaProg
        )
    }
}
