# R02–R06 独立资源缺口说明

此文件用于避免 AI Studio 将“合同中引用了资源”误判为“资源文件已随包提供”。

原始冻结包已包含核心母版、S2 参考图、规范板、水草 Mask/Path、冻结合同和实现规格；但不包含以下独立 Profile/Geometry 文件：

- R02_orb_droplet_profile.json
- R03_orb_sunlight_profile.json
- R05_orb_caustics_profile.json
- R06_orb_filament_profile.json
- R06_orb_filament_path_geometry.json
- R06_orb_filament_paths.svg

V1.1 合同对其中部分文件有 SHA 引用。特别是 R06 的源文件必须按合同 SHA 核对，不能把其他版本或候选资源标成 V1.1 冻结资产。

缺少这些文件时：允许实现本包明确提供的母版、Mask、路径、三态和已在合同内明确给出的参数；R06 的真实五条 Bezier 光丝必须标记为 BLOCKED_MISSING_SOURCE，不得以椭圆、圆环、sin 曲线或临时手绘路径替代。
