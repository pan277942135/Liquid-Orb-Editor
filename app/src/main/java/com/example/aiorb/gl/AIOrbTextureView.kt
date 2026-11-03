package com.example.aiorb.gl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.SystemClock
import android.util.Log
import android.view.TextureView

@SuppressLint("ViewConstructor")
class AIOrbTextureView(
    context: Context,
    val renderer: AIOrbRenderer
) : TextureView(context), TextureView.SurfaceTextureListener {

    private var renderThread: RenderThread? = null

    init {
        surfaceTextureListener = this
        isOpaque = false
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val (rw, rh) = computeRenderSize(width, height)
        surface.setDefaultBufferSize(rw, rh)

        stopRenderThread()
        renderThread = RenderThread(surface, rw, rh, renderer).apply {
            start()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        val (rw, rh) = computeRenderSize(width, height)
        surface.setDefaultBufferSize(rw, rh)
        renderThread?.onSizeChanged(rw, rh)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderThread()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun computeRenderSize(w: Int, h: Int): Pair<Int, Int> {
        val maxDim = maxOf(w, h)
        val scale = if (maxDim > 800) 0.5f else 1.0f
        val rw = (w * scale).toInt().coerceAtLeast(320)
        val rh = (h * scale).toInt().coerceAtLeast(320)
        return Pair(rw, rh)
    }

    private fun stopRenderThread() {
        renderThread?.let { thread ->
            thread.requestStop()
            try {
                thread.join(400)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        renderThread = null
    }

    fun onPause() {
        renderThread?.pauseRendering()
    }

    fun onResume() {
        renderThread?.resumeRendering()
    }

    private class RenderThread(
        private val surfaceTexture: SurfaceTexture,
        private var width: Int,
        private var height: Int,
        private val renderer: AIOrbRenderer
    ) : Thread("AIOrbTextureRenderThread") {

        @Volatile
        private var isRunning = true

        @Volatile
        private var isPaused = false

        @Volatile
        private var sizeChanged = false

        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var eglConfig: EGLConfig? = null

        fun onSizeChanged(w: Int, h: Int) {
            width = w
            height = h
            sizeChanged = true
        }

        fun pauseRendering() {
            isPaused = true
        }

        fun resumeRendering() {
            isPaused = false
        }

        fun requestStop() {
            isRunning = false
        }

        override fun run() {
            if (!initEGL()) {
                Log.e("AIOrbTextureView", "EGL init failed")
                return
            }

            try {
                renderer.onSurfaceCreated()
                renderer.onSurfaceChanged(width, height)

                // Render first frame immediately to complete WindowManager surface transaction
                renderer.onDrawFrame()
                EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                val targetFrameTimeMs = 16L
                while (isRunning) {
                    val frameStart = SystemClock.uptimeMillis()

                    if (!isPaused && eglDisplay != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
                        if (sizeChanged) {
                            renderer.onSurfaceChanged(width, height)
                            sizeChanged = false
                        }

                        renderer.onDrawFrame()
                        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                    }

                    val elapsed = SystemClock.uptimeMillis() - frameStart
                    val sleepTime = targetFrameTimeMs - elapsed
                    if (sleepTime > 0) {
                        sleep(sleepTime)
                    }
                }
            } catch (e: Exception) {
                Log.e("AIOrbTextureView", "Render exception", e)
            } finally {
                destroyEGL()
            }
        }

        private fun initEGL(): Boolean {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) return false
            eglConfig = configs[0]

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )

            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) return false

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, surfaceAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) return false

            return EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        }

        private fun destroyEGL() {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        }
    }
}
