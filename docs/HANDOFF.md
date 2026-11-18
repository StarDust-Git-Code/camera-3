# Handoff: read first when resuming

Last updated: 2026-11-18. Initial Android implementation; first CI setup failed.

## Current state

- User asked to start building and reattached the black eight-screen reference.
- User then requested APK compilation via GitHub Actions using
  `https://github.com/StarDust-Git-Code/camera-3`.
- Six Gradle modules and baseline Photo/settings/diagnostics source now exist.
  No APK, successful app test/build, or physical camera evidence yet.
- Source pushed to `origin/main`, commit `9e4686c`. First run 44235279035 failed
  before compilation: `sdkmanager` not on PATH (P-009). Explicit pinned SDK setup
  and source-review corrections now exist locally; commit/push rerun next.
- Git Credential Manager owner account works. Portable verified CLI:
  `C:\Users\irsha\AppData\Local\Temp\opencode\camera3-gh-v2.105.0\bin\gh.exe`.
  Auth credential is only passed in process memory; never print/persist it.

## Implemented (not yet verified)

- Camera2 discovery and LEVEL_3 gate, per-lens data/key/stream diagnostics.
- Handler-confined camera engine, run/epoch guards, close-before-reopen,
  fitted SurfaceView/native orientation, JPEG capture/request orientation.
- Image copy/storage off-main, MediaStore pending/publish/cleanup, gallery action.
- Conditional torch/zoom and real camera-ID selection; automatic AF/AE/AWB.
- DataStore grid/JPEG quality, black Compose screens, permissions action.
- Bounded DEBUG/INFO/WARNING/ERROR logger, filters/search/live/clear/report/share.
- Nine model unit tests (not executed); CI tests/lint/debug APK + report uploads.

## Constraints and limits

- Kotlin/Compose/public Camera2/MVVM/Coroutines; `minSdk = 35`; device requires
  at least one accessible LEVEL_3 camera. Auxiliary lenses are independently gated.
- No fake preview, fictional Log, fake RAW/HDR or unsupported editable settings.
- RAW/DNG/manual capture/extension sessions/video/audio are **not implemented**;
  Night/Portrait/Video/Pro Video are disabled and explained as development limits.
- AF/AE capture locking and tap metering are not implemented yet.
- UI reference is in chat, not a local asset; no screenshot comparison yet.
- User-reported A35/One UI8.5/Magisk/unlocked camera library remains unverified.
  Latest `adb devices -l`: no phone. Never infer Android API from One UI.
- Do not modify root/modules/firmware/libraries or set up release signing unasked.

## Build and entry points

- Identity: Camera3 / `dev.camera3.app` / `0.1.0-dev` (development, not final).
- AGP8.13.2 / Gradle8.13 / Kotlin+Compose compiler2.3.21 / BOM2025.11.01,
  compile/target36, JVM17 bytecode, JDK21 execution. Wrapper checksum verified.
- `.github/workflows/android.yml`: `main` pushes, PRs, manual dispatch.
  Commands: `:core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- Main source: `app/src/main/kotlin/dev/camera3/app/`,
  `core/{model,camera,media,settings,diagnostics}/src/main/kotlin/dev/camera3/`.
- Model tests: `core/model/src/test/kotlin/dev/camera3/model/CameraPolicyTest.kt`.
- Local SDK path/config must stay ignored; do not publish secrets/test media.

## Exact next action

1. Commit/push P-009 workflow and rotation/torch review corrections to `origin`
   `main` without force; follow Actions and fix actual compiler/lint failures.
2. Record run URL, test/lint outcomes and actual APK artifact; no assumed success.
3. Connect/authorize A35 and test permission/gate, preview aspect/mirroring,
   JPEG/EXIF, background/resume, rapid switching/capture, settings and log sharing.
4. Keep canonical docs current. M1/M2 have source, not device-complete milestones;
   advanced roadmap work remains pending. Read PROGRESS/REQUIREMENTS/DECISIONS and
   docs/README before continuing after restart/compaction.
