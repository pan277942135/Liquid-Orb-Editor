package com.example.gl

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
import android.view.MotionEvent
import android.view.TextureView

/**
 * High-performance TextureView-based OpenGL ES 2.0 host for Liquid Orb.
 *
 * Configures default buffer sizing and immediately draws the first frame
 * to guarantee instantaneous SurfaceSyncGroup transaction completion on Android 14+ (API 34-36).
 */
@SuppressLint("ViewConstructor")
class OrbTextureView(
    context: Context,
    val renderer: OrbRenderer
) : TextureView(context), TextureView.SurfaceTextureListener {

    private var renderThread: RenderThread? = null

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        surfaceTextureListener = this
        isOpaque = false
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val (renderW, renderH) = computeRenderSize(width, height)
        surface.setDefaultBufferSize(renderW, renderH)

        stopRenderThread()
        renderThread = RenderThread(surface, renderW, renderH, renderer).apply {
            start()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        val (renderW, renderH) = computeRenderSize(width, height)
        surface.setDefaultBufferSize(renderW, renderH)
        renderThread?.onSizeChanged(renderW, renderH)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderThread()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Frame presented
    }

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val minDim = minOf(w, h)

        val normX = (event.x - w * 0.5f) / minDim
        val normY = (h * 0.5f - event.y) / minDim

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                renderer.touchX = normX
                renderer.touchY = normY
                renderer.isTouchActive = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y

                renderer.rotY += dx * 0.008f
                renderer.rotX -= dy * 0.008f

                renderer.touchX = normX
                renderer.touchY = normY
                renderer.isTouchActive = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                renderer.isTouchActive = false
            }
        }
        return true
    }

    private class RenderThread(
        private val surfaceTexture: SurfaceTexture,
        private var width: Int,
        private var height: Int,
        private val renderer: OrbRenderer
    ) : Thread("OrbTextureRenderThread") {

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
                Log.e("OrbTextureView", "EGL initialization failed")
                return
            }

            try {
                renderer.onSurfaceCreated(null, null)
                renderer.onSurfaceChanged(null, width, height)

                // Immediately draw and swap the very first frame to instantly satisfy SurfaceSyncGroup!
                renderer.onDrawFrame(null)
                EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                val targetFrameTimeMs = 16L // ~60 fps
                while (isRunning) {
                    val frameStart = SystemClock.uptimeMillis()

                    if (!isPaused && eglDisplay != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
                        if (sizeChanged) {
                            renderer.onSurfaceChanged(null, width, height)
                            sizeChanged = false
                        }

                        renderer.onDrawFrame(null)
                        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                    }

                    val elapsed = SystemClock.uptimeMillis() - frameStart
                    val sleepTime = targetFrameTimeMs - elapsed
                    if (sleepTime > 0) {
                        sleep(sleepTime)
                    }
                }
            } catch (e: Exception) {
                Log.e("OrbTextureView", "Render loop exception", e)
            } finally {
                destroyEGL()
            }
        }

        private fun initEGL(): Boolean {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                Log.e("OrbTextureView", "eglGetDisplay failed")
                return false
            }

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                Log.e("OrbTextureView", "eglInitialize failed")
                return false
            }

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
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
                Log.e("OrbTextureView", "eglChooseConfig failed")
                return false
            }
            eglConfig = configs[0]

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )

            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                Log.e("OrbTextureView", "eglCreateContext failed")
                return false
            }

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, surfaceAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                Log.e("OrbTextureView", "eglCreateWindowSurface failed")
                return false
            }

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                Log.e("OrbTextureView", "eglMakeCurrent failed")
                return false
            }

            return true
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
