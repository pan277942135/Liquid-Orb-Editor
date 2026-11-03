package com.example.aiorb.gl

object AIOrbShader {

    val VERTEX_SHADER = """
        attribute vec4 a_position;
        varying vec2 v_texCoord;
        void main() {
            gl_Position = a_position;
            // Map [-1, 1] to UV [0, 1]
            v_texCoord = vec2((a_position.x + 1.0) * 0.5, (1.0 - a_position.y) * 0.5);
        }
    """.trimIndent()

    val FRAGMENT_SHADER = """
        precision highp float;

        varying vec2 v_texCoord;

        uniform vec2 u_resolution;
        uniform float u_time;
        uniform float u_scale; // S1 / S2 breath scale (1.0 +- amplitude)

        // Textures
        uniform sampler2D u_coreTexture;      // Immutable 透明湖水玻璃球.png
        uniform sampler2D u_grassMaskTexture; // R01B grass mask

        // State & Optical Controls
        uniform float u_wakeProgress;          // 0.0 (S1) to 1.0 (S2 ready)
        uniform float u_filamentAlphaProgress; // 0.0 to 1.0 (450ms smoothstep)
        uniform float u_releaseProgress;       // 0.0 to 1.0 during RELEASE / RETURN
        uniform int u_isS2;                   // 0 = S1, 1 = S2

        // Grass Optical Deformation (Scheme B)
        uniform vec2 u_grassDisplacementBack;  // (dx, dy) in UV
        uniform vec2 u_grassDisplacementFront; // (dx, dy) in UV
        uniform float u_grassMotionWeight;     // Motion intensity multiplier (default 1.0)
        uniform float u_grassLightResponse;    // Up to 0.10

        // Sunlight Parameters
        uniform vec2 u_sunlightSourceUV;       // Fixed UV (0.24, 0.20)
        uniform vec3 u_sunlightColor;          // #D8BC87
        uniform float u_glassSpecularAlpha;    // Up to 0.11
        uniform float u_coreRefractionAlpha;   // Up to 0.11

        // Caustics Parameters
        uniform float u_causticsAlpha;         // Up to 0.19
        uniform float u_causticsDeepTime;
        uniform float u_causticsSurfaceTime;

        // Halo
        uniform float u_haloAlpha;             // Up to 0.08 in S2

        // Droplets Data (D1, D2, D3 positions & alphas)
        uniform vec2 u_droplet1Uv;
        uniform float u_droplet1Alpha;
        uniform vec2 u_droplet2Uv;
        uniform float u_droplet2Alpha;
        uniform vec2 u_droplet3Uv;
        uniform float u_droplet3Alpha;

        // Simplex/Perlin noise helper for organic caustics
        vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec3 permute(vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }

        float snoise(vec2 v) {
            const vec4 C = vec4(0.211324865405187,  // (3.0-sqrt(3.0))/6.0
                                0.366025403784439,  // 0.5*(sqrt(3.0)-1.0)
                                -0.577350269189626, // -1.0 + 2.0 * C.x
                                0.024390243902439); // 1.0 / 41.0
            vec2 i  = floor(v + dot(v, C.yy));
            vec2 x0 = v -   i + dot(i, C.xx);
            vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
            vec4 x12 = x0.xyxy + C.xxzz;
            x12.xy -= i1;
            i = mod289(i);
            vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
            vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
            m = m * m;
            m = m * m;
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

        // Evaluate single droplet lens & specular
        vec4 renderDroplet(vec2 uv, vec2 center, float radius, float alpha) {
            vec2 diff = uv - center;
            float dist = length(diff);
            if (dist > radius) return vec4(0.0);

            float normDist = dist / radius;
            // Lens rim darkening & caustic ring
            float rim = smoothstep(0.6, 0.95, normDist) * 0.4;
            // Specular spot on upper-left
            vec2 specPos = center + vec2(-radius * 0.35, -radius * 0.35);
            float specDist = length(uv - specPos);
            float spec = exp(-specDist * specDist / (radius * radius * 0.08)) * 0.7;

            vec3 dropletCol = mix(vec3(0.95, 0.98, 0.96), vec3(1.0, 0.96, 0.84), spec);
            return vec4(dropletCol, (rim + spec) * alpha);
        }

        void main() {
            // Screen aspect ratio correction so the sphere remains perfectly circular on any screen
            float minDim = min(u_resolution.x, u_resolution.y);
            vec2 aspectFactor = (minDim > 0.0) ? (u_resolution / minDim) : vec2(1.0);
            vec2 centeredUv = (v_texCoord - vec2(0.5)) * aspectFactor;
            vec2 uv = centeredUv / u_scale + vec2(0.5);

            float distFromCenter = length(centeredUv / u_scale);

            // 1. Environmental Halo (L01) - SCREEN blend mode behind core
            vec3 haloColor = vec3(0.043, 0.466, 0.458); // #0B7775
            float haloFalloff = exp(-max(0.0, distFromCenter - 0.45) * 8.0);
            vec4 halo = vec4(haloColor, u_haloAlpha * haloFalloff * (0.2 + 0.8 * u_wakeProgress));

            // Out-of-bounds check for the sphere
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
                gl_FragColor = halo;
                return;
            }

            // 2. Read Grass Mask (R01B derived)
            vec4 grassMask = texture2D(u_grassMaskTexture, uv);
            float backMask = grassMask.r;  // Back leaves
            float frontMask = grassMask.g; // Front leaves
            float combinedMask = max(backMask, frontMask);

            // 3. Scheme B: Local source-sampled optical UV displacement
            // Displaces sample coordinates according to computed leaf bending vector
            vec2 displacement = (u_grassDisplacementBack * backMask + u_grassDisplacementFront * frontMask) * u_grassMotionWeight;
            vec2 displacedUv = clamp(uv - displacement, 0.0, 1.0);

            vec4 originalCore = texture2D(u_coreTexture, uv);
            vec4 displacedCore = texture2D(u_coreTexture, displacedUv);

            // Optical deformation mix: only displaces within the mask region, without duplicating leaves
            vec4 coreColor = mix(originalCore, displacedCore, combinedMask);

            if (coreColor.a < 0.01) {
                gl_FragColor = halo;
                return;
            }

            // 4. L04 Internal Caustics (Dual flow: Deep 6500ms + Surface 4100ms)
            // Clipped to internal water radius: innerRadiusD = 0.385, edgeFadeD = 0.035
            float waterRadius = 0.385;
            float waterEdge = 0.035;
            float waterMask = smoothstep(waterRadius, waterRadius - waterEdge, distFromCenter);

            float causticDeep = snoise(uv * 5.5 + vec2(u_causticsDeepTime * 0.015, u_causticsDeepTime * 0.004));
            float causticSurf = snoise(uv * 10.0 + vec2(-u_causticsSurfaceTime * 0.006, u_causticsSurfaceTime * 0.011));
            float causticPattern = clamp((causticDeep * 0.4 + causticSurf * 0.6) * 0.5 + 0.5, 0.0, 1.0);
            vec3 causticColor = vec3(0.55, 0.88, 0.82) * causticPattern;
            float causticsAlpha = u_causticsAlpha * waterMask;

            // 5. L05 Aquatic Grass Light Response (#A9D9C5)
            vec3 grassResponseCol = vec3(0.663, 0.851, 0.773) * u_grassLightResponse * combinedMask;

            // 6. L07 Sunlight Highlight & Refraction (Fixed upper-left UV 0.24, 0.20)
            vec2 sunVec = uv - u_sunlightSourceUV;
            float sunDist = length(sunVec);
            float sunGlint = exp(-sunDist * sunDist * 18.0) * u_glassSpecularAlpha;
            float coreRefraction = exp(-sunDist * sunDist * 6.0) * u_coreRefractionAlpha;
            vec3 sunHighlight = u_sunlightColor * (sunGlint + coreRefraction);

            // 7. Composite Core + Internal Light Responses
            vec3 finalRgb = coreColor.rgb;

            // Add caustics
            finalRgb = mix(finalRgb, finalRgb + causticColor, causticsAlpha);

            // Add grass light response (confined to mask)
            finalRgb += grassResponseCol;

            // Add sunlight specular & refraction
            finalRgb += sunHighlight;

            // 8. L02 Internal Droplets (D1, D2, D3)
            vec4 d1 = renderDroplet(uv, u_droplet1Uv, 0.018, u_droplet1Alpha * waterMask);
            vec4 d2 = renderDroplet(uv, u_droplet2Uv, 0.022, u_droplet2Alpha * waterMask);
            vec4 d3 = renderDroplet(uv, u_droplet3Uv, 0.014, u_droplet3Alpha * waterMask);
            vec4 droplets = d1 + d2 + d3;
            finalRgb = mix(finalRgb, droplets.rgb, droplets.a);

            // Composite over halo
            vec4 finalOrb = vec4(finalRgb, coreColor.a);
            gl_FragColor = mix(halo, finalOrb, finalOrb.a);
        }
    """.trimIndent()
}
