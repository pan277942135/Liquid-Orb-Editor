package com.example.glyph.gl

object YujianWaterInkShader {

    val VERTEX_SHADER = """
        attribute vec4 a_position;
        varying vec2 v_texCoord;

        void main() {
            gl_Position = a_position;
            // Map [-1, 1] to UV [0, 1] (y-flip for OpenGL)
            v_texCoord = vec2((a_position.x + 1.0) * 0.5, (1.0 - a_position.y) * 0.5);
        }
    """.trimIndent()

    val FRAGMENT_SHADER = """
        precision highp float;

        varying vec2 v_texCoord;

        uniform vec2 u_resolution;
        uniform float u_time;
        uniform float u_orbScale;

        // Timeline state uniforms
        uniform float u_progress;              // 0.0 to 1.0
        uniform float u_stretchX;              // 1.0 to 1.65
        uniform float u_stretchY;              // 0.82 to 1.0
        uniform float u_coreWarp;              // 0.2 to 1.5
        uniform float u_ribbonIntensity;       // 0.0 to 1.0
        uniform float u_ribbonDispersion;      // 0.0 to 1.0
        uniform float u_glyphAlpha;            // 0.0 to 1.0
        uniform float u_fluidErosion;          // 1.0 to 0.0
        uniform float u_crystalRefraction;     // 1.0 to 2.0
        uniform float u_specularGloss;         // 1.0 to 8.0
        uniform float u_inkOpacity;            // 0.0 to 1.0
        uniform float u_goldFilamentAlpha;     // 0.0 to 1.0

        // Palette Colors (Yujian Water Ink frozen profile)
        uniform vec3 u_lakeInk;                // #173E49
        uniform vec3 u_lakeTeal;               // #2E8B83
        uniform vec3 u_waterCyan;              // #67C8D2
        uniform vec3 u_deepWater;              // #174F5D
        uniform vec3 u_warmGold;               // #D8B36A
        uniform vec3 u_sunGlint;               // #F6ECC9

        // Textures
        uniform sampler2D u_glyphMaskTexture;

        // Interactive touch & rotation
        uniform vec2 u_rotation;
        uniform vec2 u_touch;
        uniform float u_touchActive;

        // 3D Simplex noise
        vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec3 permute(vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }

        float snoise(vec2 v) {
            const vec4 C = vec4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
            vec2 i  = floor(v + dot(v, C.yy));
            vec2 x0 = v - i + dot(i, C.xx);
            vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
            vec4 x12 = x0.xyxy + C.xxzz;
            x12.xy -= i1;
            i = mod289(i);
            vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
            vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
            m = m * m; m = m * m;
            vec3 x = 2.0 * fract(p * C.www) - 1.0;
            vec3 h = abs(x) - 0.5;
            vec3 ox = floor(x + 0.5);
            vec3 a0 = x - ox;
            m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
            vec3 g;
            g.x  = a0.x  * x0.x  + h.x  * x0.y;
            g.yz = a0.yz * x12.xz + h.yz * x12.yw;
            return 130.0 * dot(m, g);
        }

        // Fractal Brownian Motion (FBM)
        float fbm(vec2 p) {
            float total = 0.0;
            float amp = 0.5;
            for (int i = 0; i < 4; i++) {
                total += snoise(p) * amp;
                p *= 2.02;
                amp *= 0.5;
            }
            return total;
        }

        // Procedural Water Ribbon Flow Band
        float renderWaterRibbon(vec2 p, float offset, float freq, float speed) {
            float wave = sin(p.x * freq + u_time * speed + offset) * 0.12;
            wave += cos(p.x * freq * 1.6 - u_time * speed * 0.7) * 0.06;
            float dist = abs(p.y - wave);
            return exp(-dist * dist * 140.0);
        }

        void main() {
            // 1. Isotropic aspect ratio correction
            float minDim = min(u_resolution.x, u_resolution.y);
            vec2 aspectFactor = (minDim > 0.0) ? (u_resolution / minDim) : vec2(1.0);
            vec2 centeredCoord = (v_texCoord - vec2(0.5)) * aspectFactor;

            // Apply size scaling
            vec2 scaledCoord = centeredCoord / max(0.1, u_orbScale);

            // Distance to origin
            float baseRadius = 0.62;
            float dist = length(scaledCoord);

            // 2. Halo & Outer Atmosphere (L01)
            float haloFalloff = exp(-max(0.0, dist - 0.45) * 6.0);
            vec3 haloColor = mix(u_deepWater, u_lakeTeal, 0.4);
            vec4 halo = vec4(haloColor, haloFalloff * 0.35);

            // 3. Stretched Core Coordinates (KF02 Non-linear horizontal warp)
            vec2 stretchedCoord = vec2(scaledCoord.x / max(0.8, u_stretchX), scaledCoord.y / max(0.6, u_stretchY));
            float stretchedDist = length(stretchedCoord);

            // Fluid turbulence vector field
            vec2 flowNoise = vec2(
                fbm(stretchedCoord * 3.0 + vec2(u_time * 0.4, 0.0)),
                fbm(stretchedCoord * 3.0 + vec2(0.0, u_time * 0.35))
            ) * u_coreWarp * 0.15;

            vec2 warpedCoord = stretchedCoord + flowNoise;
            float warpedDist = length(warpedCoord);

            // Liquid Orb Sphere Mask
            float sphereEdge = smoothstep(baseRadius, baseRadius - 0.06, warpedDist);
            if (sphereEdge < 0.005 && dist > 0.85) {
                gl_FragColor = halo;
                return;
            }

            // 4. Liquid Glass Core Rendering
            // Normal estimation for sphere
            vec3 normal = vec3(warpedCoord / baseRadius, 0.0);
            float zSq = 1.0 - dot(normal.xy, normal.xy);
            normal.z = sqrt(max(0.0, zSq));

            // View direction
            vec3 viewDir = vec3(0.0, 0.0, 1.0);
            float fresnel = pow(1.0 - max(0.0, dot(normal, viewDir)), 2.8);

            // Light source: upper-left soft sunlight (0.24, 0.20)
            vec3 lightDir = normalize(vec3(-0.45, -0.65, 0.75));
            vec3 halfDir = normalize(lightDir + viewDir);
            float specular = pow(max(0.0, dot(normal, halfDir)), u_specularGloss * 8.0);

            // Liquid color ramp inside sphere
            float depthFactor = clamp(1.0 - warpedDist / baseRadius, 0.0, 1.0);
            vec3 liquidBase = mix(u_deepWater, u_lakeInk, 0.6);
            liquidBase = mix(liquidBase, u_lakeTeal, depthFactor * 0.7);
            liquidBase = mix(liquidBase, u_waterCyan, fresnel * 0.85);

            // 5. Procedural Bezier Water Ribbons (KF03 Water Ribbon Release)
            if (u_ribbonIntensity > 0.01) {
                float r1 = renderWaterRibbon(scaledCoord, 0.0, 4.2, 1.8);
                float r2 = renderWaterRibbon(scaledCoord, 1.8, 5.6, -1.4);
                float r3 = renderWaterRibbon(scaledCoord * vec2(1.2, 0.9), 3.4, 3.8, 1.1);

                vec3 ribbonCol1 = mix(u_waterCyan, u_sunGlint, 0.3) * r1;
                vec3 ribbonCol2 = mix(u_lakeTeal, u_waterCyan, 0.6) * r2;
                vec3 ribbonCol3 = mix(u_warmGold, u_waterCyan, 0.5) * r3;

                vec3 ribbons = (ribbonCol1 + ribbonCol2 + ribbonCol3) * u_ribbonIntensity;
                liquidBase += ribbons;
            }

            // 6. Dynamic Chinese Species Glyph Condensation (KF04, KF05, KF06)
            vec3 finalColor = liquidBase;
            float finalAlpha = sphereEdge;

            if (u_glyphAlpha > 0.01) {
                // Map screen coords to glyph texture UV [0, 1]
                // Centered and scaled with gentle breathing
                vec2 glyphUv = scaledCoord * vec2(1.0 / max(0.9, u_stretchX * 0.85), 1.0) * 0.85 + vec2(0.5);

                // Add fluid disturbance erosion (high in KF04, dissolves by KF06)
                if (u_fluidErosion > 0.02) {
                    vec2 erosionOffset = vec2(
                        snoise(glyphUv * 6.0 + vec2(u_time * 0.8, 0.0)),
                        snoise(glyphUv * 6.0 + vec2(0.0, u_time * 0.7))
                    ) * u_fluidErosion * 0.045;
                    glyphUv += erosionOffset;
                }

                // Sample multi-channel mask
                if (glyphUv.x >= 0.0 && glyphUv.x <= 1.0 && glyphUv.y >= 0.0 && glyphUv.y <= 1.0) {
                    vec4 glyphMask = texture2D(u_glyphMaskTexture, glyphUv);
                    float coreStroke = glyphMask.r;        // Sharp stroke fill
                    float rimRefraction = glyphMask.g;     // Dilated edge rim
                    float goldFilament = glyphMask.b;      // Centerline accent

                    // Calligraphy ink body (Deep lake ink #173E49 with lake teal translucency)
                    vec3 inkBody = mix(u_lakeTeal, u_lakeInk, u_inkOpacity);

                    // Crystal glass refraction & caustics along stroke edges
                    float strokeRim = max(0.0, rimRefraction - coreStroke);
                    vec3 rimColor = mix(u_waterCyan, u_sunGlint, 0.4) * (strokeRim * u_crystalRefraction);

                    // Gold filament shimmer along calligraphy strokes (#D8B36A)
                    float goldSparkle = goldFilament * (0.7 + 0.3 * sin(u_time * 3.5 + glyphUv.x * 12.0));
                    vec3 goldCol = u_warmGold * goldSparkle * u_goldFilamentAlpha;

                    // Specular glint on strokes
                    float glyphSpec = pow(coreStroke * rimRefraction, 4.0) * (specular * 1.5);
                    vec3 glintCol = u_sunGlint * glyphSpec;

                    // Composite glyph onto water sphere
                    vec3 glyphComposite = mix(inkBody, inkBody + rimColor, 0.6);
                    glyphComposite += goldCol;
                    glyphComposite += glintCol;

                    float glyphVis = max(coreStroke, rimRefraction * 0.5) * u_glyphAlpha;
                    finalColor = mix(finalColor, glyphComposite, glyphVis);
                    finalAlpha = max(finalAlpha, glyphVis);
                }
            }

            // Add overall specular sheen & edge highlight
            finalColor += u_sunGlint * specular * 0.65;
            finalColor += u_waterCyan * fresnel * 0.35;

            // Output final pixel with alpha compositing
            vec4 orbPixel = vec4(finalColor, finalAlpha);
            gl_FragColor = mix(halo, orbPixel, orbPixel.a);
        }
    """.trimIndent()
}
