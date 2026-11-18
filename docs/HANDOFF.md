# Handoff: read first when resuming

Last updated: 2026-11-18. RAW+Video is pushed and CI-green (`409c5d9`), but
its installed APK fails before camera open (P-016). Parallel investigation
identified a display-context source defect; corrections/tests pass local build,
awaiting push/CI and physical retest. Neither new pipeline is device-verified.

## Current state

- Published source: `409c5d9`, run 44251120868 success; docs checkpoint
  `0d40728`, run 44248444802 success. Both conclusions re-queried read-only.
- Installed RAW+Video APK (host-verified, NOT functional-device-verified):
  `app/build/ci/44251120868/camera3-debug-apk/app-debug.apk`, 62,235,613 bytes,
  SHA256 `685ad39061ea10f34987651602bafa9b6bd8ba144a868215854ad5d9fcc628ef`;
  v2 signature/package/API checks passed; 12 model tests / 0 failures;
  lint 0 errors / 17 warnings. Historical APKs are recorded in TESTING.
- Reinstall required uninstall after `INSTALL_FAILED_UPDATE_INCOMPATIBLE`;
  streamed install succeeded, CAMERA re-granted via `pm grant`, microphone
  user-granted. Settings reset expected; quality stays 95% (user decision).
- P-016 artifacts: `app/build/ci/device-testing/debug-report-01/`. Discovery
  passes (4 LEVEL_3); no `Opening ID`, no post-reinstall Camera3 CONNECT,
  black SurfaceView / "Camera paused", both permissions granted, no crash/ANR.
- Source finding: Application.display can throw before engine.setSource;
  platform SurfaceView catches callback exceptions. Runtime cause/retest still
  unverified. Also found incomplete configuration key and mode-dependent Photo
  availability. P-016 DEBUG gate/callback logs and D-018 fixes implemented;
  12 model + 5 app tests pass locally, lint 0 errors / 16 warnings, APK assembled.
- RAW/DNG pipeline (D-016, build-verified, NOT device-verified):
  real-size/capability/aspect gating; JPEG+RAW request and result/image pairing;
  both saves must settle with a re-arming watchdog; DngCreator streams DNG to
  MediaStore. Details/limits in DECISIONS D-016 and TESTING.
- Video pipeline first cut (D-017, CI build-verified, NOT device-verified):
  real MediaRecorder sizes + matched preview, 16:9 ≤1080p, H264/MP4 30fps;
  AAC only with mic permission; recorder prepared before open; shared finalize
  publishes recorded takes or discards unusable output on stop/background/close.
  Mode switches close/reopen; recording orientation frozen at attach. Full
  ownership/audio/encoder limits in DECISIONS D-017.
- P-016 local validation: full test/lint/assemble BUILD SUCCESSFUL (4m12s,
  then final-source rerun 49s);
  model task up-to-date, 5 new app state tests ran; XML reports no failures.
  Other Android modules' tests NO-SOURCE; CI verification pending.
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
  RAW and Video are implemented but device-unverified; disabled tabs stay
  disabled.
- Night tab: Camera2 NIGHT extension (ID 4) is NOT advertised by this HAL —
  keep disabled with that reason unless another real pipeline is built.
- Video limits (D-017): no pause/resume, no fps/bitrate settings surface, no
  high-speed capture, no per-clip HAL-rejection fallback beyond the session
  probe, mic grant not re-checked mid-session.
- AF/AE lock, tap metering, extension sessions: not implemented. RAW device
  checks + deferred UI checks (Logs/share, full-caps detail, clean close
  logs, capability labels) ride with the next device session.
- Do not modify root/modules/firmware/libraries or set up release signing unasked.
- Reference projects recorded in [REFERENCE-PROJECTS.md](REFERENCE-PROJECTS.md);
  Open Camera's GPLv3 code must never be copied into Camera3 (concepts only).
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
  auth stays in process memory only (credential-fill pipe form; PowerShell
  heredocs fail). Fresh CI debug keys may require uninstall/reinstall
  (settings reset expected; DCIM photos survive).
- Local SDK path/config must stay ignored; no secrets/test media in Git.

## Exact next action

1. Local full validation passed; commit corrections/tests + docs using inline
   OpenCode Agent identity, preserving all other work.
2. Push `main`, watch CI
   with `gh run watch --exit-status`, then download/verify the new APK/reports.
3. Latest ADB attempt: test phone not found. When reconnected and unlocked,
   install the verified fix APK; fresh CI key may require reinstall/settings
   reset. Re-grant CAMERA, launch, inspect P-016 attachment/gate/open logs.
4. Confirm real preview before media tests. User handles on-screen taps and
   reports results; assistant handles approved ADB install/log/media inspection.
   Verify foreground before any assistant input, then device-verify:
   - RAW per D-016 — chip visibility per lens, toggle → close/reopen →
     3-stream session probe outcome, JPEG+DNG pair saved, DNG opens/rotates,
     honest failure path. Record observed rawSizes in CAMERA-CAPABILITIES.
   - Video per D-017 — mode switch reconfigure, record start/stop, MP4 saved
     to `DCIM/Camera3` and playable, orientation correct, silent without mic
     grant (Grant row works), background stop still publishes, repeated
     start/stop without leaks. Record MediaRecorder sizes in
     CAMERA-CAPABILITIES.
   - Deferred UI checks (Logs/share, full-caps detail, P-011 close logs,
     P-012 capability labels).
   Record results in TESTING/PROGRESS and update the handoff; M4 boxes only
   close on device evidence.
