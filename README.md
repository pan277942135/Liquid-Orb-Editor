# Liquid Orb Editor (Android)

Inspired by [LerSent001/orb](https://github.com/LerSent001/orb), this project brings the real-time **Liquid Glass Orb Editor** to Android with hardware-accelerated OpenGL ES shaders and Jetpack Compose.

## Key Features

1. **13 Dynamic Presets**:
   - **Siri Wave** (Siri 动态光环)
   - **Voice Membrane** (语音共振膜)
   - **Aurora Veil** (极光之幔)
   - **Neural Plasma** (神经等离子体)
   - **Prismatic Field** (棱镜场)
   - **Violet Core** (紫晶核心)
   - **Crystal Drop** (纯净水晶水滴)
   - **Violet Ember** (紫烬熔岩)
   - **Refractive Gel** (折射凝胶)
   - **Chromatic Metal** (炫彩液态金属)
   - **Frost Flow** (极寒冰流)
   - **Liquid Chrome** (液态水银)
   - **Iridescent Opal** (变彩欧泊宝石)

2. **Real-time Glass Optics & Fluid Physics**:
   - 3D Simplex & Fractal Brownian Motion (FBM) fluid turbulence
   - Real-time refractive index (IOR) & chromatic dispersion (spectral RGB separation)
   - Specular gloss highlights, Fresnel rim sheen & exponential outer glow
   - Multi-layer liquid color ramps

3. **Audio Reactivity**:
   - **Rhythm Beat Mode (Auto)**: Built-in synthetic audio engine to test without permissions
   - **Microphone Mode (Live)**: Real-time frequency analysis (Bass / Mid / Treble) modulating contour, turbulence, and sparkles

4. **Interactive Touch Controls**:
   - Drag to rotate the 3D liquid sphere
   - Touch ripple deformation physics
   - Fullscreen immersive visualizer toggle

5. **Cross-Platform Code & Config Export**:
   - Export to **WebGPU (WGSL)**
   - Export to native **SwiftUI / Metal**
   - Export to **Android AGSL / GLSL**
   - Export to **JSON Configuration**
