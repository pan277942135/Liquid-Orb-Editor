package com.example

import com.example.glyph.model.GlyphFontOption
import com.example.glyph.model.GlyphKeyframe
import com.example.glyph.model.GlyphMorphTimeline
import com.example.glyph.model.SpeciesGlyphSpec
import com.example.glyph.model.TimelinePlayMode
import com.example.glyph.model.YujianGlyphMaterialProfile
import com.example.model.OrbPresets
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GlyphMorphTest {

    @Test
    fun `verify six keyframes sequence and timing contract`() {
        val keyframes = GlyphKeyframe.entries
        assertEquals(6, keyframes.size)

        assertEquals("KF01", keyframes[0].id)
        assertEquals(0L, keyframes[0].targetTimeMs)
        assertEquals(0.00f, keyframes[0].normalizedProgress, 0.001f)

        assertEquals("KF02", keyframes[1].id)
        assertEquals(480L, keyframes[1].targetTimeMs)
        assertEquals(0.20f, keyframes[1].normalizedProgress, 0.001f)

        assertEquals("KF03", keyframes[2].id)
        assertEquals(960L, keyframes[2].targetTimeMs)
        assertEquals(0.40f, keyframes[2].normalizedProgress, 0.001f)

        assertEquals("KF04", keyframes[3].id)
        assertEquals(1440L, keyframes[3].targetTimeMs)
        assertEquals(0.60f, keyframes[3].normalizedProgress, 0.001f)

        assertEquals("KF05", keyframes[4].id)
        assertEquals(1920L, keyframes[4].targetTimeMs)
        assertEquals(0.80f, keyframes[4].normalizedProgress, 0.001f)

        assertEquals("KF06", keyframes[5].id)
        assertEquals(2400L, keyframes[5].targetTimeMs)
        assertEquals(1.00f, keyframes[5].normalizedProgress, 0.001f)
    }

    @Test
    fun `verify timeline evaluation across continuous states`() {
        // KF01: Sphere only
        val stateKf1 = GlyphMorphTimeline.evaluate(0L, mode = TimelinePlayMode.AUTO)
        assertEquals(0.0f, stateKf1.glyphAlpha, 0.01f)
        assertEquals(1.0f, stateKf1.coreStretchX, 0.05f)

        // KF02: Core horizontal stretch (1.45~1.65:1)
        val stateKf2 = GlyphMorphTimeline.evaluate(480L, mode = TimelinePlayMode.AUTO)
        assertTrue("StretchX should be > 1.45:1 at KF02", stateKf2.coreStretchX >= 1.45f)
        assertTrue("StretchY should compress at KF02", stateKf2.coreStretchY < 0.95f)

        // KF03: Ribbon release active
        val stateKf3 = GlyphMorphTimeline.evaluate(960L, mode = TimelinePlayMode.AUTO)
        assertTrue("Ribbon intensity should be active at KF03", stateKf3.ribbonIntensity > 0.7f)

        // KF04: Initial glyph condensation
        val stateKf4 = GlyphMorphTimeline.evaluate(1440L, mode = TimelinePlayMode.AUTO)
        assertTrue("Glyph alpha should emerge at KF04", stateKf4.glyphAlpha > 0.35f)
        assertTrue("Fluid erosion should be noticeable at KF04", stateKf4.glyphFluidErosion > 0.15f)

        // KF05: Crystallization and gold filaments
        val stateKf5 = GlyphMorphTimeline.evaluate(1920L, mode = TimelinePlayMode.AUTO)
        assertTrue("Refraction should peak at KF05", stateKf5.glyphRefraction >= 1.6f)
        assertTrue("Gold filaments should be high at KF05", stateKf5.goldFilamentAlpha > 0.6f)

        // KF06: Settled deep lake ink stability
        val stateKf6 = GlyphMorphTimeline.evaluate(2400L, mode = TimelinePlayMode.AUTO)
        assertEquals(1.0f, stateKf6.glyphAlpha, 0.01f)
        assertTrue("Ink opacity should be near maximum at KF06", stateKf6.inkOpacity >= 0.85f)
    }

    @Test
    fun `verify manual keyframe hold mode`() {
        val stateManual = GlyphMorphTimeline.evaluate(
            elapsedMs = 99999L,
            mode = TimelinePlayMode.MANUAL,
            manualKeyframe = GlyphKeyframe.KF02_WATER_CORE_STRETCH
        )
        assertEquals(GlyphKeyframe.KF02_WATER_CORE_STRETCH, stateManual.activeKeyframe)
        assertEquals(480L, stateManual.elapsedMs)
        assertEquals(0.20f, stateManual.progress, 0.001f)
        assertTrue(stateManual.coreStretchX >= 1.45f)
    }

    @Test
    fun `verify dynamic species spec and arbitrary input`() {
        val customSpec = SpeciesGlyphSpec(
            speciesName = "加州鲈",
            scientificName = "Micropterus salmoides",
            fontOption = GlyphFontOption.MA_SHAN_ZHENG
        )
        assertEquals("加州鲈", customSpec.speciesName)
        assertEquals(GlyphFontOption.MA_SHAN_ZHENG, customSpec.fontOption)
    }

    @Test
    fun `verify material profile JSON export for Heiyu and Largemouth Bass`() {
        val profileHeiyu = YujianGlyphMaterialProfile.createDefault("黑鱼", "Channa argus")
        val jsonHeiyu = JSONObject(profileHeiyu.toJsonString())
        assertEquals("YUJIAN_GLYPH_V2_FROZEN", jsonHeiyu.getString("profileVersion"))
        assertEquals("黑鱼", jsonHeiyu.getString("speciesName"))
        val paletteHeiyu = jsonHeiyu.getJSONObject("palette")
        assertEquals("#173E49", paletteHeiyu.getString("lakeInk"))
        assertEquals("#2E8B83", paletteHeiyu.getString("lakeTeal"))
        assertEquals("#67C8D2", paletteHeiyu.getString("waterCyan"))
        assertEquals("#D8B36A", paletteHeiyu.getString("warmGold"))

        val profileBass = YujianGlyphMaterialProfile.forLargemouthBass()
        val jsonBass = JSONObject(profileBass.toJsonString())
        assertEquals("大口黑鲈", jsonBass.getString("speciesName"))
        val geo = jsonBass.getJSONObject("geometry")
        assertTrue(geo.getDouble("maxStretchX") >= 1.55)
    }

    @Test
    fun `verify 13 original presets preserved and Yujian Water Ink preset accessible`() {
        assertEquals(13, OrbPresets.all.size)
        val yujian = OrbPresets.yujianWaterInk
        assertNotNull(yujian)
        assertEquals("yujian_water_ink", yujian.presetId)
        assertEquals("渔见·水墨凝字", yujian.nameZh)

        val retrieved = OrbPresets.getById("yujian_water_ink")
        assertEquals("yujian_water_ink", retrieved.presetId)
    }
}
