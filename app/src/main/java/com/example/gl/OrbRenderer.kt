package com.example.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import com.example.model.OrbConfig
import com.example.model.SceneBackground
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OrbRenderer : GLSurfaceView.Renderer {

    private val quadVertices = floatArrayOf(
        -1.0f, -1.0f,
         1.0f, -1.0f,
        -1.0f,  1.0f,
         1.0f,  1.0f
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(quadVertices)
            position(0)
        }

    private var programId = 0
    private var aPositionLoc = -1

    // Uniform locations
    private var uResolutionLoc = -1
    private var uTimeLoc = -1
    private var uSpeedLoc = -1
    private var uRadiusLoc = -1
    private var uZoomLoc = -1
    private var uWarpLoc = -1
    private var uRidgeAmtLoc = -1
    private var uSharpLoc = -1
    private var uShadeLoc = -1
    private var uSheenLoc = -1
    private var uGlossLoc = -1
    private var uRefractionLoc = -1
    private var uDispersionLoc = -1
    private var uRoughnessLoc = -1
    private var uGlowLoc = -1
    private var uEdgeSoftnessLoc = -1

    private var uColor1Loc = -1
    private var uColor2Loc = -1
    private var uAccentColorLoc = -1
    private var uCoreColorLoc = -1
    private var uGlowColorLoc = -1

    private var uTouchLoc = -1
    private var uTouchActiveLoc = -1
    private var uRotationLoc = -1

    private var uAudioBassLoc = -1
    private var uAudioMidLoc = -1
    private var uAudioTrebleLoc = -1

    private var uSceneBgLoc = -1

    // State variables
    @Volatile
    var config: OrbConfig = OrbConfig()

    @Volatile
    var sceneBg: SceneBackground = SceneBackground.COSMIC_DEEP

    @Volatile
    var isPlaying: Boolean = true

    // Audio frequency bands (0.0 to 1.0)
    @Volatile
    var audioBass: Float = 0f
    @Volatile
    var audioMid: Float = 0f
    @Volatile
    var audioTreble: Float = 0f

    // Touch interaction
    @Volatile
    var touchX: Float = 0f
    @Volatile
    var touchY: Float = 0f
    @Volatile
    var isTouchActive: Boolean = false

    // Drag rotation
    @Volatile
    var rotX: Float = 0f
    @Volatile
    var rotY: Float = 0f

    private var surfaceWidth = 1
    private var surfaceHeight = 1
    private var startTimeMs = SystemClock.uptimeMillis()
    private var accumulatedTime = 0.0f
    private var lastFrameTimeMs = SystemClock.uptimeMillis()

    override fun onSurfaceCreated(gl: GL10?, eglConfig: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.02f, 0.05f, 1.0f)
        setupShaders()
        startTimeMs = SystemClock.uptimeMillis()
        lastFrameTimeMs = startTimeMs
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = SystemClock.uptimeMillis()
        val dt = (now - lastFrameTimeMs) / 1000.0f
        lastFrameTimeMs = now

        if (isPlaying) {
            accumulatedTime += dt
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (programId == 0) return

        GLES20.glUseProgram(programId)

        // Pass Quad Position
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionLoc)

        val cfg = config

        // Uniform values
        GLES20.glUniform2f(uResolutionLoc, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(uTimeLoc, accumulatedTime)
        GLES20.glUniform1f(uSpeedLoc, cfg.speed)
        GLES20.glUniform1f(uRadiusLoc, cfg.radius)
        GLES20.glUniform1f(uZoomLoc, cfg.zoom)
        GLES20.glUniform1f(uWarpLoc, cfg.warp)
        GLES20.glUniform1f(uRidgeAmtLoc, cfg.ridgeAmt)
        GLES20.glUniform1f(uSharpLoc, cfg.sharp)
        GLES20.glUniform1f(uShadeLoc, cfg.shade)
        GLES20.glUniform1f(uSheenLoc, cfg.sheen)
        GLES20.glUniform1f(uGlossLoc, cfg.gloss)
        GLES20.glUniform1f(uRefractionLoc, cfg.refraction)
        GLES20.glUniform1f(uDispersionLoc, cfg.dispersion)
        GLES20.glUniform1f(uRoughnessLoc, cfg.roughness)
        GLES20.glUniform1f(uGlowLoc, cfg.glow)
        GLES20.glUniform1f(uEdgeSoftnessLoc, cfg.edgeSoftness)

        // Colors
        GLES20.glUniform3f(uColor1Loc, cfg.color1.r, cfg.color1.g, cfg.color1.b)
        GLES20.glUniform3f(uColor2Loc, cfg.color2.r, cfg.color2.g, cfg.color2.b)
        GLES20.glUniform3f(uAccentColorLoc, cfg.accentColor.r, cfg.accentColor.g, cfg.accentColor.b)
        GLES20.glUniform3f(uCoreColorLoc, cfg.coreColor.r, cfg.coreColor.g, cfg.coreColor.b)
        GLES20.glUniform3f(uGlowColorLoc, cfg.glowColor.r, cfg.glowColor.g, cfg.glowColor.b)

        // Touch & Rotation
        GLES20.glUniform2f(uTouchLoc, touchX, touchY)
        GLES20.glUniform1f(uTouchActiveLoc, if (isTouchActive) 1.0f else 0.0f)
        GLES20.glUniform2f(uRotationLoc, rotX, rotY)

        // Audio reactivity
        if (cfg.audioReactive) {
            val sens = cfg.audioSensitivity
            GLES20.glUniform1f(uAudioBassLoc, (audioBass * cfg.bassInfluence * sens).coerceIn(0f, 2f))
            GLES20.glUniform1f(uAudioMidLoc, (audioMid * cfg.midInfluence * sens).coerceIn(0f, 2f))
            GLES20.glUniform1f(uAudioTrebleLoc, (audioTreble * cfg.trebleInfluence * sens).coerceIn(0f, 2f))
        } else {
            GLES20.glUniform1f(uAudioBassLoc, 0f)
            GLES20.glUniform1f(uAudioMidLoc, 0f)
            GLES20.glUniform1f(uAudioTrebleLoc, 0f)
        }

        // Scene background
        GLES20.glUniform1i(uSceneBgLoc, sceneBg.ordinal)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
    }

    private fun setupShaders() {
        val vertShader = compileShader(GLES20.GL_VERTEX_SHADER, OrbShader.VERTEX_SHADER)
        val fragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, OrbShader.FRAGMENT_SHADER)

        if (vertShader == 0 || fragShader == 0) {
            Log.e("OrbRenderer", "Error compiling shaders")
            return
        }

        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertShader)
        GLES20.glAttachShader(programId, fragShader)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("OrbRenderer", "Shader link error: " + GLES20.glGetProgramInfoLog(programId))
            GLES20.glDeleteProgram(programId)
            programId = 0
            return
        }

        // Get attribute locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_position")

        // Get uniform locations
        uResolutionLoc = GLES20.glGetUniformLocation(programId, "u_resolution")
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_time")
        uSpeedLoc = GLES20.glGetUniformLocation(programId, "u_speed")
        uRadiusLoc = GLES20.glGetUniformLocation(programId, "u_radius")
        uZoomLoc = GLES20.glGetUniformLocation(programId, "u_zoom")
        uWarpLoc = GLES20.glGetUniformLocation(programId, "u_warp")
        uRidgeAmtLoc = GLES20.glGetUniformLocation(programId, "u_ridgeAmt")
        uSharpLoc = GLES20.glGetUniformLocation(programId, "u_sharp")
        uShadeLoc = GLES20.glGetUniformLocation(programId, "u_shade")
        uSheenLoc = GLES20.glGetUniformLocation(programId, "u_sheen")
        uGlossLoc = GLES20.glGetUniformLocation(programId, "u_gloss")
        uRefractionLoc = GLES20.glGetUniformLocation(programId, "u_refraction")
        uDispersionLoc = GLES20.glGetUniformLocation(programId, "u_dispersion")
        uRoughnessLoc = GLES20.glGetUniformLocation(programId, "u_roughness")
        uGlowLoc = GLES20.glGetUniformLocation(programId, "u_glow")
        uEdgeSoftnessLoc = GLES20.glGetUniformLocation(programId, "u_edgeSoftness")

        uColor1Loc = GLES20.glGetUniformLocation(programId, "u_color1")
        uColor2Loc = GLES20.glGetUniformLocation(programId, "u_color2")
        uAccentColorLoc = GLES20.glGetUniformLocation(programId, "u_accentColor")
        uCoreColorLoc = GLES20.glGetUniformLocation(programId, "u_coreColor")
        uGlowColorLoc = GLES20.glGetUniformLocation(programId, "u_glowColor")

        uTouchLoc = GLES20.glGetUniformLocation(programId, "u_touch")
        uTouchActiveLoc = GLES20.glGetUniformLocation(programId, "u_touchActive")
        uRotationLoc = GLES20.glGetUniformLocation(programId, "u_rotation")

        uAudioBassLoc = GLES20.glGetUniformLocation(programId, "u_audioBass")
        uAudioMidLoc = GLES20.glGetUniformLocation(programId, "u_audioMid")
        uAudioTrebleLoc = GLES20.glGetUniformLocation(programId, "u_audioTreble")

        uSceneBgLoc = GLES20.glGetUniformLocation(programId, "u_sceneBg")
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("OrbRenderer", "Shader compilation error: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
