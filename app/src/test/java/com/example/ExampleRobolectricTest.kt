package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.CodeExporter
import com.example.model.OrbPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Liquid Orb Editor", appName)
    }

    @Test
    fun `verify 13 presets loaded correctly`() {
        assertEquals(13, OrbPresets.all.size)
        val siriWave = OrbPresets.getById("siri_wave")
        assertNotNull(siriWave)
        assertEquals("Siri Wave", siriWave.nameEn)
    }

    @Test
    fun `verify cross-platform code export`() {
        val preset = OrbPresets.getById("siri_wave")
        val wgsl = CodeExporter.exportWgsl(preset)
        assertTrue(wgsl.contains("struct Uniforms"))
        assertTrue(wgsl.contains("f32"))

        val swift = CodeExporter.exportSwiftUi(preset)
        assertTrue(swift.contains("import SwiftUI"))

        val json = CodeExporter.exportJson(preset)
        assertTrue(json.contains("\"preset\": \"siri_wave\""))
    }
}
