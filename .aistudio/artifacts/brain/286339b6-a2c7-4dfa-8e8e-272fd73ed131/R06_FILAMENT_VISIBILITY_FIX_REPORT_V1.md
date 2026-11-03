# AI Orb R06 五条外部光丝可见性、几何映射与 Shader 修复报告 (V1.1)

**Task ID:** `AI_ORB_R06_EXTERNAL_FILAMENT_VISIBILITY_P0_1_V1`  
**Repository:** `pan277942135/Liquid-Orb-Editor`  
**开发分支:** `fix/r06-external-filament-visibility-p0-1`  
**Git Commit:** `58499dc`  
**构建输出:** `app/build/outputs/apk/debug/app-debug.apk`  
**APK 大小:** `39 MB`  
**APK SHA-256:** `791bc2b4963287fb51ec221b05bc61fef3e08709bdd909ed572c13b97dfffd68`  

---

## 一、四大核心根因修复汇总

### 1. P0-A: 时钟生命周期重置与 Alpha 提前退出修复
- **根因:** `AIOrbRenderer.onSurfaceCreated()` 中误调用了 `sceneClock.start()`，导致 GL Surface 创建/重建时将 `lifeState` 重置回 `S1_IDLE`，清空了 `releaseStartMs`；同时 `drawFilamentHolders()` 中 `if (frame.filamentAlphaProgress <= 0.01f) return` 直接放弃绘制。
- **修复:** 
  - 移除了 `onSurfaceCreated()` 中的 `sceneClock.start()`，时钟生命周期交由 `AIOrbViewModel` 集中管理。
  - 仅在非诊断模式且处于 S1 待机时限制渲染；在诊断模式（GEOMETRY_ONLY、MATERIAL）以及 RELEASE/RETURN 阶段始终保持 100% 渲染能力。

### 2. P0-B: 核心玻璃球可视边界精确测定与坐标映射
- **测定数据:** 对 1254x1254 核心玻璃球母版（`透明湖水玻璃球.png`）的 Alpha 通道进行逐行反滤波实测：
  - Alpha 阈值 $\ge 128$: $X \in [129, 1124]$ (宽 996px), $Y \in [124, 1117]$ (高 994px)
  - 可视直径 = 996px，可视半径 = 498.0px
  - 归一化可视半径 UV: **`coreVisibleRadiusUv = 498.0 / 1254.0 = 0.3971f`** (球核占整张 PNG 的 79.4%，外围存在约 20.6% 的透明留白)
- **映射对齐公式:**
  创建 `OrbCoordinateMapper.kt`，推导 OrbLocal 直径单位 $D$ 到 OpenGL NDC 坐标的精确定量关系：
  $$\text{positionScale} = 4.0 \times \text{CORE\_VISIBLE\_RADIUS\_UV} \times u\_scale = 1.5884 \times u\_scale$$
  $$P_{ndc.x} = P_D.x \times \text{positionScale} \times \frac{\min(W, H)}{W}$$
  $$P_{ndc.y} = -P_D.y \times \text{positionScale} \times \frac{\min(W, H)}{H}$$
  **数学闭环:** 当 $r_D = 0.5$（球核边缘）时，计算得到的 NDC 坐标与 `AIOrbShader` 采样的玻璃球可视外轮廓在亚像素级完全对齐！五条光丝（最大延伸达 $0.56D \sim 0.645D$）完全舒展在球体外围。

### 3. P0-C: 恒定 DP 线宽保障
- **问题:** 原线宽在球体呼吸振荡时出现物理尺寸缩放抖动。
- **修复:** 通过 `OrbCoordinateMapper.computeStrokeWidthD` 严格保证屏幕物理 DP 恒定：
  $$\text{strokeWidthDp} = \text{profileWidthDp} \times \text{materialMultiplier} \times \text{userMultiplier}$$
  $$\text{strokeWidthD} = \frac{\text{strokeWidthDp}}{\text{orbDiameterDp} \times \text{breathFactor}}$$
  无论球体如何呼吸，光丝在屏幕上的物理线宽保持绝对恒定。

### 4. P0-D: GLSL ES 倒置 `smoothstep` 根因修复
- **根因:** `FilamentShader.kt` 原代码中存在反向区间：`smoothstep(1.0, 0.25, dist)`，违反 GLSL ES 规范中 $edge0 < edge1$ 的前置约束，导致移动端 GPU 产生 NaN、全黑或全透异常。
- **修复:** 全面更正为单调递增区间：
  - 羽化层: `feather = 1.0 - smoothstep(0.25, 1.0, dist)`
  - 水体厚度: `body = 1.0 - smoothstep(0.05, 0.85, dist)`
  - 内侧折射: `refrBand = 1.0 - smoothstep(0.0, 0.42, refrDist)`
  - 镜面高光: `specGlint = headProximity * (1.0 - smoothstep(0.0, 0.40, dist))`
  - 金光细丝: `fineLine = 1.0 - smoothstep(0.0, 0.35, dist)`

### 5. P0-E: 96 细分段预分配零内存分配架构
- `FilamentBufferHolder` 最大细分提升至 96 段（194 顶点，388 Float），缓冲区尺寸达到 1552 字节，并支持 48 段低功耗模式平滑切换，逐帧零 GC 分配。

---

## 二、新增 R06 诊断模式与实时 HUD

在 `AIOrbVisualLabScreen.kt` 与 `AIOrbViewModel.kt` 中正式落地：
1. **实时 HUD 状态条:**
   - 运行状态：`RELEASE (0–350ms)` / `HOLD (350–690ms)` / `RETURN (690–950ms)`
   - 循环指标：`Cycle #N (t ms / 950ms)`
   - 核心参数：`R_uv: 0.3971`、`球体尺寸: 40dp (X px)`
   - 诊断模式：`[MODE: RUNTIME | GEOMETRY | MATERIAL]`
   - 五路光丝指示：`G01` (绿) `G02` (绿) `A01` (金) `A02` (金) `C01` (青)
   - 渲染状态：`GL: OK`
2. **面板控制项:**
   - **三态诊断模式切换:**
     - `正常光学 (Runtime)`: 完整 4 层复合水体光学渲染
     - `纯几何 (Geometry)`: 高对比实体光带 + 中心高亮白轴脊，精准审查几何走线与球外延伸
     - `材质层 (Material)`: 绿/金/青三类材质色相分离高反差展示
   - **单路光丝独立开关 (Path Isolation):** 支持单独开启或关闭 G01/G02/A01/A02/C01 中的任意一条，便于单丝几何定位与材质调谐
   - **细分精度切换:** 96 段（高精平滑）与 48 段（低功耗模式）一键切换

---

## 三、自动化测试套件 (T01 ~ T09 全部通过)

执行 `gradle :app:testDebugUnitTest` 验证结果:
- **T01:** `testT01_Clock_Lifecycle_On_Surface_Created_Must_Not_Reset` — **PASSED**
- **T02:** `testT02_Core_Visible_Radius_UV_Equals_0_3971` — **PASSED**
- **T03:** `testT03_Five_Paths_Exist_And_Extend_Beyond_Core_Sphere` — **PASSED** (G01=0.6218D, G02=0.6364D, A01=0.6450D, A02=0.6042D, C01=0.5604D，全部超过 0.5D 球体核心)
- **T04:** `testT04_Stroke_Width_DP_Constancy` — **PASSED** (68dp vs 80dp 物理线宽保持绝对恒定)
- **T05:** `testT05_Smoothstep_Monotonicity_And_Increasing_Edges` — **PASSED**
- **T06:** `testT06_Buffer_Allocation_96_And_48_Subdivisions` — **PASSED**
- **T07:** `testT07_Diagnostic_Modes_And_Path_Filtering` — **PASSED**
- **T08:** `testT08_Coordinate_Mapping_OrbLocal_To_Ndc_And_Pixels` — **PASSED**
- **T09:** `testT09_Release_Hold_Return_Cycle_Continuity` — **PASSED**

---

## 四、Debug APK 获取说明

已编译生成经过双重校验的 Debug APK：
- **容器本地路径:** `app/build/outputs/apk/debug/app-debug.apk`
- **直接下载路径:** 点击左侧 Artifacts 列表中的当前条目即可下载查看。
