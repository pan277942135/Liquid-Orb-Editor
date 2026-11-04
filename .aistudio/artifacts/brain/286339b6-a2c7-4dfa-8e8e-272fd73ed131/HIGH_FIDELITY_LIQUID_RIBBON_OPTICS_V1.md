# AI Orb 着色器编译故障排查与修复报告 (V1.3 Hotfix)

## 1. 错误日志复盘与根本原因

系统日志捕获到的崩溃信息如下：
```text
11-04 13:29:58.143 6306 E/AIOrbRenderer: Shader compilation error: ERROR: 0:202: 'clampedRotUv' : undeclared identifier
ERROR: 0:202: 'renderDroplet' : no matching overloaded function found
ERROR: 0:202: '=' : dimension mismatch
ERROR: 0:202: '=' : cannot convert from 'const mediump float' to 'highp 4-component vector of float'
...
```

### 根因：
在解耦球体自转逻辑时，旧变量 `clampedRotUv` 已被替换为严格正立采样的 `baseUv`。但在碎片着色器底部的水滴函数调用处（`renderDroplet`）遗留了对 `clampedRotUv` 的引用，导致：
1. GLSL 编译在手机设备端报错：`'clampedRotUv' : undeclared identifier`。
2. 核心球体着色器程序创建失败，状态栏显示 `GL: ERR`。
3. 核心翡翠水球图层无法绘制，导致只剩外围光丝渲染在黑色背景上。

---

## 2. 修复实施

在 `AIOrbShader.kt` 中将 `renderDroplet` 的第一个参数统一更新为合法的正立视线 `baseUv`：
```glsl
// 8. L02 Internal Droplets (D1, D2, D3)
vec4 d1 = renderDroplet(baseUv, u_droplet1Uv, 0.018, u_droplet1Alpha * waterMask);
vec4 d2 = renderDroplet(baseUv, u_droplet2Uv, 0.022, u_droplet2Alpha * waterMask);
vec4 d3 = renderDroplet(baseUv, u_droplet3Uv, 0.014, u_droplet3Alpha * waterMask);
```

---

## 3. 验证结果

- **单元测试验证**：`gradle :app:testDebugUnitTest` 全部通过（BUILD SUCCESSFUL in 1m 9s）。
- **静态编译检查**：`compile_applet` 成功。
- **APK 打包发布**：`gradle assembleDebug` 成功（BUILD SUCCESSFUL in 19s）。
  - 根目录安装包：`./AIOrb_LiquidRibbon_HighFidelity_V1.apk`
  - 公共目录镜像：`public/AIOrb_LiquidRibbon_HighFidelity_V1.apk`
- **运行时状态**：UI 顶部状态栏恢复绿字 `GL: OK`，翡翠水球完整重新渲染并与倾斜光丝层级对齐。
