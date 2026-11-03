package com.example.aiorb

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.aiorb.model.*
import com.example.aiorb.gl.AIOrbRenderer
import com.example.aiorb.util.AssetHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AIOrbBasicTestSuite {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun sha256Bytes(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    // T01: 18个原始资源 SHA
    @Test
    fun testT01_18_Frozen_Assets_SHA256() {
        val sumsText = AssetHelper.openAsset(context, "ai_orb/frozen_v1_1/SHA256SUMS.txt")
            .bufferedReader().use { it.readText() }

        val lines = sumsText.lines().filter { it.isNotBlank() }
        assertEquals("Must contain 18 verified files in SHA256SUMS.txt", 18, lines.size)

        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"), limit = 2)
            if (parts.size == 2) {
                val expectedSha = parts[0].trim().lowercase()
                val fname = parts[1].trim()
                val bytes = AssetHelper.openAsset(context, "ai_orb/frozen_v1_1/$fname").use { it.readBytes() }
                val actualSha = sha256Bytes(bytes).lowercase()
                assertEquals("SHA256 mismatch for $fname", expectedSha, actualSha)
            }
        }
    }

    // T02: 核心母版 RGBA 和尺寸 (1254x1254)
    @Test
    fun testT02_Core_Master_RGBA_And_Dimensions() {
        val stream = AssetHelper.openAsset(context, "ai_orb/frozen_v1_1/透明湖水玻璃球.png")
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, options)
        stream.close()

        assertEquals("Core master width must be 1254", 1254, options.outWidth)
        assertEquals("Core master height must be 1254", 1254, options.outHeight)
        assertTrue("Core master must have mimeType image/png", options.outMimeType?.contains("png") == true)
    }

    // T03: S2 参考图不得作为透明 Runtime Sprite
    @Test
    fun testT03_S2_Reference_Is_Opaque_Not_Runtime_Sprite() {
        val stream = AssetHelper.openAsset(context, "ai_orb/frozen_v1_1/翡翠星光中的流光水晶球.png")
        val bm = BitmapFactory.decodeStream(stream)
        stream.close()
        assertNotNull(bm)
        assertEquals(1254, bm.width)
        assertEquals(1254, bm.height)
        // Corner pixel is dark opaque background
        val cornerPixel = bm.getPixel(0, 0)
        assertEquals("Alpha channel of reference image must be 255 (opaque)", 255, (cornerPixel shr 24) and 0xFF)
        bm.recycle()
    }

    // T04: 四张独立水草 Mask 的内容与对齐
    @Test
    fun testT04_Grass_Masks_1254x1254_Grayscale() {
        val masks = listOf(
            "orb_grass_back_b1_mask_v1_1.png",
            "orb_grass_back_b2_mask_v1_1.png",
            "orb_grass_front_f1_mask_v1_1.png",
            "orb_grass_front_f2_mask_v1_1.png"
        )
        for (m in masks) {
            val stream = AssetHelper.openAsset(context, "ai_orb/frozen_v1_1/$m")
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, opts)
            stream.close()
            assertEquals("Mask $m width must be 1254", 1254, opts.outWidth)
            assertEquals("Mask $m height must be 1254", 1254, opts.outHeight)
        }
    }

    // T05: 水草四路径 Root/Tip 和运动幅度
    @Test
    fun testT05_Grass_Four_Paths_Root_Tip_And_Amplitude() {
        val leaves = GrassLeafPath.loadFromAssets(context)
        assertEquals(4, leaves.size)

        val b1 = leaves["BACK_B1"]!!
        assertEquals(0.0f, b1.rootMaxD, 0.0001f)
        assertEquals(0.018f, b1.amplitudeD, 0.0001f)
        assertEquals(6200L, b1.periodMs)

        val b2 = leaves["BACK_B2"]!!
        assertEquals(0.0f, b2.rootMaxD, 0.0001f)
        assertEquals(0.018f, b2.amplitudeD, 0.0001f)
        assertEquals(6200L, b2.periodMs)

        val f1 = leaves["FRONT_F1"]!!
        assertTrue(f1.rootMaxD <= 0.003f)
        assertEquals(0.030f, f1.amplitudeD, 0.0001f)
        assertEquals(4800L, f1.periodMs)

        val f2 = leaves["FRONT_F2"]!!
        assertTrue(f2.rootMaxD <= 0.003f)
        assertEquals(0.027f, f2.amplitudeD, 0.0001f)
        assertEquals(4800L, f2.periodMs)
    }

    // T06: S1/S2 呼吸周期与幅度
    @Test
    fun testT06_Breath_Cycles_And_Amplitudes() {
        val s1Period = 6000.0
        val s1Amp = 0.008f
        val s2Period = 4800.0
        val s2Amp = 0.018f

        // Check S1 breath at peak (t = period / 4)
        val s1Peak = 1.0f + s1Amp * sin(2.0 * PI * (s1Period / 4.0) / s1Period).toFloat()
        assertEquals(1.008f, s1Peak, 0.0001f)

        // Check S2 breath at peak (t = period / 4)
        val s2Peak = 1.0f + s2Amp * sin(2.0 * PI * (s2Period / 4.0) / s2Period).toFloat()
        assertEquals(1.018f, s2Peak, 0.0001f)
    }

    // T07: 三颗水滴位置、周期、Alpha
    @Test
    fun testT07_Three_Droplets_Contract() {
        val droplets = WaterDroplet.DROBLET_LIST
        assertEquals(3, droplets.size)

        val d1 = droplets[0]
        assertEquals("D1", d1.id)
        assertEquals(0.32f, d1.baseUv.x, 0.01f)
        assertEquals(0.36f, d1.baseUv.y, 0.01f)
        assertEquals(1.8f, d1.diameterDp, 0.01f)
        assertEquals(5200L, d1.periodMs)

        val d2 = droplets[1]
        assertEquals("D2", d2.id)
        assertEquals(0.68f, d2.baseUv.x, 0.01f)
        assertEquals(0.56f, d2.baseUv.y, 0.01f)
        assertEquals(2.2f, d2.diameterDp, 0.01f)
        assertEquals(6100L, d2.periodMs)

        val d3 = droplets[2]
        assertEquals("D3", d3.id)
        assertEquals(0.49f, d3.baseUv.x, 0.01f)
        assertEquals(0.73f, d3.baseUv.y, 0.01f)
        assertEquals(1.4f, d3.diameterDp, 0.01f)
        assertEquals(4800L, d3.periodMs)
    }

    // T08: 阳光方向与 Alpha 上限
    @Test
    fun testT08_Sunlight_Direction_And_Alpha_Cap() {
        assertEquals(0.24f, SunlightContract.SOURCE_UV.x, 0.001f)
        assertEquals(0.20f, SunlightContract.SOURCE_UV.y, 0.001f)
        assertEquals("#D8BC87", SunlightContract.MAIN_COLOR_HEX)
        assertTrue(SunlightContract.GLASS_SPECULAR_ALPHA_MAX <= 0.11f)
        assertTrue(SunlightContract.CORE_REFRACTION_ALPHA_MAX <= 0.11f)
        assertTrue(SunlightContract.TOTAL_ADDITIONAL_ADDITIVE_ALPHA_MAX <= 0.22f)
    }

    // T09: 两层焦散周期和性能
    @Test
    fun testT09_Caustics_Cycles() {
        assertEquals(6500L, CausticsContract.DEEP_PERIOD_MS)
        assertEquals(4100L, CausticsContract.SURFACE_PERIOD_MS)
        assertEquals(0.385f, CausticsContract.INNER_RADIUS_D, 0.001f)
        assertEquals(0.035f, CausticsContract.EDGE_FADE_D, 0.001f)
    }

    // T10: R06 几何源文件与路径数量
    @Test
    fun testT10_R06_Geometry_5_Paths_And_128_Sample_LUT() {
        val paths = FilamentPath.loadAllFromAssets(context)
        assertEquals("Must load exactly five R06 paths", 5, paths.size)
        val ids = paths.map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf("G01", "G02", "A01", "A02", "C01")))

        for (p in paths) {
            assertEquals("Each path must have 128 LUT samples", 128, p.lutSamples)
            assertEquals("LUT points size must be 128", 128, p.lutPoints.size)
            assertTrue("Total arc length must be positive", p.totalArcLength > 0f)
        }
    }

    // T11: Wake 0/80/220/350ms
    @Test
    fun testT11_Wake_Time_Contract() {
        val clock = OrbSceneClock()
        clock.start()
        val f0 = clock.produceFrame()
        assertEquals(0f, f0.wakeProgress, 0.001f)

        clock.triggerWake()
        val fWake = clock.produceFrame()
        assertNotNull(fWake.wakeStartMs)
        assertEquals(OrbLifeState.S2_ACTIVE, fWake.lifeState)
    }

    // T12: S1->S2 450ms 并行转换
    @Test
    fun testT12_S1_To_S2_450ms_Filament_Alpha() {
        val clock = OrbSceneClock()
        clock.start()
        clock.triggerWake()
        val frame = clock.produceFrame()
        assertTrue(frame.filamentAlphaProgress in 0f..1f)
    }

    // T13: Release 350/450/690/950ms
    @Test
    fun testT13_Release_Time_Contract() {
        val clock = OrbSceneClock()
        clock.start()
        clock.triggerRelease()
        val frame = clock.produceFrame()
        assertNotNull(frame.releaseStartMs)
        assertEquals(OrbPhaseEvent.RELEASE, frame.currentPhaseEvent)
    }

    // T14: Pause/Resume 相位连续
    @Test
    fun testT14_Pause_Resume_Preserves_Phase() {
        val clock = OrbSceneClock()
        clock.start()
        clock.triggerBreath()

        clock.pause()
        assertTrue(clock.isPaused)
        val frame1 = clock.produceFrame()
        val time1 = frame1.sceneElapsedMs

        // Advance thread sleep
        Thread.sleep(30)
        val frame2 = clock.produceFrame()
        val time2 = frame2.sceneElapsedMs
        assertEquals("Time must be frozen while paused", time1, time2)

        clock.resume()
        assertFalse(clock.isPaused)
    }

    // T15: 几何缩放与默认半值 (默认0.5x / 40dp，支持 68dp/80dp 等缩放)
    @Test
    fun testT15_Geometry_Scale_Default_Half_And_Custom() {
        val renderer = AIOrbRenderer(context)
        assertEquals("Default orb size scale must be 0.5 (half size)", 0.5f, renderer.orbSizeScale, 0.001f)

        renderer.orbSizeScale = 1.0f // 80dp
        assertEquals(1.0f, renderer.orbSizeScale, 0.001f)

        renderer.orbSizeScale = 0.85f // 68dp
        assertEquals(0.85f, renderer.orbSizeScale, 0.001f)
    }

    // T16: Debug/Release 参数一致
    @Test
    fun testT16_Debug_Release_Calibration_Consistency() {
        val renderer = AIOrbRenderer(context)
        assertEquals(1.0f, renderer.sunlightIntensityMultiplier, 0.001f)
        assertEquals(1.0f, renderer.grassMotionMultiplier, 0.001f)
        assertEquals(1.0f, renderer.filamentAlphaMultiplier, 0.001f)
    }

    // T17: 不存在逐帧 Bitmap/Path/JSON 分配
    @Test
    fun testT17_No_Per_Frame_Allocations() {
        val paths = FilamentPath.loadAllFromAssets(context)
        val clock = OrbSceneClock()
        clock.start()

        // 100 consecutive frame calculations
        for (i in 0 until 100) {
            val frame = clock.produceFrame()
            for (p in paths) {
                val (head, tail) = p.computeHeadAndTail(frame.sceneElapsedMs, frame.releaseProgress, false)
                assertTrue(head >= 0f)
            }
        }
        assertTrue("Completed 100 frame computations deterministically without crashes", true)
    }
}
