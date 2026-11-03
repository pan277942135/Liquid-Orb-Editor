package com.example.glyph.gl

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
 * Hardware-accelerated TextureView hosting the GlyphOrbRenderer for dynamic Chinese species glyphs.
 */
@SuppressLint("ViewConstructor")
class GlyphOrbTextureView(
    context: Context,
    val renderer: GlyphOrbRenderer
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
                renderer.isTouching = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y

                renderer.rotationY += dx * 0.008f
                renderer.rotationX -= dy * 0.008f

                renderer.touchX = normX
                renderer.touchY = normY
                renderer.isTouching = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                renderer.isTouching = false
            }
        }
        return true
    }

    private class RenderThread(
        private val surfaceTexture: SurfaceTexture,
        private var width: Int,
        private var height: Int,
        private val renderer: GlyphOrbRenderer
    ) : Thread("GlyphOrbRenderThread") {

        @Volatile
        private var isRunning = true

        @Volatile
        private var isPaused = false

        private val lock = Object()

        fun requestStop() {
            isRunning = false
            synchronized(lock) {
                lock.notifyAll()
            }
        }

        fun pauseRendering() {
            isPaused = true
        }

        fun resumeRendering() {
            isPaused = false
            synchronized(lock) {
                lock.notifyAll()
            }
        }

        fun onSizeChanged(newW: Int, newH: Int) {
            width = newW
            height = newH
        }

        override fun run() {
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) return

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return

            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
                EGL14.eglTerminate(eglDisplay)
                return
            }
            val eglConfig = configs[0]

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            val eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                EGL14.eglTerminate(eglDisplay)
                return
            }

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, surfaceAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
                return
            }

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
                return
            }

            try {
                renderer.onSurfaceCreated()
                renderer.onSurfaceChanged(width, height)

                // Render initial frame immediately
                renderer.onDrawFrame()
                EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                var currentW = width
                var currentH = height
                val targetFrameTimeMs = 16L

                while (isRunning) {
                    if (isPaused) {
                        synchronized(lock) {
                            while (isPaused && isRunning) {
                                try {
                                    lock.wait(100)
                                } catch (e: InterruptedException) {
                                    break
                                }
                            }
                        }
                    }

                    if (!isRunning) break

                    val frameStart = SystemClock.elapsedRealtime()

                    if (currentW != width || currentH != height) {
                        currentW = width
                        currentH = height
                        renderer.onSurfaceChanged(currentW, currentH)
                    }

                    renderer.onDrawFrame()
                    EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                    val elapsed = SystemClock.elapsedRealtime() - frameStart
                    val sleepMs = targetFrameTimeMs - elapsed
                    if (sleepMs > 0) {
                        try {
                            sleep(sleepMs)
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GlyphOrbRenderThread", "Render thread error", e)
            } finally {
                renderer.release()
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
            }
        }
    }
}
