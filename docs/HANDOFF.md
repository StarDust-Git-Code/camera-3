# Handoff: read first when resuming

Last updated: 2026-11-18. P-016 correction `cc73c69` is CI-green and passed its
device retest. Maximum-resolution still discovery plus smooth Video zoom are
implemented and locally tested but uncommitted/device-unverified. No phone access
is authorized for the current implementation phase.

## Current state

- Published correction: `cc73c69`, run 44259956383 success. Verified/installed
  APK: `app/build/ci/44259956383/camera3-debug-apk/app-debug.apk`, 62,251,997
  bytes, SHA256 `33430BEB11A683E0944002CC2CD608E7AF358F7AE780085F007C4A80F0768F1E`;
  v2 signature/package passed; CI reports 12 model + 5 app tests, 0 failures,
  lint 0 errors/17 warnings.
- P-016 is resolved for the reproduced startup path. Retest log under
  `app/build/ci/device-testing/p016-retest/` shows valid Surface attachment,
  camera open/configure, Photo↔Video, ID 0↔1 and background/relaunch sequencing.
- RAW/DNG (D-016): log-confirmed JPEG+DNG save completion on IDs 0 and 1. DNG
  files were not pulled/opened, so validity, metadata and rotation remain open.
- Standard Video (D-017): configured 1920×1080 session; user reports FHD/30
  recording works. MP4 track/orientation/cadence/A/V checks remain. User reports
  OIS does not work; current code does not request/verify OIS/EIS (P-017).
- Current uncommitted tranche (D-019): separate maximum-resolution map discovery,
  writable pixel-mode and real-output policy, pixel-mode-aware output/session/
  still request, persisted HI-RES UI with actual-MP labels, and smoothstep Video
  zoom in logarithmic ratio space at approximately 30 Hz.
- Current local evidence: final full gate passed in 36s with 14 model + 6 app
  tests, no failures/errors/skips; lint 0 errors/16 warnings. Local debug APK is
  62,568,619 bytes, SHA256 `5E01DBFA…942D13`; v2 signature/package check passed.
- D-020 GPU/MediaCodec/AudioRecord architecture for LUT, defined Log/HDR,
  bounded RAW frames, stereo and Type-C monitoring is planned only; no UI exposed.
- Device identity: Android 16 / API 36 with spoofed Samsung props (a35x board,
  SM-S711B model string, mixed fingerprint) consistent with the user's Magisk/
  modified-library report. Do not copy the device serial into docs/exports.
- Per-lens facts from the installed pre-D-019 build: all four LEVEL_3 with RAW/manual/burst/reprocessing;
  baseline JPEGs 4080×3060 / 1920×1440 / 3264×2448 / 3408×2556; preview
  1440×1080; zoom 1.0–8.0 rear / 1.0–4.0 front; extensions only BEAUTY(1) +
  BOKEH(2); NIGHT(4) not advertised; no physical sub-IDs (CAMERA-CAPABILITIES).

## Implemented and device-evidenced

- Install/permission/eligibility gate; preview fitted with grid and mirroring;
  JPEG capture to MediaStore with EXIF orientation/model/ISO; torch; zoom;
  four-lens close-before-reopen switching; background/resume; settings
  persistence; diagnostics screens with real data.
- P-016 attachment/open/session regression; partial RAW save and standard Video
  evidence as qualified above. Do not promote these partial checks to full media acceptance.

## Constraints and limits

- Kotlin/Compose/public Camera2/MVVM/Coroutines; `minSdk = 35`; LEVEL_3 device
  gate (passed honestly on-device). Reported flags remain claims (P-004);
  RAW and Video are implemented with partial device evidence; disabled tabs stay disabled.
- Existing regular ID 0 JPEG/RAW is 4080×3060 (~12.5 MP), not 50 MP. No installed
  build has queried maximum-resolution maps or saved a maximum-resolution file.
- Night tab: Camera2 NIGHT extension (ID 4) is NOT advertised by this HAL —
  keep disabled with that reason unless another real pipeline is built.
- Video limits (D-017): no pause/resume, no fps/bitrate settings surface, no
  high-speed capture, no per-clip HAL-rejection fallback beyond the session
  probe, mic grant not re-checked mid-session.
- Stabilization is not implemented/verified. Do not label current FHD/30 as OIS/EIS.
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

1. Review the complete diff, commit/push the maximum-resolution/smooth-zoom
   tranche and validate its CI artifacts.
2. Begin D-020 architecture without exposing unfinished controls.
3. When phone testing is authorized again, first record maximum-resolution maps,
   session acceptance and saved dimensions; then inspect DNG/MP4, zoom feel,
   stabilization, RAW throughput, stereo channels and effective USB/IEM routing.
