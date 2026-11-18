# Camera3

Working name for a native Android camera application using **Kotlin, Jetpack
Compose and Camera2**, for **Android 15+ phones with LEVEL_3 camera support**.

## Current status

**Debug APK built successfully with GitHub Actions. Physical-device testing is pending.**

The modular Android project now contains real Camera2 discovery, a LEVEL_3 gate,
SurfaceView preview, JPEG capture to MediaStore, camera switching, supported
torch/zoom, persisted grid/JPEG quality, diagnostics and log export/share.
Advanced modes are disabled, not fake captures. ADB still shows no connected phone.

## Build an APK with GitHub Actions

Repository: <https://github.com/StarDust-Git-Code/camera-3>.
The [Android APK workflow](.github/workflows/android.yml) runs on pushes to `main`,
pull requests, or manual dispatch. It runs unit tests, lint and `assembleDebug`,
then uploads **camera3-debug-apk** and **verification-reports** artifacts.
Download the APK artifact from a successful Actions run and unzip it to install.
This is a development/debug APK, not a signed production release.

First verified build: [Actions run 44235722163](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163),
commit `9174a63`. [Download camera3-debug-apk](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163/artifacts/14397496809)
(GitHub sign-in may be required; unzip `app-debug.apk`; retained until 2026-12-02).
Nine model tests passed; lint: 0 errors, 17 reviewed warnings. APK signature and
package/minimum SDK were checked locally. Details: [testing evidence](docs/TESTING.md).

## Product direction

- Photo, Portrait, Night, Video and Pro Video, subject to genuine availability.
- Dynamically detected camera, lens, output and encoder capabilities.
- RAW/DNG stills, supported manual controls, HDR/10-bit, high FPS, stabilization,
  zoom and multi-camera support where a real pipeline can use them.
- Premium black portrait UI based on the user's attached reference image.
- Comprehensive functional settings, hardware diagnostics and exportable logs.
- Incremental development and validation on the user's Samsung Galaxy A35.

The user reports One UI 8.5, Magisk, an unlocked bootloader and a modified camera
library on that phone. These details and its Camera2 behavior are not yet verified
through the device. The app will not depend on root or alter that configuration.

## Start here

- [Documentation index](docs/README.md)
- [Current handoff / resume point](docs/HANDOFF.md)
- [Progress and verification evidence](docs/PROGRESS.md)
- [Requirements](docs/REQUIREMENTS.md)
- [Incremental roadmap](docs/ROADMAP.md)
- [Development environment](docs/DEVELOPMENT.md)

[AGENTS.md](AGENTS.md) instructs future coding sessions to read and maintain these
files. They preserve working context without pretending to expand a model's
context window or automatically prove implementation status.

## Next implementation milestone

Connect/authorize the A35, install the verified APK, then test permissions,
eligibility, preview, JPEG orientation/saving, lifecycle and diagnostics.
Recording, RAW/DNG, extensions and manual capture controls remain unimplemented.

Development identity: Camera3 / `dev.camera3.app` / `0.1.0-dev`. Final branding and
release signing are still open. Versions are pinned in `gradle/libs.versions.toml`.
