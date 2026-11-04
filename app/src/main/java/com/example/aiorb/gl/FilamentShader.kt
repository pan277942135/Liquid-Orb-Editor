package com.example.aiorb.gl

object FilamentShader {

    val VERTEX_SHADER = """
        attribute vec2 a_position;
        attribute vec2 a_uv;
        varying vec2 v_uv;
        varying vec2 v_orbPos;

        uniform vec2 u_resolution;
        uniform float u_scale;
        uniform float u_coreVisibleRadiusUv;
        uniform float u_orbitInclination; // Master design inclination angle (-30 deg = -0.5235988 rad)

        void main() {
            // Apply master orbital inclination: tilts the entire filament system by -30 deg
            // aligning with the master diagonal trajectory (top-left ~10:30 to bottom-right ~4:30)
            float cosInc = cos(u_orbitInclination);
            float sinInc = sin(u_orbitInclination);
            vec2 rotatedPos = vec2(
                a_position.x * cosInc - a_position.y * sinInc,
                a_position.x * sinInc + a_position.y * cosInc
            );

            // Map OrbLocal D units to screen NDC
            // At core radius = 0.5D, positionScale matches visible core edge on screen
            float posScale = 4.0 * u_coreVisibleRadiusUv * u_scale;
            vec2 scaled = rotatedPos * posScale;
            float minDim = min(u_resolution.x, u_resolution.y);
            if (minDim > 0.0) {
                scaled = scaled * (minDim / u_resolution);
            }
            // Map to screen clip coordinates (SVG y-down to GL clip y-up)
            gl_Position = vec4(scaled.x, -scaled.y, 0.0, 1.0);
            v_uv = a_uv;
            v_orbPos = rotatedPos;
        }
    """.trimIndent()

    val FRAGMENT_SHADER = """
        precision mediump float;
        varying vec2 v_uv;
        varying vec2 v_orbPos;

        uniform vec4 u_baseColor;        // rgb + alphaMax
        uniform vec4 u_highlightColor;   // rgb + alphaMax
        uniform vec4 u_refractionColor;  // rgb + alphaMax
        uniform vec4 u_layerParams;      // x: headBoost, y: alphaMult, z: specLen, w: isGold
        uniform int u_diagnosticMode;     // 0 = RUNTIME, 1 = GEOMETRY_ONLY, 2 = MATERIAL
        uniform float u_glowIntensity;   // Optical vibrance multiplier (default ~1.2)

        void main() {
            float u = clamp(v_uv.x, 0.0, 1.0);
            float v = clamp(v_uv.y, -1.0, 1.0);
            float dist = abs(v);

            // Natural Longitudinal Taper & Graceful Tip Fade (both tail and head fade to transparent tips)
            float lenTaper = pow(sin(u * 3.14159265), 0.72);
            float tailFade = smoothstep(0.0, 0.12, u);
            float headFade = smoothstep(1.0, 0.88, u);
            float lengthAlpha = lenTaper * tailFade * headFade;

            // Traveling Specular Highlight along leading head
            float specLen = max(u_layerParams.z, 0.12);
            float headProximity = clamp((u - (1.0 - specLen)) / specLen, 0.0, 1.0);
            float specGlint = pow(headProximity, 1.6) * (1.0 - smoothstep(0.0, 0.65, dist));

            // Alignment with Scene Sunlight (Sun source in top-left quadrant, ~10:30 o'clock)
            // In OrbLocal D space, center is (0,0), top-left sun direction is (-0.52, -0.68)
            vec2 sunDir = normalize(vec2(-0.52, -0.68));
            float posLen = length(v_orbPos);
            float sunAlign = 0.0;
            if (posLen > 0.001) {
                sunAlign = max(0.0, dot(v_orbPos / posLen, sunDir));
            }
            float sunFlare = pow(sunAlign, 3.2);

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
                vec3 matCol = mix(u_baseColor.rgb * 1.4, u_highlightColor.rgb * 1.2, specGlint * 0.85);
                float matAlpha = clamp(u_baseColor.a * 1.5 * matBody, 0.0, 0.95);
                gl_FragColor = vec4(matCol * matAlpha, matAlpha);
                return;
            }

            // --- HIGH-FIDELITY RUNTIME MODE (0): RADIANT LIQUID SILK & CRYSTAL AMBER OPTICS ---
            bool isGold = (u_layerParams.w > 0.5);
            vec3 emittedLight = vec3(0.0);
            vec3 fluidColor = vec3(0.0);
            float fluidAlpha = 0.0;

            if (isGold) {
                // === MATERIAL 1: RADIANT AMBER GOLD CORONA RING (CRYSTALLINE LIGHT TUBE) ===
                // 1. Translucent Liquid Amber Core Body
                float bodyFactor = 1.0 - smoothstep(0.48, 0.98, dist);
                fluidColor = mix(u_baseColor.rgb, u_highlightColor.rgb, (1.0 - dist) * 0.45);
                fluidAlpha = bodyFactor * 0.48;

                // 2. Dual-Rim Specular Edge Sheen (Crystalline reflections at tube outer boundaries)
                float dualRim = exp(-pow((dist - 0.86) / 0.10, 2.0));
                vec3 rimLight = mix(u_highlightColor.rgb, vec3(1.0, 0.98, 0.92), 0.80) * (dualRim * 1.35);

                // 3. Central Hot Crystalline Spine (Intense golden energy core)
                float coreSpine = exp(-dist * dist * 32.0);
                vec3 coreLight = mix(u_baseColor.rgb * 1.4, vec3(1.0, 1.0, 0.95), coreSpine * 0.70) * (coreSpine * 1.5);

                // 4. Sunlight Glint & Head Traveling Flash
                float glintAmount = specGlint * 1.6 + sunFlare * 1.2 * (1.0 - smoothstep(0.0, 0.55, dist));
                vec3 glintLight = vec3(1.0, 0.98, 0.90) * glintAmount;

                emittedLight = rimLight + coreLight + glintLight;
            } else {
                // === MATERIAL 2: LUMINOUS EMERALD WATER SILK & CYCLIC CYAN WATER STREAM ===
                // 1. Silky Translucent Water Ribbon Body
                float waterBody = 1.0 - smoothstep(0.72, 0.99, dist);
                // Three-stop transverse chromatic gradient (mimics light refraction through curved water sheet)
                vec3 gradColor = mix(u_baseColor.rgb, u_highlightColor.rgb, clamp(v * 0.6 + 0.35, 0.0, 1.0));
                gradColor = mix(gradColor, u_refractionColor.rgb, clamp(-v * 0.5, 0.0, 0.75));
                fluidColor = gradColor;
                fluidAlpha = waterBody * 0.58;

                // 2. Liquid Boundary Fresnel Rim Sheen (Dual bright edges of the water ribbon)
                float waterRim = exp(-pow((dist - 0.88) / 0.10, 2.0));
                vec3 rimLight = mix(u_highlightColor.rgb, vec3(0.95, 1.0, 0.98), 0.70) * (waterRim * 1.25);

                // 3. Central Aquatic Caustic Vein (Internal optical refraction focus)
                float vein = exp(-dist * dist * 24.0);
                vec3 veinLight = mix(u_baseColor.rgb * 1.35, u_highlightColor.rgb, 0.55) * (vein * 0.65);

                // 4. Sunlight Flash along wave & Traveling Head Crest
                float glintAmount = specGlint * 1.35 + sunFlare * 1.15 * (1.0 - smoothstep(0.0, 0.60, dist));
                vec3 glintLight = mix(u_highlightColor.rgb, vec3(1.0, 1.0, 0.96), 0.80) * glintAmount;

                emittedLight = rimLight + veinLight + glintLight;
            }

            // Apply longitudinal length taper & head/alpha multipliers
            float headBoost = u_layerParams.x;
            float alphaMult = u_layerParams.y;
            float finalAlpha = clamp(fluidAlpha * lengthAlpha * headBoost * alphaMult, 0.0, 0.95);

            // Composite Radiant Fluid Volume with Additive Energy Emission
            // Base fluid color is premultiplied by finalAlpha; emitted high-energy light is scaled by vibrancy and length
            float lightEnv = lengthAlpha * headBoost * max(u_glowIntensity, 0.75);
            vec3 finalRgb = (fluidColor * finalAlpha) + (emittedLight * lightEnv);

            gl_FragColor = vec4(finalRgb, finalAlpha);
        }
    """.trimIndent()
}
