# Liquid Orb Editor — Debug APK 交付包

**Task ID:** `LIQUID_ORB_EDITOR_R06_RELEASE_LOOP_RETURN_MATERIAL_INTEGRATION_V1`  
**Git Commit:** `ff9a0b0`  
**构建状态:** BUILD SUCCESSFUL (35/35 Unit Tests Passed)  

---

### APK 文件信息

- **文件名:** `app-debug.apk`
- **文件大小:** `40,681,986 字节` (~38.8 MB)
- **SHA-256 校验码:** `83aa968a27ad40235497e807fff59a9449874054a8a5c741ea0295608fe28f11`
- **Artifact 路径:** `app-debug.apk` (已放置在当前 Artifacts 产物目录中，可直接在 Artifacts 面板中点击下载或导出)

---

### 核心功能与验证点

1. **RELEASE 独立启动与预览:** 打开即直接进入 S2 Orb 状态，无需手动先触发 WAKE。
2. **950ms 自动循环:** 0~350ms 释放、350~690ms 保持、690~950ms 连续反向回归，无缝进入下一周期。
3. **C1 连续反向运动:** 彻底修复旧版先缩回再扩展的问题，690ms 切换点位置严格连续，速度平滑反转，无瞬移、不闪烁。
4. **R06 冻结材质正式驱动:** 完整驱动 4 层水体材质（外围柔化、半透明水体、内侧折射带、头部镜面高光），采用预乘 Alpha 混合。
5. **预分配 Buffer 零内存分配:** 彻底消除逐帧 `allocateDirect` 与 `filter`，稳定流畅 60+ FPS。
6. **全功能保留:** 13 款 Liquid Orb 预设、06 凝字实验室（KF01~KF06）全部完好。
