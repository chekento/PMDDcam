# PMDDcam Web 0.2.0

Interactive browser subproject for **PMDDcam / PMDD 4.0**.

## Launch

https://raw.githack.com/chekento/PMDDcam/main/webapp/index.html

## What changed in 0.2.0

The web project now has two distinct PMDD layers:

1. **Static PMDD image rendering** — depth-aware structure, layer separation and asymmetric motion cues are encoded into the still image itself.
2. **Interactive perspective rendering** — WebGL ray marching moves the viewpoint through the estimated depth field and adds bounded layer motion.

### Depth

- Fast edge-guided heuristic depth is available immediately.
- Optional **Depth Anything V2 Small** runs in the browser through Transformers.js.
- WebGPU is used when the browser/model path supports it; otherwise the model falls back to the browser runtime.
- Depth is kept separate from the original image.

### Viewer / motion

- 8–128 depth sampling layers.
- X/Y/Z viewpoint: left/right, up/down and near/far.
- Depth-aware disocclusion fill reduces broad holes when foreground layers move.
- Independent bounded layer animation creates temporal motion without replacing the source image.
- Pointer/touch and mouse-wheel distance controls.
- DeviceOrientation tilt mode.
- Optional **MediaPipe Face Landmarker** head tracking with the front camera.

### Looks

The 80 named Android look IDs remain available, but the browser renderer now uses visibly different rendering families instead of treating them mainly as color presets:

- photo / film
- comic / cel
- halftone / print
- watercolor
- oil / gouache
- ink / pencil / charcoal
- pixel / console palettes
- neon / future
- duotone
- thermal
- solarized
- bloom / atmosphere

### Workflow

- Image upload and browser camera capture
- PMDD / split compare / original display modes
- Depth preview
- Local IndexedDB project save and restore
- PNG export
- PMDD recipe JSON import/export
- Installable PWA shell
- UI auto-detect / switch for DE, EN, FR, ES, ZH and JA

## Privacy and external model assets

Source images, generated depth maps and PMDD recipes are processed locally in the browser and are not uploaded to a PMDD server.

The optional AI modes download third-party runtime/model assets when first enabled:

- Transformers.js / Depth Anything V2 for AI depth
- MediaPipe Tasks Vision / Face Landmarker for head tracking

MediaPipe documents that vision input is processed on-device; its SDK may send product usage/performance metrics according to Google's MediaPipe privacy notice.

## Browser note

Camera, device orientation, WebGPU and some install/PWA behavior require a secure origin (HTTPS) and depend on browser/device support. Pointer/touch plus heuristic depth remain the baseline path.

## License / feedback

The repository license applies. Feedback and licensing inquiries:  
kolja.schumann+PMDDcam@gmail.com
