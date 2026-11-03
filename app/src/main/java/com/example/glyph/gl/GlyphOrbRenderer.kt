package com.example.glyph.gl

import android.content.Context
import android.opengl.GLES20
import android.os.SystemClock
import com.example.glyph.model.GlyphKeyframe
import com.example.glyph.model.GlyphMorphTimeline
import com.example.glyph.model.GlyphTimelineState
import com.example.glyph.model.SpeciesGlyphSpec
import com.example.glyph.model.TimelinePlayMode
import com.example.glyph.model.YujianGlyphMaterialProfile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 2.0 Renderer for the Yujian Water Ink Dynamic Chinese Species Glyph system.
 */
class GlyphOrbRenderer(private val context: Context) {

    // Shaders & Program
    private var programId: Int = 0
    private var aPositionLoc: Int = -1

    // Uniform locations
    private var uResolutionLoc: Int = -1
    private var uTimeLoc: Int = -1
    private var uOrbScaleLoc: Int = -1

    private var uProgressLoc: Int = -1
    private var uStretchXLoc: Int = -1
    private var uStretchYLoc: Int = -1
    private var uCoreWarpLoc: Int = -1
    private var uRibbonIntensityLoc: Int = -1
    private var uRibbonDispersionLoc: Int = -1
    private var uGlyphAlphaLoc: Int = -1
    private var uFluidErosionLoc: Int = -1
    private var uCrystalRefractionLoc: Int = -1
    private var uSpecularGlossLoc: Int = -1
    private var uInkOpacityLoc: Int = -1
    private var uGoldFilamentAlphaLoc: Int = -1

    private var uLakeInkLoc: Int = -1
    private var uLakeTealLoc: Int = -1
    private var uWaterCyanLoc: Int = -1
    private var uDeepWaterLoc: Int = -1
    private var uWarmGoldLoc: Int = -1
    private var uSunGlintLoc: Int = -1

    private var uGlyphMaskTextureLoc: Int = -1
    private var uRotationLoc: Int = -1
    private var uTouchLoc: Int = -1
    private var uTouchActiveLoc: Int = -1

    // Fullscreen quad buffer
    private val quadBuffer: FloatBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(floatArrayOf(
                -1f, -1f,
                 1f, -1f,
                -1f,  1f,
                 1f,  1f
            ))
            position(0)
        }
    }

    // Viewport
    private var width: Int = 1080
    private var height: Int = 1920

    // Text & Texture generator
    val glyphMaskTexture = GlyphMaskTexture()
    var currentSpec: SpeciesGlyphSpec = SpeciesGlyphSpec()
        private set
    var currentProfile: YujianGlyphMaterialProfile = YujianGlyphMaterialProfile.createDefault()

    // Timeline & Playback control
    var playMode: TimelinePlayMode = TimelinePlayMode.AUTO
    var manualKeyframe: GlyphKeyframe? = null
    var scrubProgress: Float = 0f
    var orbSizeScale: Float = 0.5f // Default 0.5x matching requirement

    private var animStartTime: Long = SystemClock.elapsedRealtime()
    private var pausedElapsedMs: Long = 0L
    var isPaused: Boolean = false
        private set

    // Touch interaction
    var rotationX: Float = 0f
    var rotationY: Float = 0f
    var touchX: Float = 0.5f
    var touchY: Float = 0.5f
    var isTouching: Boolean = false

    init {
        // Generate initial default bitmap
        regenerateGlyphMask(currentSpec)
    }

    /**
     * Updates the species specification and queues bitmap mask regeneration.
     */
    fun updateSpecies(newSpec: SpeciesGlyphSpec) {
        currentSpec = newSpec
        currentProfile = currentProfile.copy(
            speciesName = newSpec.speciesName,
            scientificName = newSpec.scientificName,
            fontOption = newSpec.fontOption
        )
        regenerateGlyphMask(newSpec)
    }

    private fun regenerateGlyphMask(spec: SpeciesGlyphSpec) {
        val bitmap = SpeciesGlyphMaskGenerator.generateMaskBitmap(context, spec)
        glyphMaskTexture.updateBitmap(bitmap)
    }

    /**
     * Advances or evaluates current timeline state.
     */
    fun evaluateCurrentState(): GlyphTimelineState {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (isPaused) {
            pausedElapsedMs
        } else {
            now - animStartTime
        }

        return GlyphMorphTimeline.evaluate(
            elapsedMs = elapsed,
            totalDurationMs = GlyphMorphTimeline.TOTAL_DURATION_MS,
            mode = playMode,
            manualKeyframe = manualKeyframe,
            scrubProgress = scrubProgress
        )
    }

    fun stepToKeyframe(keyframe: GlyphKeyframe) {
        playMode = TimelinePlayMode.MANUAL
        manualKeyframe = keyframe
        scrubProgress = keyframe.normalizedProgress
        pausedElapsedMs = keyframe.targetTimeMs
        isPaused = true
    }

    fun setScrub(progress: Float) {
        playMode = TimelinePlayMode.SCRUB
        scrubProgress = progress.coerceIn(0f, 1f)
        pausedElapsedMs = (scrubProgress * GlyphMorphTimeline.TOTAL_DURATION_MS).toLong()
        manualKeyframe = GlyphKeyframe.fromProgress(scrubProgress)
    }

    fun play() {
        if (playMode == TimelinePlayMode.MANUAL || playMode == TimelinePlayMode.SCRUB) {
            playMode = TimelinePlayMode.AUTO
        }
        if (isPaused) {
            animStartTime = SystemClock.elapsedRealtime() - pausedElapsedMs
            isPaused = false
        }
    }

    fun pause() {
        if (!isPaused) {
            pausedElapsedMs = SystemClock.elapsedRealtime() - animStartTime
            isPaused = true
        }
    }

    fun replay() {
        animStartTime = SystemClock.elapsedRealtime()
        pausedElapsedMs = 0L
        playMode = TimelinePlayMode.AUTO
        manualKeyframe = null
        scrubProgress = 0f
        isPaused = false
    }

    fun reset() {
        replay()
    }

    fun onSurfaceCreated() {
        programId = createProgram(YujianWaterInkShader.VERTEX_SHADER, YujianWaterInkShader.FRAGMENT_SHADER)
        if (programId == 0) return

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_position")

        uResolutionLoc = GLES20.glGetUniformLocation(programId, "u_resolution")
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_time")
        uOrbScaleLoc = GLES20.glGetUniformLocation(programId, "u_orbScale")

        uProgressLoc = GLES20.glGetUniformLocation(programId, "u_progress")
        uStretchXLoc = GLES20.glGetUniformLocation(programId, "u_stretchX")
        uStretchYLoc = GLES20.glGetUniformLocation(programId, "u_stretchY")
        uCoreWarpLoc = GLES20.glGetUniformLocation(programId, "u_coreWarp")
        uRibbonIntensityLoc = GLES20.glGetUniformLocation(programId, "u_ribbonIntensity")
        uRibbonDispersionLoc = GLES20.glGetUniformLocation(programId, "u_ribbonDispersion")
        uGlyphAlphaLoc = GLES20.glGetUniformLocation(programId, "u_glyphAlpha")
        uFluidErosionLoc = GLES20.glGetUniformLocation(programId, "u_fluidErosion")
        uCrystalRefractionLoc = GLES20.glGetUniformLocation(programId, "u_crystalRefraction")
        uSpecularGlossLoc = GLES20.glGetUniformLocation(programId, "u_specularGloss")
        uInkOpacityLoc = GLES20.glGetUniformLocation(programId, "u_inkOpacity")
        uGoldFilamentAlphaLoc = GLES20.glGetUniformLocation(programId, "u_goldFilamentAlpha")

        uLakeInkLoc = GLES20.glGetUniformLocation(programId, "u_lakeInk")
        uLakeTealLoc = GLES20.glGetUniformLocation(programId, "u_lakeTeal")
        uWaterCyanLoc = GLES20.glGetUniformLocation(programId, "u_waterCyan")
        uDeepWaterLoc = GLES20.glGetUniformLocation(programId, "u_deepWater")
        uWarmGoldLoc = GLES20.glGetUniformLocation(programId, "u_warmGold")
        uSunGlintLoc = GLES20.glGetUniformLocation(programId, "u_sunGlint")

        uGlyphMaskTextureLoc = GLES20.glGetUniformLocation(programId, "u_glyphMaskTexture")
        uRotationLoc = GLES20.glGetUniformLocation(programId, "u_rotation")
        uTouchLoc = GLES20.glGetUniformLocation(programId, "u_touch")
        uTouchActiveLoc = GLES20.glGetUniformLocation(programId, "u_touchActive")

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun onSurfaceChanged(w: Int, h: Int) {
        width = w
        height = h
        GLES20.glViewport(0, 0, w, h)
    }

    fun onDrawFrame() {
        GLES20.glClearColor(0.027f, 0.035f, 0.075f, 1.0f) // #070913 deep cosmic
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (programId == 0) return

        val state = evaluateCurrentState()

        GLES20.glUseProgram(programId)

        // Bind multi-channel glyph mask
        glyphMaskTexture.bindOrUpload(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(uGlyphMaskTextureLoc, 0)

        // Viewport & time uniforms
        GLES20.glUniform2f(uResolutionLoc, width.toFloat(), height.toFloat())
        val timeSec = (SystemClock.elapsedRealtime() % 1000000L) / 1000f
        GLES20.glUniform1f(uTimeLoc, timeSec)
        GLES20.glUniform1f(uOrbScaleLoc, orbSizeScale * state.ambientBreathScale)

        // Morphing timeline parameters
        GLES20.glUniform1f(uProgressLoc, state.progress)
        GLES20.glUniform1f(uStretchXLoc, state.coreStretchX)
        GLES20.glUniform1f(uStretchYLoc, state.coreStretchY)
        GLES20.glUniform1f(uCoreWarpLoc, state.coreWarpDistortion * currentProfile.fluidTurbulence)
        GLES20.glUniform1f(uRibbonIntensityLoc, state.ribbonIntensity * currentProfile.ribbonIntensity)
        GLES20.glUniform1f(uRibbonDispersionLoc, state.ribbonDispersion)
        GLES20.glUniform1f(uGlyphAlphaLoc, state.glyphAlpha)
        GLES20.glUniform1f(uFluidErosionLoc, state.glyphFluidErosion)
        GLES20.glUniform1f(uCrystalRefractionLoc, state.glyphRefraction * (currentProfile.crystalRefraction / 1.62f))
        GLES20.glUniform1f(uSpecularGlossLoc, state.glyphGloss * (currentProfile.specularGloss / 4.5f))
        GLES20.glUniform1f(uInkOpacityLoc, state.inkOpacity * currentProfile.inkOpacity)
        GLES20.glUniform1f(uGoldFilamentAlphaLoc, state.goldFilamentAlpha * currentProfile.goldFilamentAlpha)

        // Palette Colors
        val p = currentProfile
        GLES20.glUniform3f(uLakeInkLoc, p.lakeInkColor.r, p.lakeInkColor.g, p.lakeInkColor.b)
        GLES20.glUniform3f(uLakeTealLoc, p.lakeTealColor.r, p.lakeTealColor.g, p.lakeTealColor.b)
        GLES20.glUniform3f(uWaterCyanLoc, p.waterCyanColor.r, p.waterCyanColor.g, p.waterCyanColor.b)
        GLES20.glUniform3f(uDeepWaterLoc, p.deepWaterColor.r, p.deepWaterColor.g, p.deepWaterColor.b)
        GLES20.glUniform3f(uWarmGoldLoc, p.warmGoldColor.r, p.warmGoldColor.g, p.warmGoldColor.b)
        GLES20.glUniform3f(uSunGlintLoc, p.sunGlintColor.r, p.sunGlintColor.g, p.sunGlintColor.b)

        // Touch & Rotation
        GLES20.glUniform2f(uRotationLoc, rotationX, rotationY)
        GLES20.glUniform2f(uTouchLoc, touchX, touchY)
        GLES20.glUniform1f(uTouchActiveLoc, if (isTouching) 1f else 0f)

        // Draw quad
        quadBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
    }

    fun release() {
        glyphMaskTexture.release()
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compilation error: $error")
        }
        return shader
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program link error: $error")
        }
        return program
    }
}
