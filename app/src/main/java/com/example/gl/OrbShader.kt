package com.example.gl

object OrbShader {

    val VERTEX_SHADER = """
        attribute vec4 a_position;
        varying vec2 v_texCoord;
        void main() {
            gl_Position = a_position;
            v_texCoord = (a_position.xy + 1.0) * 0.5;
        }
    """.trimIndent()

    val FRAGMENT_SHADER = """
        precision highp float;

        varying vec2 v_texCoord;

        uniform vec2 u_resolution;
        uniform float u_time;
        uniform float u_speed;
        uniform float u_radius;
        uniform float u_zoom;
        uniform float u_warp;
        uniform float u_ridgeAmt;
        uniform float u_sharp;
        uniform float u_shade;
        uniform float u_sheen;
        uniform float u_gloss;
        uniform float u_refraction;
        uniform float u_dispersion;
        uniform float u_roughness;
        uniform float u_glow;
        uniform float u_edgeSoftness;

        uniform vec3 u_color1;
        uniform vec3 u_color2;
        uniform vec3 u_accentColor;
        uniform vec3 u_coreColor;
        uniform vec3 u_glowColor;

        uniform vec2 u_touch;
        uniform float u_touchActive;
        uniform vec2 u_rotation;

        uniform float u_audioBass;
        uniform float u_audioMid;
        uniform float u_audioTreble;

        uniform int u_sceneBg;

        // 3D Simplex-like Perlin noise implementation
        vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec4 permute(vec4 x) { return mod289(((x * 34.0) + 1.0) * x); }
        vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

        float snoise(vec3 v) {
            const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
            const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

            vec3 i  = floor(v + dot(v, C.yyy));
            vec3 x0 = v - i + dot(i, C.xxx);

            vec3 g = step(x0.yzx, x0.xyz);
            vec3 l = 1.0 - g;
            vec3 i1 = min(g.xyz, l.zxy);
            vec3 i2 = max(g.xyz, l.zxy);

            vec3 x1 = x0 - i1 + C.xxx;
            vec3 x2 = x0 - i2 + C.yyy;
            vec3 x3 = x0 - D.yyy;

            i = mod289(i);
            vec4 p = permute(permute(permute(
                        i.z + vec4(0.0, i1.z, i2.z, 1.0))
                    + i.y + vec4(0.0, i1.y, i2.y, 1.0))
                    + i.x + vec4(0.0, i1.x, i2.x, 1.0));

            float n_ = 0.142857142857;
            vec3 ns = n_ * D.wyz - D.xzx;

            vec4 j = p - 49.0 * floor(p * ns.z * ns.z);

            vec4 x_ = floor(j * ns.z);
            vec4 y_ = floor(j - 7.0 * x_);

            vec4 x = x_ *ns.x + ns.yyyy;
            vec4 y = y_ *ns.x + ns.yyyy;
            vec4 h = 1.0 - abs(x) - abs(y);

            vec4 b0 = vec4(x.xy, y.xy);
            vec4 b1 = vec4(x.zw, y.zw);

            vec4 s0 = floor(b0) * 2.0 + 1.0;
            vec4 s1 = floor(b1) * 2.0 + 1.0;
            vec4 sh = -step(h, vec4(0.0));

            vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
            vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

            vec3 p0 = vec3(a0.xy, h.x);
            vec3 p1 = vec3(a0.zw, h.y);
            vec3 p2 = vec3(a1.xy, h.z);
            vec3 p3 = vec3(a1.zw, h.w);

            vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
            p0 *= norm.x;
            p1 *= norm.y;
            p2 *= norm.z;
            p3 *= norm.w;

            vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
            m = m * m;
            return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
        }

        // Optimized 2-octave Fractal Brownian Motion
        float fbm(vec3 p) {
            return 0.65 * snoise(p) + 0.35 * snoise(p * 2.08 + vec3(4.1, 1.9, 3.2));
        }

        // 3D rotation around X and Y
        vec3 rotateXY(vec3 p, vec2 rot) {
            float cx = cos(rot.x);
            float sx = sin(rot.x);
            float cy = cos(rot.y);
            float sy = sin(rot.y);
            vec3 r1 = vec3(p.x, p.y * cx - p.z * sx, p.y * sx + p.z * cx);
            return vec3(r1.x * cy + r1.z * sy, r1.y, -r1.x * sy + r1.z * cy);
        }

        // Background environment rendering
        vec3 renderBackground(vec2 uv, int bgType) {
            if (bgType == 1) {
                return vec3(0.005, 0.005, 0.008);
            } else if (bgType == 2) {
                vec3 darkBg = vec3(0.03, 0.02, 0.07);
                vec2 g = abs(fract(uv * 12.0) - 0.5);
                float line = smoothstep(0.04, 0.01, min(g.x, g.y)) * 0.18;
                return darkBg + vec3(0.12, 0.05, 0.28) * line;
            } else if (bgType == 3) {
                float spotlight = max(0.0, 1.0 - length(uv) * 1.2);
                return mix(vec3(0.02, 0.03, 0.05), vec3(0.15, 0.17, 0.22), spotlight * spotlight);
            } else if (bgType == 4) {
                float aurora = sin(uv.x * 3.0 + u_time * 0.5) * 0.5 + 0.5;
                vec3 colA = vec3(0.02, 0.08, 0.09);
                vec3 colB = vec3(0.08, 0.03, 0.12);
                return mix(colA, colB, aurora);
            } else {
                float len = length(uv);
                vec3 center = vec3(0.06, 0.04, 0.14);
                vec3 edge = vec3(0.01, 0.01, 0.03);
                return mix(center, edge, smoothstep(0.1, 1.1, len));
            }
        }

        void main() {
            vec2 minRes = vec2(min(u_resolution.x, u_resolution.y));
            vec2 uv = (gl_FragCoord.xy - 0.5 * u_resolution) / minRes.x;

            // Audio-driven radius expansion
            float activeRadius = u_radius * (1.0 + u_audioBass * 0.14);
            float dist = length(uv);
            float normDist = dist / max(0.001, activeRadius);

            // Background color
            vec3 bgColor = renderBackground(uv, u_sceneBg);

            // Outer soft glow
            float glowEdge = max(0.001, u_edgeSoftness * 2.2);
            float glowDist = max(0.0, normDist - 0.95);
            float glowFalloff = exp(-glowDist / glowEdge);
            float audioGlow = 1.0 + u_audioBass * 0.35 + u_audioTreble * 0.2;
            vec3 outerGlow = u_glowColor * (u_glow * glowFalloff * audioGlow * 0.85);

            if (normDist > 1.25) {
                gl_FragColor = vec4(bgColor + outerGlow, 1.0);
                return;
            }

            // Ray-sphere intersection
            float zSq = 1.0 - normDist * normDist;
            float z = sqrt(max(0.0, zSq));
            vec3 sphereP = vec3(uv / activeRadius, z);

            // Apply interactive drag rotation
            sphereP = rotateXY(sphereP, u_rotation);

            // Interactive touch ripple
            if (u_touchActive > 0.5) {
                float touchD = length(uv - u_touch);
                float ripple = sin(touchD * 28.0 - u_time * 8.0) * exp(-touchD * 4.5) * 0.22;
                sphereP += vec3(ripple * 0.4, ripple * 0.4, ripple);
            }

            // Animated noise coordinate
            float timeVal = u_time * u_speed;
            vec3 noiseCoord = sphereP * u_zoom + vec3(0.0, 0.0, timeVal * 0.35);

            // Fluid warp displacement with audio mid influence
            float warpScale = u_warp * (1.0 + u_audioMid * 0.28);
            vec3 warpQ = vec3(
                fbm(noiseCoord),
                fbm(noiseCoord + vec3(5.2, 1.3, 2.7)),
                fbm(noiseCoord + vec3(1.9, 8.4, 4.1))
            );
            vec3 warpedPos = noiseCoord + warpQ * warpScale;

            // Multi-scale noise evaluation
            float n = fbm(warpedPos);

            // Ridge amount computation
            float ridge = 1.0 - abs(n * 2.0 - 1.0);
            ridge = pow(clamp(ridge, 0.001, 1.0), max(0.1, u_ridgeAmt));

            // Fast forward gradient for normal
            float eps = 0.035;
            vec3 grad = vec3(
                fbm(warpedPos + vec3(eps, 0.0, 0.0)) - n,
                fbm(warpedPos + vec3(0.0, eps, 0.0)) - n,
                fbm(warpedPos + vec3(0.0, 0.0, eps)) - n
            ) / eps;

            // Perturbed normal
            vec3 N = normalize(sphereP + grad * (warpScale * 0.32));
            vec3 V = vec3(0.0, 0.0, 1.0);

            // Fresnel rim lighting
            float NdotV = max(0.0, dot(N, V));
            float fresnel = pow(1.0 - NdotV, max(0.2, u_sharp)) * u_shade;

            // Chromatic dispersion refraction
            float baseIor = 1.0 + u_refraction;
            float disp = u_dispersion * 0.25;

            vec3 refR = refract(-V, N, 1.0 / (baseIor - disp));
            vec3 refB = refract(-V, N, 1.0 / (baseIor + disp));

            float depthG = n;
            float depthR = fbm(sphereP * u_zoom + refR * 0.35 + vec3(0.0, 0.0, timeVal * 0.25));
            float depthB = fbm(sphereP * u_zoom + refB * 0.35 + vec3(0.0, 0.0, timeVal * 0.25));

            // Internal liquid gradient blending
            float internalGrad = clamp(ridge * 0.65 + (depthG * 0.5 + 0.2), 0.0, 1.0);
            vec3 liquidColor = mix(u_coreColor, u_color1, smoothstep(0.0, 0.55, internalGrad));
            liquidColor = mix(liquidColor, u_color2, smoothstep(0.35, 0.85, internalGrad));
            liquidColor = mix(liquidColor, u_accentColor, smoothstep(0.70, 1.0, internalGrad * (1.0 + u_audioMid * 0.3)));

            // Add chromatic dispersion tint
            liquidColor.r += depthR * u_dispersion * 0.35;
            liquidColor.b += depthB * u_dispersion * 0.40;

            // Dual specular highlights (Phone / Blinn-Phong)
            vec3 light1 = normalize(vec3(0.65, 0.85, 1.1));
            vec3 H1 = normalize(light1 + V);
            float specPower = exp2(u_gloss * 1.35 + 2.0);
            float spec1 = pow(max(0.0, dot(N, H1)), specPower) * u_sheen;

            vec3 light2 = normalize(vec3(-0.55, -0.45, 0.9));
            vec3 H2 = normalize(light2 + V);
            float spec2 = pow(max(0.0, dot(N, H2)), specPower * 0.5) * (u_sheen * 0.38);

            // Audio treble shimmer on specular highlights
            float trebleSparkle = 1.0 + u_audioTreble * 0.6;
            vec3 specular = (vec3(1.0, 0.98, 0.95) * spec1 + u_accentColor * spec2) * trebleSparkle;

            // Glass rim sheen
            vec3 rimColor = mix(u_glowColor, vec3(1.0), 0.45) * (fresnel * u_sheen);

            // Composite internal liquid glass color
            vec3 orbColor = liquidColor + rimColor + specular;

            // Edge anti-aliasing feathering
            float edgeSoft = max(0.005, u_edgeSoftness);
            float sphereAlpha = smoothstep(1.0, 1.0 - edgeSoft, normDist);

            // Final blended pixel color
            vec3 finalColor = mix(bgColor, orbColor, sphereAlpha) + outerGlow;

            gl_FragColor = vec4(finalColor, 1.0);
        }
    """.trimIndent()
}
