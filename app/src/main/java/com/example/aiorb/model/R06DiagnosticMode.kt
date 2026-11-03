package com.example.aiorb.model

/**
 * Diagnostic rendering modes for R06 External Filaments.
 *
 * RUNTIME: Normal 4-layer optical shader composition with core & grass.
 * GEOMETRY_ONLY: High-contrast solid ribbons revealing exact Bezier centerlines,
 *                width, and extension outside 0.5D core sphere.
 * MATERIAL: Diagnostic high-saturation coloration clearly distinguishing
 *           the 3 material classes (Green Ribbon, Warm Gold, Cyan Water Ribbon).
 */
enum class R06DiagnosticMode(val modeId: Int, val label: String) {
    RUNTIME(0, "RUNTIME"),
    GEOMETRY_ONLY(1, "GEOMETRY"),
    MATERIAL(2, "MATERIAL")
}
