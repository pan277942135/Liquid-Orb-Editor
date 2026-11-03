package com.example.glyph.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.glyph.model.GlyphFontOption
import com.example.glyph.model.SpeciesGlyphSpec

/**
 * Procedural generator for dynamic Chinese species glyph textures.
 * Renders any input string (e.g. 黑鱼, 大口黑鲈, 草鱼) to an OpenGL-ready multi-channel mask.
 *
 * Channel mapping:
 * - R: Sharp calligraphy stroke mask (1.0 inside character, 0.0 outside)
 * - G: Soft dilated stroke rim (used for Fresnel crystal refraction & caustics)
 * - B: Fine accent / gold thread stroke skeleton
 * - A: Combined alpha envelope
 */
object SpeciesGlyphMaskGenerator {

    const val TEXTURE_SIZE = 512

    /**
     * Resolves the Typeface from the app's bundled resources or system fonts.
     */
    fun resolveTypeface(context: Context, option: GlyphFontOption): Typeface {
        val resId = option.fontResId
        if (resId != null) {
            try {
                val tf = ResourcesCompat.getFont(context, resId)
                if (tf != null) return tf
            } catch (e: Exception) {
                // Fallback to system bold if resource fails
            }
        }
        return Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }

    /**
     * Generates a 512x512 multi-channel bitmap for any arbitrary Chinese species text.
     */
    fun generateMaskBitmap(
        context: Context,
        spec: SpeciesGlyphSpec
    ): Bitmap {
        val size = TEXTURE_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val text = spec.speciesName.ifEmpty { "鱼" }
        val typeface = resolveTypeface(context, spec.fontOption)

        // 1. Adaptive size calculation so any length (2-char, 4-char, etc.) fits well
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textAlign = Paint.Align.CENTER
        }

        // Target box: center 72% of texture
        val targetWidth = size * 0.72f
        var fontSize = when {
            text.length <= 2 -> 160f
            text.length == 3 -> 125f
            text.length == 4 -> 98f
            else -> (size * 0.65f) / text.length
        }

        basePaint.textSize = fontSize
        var measuredW = basePaint.measureText(text)
        if (measuredW > targetWidth) {
            fontSize *= (targetWidth / measuredW)
            basePaint.textSize = fontSize
        }

        val bounds = Rect()
        basePaint.getTextBounds(text, 0, text.length, bounds)
        val centerX = size / 2f
        val centerY = size / 2f - bounds.exactCenterY()

        // 2. Layer 1: Soft rim glow / refraction corridor (Green Channel)
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textAlign = Paint.Align.CENTER
            this.textSize = fontSize
            this.color = Color.argb(180, 0, 255, 0) // Pure Green in mask
            this.style = Paint.Style.STROKE
            this.strokeWidth = fontSize * 0.16f
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
        }
        canvas.drawText(text, centerX, centerY, rimPaint)

        // 3. Layer 2: Main Calligraphy Fill (Red Channel & White Core)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textAlign = Paint.Align.CENTER
            this.textSize = fontSize
            this.color = Color.argb(255, 255, 220, 160) // High Red, Medium Green, subtle Blue
            this.style = Paint.Style.FILL
        }
        canvas.drawText(text, centerX, centerY, fillPaint)

        // 4. Layer 3: Gold filigree centerline accent (Blue Channel)
        val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textAlign = Paint.Align.CENTER
            this.textSize = fontSize
            this.color = Color.argb(220, 180, 200, 255) // High Blue for golden specular highlights
            this.style = Paint.Style.STROKE
            this.strokeWidth = fontSize * 0.04f
        }
        canvas.drawText(text, centerX, centerY, centerLinePaint)

        return bitmap
    }
}
