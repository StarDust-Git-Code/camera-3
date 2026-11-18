# Camera3

Working name for a native Android camera application using **Kotlin, Jetpack
Compose and Camera2**, for **Android 15+ phones with LEVEL_3 camera support**.

## Current status

**Baseline Photo and the P-016 startup correction have physical-device evidence;
RAW/DNG and the earlier FHD/30 Video path have partial runtime evidence.**

The modular Android project now contains real Camera2 discovery, a LEVEL_3 gate,
SurfaceView preview, JPEG capture to MediaStore, camera switching, supported
torch/zoom, RAW/DNG capture, first-cut MediaRecorder video, persisted settings,
diagnostics and log export/share. Genuine maximum-resolution still discovery and
smooth Video zoom are CI build-verified but not device-verified. A focused
1080p60-only Video policy is locally build-verified and awaits CI/device evidence. Advanced
modes remain absent or disabled rather than simulated.

## Build an APK with GitHub Actions

Repository: <https://github.com/StarDust-Git-Code/camera-3>.
The [Android APK workflow](.github/workflows/android.yml) runs on pushes to `main`,
pull requests, or manual dispatch. It runs unit tests, lint and `assembleDebug`,
then uploads **camera3-debug-apk** and **verification-reports** artifacts.
Download the APK artifact from a successful Actions run and unzip it to install.
This is a development/debug APK, not a signed production release.

Latest published source: [Actions run 44298287574](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44298287574),
commit `d0ccb40`. It passed 14 model + 6 app tests and lint; the downloaded APK's
signature, package and SDK identity were checked. Maximum-resolution/smooth-zoom
device behavior remains unverified; details are in [testing evidence](docs/TESTING.md).

## Product direction

- Photo, Portrait, Night, Video and Pro Video, subject to genuine availability.
- Dynamically detected camera, lens, output and encoder capabilities.
- RAW/DNG stills, supported manual controls, HDR/10-bit, high FPS, stabilization,
  zoom and multi-camera support where a real pipeline can use them.
- Genuine maximum-resolution JPEG/RAW only when public map/key/session evidence
  supports it; never relabel the regular approximately 12.5 MP stream as 50 MP.
- Planned app-owned GPU/codec/audio path for LUT, defined Log/HDR, bounded RAW
  frames, stereo recording and verified Type-C IEM monitoring.
- Premium black portrait UI based on the user's attached reference image.
- Comprehensive functional settings, hardware diagnostics and exportable logs.
- Incremental development and validation on the user's Samsung Galaxy A35.

The user reports One UI 8.5, Magisk, an unlocked bootloader and a modified camera
library on that phone. Public Camera2 behavior has partial device evidence, while
the reported system modifications remain user-reported. The app does not depend
on root or alter that configuration.

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

Publish the focused 1080p60-only Video tranche and pass CI. Physical-device
cadence/file verification remains separate and requires later authorization.

Development identity: Camera3 / `dev.camera3.app` / `0.1.0-dev`. Final branding and
release signing are still open. Versions are pinned in `gradle/libs.versions.toml`.
