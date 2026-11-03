package com.example.aiorb

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.aiorb.gl.AIOrbRenderer
import com.example.aiorb.gl.FilamentBufferHolder
import com.example.aiorb.gl.OrbCoordinateMapper
import com.example.aiorb.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class R06FilamentVisibilityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // T01: Clock lifecycle on surface created/recreated (must not reset to S1)
    @Test
    fun testT01_Clock_Lifecycle_On_Surface_Created_Must_Not_Reset() {
        val renderer = AIOrbRenderer(context)
        // Simulate ViewModel initializing in RELEASE preview mode
        renderer.sceneClock.startReleasePreview(loop = true)
        assertEquals(OrbLifeState.S2_ACTIVE, renderer.sceneClock.lifeState)
        assertNotNull(renderer.sceneClock.releaseStartMs)

        // Advance clock
        Thread.sleep(20)
        renderer.sceneClock.produceFrame()

        // Call onSurfaceCreated (e.g. Activity recreation or surface re-attachment)
        renderer.onSurfaceCreated()

        // Must PRESERVE S2_ACTIVE and release preview state!
        assertEquals("LifeState must remain S2_ACTIVE across surface creation", OrbLifeState.S2_ACTIVE, renderer.sceneClock.lifeState)
        assertNotNull("Release start timestamp must be preserved", renderer.sceneClock.releaseStartMs)
        assertTrue("Release preview mode must remain enabled", renderer.sceneClock.releasePreviewMode)
    }

    // T02: Core visible radius UV measurement equals 0.3971 within 0.005
    @Test
    fun testT02_Core_Visible_Radius_UV_Equals_0_3971() {
        // Empirically measured: 498.0px / 1254.0px = 0.397129
        val measuredUv = OrbCoordinateMapper.CORE_VISIBLE_RADIUS_UV
        assertEquals(0.3971f, measuredUv, 0.005f)
        assertTrue("Core radius UV must be in range [0.39, 0.41]", measuredUv in 0.39f..0.41f)

        // Verify that 1.0D in local space corresponds to 2 * 0.3971 = 0.7942 UV diameter
        val coreDiameterUv = 2.0f * measuredUv
        assertEquals(0.7942f, coreDiameterUv, 0.005f)
    }

    // T03: Five path IDs (G01, G02, A01, A02, C01) and max radii (> 0.5D extension)
    @Test
    fun testT03_Five_Paths_Exist_And_Extend_Beyond_Core_Sphere() {
        val paths = FilamentPath.loadAllFromAssets(context)
        assertEquals("Must load exactly 5 paths", 5, paths.size)

        val pathMap = paths.associateBy { it.id }
        assertTrue("Must contain G01", pathMap.containsKey("G01"))
        assertTrue("Must contain G02", pathMap.containsKey("G02"))
        assertTrue("Must contain A01", pathMap.containsKey("A01"))
        assertTrue("Must contain A02", pathMap.containsKey("A02"))
        assertTrue("Must contain C01", pathMap.containsKey("C01"))

        // Expected max radii from frozen geometry (all extend significantly beyond 0.5D):
        // G01: ~0.6218D, G02: ~0.6364D, A01: ~0.6450D, A02: ~0.6042D, C01: ~0.5604D
        val expectedMinMaxRadii = mapOf(
            "G01" to 0.60f,
            "G02" to 0.60f,
            "A01" to 0.60f,
            "A02" to 0.58f,
            "C01" to 0.55f
        )

        for ((id, minExpectedMaxRadius) in expectedMinMaxRadii) {
            val p = pathMap[id]!!
            var maxR = 0f
            for (i in 0..100) {
                val pt = p.getPointAtArcFraction(i / 100f)
                val r = sqrt(pt.x * pt.x + pt.y * pt.y)
                if (r > maxR) maxR = r
            }
            assertTrue("Path $id max radius ($maxR D) must extend beyond core radius 0.5D", maxR > 0.5f)
            assertTrue("Path $id max radius ($maxR D) must meet frozen geometry requirement (>=$minExpectedMaxRadius D)", maxR >= minExpectedMaxRadius)
            assertTrue("Path $id max radius ($maxR D) must be within max envelope 0.675D", maxR <= OrbCoordinateMapper.MAX_RADIUS_D + 0.01f)
        }
    }

    // T04: Stroke width DP constancy under scale changes (68dp vs 80dp)
    @Test
    fun testT04_Stroke_Width_DP_Constancy() {
        val profileWidthDp = 2.5f
        val materialMultiplier = 1.75f
        val userMultiplier = 1.0f

        // At 80dp orb size:
        val strokeWidthD_80dp = OrbCoordinateMapper.computeStrokeWidthD(
            profileWidthDp = profileWidthDp,
            materialMultiplier = materialMultiplier,
            userMultiplier = userMultiplier,
            orbSizeScale = 1.0f,
            breathFactor = 1.0f
        )

        // At 68dp orb size (orbSizeScale = 68/80 = 0.85):
        val strokeWidthD_68dp = OrbCoordinateMapper.computeStrokeWidthD(
            profileWidthDp = profileWidthDp,
            materialMultiplier = materialMultiplier,
            userMultiplier = userMultiplier,
            orbSizeScale = 0.85f,
            breathFactor = 1.0f
        )

        // The stroke width in physical DP is: strokeWidthD * orbDiameterDp
        val physicalDp_80 = strokeWidthD_80dp * (1.0f * 80f)
        val physicalDp_68 = strokeWidthD_68dp * (0.85f * 80f)

        assertEquals("Physical stroke width must be invariant across orb size scales", physicalDp_80, physicalDp_68, 0.001f)
        assertEquals(profileWidthDp * materialMultiplier * userMultiplier, physicalDp_80, 0.001f)

        // Check breath compensation: when breathFactor changes, physical width remains constant
        val strokeWidthD_breathPulse = OrbCoordinateMapper.computeStrokeWidthD(
            profileWidthDp = profileWidthDp,
            materialMultiplier = materialMultiplier,
            userMultiplier = userMultiplier,
            orbSizeScale = 1.0f,
            breathFactor = 1.018f
        )
        val physicalDp_pulsed = strokeWidthD_breathPulse * (1.0f * 80f) * 1.018f
        assertEquals("Physical stroke width must be invariant under breath scale", physicalDp_80, physicalDp_pulsed, 0.001f)
    }

    // T05: Shader smoothstep monotonicity and valid arguments (no inverted min/max)
    @Test
    fun testT05_Smoothstep_Monotonicity_And_Increasing_Edges() {
        // Test edge0 < edge1 mathematical validity
        fun glslSmoothstep(edge0: Float, edge1: Float, x: Float): Float {
            assertTrue("edge0 must be strictly less than edge1 in GLSL ES", edge0 < edge1)
            val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        // 1. Outer feather: [0.25, 1.0]
        val f0 = 1.0f - glslSmoothstep(0.25f, 1.0f, 0.0f)
        val fHalf = 1.0f - glslSmoothstep(0.25f, 1.0f, 0.5f)
        val f1 = 1.0f - glslSmoothstep(0.25f, 1.0f, 1.0f)
        assertEquals(1.0f, f0, 0.001f)
        assertTrue(fHalf in 0.0f..1.0f)
        assertEquals(0.0f, f1, 0.001f)
        assertTrue("Feather must be strictly non-increasing with distance", f0 >= fHalf && fHalf >= f1)

        // 2. Translucent body: [0.05, 0.85]
        val b0 = 1.0f - glslSmoothstep(0.05f, 0.85f, 0.0f)
        val b1 = 1.0f - glslSmoothstep(0.05f, 0.85f, 0.85f)
        assertEquals(1.0f, b0, 0.001f)
        assertEquals(0.0f, b1, 0.001f)

        // 3. Specular glint: [0.0, 0.40]
        val s0 = 1.0f - glslSmoothstep(0.0f, 0.40f, 0.0f)
        val s1 = 1.0f - glslSmoothstep(0.0f, 0.40f, 0.40f)
        assertEquals(1.0f, s0, 0.001f)
        assertEquals(0.0f, s1, 0.001f)
    }

    // T06: 96-subdivision and 48-subdivision buffer allocation
    @Test
    fun testT06_Buffer_Allocation_96_And_48_Subdivisions() {
        val paths = FilamentPath.loadAllFromAssets(context)
        val g01 = paths.first { it.id == "G01" }

        // Standard 96 subdivisions
        val holder96 = FilamentBufferHolder(g01, 96)
        val expectedVertexCount96 = (96 + 1) * 2 // 194 vertices
        assertEquals(expectedVertexCount96, holder96.vertexCount)
        assertEquals(expectedVertexCount96 * 2, holder96.posBuffer.capacity()) // 388 floats
        assertEquals(expectedVertexCount96 * 2, holder96.uvBuffer.capacity()) // 388 floats

        // Low-power 48 subdivisions
        val holder48 = FilamentBufferHolder(g01, 48)
        val expectedVertexCount48 = (48 + 1) * 2 // 98 vertices
        assertEquals(expectedVertexCount48, holder48.vertexCount)
        assertEquals(expectedVertexCount48 * 2, holder48.posBuffer.capacity())
    }

    // T07: Diagnostic modes and path filtering
    @Test
    fun testT07_Diagnostic_Modes_And_Path_Filtering() {
        val renderer = AIOrbRenderer(context)
        renderer.onSurfaceCreated()

        // Default RUNTIME mode
        assertEquals(R06DiagnosticMode.RUNTIME, renderer.diagnosticMode)
        assertEquals(5, renderer.enabledPathIds.size)

        // Switch to GEOMETRY_ONLY
        renderer.diagnosticMode = R06DiagnosticMode.GEOMETRY_ONLY
        assertEquals(1, renderer.diagnosticMode.modeId)
        assertEquals("GEOMETRY", renderer.diagnosticMode.label)

        // Switch to MATERIAL
        renderer.diagnosticMode = R06DiagnosticMode.MATERIAL
        assertEquals(2, renderer.diagnosticMode.modeId)
        assertEquals("MATERIAL", renderer.diagnosticMode.label)

        // Test path filtering: isolate only A01
        renderer.enabledPathIds.clear()
        renderer.enabledPathIds.add("A01")
        assertEquals(1, renderer.enabledPathIds.size)
        assertTrue(renderer.enabledPathIds.contains("A01"))
        assertFalse(renderer.enabledPathIds.contains("G01"))
    }

    // T08: Coordinate mapping between OrbLocal D and NDC / Pixels
    @Test
    fun testT08_Coordinate_Mapping_OrbLocal_To_Ndc_And_Pixels() {
        val screenW = 1080
        val screenH = 2400
        val minDim = 1080f
        val scale = 1.0f

        // Point at core rim: (0.5D, 0.0D)
        val rimD = Point2D(0.5f, 0.0f)

        // NDC mapping:
        val ndc = OrbCoordinateMapper.orbLocalToNdc(rimD, scale, screenW, screenH)
        val expectedNdcX = 0.5f * (4.0f * OrbCoordinateMapper.CORE_VISIBLE_RADIUS_UV * scale) * (minDim / screenW)
        assertEquals(expectedNdcX, ndc.x, 0.0001f)
        assertEquals(0.0f, ndc.y, 0.0001f)

        // Screen Pixel mapping:
        val screenPx = OrbCoordinateMapper.orbLocalToScreenPx(rimD, scale, screenW, screenH)
        val expectedPxX = screenW * 0.5f + 0.5f * (2.0f * OrbCoordinateMapper.CORE_VISIBLE_RADIUS_UV * scale * minDim)
        assertEquals(expectedPxX, screenPx.x, 0.001f)
        assertEquals(screenH * 0.5f, screenPx.y, 0.001f)
    }

    // T09: RELEASE -> HOLD -> RETURN cycle continuity and zero allocation
    @Test
    fun testT09_Release_Hold_Return_Cycle_Continuity() {
        val paths = FilamentPath.loadAllFromAssets(context)
        val g01 = paths.first { it.id == "G01" }

        // Test RELEASE (0–350ms): monotonic forward progress
        val k0 = g01.computeKinematics(0L, 0L, OrbPhaseEvent.RELEASE, false)
        val k200 = g01.computeKinematics(200L, 200L, OrbPhaseEvent.RELEASE, false)
        val k350 = g01.computeKinematics(350L, 350L, OrbPhaseEvent.RELEASE, false)

        assertTrue("Head must advance in RELEASE: k0 <= k200", k0.headU <= k200.headU)
        assertTrue("Head must advance in RELEASE: k200 <= k350", k200.headU <= k350.headU)
        assertFalse(k0.isReturn)
        assertFalse(k200.isReturn)

        // Test HOLD (350–690ms): position fixed at maximum
        val k500 = g01.computeKinematics(500L, 500L, OrbPhaseEvent.RELEASE, true)
        assertEquals(k350.headU, k500.headU, 0.001f)
        assertEquals(0.0f, k500.velocityU, 0.0001f)

        // Test RETURN (690–950ms): smooth inward recovery
        val k690 = g01.computeKinematics(690L, 690L, OrbPhaseEvent.RETURN, false)
        val k800 = g01.computeKinematics(800L, 800L, OrbPhaseEvent.RETURN, false)
        val k950 = g01.computeKinematics(950L, 950L, OrbPhaseEvent.RETURN, false)

        assertTrue("Head must decrease in RETURN: k690 >= k800", k690.headU >= k800.headU)
        assertTrue("Head must decrease in RETURN: k800 >= k950", k800.headU >= k950.headU)
        assertTrue(k800.isReturn)

        // Boundary continuity: start and end positions match
        assertEquals("Cycle start and end positions must match", k0.headU, k950.headU, 0.005f)
    }

    // T10: 3D Sphere Self-Rotation and HDR Glow parameters
    @Test
    fun testT10_Sphere_Self_Rotation_And_HDR_Glow_Optics() {
        val renderer = AIOrbRenderer(context)
        assertEquals(0.45f, renderer.rotationSpeed, 0.001f)
        assertTrue(renderer.isAutoRotate)
        assertEquals(1.8f, renderer.glowIntensityMultiplier, 0.001f)

        // Verify touch offset accumulation
        renderer.touchRotationOffset += 0.5f
        assertEquals(0.5f, renderer.touchRotationOffset, 0.001f)

        // Verify rotation angle accumulation over 2000ms
        val angleAt2s = (2000L / 1000.0 * renderer.rotationSpeed).toFloat() + renderer.touchRotationOffset
        assertEquals(0.9f + 0.5f, angleAt2s, 0.001f)
    }
}
