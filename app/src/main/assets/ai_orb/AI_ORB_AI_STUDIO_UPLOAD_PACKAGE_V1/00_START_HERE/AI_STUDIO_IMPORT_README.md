# AI Orb — Google AI Studio 上传资源包 V1

本包用于 Google AI Studio 中的独立 AI Orb 三态 Demo 开发。

## 1. 资源来源与完整性

- 冻结合同：`YUJIAN_AI_ORB_WAKE_BREATH_RELEASE_VISUAL_PROFILE_V1_1`
- 资源状态：`FROZEN_FOR_WORK`
- 水草方案：Scheme B（从原始母版派生 Mask / Path；禁止叠加重复水草图层）
- 原始资源包 SHA-256：`677ea9dddf3d445bc9c8385a14d9c6bb20e45619453f766f8b6c21a36d811500`
- 本包内 `01_FROZEN_V1_1/` 中的原始文件均从冻结 ZIP 解压，未重新编码或修改。
- 原始 `SHA256SUMS.txt` 中的 18 个文件已逐一校验通过。

## 2. 目录

`01_FROZEN_V1_1/` 包含：
- 原始透明湖水玻璃球母版 PNG
- S2 视觉参考图
- AI Orb 三态视觉规范板
- L01–L07 图层和关键帧标注图
- 水草四张独立 Mask、两张合并 Mask
- 水草派生 JSON / SVG 路径
- V1.1 冻结视觉合同和实现规格
- 原始 Manifest、README、SHA256 校验文件

## 3. 必须遵守

1. `透明湖水玻璃球.png` 是不可修改的核心母版；保持字节、Alpha 和原始比例。
2. `翡翠星光中的流光水晶球.png` 是 S2 视觉参考，不是透明 Runtime Sprite，不得直接覆盖到 Orb 上。
3. 水草必须使用本包中的 Scheme B Mask / Path 做局部 UV 光学位移；不得复制一层移动水草造成重影。
4. 不得使用已撤销的 `R01_grass_back.svg`、`R01_grass_front.svg`。
5. 仅实现 S1_IDLE、WAKE、S2_ACTIVE_BREATH、S2_ACTIVE_RELEASE_RETURN；不实现 S3_GLYPH 和鱼获业务。

## 4. R02–R06 独立 Profile 资源缺口（重要）

原始 V1.1 冻结包本身没有附带以下独立文件：
- `R02_orb_droplet_profile.json`
- `R03_orb_sunlight_profile.json`
- `R05_orb_caustics_profile.json`
- `R06_orb_filament_profile.json`
- `R06_orb_filament_path_geometry.json`
- `R06_orb_filament_paths.svg`

V1.1 冻结合同中包含部分 R02/R03/R05 参数，以及对 R06 文件的 SHA 引用，但不等于这些独立源文件已经包含在本包。

合同引用的 R06 SHA：
- Profile: `075e8390aa08638ba3baca5a12f9b92b73718dc87ec1175fe3325e4cc7cad864`
- Geometry: `a37d947d32cdf6902cabe87be03458ab6d0dd236c4d033564a98f8a5a1a601a5`
- SVG: `66966e0b9aaf1fe6cbb3930ae99d382a1d3a9d292ef8c3911a9e2ef45c4db52b`

不得自行伪造这些文件、重画椭圆路径，或把其他版本候选冒充 V1.1 冻结文件。若 AI Studio 无法取得它们，应先完成本包内已有资源的工作，并将 R06 标记为 `BLOCKED_MISSING_SOURCE`，等待补充准确资源。

## 5. 给 AI Studio 的导入方式

将本 ZIP 上传到 AI Studio 项目附件/资源区。先解压并读取：
1. `01_FROZEN_V1_1/README.txt`
2. `01_FROZEN_V1_1/ORB_WAKE_BREATH_RELEASE_VISUAL_CONTRACT_FROZEN_V1_1.json`
3. `01_FROZEN_V1_1/ORB_WAKE_BREATH_RELEASE_VISUAL_SPEC_FROZEN_V1_1.md`
4. `01_FROZEN_V1_1/WORK_P0_2_AI_ORB_WAKE_BREATH_RELEASE_IMPLEMENTATION_V1_1.md`
5. `01_FROZEN_V1_1/SHA256SUMS.txt`

先校验 SHA，再开始代码实现。不得仅凭本 README 中的摘要覆盖正式冻结合同。
