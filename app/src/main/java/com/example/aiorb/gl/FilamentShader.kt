package com.example.aiorb.gl

object FilamentShader {

    val VERTEX_SHADER = """
        attribute vec2 a_position;
        attribute vec2 a_uv;
        varying vec2 v_uv;

        uniform vec2 u_resolution;
        uniform float u_scale;
        uniform float u_coreVisibleRadiusUv;

        void main() {
            // Map OrbLocal D units to screen NDC
            // At core radius = 0.5D, positionScale matches visible core edge on screen
            float posScale = 4.0 * u_coreVisibleRadiusUv * u_scale;
            vec2 scaled = a_position * posScale;
            float minDim = min(u_resolution.x, u_resolution.y);
            if (minDim > 0.0) {
                scaled = scaled * (minDim / u_resolution);
            }
            // Map to screen clip coordinates (SVG y-down to GL clip y-up)
            gl_Position = vec4(scaled.x, -scaled.y, 0.0, 1.0);
            v_uv = a_uv;
        }
    """.trimIndent()

    val FRAGMENT_SHADER = """
        precision mediump float;
        varying vec2 v_uv;

        uniform vec4 u_baseColor;        // rgb + alphaMax
        uniform vec4 u_highlightColor;   // rgb + alphaMax
        uniform vec4 u_refractionColor;  // rgb + alphaMax
        uniform vec4 u_layerParams;      // x: headBoost, y: alphaMult, z: specLen, w: isGold
        uniform int u_diagnosticMode;     // 0 = RUNTIME, 1 = GEOMETRY_ONLY, 2 = MATERIAL
        uniform float u_glowIntensity;   // HDR Bloom / Glow multiplier (default 1.8)

        void main() {
            float u = clamp(v_uv.x, 0.0, 1.0);
            float v = clamp(v_uv.y, -1.0, 1.0);
            float dist = abs(v);

            // Tail Feather Taper (Smooth fade in along arc-length from root)
            float tailFade = smoothstep(0.0, 0.16, u);

            // Traveling Specular Highlight along head
            float specLen = max(u_layerParams.z, 0.08);
            float headProximity = clamp((u - (1.0 - specLen)) / specLen, 0.0, 1.0);
            float specGlint = headProximity * (1.0 - smoothstep(0.0, 0.45, dist));

            // --- DIAGNOSTIC MODE 1: GEOMETRY ONLY ---
            if (u_diagnosticMode == 1) {
                float centerSpine = 1.0 - smoothstep(0.0, 0.22, dist);
                vec3 diagCol = mix(u_highlightColor.rgb, vec3(1.0, 1.0, 1.0), centerSpine * 0.85);
                float diagAlpha = clamp(0.80 + 0.18 * centerSpine, 0.0, 0.98);
                gl_FragColor = vec4(diagCol * diagAlpha, diagAlpha);
                return;
            }

            // --- DIAGNOSTIC MODE 2: MATERIAL DIFFERENTIATION ---
            if (u_diagnosticMode == 2) {
                float matBody = 1.0 - smoothstep(0.65, 0.98, dist);
                vec3 matCol = mix(u_baseColor.rgb * 1.6, u_highlightColor.rgb * 1.4, specGlint * 0.90);
                float matAlpha = clamp(u_baseColor.a * 2.0 * matBody, 0.0, 0.98);
                gl_FragColor = vec4(matCol * matAlpha, matAlpha);
                return;
            }

            // --- HIGH-FIDELITY RUNTIME MODE (0): HDR GLOW & VOLUMETRIC LIQUID OPTICS ---
            float glowMultiplier = max(u_glowIntensity, 0.5);

            // 1. Central White-Hot Energy Spine (Core brilliance)
            float spine = exp(-dist * dist * 32.0);
            vec3 spineCol = mix(u_highlightColor.rgb, vec3(1.0, 1.0, 0.98), spine * 0.92);

            // 2. Translucent Volumetric Ribbon Body (Saturated jewel tone)
            float bodyFactor = 1.0 - smoothstep(0.06, 0.86, dist);
            vec3 bodyCol = u_baseColor.rgb * 1.45;

            // 3. Radiant Outer Bloom Glow Envelope
            float outerGlow = exp(-dist * dist * 3.8);
            vec3 glowCol = u_highlightColor.rgb * outerGlow * 1.8 * glowMultiplier;

            // 4. Solar Glint Burst / Flare (Blinding incandescent flare)
            float flareIntensity = specGlint * 2.6 * glowMultiplier;
            vec3 flareCol = vec3(1.0, 0.98, 0.88) * flareIntensity;

            // 5. Asymmetric Inner Refractive Band
            float refrDist = abs(v + 0.12);
            float refrBand = 1.0 - smoothstep(0.0, 0.42, refrDist);
            vec3 refrCol = u_refractionColor.rgb * refrBand * 0.6;

            // 6. Warm Gold Filament Special Sparkle
            vec3 goldBonus = vec3(0.0);
            if (u_layerParams.w > 0.5) {
                float fineLine = 1.0 - smoothstep(0.0, 0.28, dist);
                goldBonus = vec3(1.0, 0.92, 0.70) * fineLine * 0.85 * glowMultiplier;
            }

            // High dynamic range composition
            vec3 finalRgb = mix(bodyCol, spineCol, spine * 0.85);
            finalRgb += glowCol * 0.65;
            finalRgb += flareCol;
            finalRgb += refrCol;
            finalRgb += goldBonus;

            // Alpha calculation: solid core + smooth falloff
            float alpha = clamp(bodyFactor * 0.88 + spine * 0.65 + outerGlow * 0.45 + flareIntensity * 0.7, 0.0, 1.0);

            // Head brightness boost & global alpha scaling
            float headBoost = u_layerParams.x;
            float alphaMult = u_layerParams.y;
            alpha = clamp(alpha * tailFade * headBoost * alphaMult, 0.0, 1.0);

            // Premultiplied alpha output
            gl_FragColor = vec4(finalRgb * alpha, alpha);
        }
    """.trimIndent()
}
