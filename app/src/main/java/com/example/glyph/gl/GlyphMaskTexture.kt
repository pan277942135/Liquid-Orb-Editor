package com.example.glyph.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils

/**
 * Handles OpenGL 2D texture allocation, bitmap upload, and resource cleanup for the glyph mask.
 */
class GlyphMaskTexture {

    var textureId: Int = 0
        private set

    private var currentBitmap: Bitmap? = null
    private var isDirty: Boolean = false

    /**
     * Updates the pending bitmap on any thread. The actual GL upload occurs in [bindOrUpload].
     */
    fun updateBitmap(bitmap: Bitmap) {
        synchronized(this) {
            currentBitmap = bitmap
            isDirty = true
        }
    }

    /**
     * Binds the texture to the active texture unit, uploading a newly queued bitmap if dirty.
     * Must be called from the OpenGL thread with an active EGL context.
     */
    fun bindOrUpload(textureUnit: Int = GLES20.GL_TEXTURE0) {
        GLES20.glActiveTexture(textureUnit)

        var bmpToUpload: Bitmap? = null
        synchronized(this) {
            if (isDirty && currentBitmap != null) {
                bmpToUpload = currentBitmap
                isDirty = false
            }
        }

        if (textureId == 0) {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            textureId = ids[0]

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }

        if (bmpToUpload != null && !bmpToUpload!!.isRecycled) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmpToUpload, 0)
        }
    }

    /**
     * Releases OpenGL texture handle.
     */
    fun release() {
        if (textureId != 0) {
            val ids = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, ids, 0)
            textureId = 0
        }
    }
}
