# Handoff: read first when resuming

Last updated: 2026-11-18. First device session verified the Photo baseline;
two runtime fixes await the next CI build + reinstall.

## Current state

- Verified source commit `9174a63` (run 44235722163) remains the published
  build. The docs-only checkpoint plus two code fixes are local and uncommitted.
- First device session completed on the user's connected phone: verified APK
  installed; permission/LEVEL_3 gate, preview+grid, torch, zoom 1.0–8.0, all
  four lens chips, capture+storage, lifecycle, settings persistence and
  diagnostics rendering all device-verified; four real 4080×3060 JPEGs with
  correct EXIF in `DCIM/Camera3`; persistent `camera-0.log` captured everything.
  No crash. Evidence: TESTING execution record + `app/build/ci/device-testing/`.
- Device identity: Android 16 / API 36 with spoofed Samsung props (a35x board,
  SM-S711B model string, mixed fingerprint) consistent with the user's Magisk/
  modified-library report. Do not copy the device serial into docs/exports.
- Per-lens facts: all four LEVEL_3 with RAW/manual/burst/reprocessing;
  baseline JPEGs 4080×3060 / 1920×1440 / 3264×2448 / 3408×2556; preview
  1440×1080; zoom 1.0–8.0 rear / 1.0–4.0 front; extensions only BEAUTY(1) +
  BOKEH(2); NIGHT(4) not advertised; no physical sub-IDs (CAMERA-CAPABILITIES).
- Fixes applied locally, compile-verified (`:core:camera:compileDebugKotlin`
  BUILD SUCCESSFUL): P-011 idempotent session teardown in `CameraEngine.close()`
  (duplicate ISE close logs), P-012 full API-36 capability-name map in
  `CameraDiscovery` (5=READ_SENSOR_SETTINGS, 19=STREAM_USE_CASE, verified via
  javap on android.jar). P-001 closed; P-013 (corrupt bundletool cache) resolved.
- Pushed as `6b8b35f`; CI run 44240950175 passed (watched). Fix-build APK
  verified locally: `app/build/ci/44240950175/apk/app-debug.apk`, SHA256
  `4f9d9661…5840e30e44`, v2 signature/package checks passed; reports downloaded.
  Device install is pending — the phone was disconnected at the attempt.
- ROADMAP M3 now records the device facts (BOKEH(2)/BEAUTY(1) exposed,
  NIGHT(4) not advertised).

## Implemented and device-verified (baseline Photo only)

- Install/permission/eligibility gate; preview fitted with grid and mirroring;
  JPEG capture to MediaStore with EXIF orientation/model/ISO; torch; zoom;
  four-lens close-before-reopen switching; background/resume; settings
  persistence (quality 100%, grid ON); diagnostics screens with real data.

## Constraints and limits

- Kotlin/Compose/public Camera2/MVVM/Coroutines; `minSdk = 35`; LEVEL_3 device
  gate (passed honestly on-device). Reported flags remain claims (P-004);
  advanced pipelines are not implemented; disabled tabs stay disabled.
- Night tab: Camera2 NIGHT extension (ID 4) is NOT advertised by this HAL —
  keep disabled with that reason unless another real pipeline is built.
- AF/AE lock, tap metering, RAW/DNG, extension sessions, video/audio: not
  implemented. Logs screen/share intent/full-caps detail/quality-isolation
  capture still to exercise on-device.
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
  auth stays in process memory only. Remote artifacts from run 44235722163
  expire 2026-12-02. Fresh CI debug keys may require uninstall/reinstall.
- Local SDK path/config must stay ignored; no secrets/test media in Git.

## Exact next action

1. When the user reconnects the phone, verify it in `adb devices -l` and install
   `app/build/ci/44240950175/apk/app-debug.apk` (`install -r`; if the runner's
   debug key changed, uninstall/reinstall and note the settings reset).
2. With the phone idle and Camera3 foreground-verified, finish deferred checks:
   Logs screen render + share/export intent, "View full capabilities" detail,
   one fresh quality-100 capture, clean close logs (P-011), capability labels
   (P-012). Record results in TESTING/PROGRESS and update the handoff.
3. Then plan M3 (RAW/DNG, manual controls, BOKEH extension path) against the
   observed per-lens capabilities, revalidating key ranges per lens first.
