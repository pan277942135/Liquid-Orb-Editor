# Work P0-2 第一专项｜AI Orb 苏醒、持续呼吸、局部水光释放与 R01B 水草动态高可见度集成 V1.1

**Task ID:** `YUJIAN_AI_ORB_WAKE_BREATH_RELEASE_RUNTIME_FROZEN_V1_1`

**Repository:** `pan277942135/Yujian_App`

**执行级别:** P0 / 单一完整产品开发任务；中途可分阶段提交，但本轮必须完成开发、基础测试、Debug APK 后才能宣称工程交付。

**重要：本合同的冻结已完成。产品负责人已选择方案 B。不要重新询问是否允许派生水草资源，不要再做设计探索、重绘视觉母版或推迟实现。**

## 0. 启动与权威

以 GitHub **实时最新 Main** 为基线，读取 AGENTS.md、仓库任务状态文件，以及刚合并的 P0-1 统一场景控制实现。经核对 PR #313《fix(android): unify recognition scene clock and 13-stage reveal》已合入 Main，Work 必须保留它的统一 SceneFrame、MANUAL/AUTO、模型门禁与 Result 揭晓，不允许拿之前旧分支覆盖。

建议工作分支：`feature/p0-2-ai-orb-wake-breath-release-v1-1`。遵守仓库必需的最小控制面和准入流程，不做额外无关治理或扩大任务。

**必须读取的附件（同一冻结包）：**

1. `ORB_WAKE_BREATH_RELEASE_VISUAL_CONTRACT_FROZEN_V1_1.json`：全部渲染/时间/材质参数的机器可读唯一权威。
2. `ORB_GRASS_DERIVED_MASK_PATH_V1_1.json`：四条水草源图 Polygon/Bezier/UV/根叶参数与生成方法。
3. `ORB_GRASS_DERIVED_PATHS_V1_1.svg`：技术路径参考，不是旧撤销 SVG。
4. `orb_grass_back_*_mask_v1_1.png` 与 `orb_grass_front_*_mask_v1_1.png`：六张源派生灰度 Mask。
5. `ORB_GRASS_MASK_PATH_ALIGNMENT_PREVIEW_V1_1.png`：源图对位诊断参考。
6. `透明湖水玻璃球.png`、`翡翠星光中的流光水晶球.png`、`AI Orb三态视觉规范板.png`：三份原始视觉权威。
7. `ORB_WAKE_BREATH_RELEASE_VISUAL_SPEC_FROZEN_V1_1.md`：人类可读约束与状态矩阵。

验证 `SHA256SUMS.txt` / `PACKAGE_MANIFEST.json`。一切数值以 JSON 合同为准，不能从示意图主观猜参数。**用户已批准新 R01B 源派生 Mask/Path；仍严禁导入撤销的 `R01_grass_back.svg` / `R01_grass_front.svg`。**

## 1. 唯一开发目标与范围

只解决三部分：

- ORB_WAKE：相册与拍摄共用水核苏醒，1–350ms，光学元素真正苏醒；450ms S1→S2 光丝转换与 Wake 并行。
- ORB_BREATH：S2 4.8s/±1.8%，S1 6.0s/±0.8%，三颗水滴、左上阳光、内部焦散、R01B 真实前后景水草随同一场景时钟持续动态。
- ORB_RELEASE：S2 内 350–950ms 五条既有 R06 光丝从舒展转为局部回收，260ms C1 方向转换，目标是显著可见的 S2 活化效果。

**不做**：新球体、长距离 Orb→照片外围 Path、寻鱼、轮廓、真实 Mask、鱼体金光、识别全局 Stage10 Orb Return、S3 解体凝字、结果页新设计。不得修改真实模型接口。

## 2. 先修复确切根因

### 2.1 `AIOrbMotion.kt`

当前 Main 中 `waterAlpha = if (reduceMotion || !fromCamera) 1f ...` 使相册选图直接绕过水核苏醒。将 `fromCamera` 的 Hero 按钮交接与 Orb 水核苏醒解耦：无论 Camera/Gallery 均执行同一 Wake Frame；`reduceMotion` 只对动画幅度降级，不影响 Orb 最终可见。核心母版不做全体淡入的替换贴图。

### 2.2 `RecognizingScreen.kt` 与 `RecognitionSceneFrame.kt`

读取 **最新合并 P0-1** 的权威场景状态与 Step 逻辑：`sceneElapsedMs` 驱动 Breath/R01B/焦散/水滴；`opticalReferenceElapsedMs` 驱动 Stage Wake/Release 包络。必须来自同一单调时间、同一个 Session/Attempt/Photo 与 GeometryVersion。

MANUAL 在 01/13 停留；下一步只推进一格；每步 3000ms 本步预览后停止，生活动画在本步持续运行，但暂停时全部冻结。切换 AUTO/MANUAL、页面重组、接收模型事件均不能重置生命相位。

### 2.3 `AIOrbCore.kt`

显式接入 S1_IDLE → S2_ACTIVE 的生命状态和局部 RELEASE 能量事件，不依赖默认始终 S2_ACTIVE。保留同一 Runtime Core 图片和已验证坐标锚点；把内部焦散、草叶局部扭动、水滴与光丝映射到唯一场景帧。

不要新增独立 Ticker、第二颗 Orb、全局光环或新 GLSurfaceView。

## 3. R01B 水草动态完整实现（方案 B，必须完成）

### 3.1 禁止直接加载旧 SVG

包中的 `ORB_GRASS_DERIVED_PATHS_V1_1.svg` 是本次从冻结 `透明湖水玻璃球.png` 标注的四条光学运动控制路径，与旧 `R01_grass_back.svg`、`R01_grass_front.svg` 无关；后两者依然禁止使用。

### 3.2 导入新派生 Mask

使用四张叶片 Mask（B1/B2/F1/F2）和两张深度合并 Mask，保持文件校验。将 1254×1254 源UV与当前 Runtime WebP 384×384 的实际内容矩形对齐，记录变换矩阵。不能假设源图和 WebP 无裁切差异，不能通过修改母版来适配。

### 3.3 实现真正叶尖摆动，而不是复制叶片贴图

对同一源纹理进行 **局部 UV 光学变形**：在四条现有绿色区域内，以 Mask Alpha 混合原始采样和局部弯曲采样，不新增植物颜色块，不先画静态母版后叠加一份移动叶片造成重影。

参数来自派生 JSON：

- BACK_B1：6200ms，0°，叶尖±0.018D，根部固定。
- BACK_B2：6200ms，100°，叶尖±0.018D，根部固定。
- FRONT_F1：4800ms，120°，叶尖±0.030D，根部≤0.003D。
- FRONT_F2：4800ms，240°，叶尖±0.027D，根部≤0.003D。
- 根到尖的弯曲包络 `smoothstep(0.10,1.0,s)^1.6`；局部位移沿对应 Bezier 中线法向，按统一 `sceneElapsedMs` 的正弦相位运行。
- 水草局部漫反射 `#A9D9C5`、Alpha≤0.10，叶尖高光 Alpha≤0.12，只允许落在新 Mask 内。

Android16 优先用既有 Orb Renderer 内的 UV-displacement Shader；不引入新 EGL / GLSurfaceView。无支持的旧设备允许安全降级为原水草和受光变化，但要记录其未实现真实摆动；目标真机路径必须实现可见叶尖运动。

必要时新增一个独立、无状态的 `OrbGrassOpticalDeformer` / 等价小组件，仅负责消费 SceneFrame 与四 Mask，不许管理生命时钟或移动花草素材。

## 4. Orb 苏醒、呼吸、光效参数完整落实

### 4.1 Wake

Wake `0–350ms`，关键帧 0/80/220/350ms，`cubicBezier(0.16,1.0,0.30,1.0)`；Core 保持完整，从水体、焦散、水滴、草叶受光与左上折射这些子层逐步激活，禁止整球闪白。S1→S2 光丝 Alpha 并行 `0–450ms smoothstep`，不能串行叠加 450ms 导致释放后移。

### 4.2 Breath

- S1 6.0s/±0.8%；S2 4.8s/±1.8%；中心固定、等比缩放、阶段相位不重置。
- R02 三颗水滴固定位置/径向透镜材质按 JSON，漂浮速度 0.2–0.8dp/s，不能生成白点或随机新水滴。
- R03 左上入射光 UV(0.24,0.20)，主色 `#D8BC87`，玻璃高光6400ms/Alpha≤0.11、折射5200ms/Alpha≤0.11、局部焦散Alpha≤0.14；不改变太阳方位。
- R05 复用内部 WaterCausticsField，Deep 6500ms/Alpha≤0.07，Surface 4100ms/Alpha≤0.12，96×96/66ms 或低性能64×64/120ms，禁止每帧分配 Bitmap/黑底纹理。
- L01–L07 的绘制前后顺序严格按合同执行。

### 4.3 Release / Local Return

只使用 R06 已有五条 Bezier G01/G02/A01/A02/C01，保持原空间范围最大外扩 14dp，前后层关系不变。详细 Profile 路径周期和相位优先于规范板的宣传概览周期。

350ms RELEASE 发射，450ms 光丝 Alpha 完成 S1→S2，690ms 开始局部 RETURN，950ms 回归持续 S2 光丝游动；260ms 方向转换遵守 C1 速度连续，不重置弧长位置。**不要将本次局部 RETURN 当作识别 Stage10 的鱼体回馈。**

**首轮高可见度强化：** 按源 R06 原 Alpha 乘以1.28，上限0.82；局部移动高光增益1.15，RELEASE 头部亮度1.18；仅增强既有路径的可读性，不增加光丝数量、不改为椭圆、不使整球泛白。Debug 与 Release 同一参数合同。

## 5. 工程风险与集成限制

1. 三张原始资源文件 SHA 必须与冻结包一致；Android Runtime WebP 原有 SHA 不得改。
2. 绘制时严格按 BACK→Core→内部→FRONT 构建，避免透明度被提前消费或全屏合成变黑。
3. 只改与 Orb WAKE/BREATH/RELEASE 有关的文件；不得覆盖已合并 P0-1 的 Stage Controller、Manual/Auto 和 Result 揭晓。
4. 不引入第二套场景时钟；不重播 Detector/Classifier/SlimSAM。
5. 新旧 Session 与不同 GeometryVersion 不得串用采样 Mask 或能量事件。
6. 一切计算可以单元测试，Runtime 中不允许每帧分配 Bitmap、Paint、Path、大量对象或重新解析 JSON。
7. ReduceMotion 通过关闭光学位移/降低光丝运动实现，但原始 Orb 必须可见；所有动态仍绑定可暂停的唯一时钟。
8. **不得为了追求第一版夸张效果将球体本体 Scale 调到 ±2.5%；本轮以视觉板 S2 ±1.8% 为准，增强叶尖和 R06 可见度。**
9. 不调用 API28 模拟器、不运行 connectedAndroidTest、不安装 Work 真机、不做 Work 逐帧视觉验收。

## 6. 基础测试（必须有实际证据）

至少覆盖以下测试 ID：

| ID | 测试要求 |
|---|---|
| G01 | Asset01/02/03 与 Runtime Core 原二进制 SHA 不变；旧 R01 撤销资源绝不编入 Runtime |
| G02 | 四个 R01B Mask 的文件、尺寸、Alpha 非零区域、包 SHA 完全符合冻结值 |
| G03 | 四条 Path 的 Root/Tip/Bezier UV 归一化正确，所有点落在原图范围 |
| G04 | 80dp/68dp 与任意实际 Orb D 的 UV-to-View 映射正确，不出现黑边/越界 |
| G05 | S1 / S2 呼吸周期、振幅、暂停恢复以及切阶段不重置相位 |
| G06 | BACK 根部固定、FRONT 根部≤0.003D；叶尖±0.018/0.027/0.030D，路径相位确定 |
| G07 | 草叶局部 UV 变形只发生在 Mask 范围，不增加第二套叶片图像，不覆盖原球体反射 |
| G08 | 相机与相册均执行 Wake，fromCamera 只控制 Hero 交接 |
| G09 | 0/80/220/350/450/690/950ms 的阶段包络与 900ms 入口合同一致 |
| G10 | 5条 R06 原路径、深度、各自周期和首轮1.28可见度倍增正确 |
| G11 | MANUAL 01/13 停留、Next 单阶段推进、Pause/Resume 全部同步 |
| G12 | Debug/Release 编译不丢失同一 Orb 候选 Renderer；不得仅 Debug 特效有效 |

执行仓库已有对应资源校验，加上：

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew compileReleaseKotlin
```

命令若名称与仓库实际 Task 不一致，查 Gradle Tasks 并替换为等价可运行 Task，记录实际命令。由于本任务要求只做基础测试，不运行模拟器/API28/真机，不得把这些项目标为 PASS。

## 7. 交付与验收分工

- 基于最新 Main 的隔离分支开发；完成全部功能和基础测试；创建 PR。遇到冲突仅做与本任务直接相关的最小修复。
- 必须生成真实安装包 `YuJian-Orb-Wake-Breath-Release-V1_1-<shortSHA>.apk`，放到可下载位置，并计算 APK SHA-256、大小、源码 HEAD SHA、applicationId、versionCode、versionName。
- 列出真实改动文件、每个关键参数的最终值、使用的 Mask/Path 的 SHA 和映射方式、基础测试结果和完整的真机人工测试清单。
- **产品负责人自己进行真机视觉测试。** 普通开发 Work 不得以缺少设备视觉验证为由阻塞 APK 交付。
- 如果基础构建失败，必须修复并重跑，不能将失败状态写为完成；确实无法解决时如实列出失败及 APK 状态。
- 真机视觉尚未验证时，状态写 `ENGINEERING_COMPLETE / VISUAL_PENDING_OWNER`，不能宣称“高保真验收通过”。

**结束条件：** 三阶段全部接到共享 SceneFrame，方案 B 草叶局部动态真实实现，R06 局部释放/回收明显可见，基础测试成功，APK 及完整 SHA 已交付。只有这些一起完成才能结束本轮 Work，不得只报进展或交付半成品。
