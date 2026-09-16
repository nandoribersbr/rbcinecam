# RB CineCam Professional Functional HUD Design

## Goal
Transform the approved cinematic mockup into the real RB CineCam interface while preserving working capture behavior and forbidding decorative/dead controls.

## Visual architecture
- Full-bleed camera preview remains the dominant surface.
- A fixed black technical top bar sits inside the device safe area.
- FHD/4K, FPS, aspect ratio, scopes and grid move into the top bar.
- ISO, shutter, WB, focus and EV remain as compact translucent bottom controls.
- Video/photo/gallery stay on the left as compact translucent tools.
- Camera switch, audio meters and REC stay on the right, but occupy less preview area.
- Audio monitor is collapsible.
- Scopes are hidden until explicitly enabled.

## Functional contract
- No visible control may be decorative.
- Resolution/FPS/aspect controls expose only supported choices for the active camera pipeline.
- Requested capture state is distinct from applied capture state.
- HUD changes to a new resolution/FPS only after successful camera bind.
- Failed reconfiguration rolls back to the last applied state without crashing.
- Scopes may be suspended when required to preserve a supported 4K capture path, with explicit UI feedback.
- Unsupported professional features such as LUT or focus peaking must not appear until a real implementation exists.
- Large Format LF 1.90/LF 1.43 remains available only through the real implemented policy.

## Release gate
The build is blocked by dead controls, silent FHD/4K rollback, camera crash during resolution/FPS changes, permanent black preview, or HUD values that disagree with applied capture state.
