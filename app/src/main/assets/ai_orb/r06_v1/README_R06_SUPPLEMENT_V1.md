# AI Orb R06 Supplement Package V1

## Purpose
This is a standalone three-file supplement for the AI Orb demo in Google AI Studio.
It supplies the five real R06 filament centerlines, geometry and material/motion profile.
It does not contain YuJian application business code.

## Contents
- `profiles/R06_orb_filament_profile.json`
  - Material profiles, widths, alpha ranges, periods, phases, path bindings and motion/event contracts.
- `assets/paths/R06_orb_filament_path_geometry.json`
  - Five two-segment cubic Bezier geometries in OrbLocal coordinates.
- `assets/paths/R06_orb_filament_paths.svg`
  - Five editable SVG centerlines. Material styling is defined by the JSON profile, not by the SVG.

The five IDs must remain exactly:
`G01`, `G02`, `A01`, `A02`, `C01`.

## Source provenance
- Repository: `pan277942135/Yujian_App`
- PR: #304 (merged)
- Source commit: `64700592714b8fb1c06b659b9d79c92149d2ea95`
- Source archive: `YuJian_AI_Orb_Gap_Closure_V1_FINAL.zip`
- Source archive SHA-256: `2431b2181a073d0e1cba575b899f915a1fe8db1066ea6c753a84ebbfd12f65d5`
- The three extracted files were verified against the merged commit's Git blob identities and the V1.2 package manifest.

## Version and approval status
These are the coherent current V1.2 candidate files from the merged resource package.
The resource register marks them `NEW_CANDIDATE` / `VISUAL_REVIEW_PENDING`, with `runtime_ready=false` and `product_approval_required=true`.
The profile itself declares `V2_VISUAL_CANDIDATE_PENDING_PRODUCT_REVIEW`.

Therefore:
- Use this set as the current R06 source-backed candidate for isolated visual debugging.
- Do not describe it as product-approved or runtime-frozen.
- Do not silently rewrite the frozen V1.1 visual contract.
- The V1.1 contract's referenced hashes do not match this current V1.2 candidate set. The exact expected V1.1 hashes are recorded in `SOURCE_PROVENANCE.json` for audit only.

## Import into Liquid-Orb-Editor
Copy the contents of this package into the demo's assets directory, preserving relative paths:
`app/src/main/assets/ai_orb/r06_v1/`

Do not rename files or flatten the `assets/paths` and `profiles` folders.

## Rendering requirements
- Use the JSON geometry as the numerical source of truth.
- Use the SVG as the editable vector centerline representation.
- Use the profile JSON for materials, widths, alpha, motion periods, phases and event parameters.
- Preserve BACK/FRONT ordering.
- Preserve RELEASE/RETURN on the same five paths.
- Generate a 128-sample arc-length lookup table per path at load/init time.
- Do not replace these paths with ellipses, rings, random splines or newly drawn curves.
- Do not duplicate the legacy three-path renderer on top of these five paths.

## Integrity
Verify every file against `SHA256SUMS.txt` before integration.
The ZIP contains only the three R06 source files plus provenance, manifest and this README.
