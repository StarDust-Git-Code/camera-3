# Handoff: read first when resuming

Last updated: 2026-11-18. RAW (M3) is pushed and CI-green with its APK
verified locally; Video (M4) first cut is implemented locally and awaiting
commit/push/CI. Both pipelines are device-unverified pending the next install.

## Current state

- Published CI-green source: `7905a89` (RAW/DNG pipeline + docs, run
  44247877745 passed). The RAW-build APK at that run is verified locally but
  superseded as an install candidate by the upcoming RAW+Video build.
- RAW-build APK verification (host-observed):
  `app/build/ci/44247877745/camera3-debug-apk/app-debug.apk`, 62,186,421 bytes,
  SHA256 `17ab7be0d15ed57b8f57bc56297cb8c50440f8aff74cd6aff4b2a26c1128cd78`,
  v2 signature/package checks passed; 11 tests / 0 failures; lint 0 errors.
- Local working tree: Video (M4) first cut implemented per D-017 (compiles,
  12/12 model tests) + docs updates, **uncommitted**. Install of the installed
  fix build (run 44240950175, `6b8b35f`) already went through the
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → uninstall/reinstall path once;
  CAMERA re-granted via system dialog, DataStore reset observed (quality 95%,
  grid OFF), grid re-enabled in-app, quality left at 95% (user decision: no
  more slider work). DCIM photos survived.
- RAW/DNG pipeline (D-016, build-verified, NOT device-verified):
  - `CameraDescriptor.rawSizes` + `supportsRaw` (capability RAW **and** real
    sizes); discovery reads `getOutputSizes(RAW_SENSOR)` defensively and logs
    it; diagnostics show "RAW output sizes".
  - `CameraPolicy.configuration(camera, rawEnabled)` pairs the largest
    aspect-matching (±0.03) RAW size only when RAW is advertised; otherwise no
    configuration is invented.
  - Engine: optional RAW_SENSOR `ImageReader` (maxImages 2), still request
    targets JPEG+RAW surfaces, `TotalCaptureResult` + RAW `Image` paired in
    either arrival order, capture stays CAPTURING until both outputs settle,
    watchdog re-arms per remaining output, honest per-output messages.
  - `DngCreator(cameraCharacteristics, result)` — constructor-bound metadata,
    no `setResult` at API 36 (javap-verified, P-014); `setOrientation` takes
    EXIF 1–8 (degrees→1/6/3/8 at capture time).
  - `PhotoStore.saveDng` streams `image/x-adobe-dng` into `DCIM/Camera3`;
    P-015 (explicit return in `PhotoStore.insert`) recorded.
  - UI: RAW FilterChip on supporting lenses, "MP · RAW" label, Settings raw
    boolean persisted, gallery opens with the resolver's MIME.
- Video pipeline first cut (D-017, build-verified locally, NOT device-verified):
  - `CameraDescriptor.videoSizes` via `getOutputSizes(MediaRecorder)`; bounded
    16:9 ≤1080p policy with aspect-matched (±0.015) preview; no pairing →
    "Video unavailable on this lens" message, Photo remains available.
  - Plan-driven sessions (`StreamPlan.Photo`/`Video`); mode switch rekeys the
    preview surface → close/reopen with preview + recorder surface,
    TEMPLATE_RECORD repeating request.
  - `MediaRecorder` prepared before `openCamera` with a pending MediaStore fd
    (`PhotoStore.openVideoOutput`); MPEG-4/H264 + AAC 44.1 kHz/96 kbps only
    with `RECORD_AUDIO`; 30 fps; ~5 Mbps/MP (4–24 Mbps); `setOrientationHint`
    degrees frozen at attach.
  - Record start adds the recorder surface to the repeating request; zoom and
    torch remain usable during RECORDING (`CONTROL_PHASES`).
  - `finalizeRecording` shared by user stop, backgrounding and `close()`:
    recorder stopped/released exactly once, publish if frames recorded,
    discard otherwise ("Recording too short to save" for unusable output).
  - UI: Photo/Video tabs, record/stop shutter with REC timer, "MP4 · <height>p"
    (+ "· silent" without mic), Settings mic Grant row; `RECORD_AUDIO` in the
    manifest. Mic state checked at attach time only (known limit).
- Local verification: `:core:model:test` 12 tests 0 failures +
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

1. Commit the Video work + docs together (inline identity
   `git -c user.name="OpenCode Agent" -c user.email="opencode-agent@localhost"
   commit --author=…`), push `main`, watch CI with `gh run watch --exit-status`.
2. Download + verify the new APK (SHA256, `apksigner verify`, `aapt2 badging`,
   test report 12/12); it becomes the install candidate.
3. When the phone reconnects (`adb devices -l`), install (`install -r`; the
   fresh runner key will likely force uninstall/reinstall — expect the
   settings reset, re-grant CAMERA via the system dialog).
4. Verify the foreground app before any `input tap`, then device-verify:
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
