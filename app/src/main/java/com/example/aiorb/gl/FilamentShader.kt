package com.example.aiorb.gl

object FilamentShader {

    val VERTEX_SHADER = """
        attribute vec2 a_position;
        attribute vec2 a_uv;
        varying vec2 v_uv;

        uniform vec2 u_resolution;
        uniform float u_scale;

        void main() {
            // Apply scale centered at (0, 0)
            vec2 scaled = a_position * u_scale;
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

        void main() {
            float u = clamp(v_uv.x, 0.0, 1.0);
            float v = clamp(v_uv.y, -1.0, 1.0);
            float dist = abs(v);

            // 1. Outer Feather (外围柔化光层): smooth falloff near ribbon edge
            float feather = smoothstep(1.0, 0.25, dist);

            // 2. Translucent Body (半透明水体厚度): dense liquid center
            float body = smoothstep(0.85, 0.05, dist);

            // 3. Inner Refractive Band (内侧折射带): subtle asymmetric optical band
            float refrDist = abs(v + 0.12);
            float refrBand = smoothstep(0.42, 0.0, refrDist);

            // 4. Specular Highlight (镜面高光): travels with head along arc length
            float specLen = max(u_layerParams.z, 0.08);
            float headProximity = clamp((u - (1.0 - specLen)) / specLen, 0.0, 1.0);
            float specGlint = headProximity * smoothstep(0.40, 0.0, dist);

            // 5. Tail Feather Taper (尾部连续淡出)
            float tailFade = smoothstep(0.0, 0.18, u);

            // Composition
            vec3 col = u_baseColor.rgb;
            float alpha = u_baseColor.a * (0.38 * feather + 0.62 * body);

            // Blend refraction band
            col = mix(col, u_refractionColor.rgb, refrBand * 0.42);
            alpha = max(alpha, u_refractionColor.a * refrBand * 0.38);

            // Blend specular highlight
            col = mix(col, u_highlightColor.rgb, specGlint * 0.85);
            alpha = max(alpha, u_highlightColor.a * specGlint * 0.75);

            // Fine energy line bonus for warm gold filament
            if (u_layerParams.w > 0.5) {
                float fineLine = smoothstep(0.35, 0.0, dist);
                col = mix(col, u_highlightColor.rgb, fineLine * 0.4);
                alpha = max(alpha, fineLine * 0.70);
            }

            // Head brightness boost & global alpha scaling
            float headBoost = u_layerParams.x;
            float alphaMult = u_layerParams.y;
            alpha = clamp(alpha * tailFade * headBoost * alphaMult, 0.0, 0.82);

            // Premultiplied alpha output for clean compositing
            gl_FragColor = vec4(col * alpha, alpha);
        }
    """.trimIndent()
}
