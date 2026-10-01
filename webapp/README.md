# PMDDcam Web

Interactive browser subproject for **PMDDcam / PMDD 4.0**.

## Launch

https://raw.githack.com/chekento/PMDDcam/main/webapp/index.html

## Included

- Image upload and browser camera capture
- Non-destructive Original ↔ PMDD comparison
- 80 named PMDD styles matching the Android app catalog
- 8–128 interactive depth sampling layers
- Depth strength, focus, motion illusion, parallax, atmosphere, detail and saturation controls
- Static PMDD motion cues plus interactive 2.5D motion
- Pointer/touch, mouse-wheel distance, optional DeviceOrientation tilt
- Optional browser `FaceDetector` head tracking where supported
- Local-only processing; no cloud upload and no API key
- PNG export
- UI language auto-detect for DE / EN / FR / ES / ZH / JA

## Rendering note

The Android app can use native/on-device ML models and a richer object pipeline. This web subproject deliberately stays dependency-free and uses a local heuristic depth estimate plus WebGL parallax so it can run directly from a static host.

The PMDD image recipe remains separate from the original source during the browser session.

## License / feedback

The repository license applies. Feedback and licensing inquiries:  
kolja.schumann+PMDDcam@gmail.com
