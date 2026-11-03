package com.example.export

import com.example.model.OrbConfig
import org.json.JSONObject
import java.util.Locale

object CodeExporter {

    /**
     * Generates standard JSON config matching LerSent001/orb parameter serialization
     */
    fun exportJson(cfg: OrbConfig): String {
        val obj = JSONObject()
        obj.put("preset", cfg.presetId)
        obj.put("speed", cfg.speed)
        obj.put("radius", cfg.radius)
        obj.put("zoom", cfg.zoom)
        obj.put("warp", cfg.warp)
        obj.put("ridgeAmt", cfg.ridgeAmt)
        obj.put("sharp", cfg.sharp)
        obj.put("shade", cfg.shade)
        obj.put("sheen", cfg.sheen)
        obj.put("gloss", cfg.gloss)
        obj.put("refraction", cfg.refraction)
        obj.put("dispersion", cfg.dispersion)
        obj.put("roughness", cfg.roughness)
        obj.put("glow", cfg.glow)
        obj.put("edgeSoftness", cfg.edgeSoftness)
        obj.put("color1", cfg.color1.toHex())
        obj.put("color2", cfg.color2.toHex())
        obj.put("accentColor", cfg.accentColor.toHex())
        obj.put("coreColor", cfg.coreColor.toHex())
        obj.put("glowColor", cfg.glowColor.toHex())
        obj.put("audioReactive", cfg.audioReactive)
        obj.put("audioSensitivity", cfg.audioSensitivity)
        return obj.toString(2)
    }

    /**
     * Generates WebGPU WGSL uniform struct and configuration
     */
    fun exportWgsl(cfg: OrbConfig): String {
        return """
            // WGSL Shader Uniforms for LerSent001/orb WebGPU
            struct Uniforms {
              speed: f32,
              radius: f32,
              zoom: f32,
              warp: f32,
              ridgeAmt: f32,
              sharp: f32,
              shade: f32,
              sheen: f32,
              gloss: f32,
              refraction: f32,
              dispersion: f32,
              roughness: f32,
              glow: f32,
              edgeSoftness: f32,
              color1: vec3<f32>,
              color2: vec3<f32>,
              accentColor: vec3<f32>,
              coreColor: vec3<f32>,
              glowColor: vec3<f32>
            };

            // Preset: ${cfg.nameEn}
            const config = {
              speed: ${f(cfg.speed)},
              radius: ${f(cfg.radius)},
              zoom: ${f(cfg.zoom)},
              warp: ${f(cfg.warp)},
              ridgeAmt: ${f(cfg.ridgeAmt)},
              sharp: ${f(cfg.sharp)},
              shade: ${f(cfg.shade)},
              sheen: ${f(cfg.sheen)},
              gloss: ${f(cfg.gloss)},
              refraction: ${f(cfg.refraction)},
              dispersion: ${f(cfg.dispersion)},
              roughness: ${f(cfg.roughness)},
              glow: ${f(cfg.glow)},
              edgeSoftness: ${f(cfg.edgeSoftness)},
              color1: vec3<f32>(${f(cfg.color1.r)}, ${f(cfg.color1.g)}, ${f(cfg.color1.b)}),
              color2: vec3<f32>(${f(cfg.color2.r)}, ${f(cfg.color2.g)}, ${f(cfg.color2.b)}),
              accentColor: vec3<f32>(${f(cfg.accentColor.r)}, ${f(cfg.accentColor.g)}, ${f(cfg.accentColor.b)}),
              coreColor: vec3<f32>(${f(cfg.coreColor.r)}, ${f(cfg.coreColor.g)}, ${f(cfg.coreColor.b)}),
              glowColor: vec3<f32>(${f(cfg.glowColor.r)}, ${f(cfg.glowColor.g)}, ${f(cfg.glowColor.b)})
            };
        """.trimIndent()
    }

    /**
     * Generates native Apple SwiftUI + Metal shader integration code
     */
    fun exportSwiftUi(cfg: OrbConfig): String {
        return """
            import SwiftUI

            // SwiftUI Liquid Glass Orb Component
            // Preset: ${cfg.nameEn} (${cfg.nameZh})
            struct LiquidOrbView: View {
                var body: some View {
                    TimelineView(.animation) { timeline in
                        let time = timeline.date.timeIntervalSinceReferenceDate
                        Canvas { context, size in
                            // Render with Metal shader 'liquidOrb'
                            context.addFilter(.distortionShader(
                                ShaderLibrary.liquidOrb(
                                    .float2(Float(size.width), Float(size.height)),
                                    .float(Float(time)),
                                    .float(${f(cfg.speed)}),
                                    .float(${f(cfg.radius)}),
                                    .float(${f(cfg.zoom)}),
                                    .float(${f(cfg.warp)}),
                                    .float(${f(cfg.refraction)}),
                                    .color(Color(hex: "${cfg.color1.toHex()}")),
                                    .color(Color(hex: "${cfg.color2.toHex()}"))
                                ),
                                maxSampleOffset: CGSize(width: 40, height: 40)
                            ))
                            context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(Color(hex: "${cfg.coreColor.toHex()}")))
                        }
                    }
                    .frame(width: 320, height: 320)
                }
            }
        """.trimIndent()
    }

    /**
     * Generates Android AGSL / GLSL snippet
     */
    fun exportAndroid(cfg: OrbConfig): String {
        return """
            // Android AGSL RuntimeShader / GLSL Uniform Setup
            // Preset: ${cfg.nameEn}
            val shader = RuntimeShader(LIQUID_ORB_AGSL)
            shader.setFloatUniform("u_speed", ${f(cfg.speed)}f)
            shader.setFloatUniform("u_radius", ${f(cfg.radius)}f)
            shader.setFloatUniform("u_zoom", ${f(cfg.zoom)}f)
            shader.setFloatUniform("u_warp", ${f(cfg.warp)}f)
            shader.setFloatUniform("u_ridgeAmt", ${f(cfg.ridgeAmt)}f)
            shader.setFloatUniform("u_sharp", ${f(cfg.sharp)}f)
            shader.setFloatUniform("u_refraction", ${f(cfg.refraction)}f)
            shader.setFloatUniform("u_dispersion", ${f(cfg.dispersion)}f)
            shader.setFloatUniform("u_glow", ${f(cfg.glow)}f)
            shader.setFloatUniform("u_color1", ${f(cfg.color1.r)}f, ${f(cfg.color1.g)}f, ${f(cfg.color1.b)}f)
            shader.setFloatUniform("u_color2", ${f(cfg.color2.r)}f, ${f(cfg.color2.g)}f, ${f(cfg.color2.b)}f)
            shader.setFloatUniform("u_glowColor", ${f(cfg.glowColor.r)}f, ${f(cfg.glowColor.g)}f, ${f(cfg.glowColor.b)}f)
        """.trimIndent()
    }

    private fun f(v: Float): String = String.format(Locale.US, "%.3f", v)
}
