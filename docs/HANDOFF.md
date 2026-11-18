# Handoff: read first when resuming

Last updated: 2026-11-18. The fix build is installed on the phone; the RAW/DNG
pipeline is implemented and locally build-verified, awaiting CI + device checks.

## Current state

- Published CI-green source: `6212f3c` (identical app sources to `6b8b35f`).
  The local working tree now adds the RAW pipeline + docs, uncommitted.
- Fix build (run 44240950175, `6b8b35f`) IS installed on the phone: fresh
  runner key forced uninstall/reinstall (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`
  first), CAMERA re-granted via the system dialog, DataStore reset observed
  (quality 95%, grid OFF), grid re-enabled in-app, quality left at 95% (user
  decision: no more slider work). DCIM photos survived.
- RAW/DNG pipeline implemented per D-016 (build-verified locally, NOT yet
  device-verified):
  - `CameraDescriptor.rawSizes` + `supportsRaw`; discovery reads
    `getOutputSizes(RAW_SENSOR)` (defensive) and logs it; diagnostics show
    "RAW output sizes".
  - `CameraPolicy.configuration(camera, rawEnabled)` pairs the largest
    aspect-matching (±0.03) RAW size only when capability RAW is advertised;
    otherwise no configuration is invented. Two new tests; 11/11 pass.
  - Engine: optional RAW_SENSOR `ImageReader` (maxImages 2), still request
    targets JPEG+RAW surfaces, `TotalCaptureResult` + RAW `Image` paired in
    either arrival order, capture stays CAPTURING until both outputs settle,
    watchdog re-arms per remaining output, honest per-output messages.
  - `DngCreator(cameraCharacteristics, result)` — constructor-bound metadata,
    no `setResult` at API 36 (javap-verified, P-014); `setOrientation` takes
    EXIF 1–8 (degrees→1/6/3/8 mapping at capture time).
  - `PhotoStore.saveDng` streams `image/x-adobe-dng` into `DCIM/Camera3`.
  - Settings gain a persisted `raw` boolean; ViewModel computes RAW-aware
    configuration everywhere; UI shows a RAW FilterChip on supporting lenses
    and "MP · RAW" label; gallery opens with `image/*`.
  - P-015 (explicit return in `PhotoStore.insert`) recorded.
- Local verification: `:core:model:test` 11 tests 0 failures +
  `:core:camera|media|settings|app` compile BUILD SUCCESSFUL (2026-11-18).
- Device identity: Android 16 / API 36 with spoofed Samsung props (a35x board,
  SM-S711B model string, mixed fingerprint) consistent with the user's Magisk/
  modified-library report. Do not copy the device serial into docs/exports.
- Per-lens facts: all four LEVEL_3 with RAW/manual/burst/reprocessing;
  baseline JPEGs 4080×3060 / 1920×1440 / 3264×2448 / 3408×2556; preview
  1440×1080; zoom 1.0–8.0 rear / 1.0–4.0 front; extensions only BEAUTY(1) +
  BOKEH(2); NIGHT(4) not advertised; no physical sub-IDs (CAMERA-CAPABILITIES).

## Implemented and device-verified (baseline Photo only)

- Install/permission/eligibility gate; preview fitted with grid and mirroring;
  JPEG capture to MediaStore with EXIF orientation/model/ISO; torch; zoom;
  four-lens close-before-reopen switching; background/resume; settings
  persistence; diagnostics screens with real data.

## Constraints and limits

- Kotlin/Compose/public Camera2/MVVM/Coroutines; `minSdk = 35`; LEVEL_3 device
  gate (passed honestly on-device). Reported flags remain claims (P-004);
  the RAW pipeline is implemented but device-unverified; disabled tabs stay
  disabled.
- Night tab: Camera2 NIGHT extension (ID 4) is NOT advertised by this HAL —
  keep disabled with that reason unless another real pipeline is built.
- AF/AE lock, tap metering, extension sessions, video/audio: not implemented.
  RAW device checks + deferred UI checks (Logs/share, full-caps detail, clean
  close logs, capability labels) ride with the next device session.
- Do not modify root/modules/firmware/libraries or set up release signing unasked.
- P-010 maintenance follow-ups (DataExtractionRules, action runtime/exec bit)
  remain open; warnings not suppressed.
- Testing etiquette: user may be using the phone; verify the foreground app
  (`topResumedActivity`) before every `adb input` action; stop when in doubt.

## Build and entry points

- Identity: Camera3 / `dev.camera3.app` / `0.1.0-dev` (development, not final).
- AGP8.13.2 / Gradle8.13 / Kotlin+Compose compiler2.3.21 / BOM2025.11.01,
  compile/target36, JVM17 bytecode, JDK21 execution. Wrapper checksum verified.
- `.github/workflows/android.yml`: `main` pushes, PRs, manual dispatch.
  Commands: `:core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- Portable `gh`: `C:\Users\irsha\AppData\Local\Temp\opencode\camera3-gh-v2.105.0\bin\gh.exe`;
  auth stays in process memory only. Fresh CI debug keys may require
  uninstall/reinstall (settings reset expected; DCIM photos survive).
- Local SDK path/config must stay ignored; no secrets/test media in Git.

## Exact next action

1. Commit source + docs together, push, watch CI (same local compile already
   passed; `:core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug`).
2. Install the new APK (`install -r`; uninstall/reinstall if the runner key
   changed — settings reset expected). Verify foreground before any `input tap`.
3. Device-verify RAW per D-016: RAW chip visibility per lens (all four?),
   toggle→close/reopen→3-stream session, session-probe outcome, capture saves
   JPEG + DNG pair, DNG opens/rotates correctly, failure path if the combo is
   unsupported. Record in TESTING/PROGRESS/CAMERA-CAPABILITIES (observed
   rawSizes), update P-011/P-012 re-verification and deferred UI checks.
4. Then implement Video (M4): MediaRecorder surface pairing, record/stop state
   machine, orientation hint, silent mode without mic grant, session
   reconfiguration.
