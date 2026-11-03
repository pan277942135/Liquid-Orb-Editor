package com.example.aiorb.gl

object FilamentShader {

    val VERTEX_SHADER = """
        attribute vec2 a_position;
        attribute vec4 a_color;
        varying vec4 v_color;

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
            v_color = a_color;
        }
    """.trimIndent()

    val FRAGMENT_SHADER = """
        precision mediump float;
        varying vec4 v_color;

        void main() {
            gl_FragColor = v_color;
        }
    """.trimIndent()
}
