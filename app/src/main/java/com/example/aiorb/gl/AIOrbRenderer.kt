package com.example.aiorb.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import com.example.aiorb.model.*
import com.example.aiorb.util.AssetHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Preallocated vertex and UV buffer holder for zero per-frame allocation.
 */
class FilamentBufferHolder(
    val path: FilamentPath,
    val maxSubdivisions: Int = 32
) {
    val vertexCount = (maxSubdivisions + 1) * 2

    val posBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertexCount * 2 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    val uvBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertexCount * 2 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
}

class AIOrbRenderer(private val context: Context) {

    val sceneClock = OrbSceneClock()

    // Configurable parameters from debug panel
    var sunlightIntensityMultiplier: Float = 1.0f
    var refractionIntensityMultiplier: Float = 1.0f
    var causticsIntensityMultiplier: Float = 1.0f
    var grassMotionMultiplier: Float = 1.0f
    var dropletVisibilityMultiplier: Float = 1.0f
    var filamentAlphaMultiplier: Float = 1.0f
    var filamentWidthMultiplier: Float = 1.0f
    var orbSizeScale: Float = 0.5f // Default is half size (0.5 = 40dp, 1.0 = 80dp)

    // Fullscreen quad for core rendering
    private val quadVertices = floatArrayOf(
        -1.0f, -1.0f,
         1.0f, -1.0f,
        -1.0f,  1.0f,
         1.0f,  1.0f
    )
    private val quadBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(quadVertices)
            position(0)
        }

    // Shader programs
    private var coreProgramId = 0
    private var filamentProgramId = 0

    // Texture IDs
    private var coreTextureId = 0
    private var grassMaskTextureId = 0

    // Core Uniform locations
    private var uResolutionLoc = -1
    private var uTimeLoc = -1
    private var uScaleLoc = -1
    private var uCoreTexLoc = -1
    private var uGrassMaskTexLoc = -1

    private var uWakeProgressLoc = -1
    private var uFilamentAlphaProgLoc = -1
    private var uReleaseProgressLoc = -1
    private var uIsS2Loc = -1

    private var uGrassDispBackLoc = -1
    private var uGrassDispFrontLoc = -1
    private var uGrassMotionWeightLoc = -1
    private var uGrassLightResponseLoc = -1

    private var uSunlightSourceUvLoc = -1
    private var uSunlightColorLoc = -1
    private var uGlassSpecularAlphaLoc = -1
    private var uCoreRefractionAlphaLoc = -1

    private var uCausticsAlphaLoc = -1
    private var uCausticsDeepTimeLoc = -1
    private var uCausticsSurfaceTimeLoc = -1
    private var uHaloAlphaLoc = -1

    private var uDroplet1UvLoc = -1
    private var uDroplet1AlphaLoc = -1
    private var uDroplet2UvLoc = -1
    private var uDroplet2AlphaLoc = -1
    private var uDroplet3UvLoc = -1
    private var uDroplet3AlphaLoc = -1

    // Filament Uniform & Attrib locations
    private var fPositionLoc = -1
    private var fUvLoc = -1
    private var fResolutionLoc = -1
    private var fScaleLoc = -1
    private var fBaseColorLoc = -1
    private var fHighlightColorLoc = -1
    private var fRefractionColorLoc = -1
    private var fLayerParamsLoc = -1

    // Data structures & Pre-allocated buffer holders (Zero per-frame allocations)
    var filamentPaths: List<FilamentPath> = emptyList()
        private set
    private var backHolders: List<FilamentBufferHolder> = emptyList()
    private var frontHolders: List<FilamentBufferHolder> = emptyList()

    private var grassPaths: Map<String, GrassLeafPath> = emptyMap()

    private var viewWidth = 1
    private var viewHeight = 1

    fun onSurfaceCreated() {
        GLES20.glClearColor(0.027f, 0.035f, 0.075f, 1.0f)
        setupShaders()
        loadTextures()
        filamentPaths = FilamentPath.loadAllFromAssets(context)
        grassPaths = GrassLeafPath.loadFromAssets(context)

        // Preallocate and pre-group holders by BACK/FRONT depth once
        val holders = filamentPaths.map { FilamentBufferHolder(it, 32) }
        backHolders = holders.filter { it.path.depth == FilamentDepth.BACK }
        frontHolders = holders.filter { it.path.depth == FilamentDepth.FRONT }

        sceneClock.start()
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        viewWidth = width.coerceAtLeast(1)
        viewHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
    }

    fun onDrawFrame() {
        val frame = sceneClock.produceFrame()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_BLEND)

        // Compute breath scale (centered uniform scale)
        val breathAmp = if (frame.lifeState == OrbLifeState.S2_ACTIVE) 0.018f else 0.008f
        val breathPeriod = if (frame.lifeState == OrbLifeState.S2_ACTIVE) 4800.0 else 6000.0
        val breathPhase = 2.0 * PI * (frame.sceneElapsedMs.toDouble() / breathPeriod)
        val currentScale = (1.0f + breathAmp * sin(breathPhase).toFloat()) * orbSizeScale

        // 1. Render L03 BACK Filaments (G01 & A01)
        drawFilamentHolders(backHolders, frame, currentScale)

        // 2. Render Core + L01 Halo + L04 Caustics + L05 Grass + L02 Droplets + L07 Sunlight
        drawCore(frame, currentScale)

        // 3. Render L03 FRONT Filaments (G02, A02, C01)
        drawFilamentHolders(frontHolders, frame, currentScale)
    }

    private fun drawCore(frame: OrbSceneFrame, currentScale: Float) {
        if (coreProgramId == 0 || coreTextureId == 0) return

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(coreProgramId)

        // Attributes
        quadBuffer.position(0)
        val aPosLoc = GLES20.glGetAttribLocation(coreProgramId, "a_position")
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)
        GLES20.glEnableVertexAttribArray(aPosLoc)

        // Bind Core Texture (Texture Unit 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, coreTextureId)
        GLES20.glUniform1i(uCoreTexLoc, 0)

        // Bind Grass Mask Texture (Texture Unit 1)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, grassMaskTextureId)
        GLES20.glUniform1i(uGrassMaskTexLoc, 1)

        // Uniforms
        GLES20.glUniform2f(uResolutionLoc, viewWidth.toFloat(), viewHeight.toFloat())
        GLES20.glUniform1f(uTimeLoc, (frame.sceneElapsedMs / 1000f))
        GLES20.glUniform1f(uScaleLoc, currentScale)

        GLES20.glUniform1f(uWakeProgressLoc, frame.wakeProgress)
        GLES20.glUniform1f(uFilamentAlphaProgLoc, frame.filamentAlphaProgress)
        GLES20.glUniform1f(uReleaseProgressLoc, frame.releaseProgress)
        GLES20.glUniform1i(uIsS2Loc, if (frame.lifeState == OrbLifeState.S2_ACTIVE) 1 else 0)

        // Grass optical displacement calculation (Scheme B)
        val b1Disp = grassPaths["BACK_B1"]?.computeDisplacementUV(frame.sceneElapsedMs) ?: Point2D(0f, 0f)
        val f1Disp = grassPaths["FRONT_F1"]?.computeDisplacementUV(frame.sceneElapsedMs) ?: Point2D(0f, 0f)
        GLES20.glUniform2f(uGrassDispBackLoc, b1Disp.x, b1Disp.y)
        GLES20.glUniform2f(uGrassDispFrontLoc, f1Disp.x, f1Disp.y)
        GLES20.glUniform1f(uGrassMotionWeightLoc, grassMotionMultiplier)
        GLES20.glUniform1f(uGrassLightResponseLoc, 0.08f * frame.wakeProgress)

        // Sunlight parameters
        GLES20.glUniform2f(uSunlightSourceUvLoc, SunlightContract.SOURCE_UV.x, SunlightContract.SOURCE_UV.y)
        val sc = SunlightContract.MAIN_COLOR
        GLES20.glUniform3f(uSunlightColorLoc, Color.red(sc) / 255f, Color.green(sc) / 255f, Color.blue(sc) / 255f)
        val specAlpha = SunlightContract.computeGlassSpecularAlpha(frame.sceneElapsedMs, frame.wakeProgress) * sunlightIntensityMultiplier
        val refrAlpha = SunlightContract.computeCoreRefractionAlpha(frame.sceneElapsedMs, frame.wakeProgress) * refractionIntensityMultiplier
        GLES20.glUniform1f(uGlassSpecularAlphaLoc, specAlpha)
        GLES20.glUniform1f(uCoreRefractionAlphaLoc, refrAlpha)

        // Caustics parameters
        val causticsAlpha = CausticsContract.computeCausticsAlpha(frame.sceneElapsedMs, frame.wakeProgress) * causticsIntensityMultiplier
        GLES20.glUniform1f(uCausticsAlphaLoc, causticsAlpha)
        GLES20.glUniform1f(uCausticsDeepTimeLoc, (frame.sceneElapsedMs / 1000f))
        GLES20.glUniform1f(uCausticsSurfaceTimeLoc, (frame.sceneElapsedMs / 1000f))

        // Halo
        val targetHalo = if (frame.lifeState == OrbLifeState.S2_ACTIVE) 0.08f else 0.0f
        GLES20.glUniform1f(uHaloAlphaLoc, targetHalo)

        // Droplets
        val d1 = WaterDroplet.DROBLET_LIST[0]
        val d2 = WaterDroplet.DROBLET_LIST[1]
        val d3 = WaterDroplet.DROBLET_LIST[2]
        val isS2 = frame.lifeState == OrbLifeState.S2_ACTIVE
        val d1Uv = d1.computeCurrentUv(frame.sceneElapsedMs)
        val d2Uv = d2.computeCurrentUv(frame.sceneElapsedMs)
        val d3Uv = d3.computeCurrentUv(frame.sceneElapsedMs)
        GLES20.glUniform2f(uDroplet1UvLoc, d1Uv.x, d1Uv.y)
        GLES20.glUniform1f(uDroplet1AlphaLoc, d1.computeCurrentAlpha(frame.wakeProgress, isS2) * dropletVisibilityMultiplier)
        GLES20.glUniform2f(uDroplet2UvLoc, d2Uv.x, d2Uv.y)
        GLES20.glUniform1f(uDroplet2AlphaLoc, d2.computeCurrentAlpha(frame.wakeProgress, isS2) * dropletVisibilityMultiplier)
        GLES20.glUniform2f(uDroplet3UvLoc, d3Uv.x, d3Uv.y)
        GLES20.glUniform1f(uDroplet3AlphaLoc, d3.computeCurrentAlpha(frame.wakeProgress, isS2) * dropletVisibilityMultiplier)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPosLoc)
    }

    private fun drawFilamentHolders(
        holders: List<FilamentBufferHolder>,
        frame: OrbSceneFrame,
        currentScale: Float
    ) {
        if (filamentProgramId == 0 || holders.isEmpty()) return
        if (frame.filamentAlphaProgress <= 0.01f) return // Filaments hidden in S1

        // Use premultiplied source-over blend for R06 layered water ribbon optics
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(filamentProgramId)

        GLES20.glUniform2f(fResolutionLoc, viewWidth.toFloat(), viewHeight.toFloat())
        GLES20.glUniform1f(fScaleLoc, currentScale)

        val headBoost = when (frame.currentPhaseEvent) {
            OrbPhaseEvent.RELEASE -> 1.18f
            OrbPhaseEvent.RETURN -> 1.07f
            else -> 1.0f
        }
        val alphaMult = (1.28f * frame.filamentAlphaProgress * filamentAlphaMultiplier).coerceIn(0f, 1.4f)

        for (holder in holders) {
            val path = holder.path
            val kin = path.computeKinematics(
                elapsedMs = frame.sceneElapsedMs,
                releaseCycleElapsedMs = frame.releaseCycleElapsedMs,
                phaseEvent = frame.currentPhaseEvent,
                isHold = frame.isHold
            )

            if (kin.headU <= kin.tailU) continue

            // Build ribbon mesh into preallocated buffer without heap allocation
            val subdivisions = holder.maxSubdivisions
            val vertexCount = (subdivisions + 1) * 2

            holder.posBuffer.clear()
            holder.uvBuffer.clear()

            val strokeWidthD = ((path.widthMinDp + path.widthMaxDp) * 0.5f * path.materialConfig.widthMultiplier * filamentWidthMultiplier / 80f)

            for (step in 0..subdivisions) {
                val frac = step.toFloat() / subdivisions
                val s = kin.tailU + (kin.headU - kin.tailU) * frac
                val pt = path.getPointAtArcFraction(s)

                val nextPt = path.getPointAtArcFraction((s + 0.02f).coerceAtMost(1f))
                val tdx = nextPt.x - pt.x
                val tdy = nextPt.y - pt.y
                val tlen = sqrt(tdx * tdx + tdy * tdy).coerceAtLeast(0.0001f)
                val nx = -tdy / tlen * strokeWidthD * 0.5f
                val ny = tdx / tlen * strokeWidthD * 0.5f

                // Vertex 1 (positive transverse normal)
                holder.posBuffer.put(pt.x + nx)
                holder.posBuffer.put(pt.y + ny)
                holder.uvBuffer.put(frac)
                holder.uvBuffer.put(1.0f)

                // Vertex 2 (negative transverse normal)
                holder.posBuffer.put(pt.x - nx)
                holder.posBuffer.put(pt.y - ny)
                holder.uvBuffer.put(frac)
                holder.uvBuffer.put(-1.0f)
            }

            holder.posBuffer.position(0)
            holder.uvBuffer.position(0)

            // Feed R06 material layers
            val mat = path.materialConfig
            val baseC = mat.baseColor
            val highC = mat.highlightColor
            val refrC = mat.refractionColor
            val baseAlphaMax = ((path.alphaMin + path.alphaMax) * 0.5f).coerceIn(0.18f, 0.75f)

            GLES20.glUniform4f(
                fBaseColorLoc,
                Color.red(baseC) / 255f,
                Color.green(baseC) / 255f,
                Color.blue(baseC) / 255f,
                baseAlphaMax
            )
            GLES20.glUniform4f(
                fHighlightColorLoc,
                Color.red(highC) / 255f,
                Color.green(highC) / 255f,
                Color.blue(highC) / 255f,
                0.80f
            )
            GLES20.glUniform4f(
                fRefractionColorLoc,
                Color.red(refrC) / 255f,
                Color.green(refrC) / 255f,
                Color.blue(refrC) / 255f,
                0.40f
            )

            val isGold = if (path.materialType == FilamentMaterialType.WARM_GOLD_FILAMENT) 1.0f else 0.0f
            val specLen = if (isGold > 0.5f) 0.18f else 0.12f
            GLES20.glUniform4f(
                fLayerParamsLoc,
                headBoost,
                alphaMult,
                specLen,
                isGold
            )

            // Bind attributes
            GLES20.glVertexAttribPointer(fPositionLoc, 2, GLES20.GL_FLOAT, false, 0, holder.posBuffer)
            GLES20.glEnableVertexAttribArray(fPositionLoc)

            GLES20.glVertexAttribPointer(fUvLoc, 2, GLES20.GL_FLOAT, false, 0, holder.uvBuffer)
            GLES20.glEnableVertexAttribArray(fUvLoc)

            // Draw triangle strip ribbon
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount)

            GLES20.glDisableVertexAttribArray(fPositionLoc)
            GLES20.glDisableVertexAttribArray(fUvLoc)
        }
    }

    private fun setupShaders() {
        // 1. Core Program
        val cVert = compileShader(GLES20.GL_VERTEX_SHADER, AIOrbShader.VERTEX_SHADER)
        val cFrag = compileShader(GLES20.GL_FRAGMENT_SHADER, AIOrbShader.FRAGMENT_SHADER)
        coreProgramId = createAndLinkProgram(cVert, cFrag)

        if (coreProgramId != 0) {
            uResolutionLoc = GLES20.glGetUniformLocation(coreProgramId, "u_resolution")
            uTimeLoc = GLES20.glGetUniformLocation(coreProgramId, "u_time")
            uScaleLoc = GLES20.glGetUniformLocation(coreProgramId, "u_scale")
            uCoreTexLoc = GLES20.glGetUniformLocation(coreProgramId, "u_coreTexture")
            uGrassMaskTexLoc = GLES20.glGetUniformLocation(coreProgramId, "u_grassMaskTexture")

            uWakeProgressLoc = GLES20.glGetUniformLocation(coreProgramId, "u_wakeProgress")
            uFilamentAlphaProgLoc = GLES20.glGetUniformLocation(coreProgramId, "u_filamentAlphaProgress")
            uReleaseProgressLoc = GLES20.glGetUniformLocation(coreProgramId, "u_releaseProgress")
            uIsS2Loc = GLES20.glGetUniformLocation(coreProgramId, "u_isS2")

            uGrassDispBackLoc = GLES20.glGetUniformLocation(coreProgramId, "u_grassDisplacementBack")
            uGrassDispFrontLoc = GLES20.glGetUniformLocation(coreProgramId, "u_grassDisplacementFront")
            uGrassMotionWeightLoc = GLES20.glGetUniformLocation(coreProgramId, "u_grassMotionWeight")
            uGrassLightResponseLoc = GLES20.glGetUniformLocation(coreProgramId, "u_grassLightResponse")

            uSunlightSourceUvLoc = GLES20.glGetUniformLocation(coreProgramId, "u_sunlightSourceUV")
            uSunlightColorLoc = GLES20.glGetUniformLocation(coreProgramId, "u_sunlightColor")
            uGlassSpecularAlphaLoc = GLES20.glGetUniformLocation(coreProgramId, "u_glassSpecularAlpha")
            uCoreRefractionAlphaLoc = GLES20.glGetUniformLocation(coreProgramId, "u_coreRefractionAlpha")

            uCausticsAlphaLoc = GLES20.glGetUniformLocation(coreProgramId, "u_causticsAlpha")
            uCausticsDeepTimeLoc = GLES20.glGetUniformLocation(coreProgramId, "u_causticsDeepTime")
            uCausticsSurfaceTimeLoc = GLES20.glGetUniformLocation(coreProgramId, "u_causticsSurfaceTime")
            uHaloAlphaLoc = GLES20.glGetUniformLocation(coreProgramId, "u_haloAlpha")

            uDroplet1UvLoc = GLES20.glGetUniformLocation(coreProgramId, "u_droplet1Uv")
            uDroplet1AlphaLoc = GLES20.glGetUniformLocation(coreProgramId, "u_droplet1Alpha")
            uDroplet2UvLoc = GLES20.glGetUniformLocation(coreProgramId, "u_droplet2Uv")
            uDroplet2AlphaLoc = GLES20.glGetUniformLocation(coreProgramId, "u_droplet2Alpha")
            uDroplet3UvLoc = GLES20.glGetUniformLocation(coreProgramId, "u_droplet3Uv")
            uDroplet3AlphaLoc = GLES20.glGetUniformLocation(coreProgramId, "u_droplet3Alpha")
        }

        // 2. Filament Program (with 4-layer R06 optics)
        val fVert = compileShader(GLES20.GL_VERTEX_SHADER, FilamentShader.VERTEX_SHADER)
        val fFrag = compileShader(GLES20.GL_FRAGMENT_SHADER, FilamentShader.FRAGMENT_SHADER)
        filamentProgramId = createAndLinkProgram(fVert, fFrag)

        if (filamentProgramId != 0) {
            fPositionLoc = GLES20.glGetAttribLocation(filamentProgramId, "a_position")
            fUvLoc = GLES20.glGetAttribLocation(filamentProgramId, "a_uv")
            fResolutionLoc = GLES20.glGetUniformLocation(filamentProgramId, "u_resolution")
            fScaleLoc = GLES20.glGetUniformLocation(filamentProgramId, "u_scale")
            fBaseColorLoc = GLES20.glGetUniformLocation(filamentProgramId, "u_baseColor")
            fHighlightColorLoc = GLES20.glGetUniformLocation(filamentProgramId, "u_highlightColor")
            fRefractionColorLoc = GLES20.glGetUniformLocation(filamentProgramId, "u_refractionColor")
            fLayerParamsLoc = GLES20.glGetUniformLocation(filamentProgramId, "u_layerParams")
        }
    }

    private fun loadTextures() {
        // Load Immutable Core Master
        try {
            val coreStream = AssetHelper.openAsset(context, "ai_orb/frozen_v1_1/透明湖水玻璃球.png")
            val coreBitmap = BitmapFactory.decodeStream(coreStream)
            coreTextureId = createTextureFromBitmap(coreBitmap)
            coreBitmap.recycle()
        } catch (e: Exception) {
            Log.e("AIOrbRenderer", "Error loading core texture", e)
        }

        // Load Grass Masks (Combine Back into Red channel, Front into Green channel)
        try {
            val backStream = AssetHelper.openAsset(context, "ai_orb/frozen_v1_1/orb_grass_back_combined_mask_v1_1.png")
            val frontStream = AssetHelper.openAsset(context, "ai_orb/frozen_v1_1/orb_grass_front_combined_mask_v1_1.png")
            val backBm = BitmapFactory.decodeStream(backStream)
            val frontBm = BitmapFactory.decodeStream(frontStream)

            val w = backBm.width
            val h = backBm.height
            val combinedBm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

            val backPixels = IntArray(w * h)
            val frontPixels = IntArray(w * h)
            val combinedPixels = IntArray(w * h)

            backBm.getPixels(backPixels, 0, w, 0, 0, w, h)
            frontBm.getPixels(frontPixels, 0, w, 0, 0, w, h)

            for (i in 0 until w * h) {
                val bAlpha = (backPixels[i] and 0xFF)
                val fAlpha = (frontPixels[i] and 0xFF)
                // Pack Back mask into Red channel, Front mask into Green channel
                combinedPixels[i] = Color.argb(255, bAlpha, fAlpha, 0)
            }

            combinedBm.setPixels(combinedPixels, 0, w, 0, 0, w, h)
            grassMaskTextureId = createTextureFromBitmap(combinedBm)

            backBm.recycle()
            frontBm.recycle()
            combinedBm.recycle()
        } catch (e: Exception) {
            Log.e("AIOrbRenderer", "Error loading grass mask textures", e)
        }
    }

    private fun createTextureFromBitmap(bitmap: Bitmap): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        return textures[0]
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("AIOrbRenderer", "Shader compilation error: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun createAndLinkProgram(vShader: Int, fShader: Int): Int {
        if (vShader == 0 || fShader == 0) return 0
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vShader)
        GLES20.glAttachShader(program, fShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("AIOrbRenderer", "Program link error: " + GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }
}
