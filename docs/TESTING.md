# Testing and evidence

Last updated: 2026-11-18. Debug build, nine model tests and lint passed through
GitHub Actions. APK metadata/signing checked locally. First physical-device
baseline session completed on the connected phone; remaining UI checks deferred
to the next install (see the device session section).

## Evidence rules

- Record command/action, outcome, date, app version, relevant device/software
  identity and reproducible configuration.
- "Implemented" is not "tested". A passing build is not a camera runtime test.
- Never infer an actual camera capability from phone marketing/model, a modified
  library flag, a UI label or an encoder's nominal metadata alone.
- Store concise evidence here/progress and link real artifacts. Do not invent
  logs, images, APK paths, FPS measurements or test results.
- Preserve failed outcomes and attempted fixes; rerunning successfully does not
  erase the reason for the original failure.

## Device matrix

| Target | Status | Intended use |
| --- | --- | --- |
| Samsung Galaxy A35 | User-reported; connected and queried 2026-11-18 (see execution record) | Primary camera/runtime/quality validation |
| One UI 8.5 / modified camera stack | User-reported; device property query showed spoofed build props (Android 16 props on a35x board with S711B model string) | Validate exposed versus effective Camera2 behavior |
| Android 15/API 35 | Required minimum | Minimum-platform compatibility |
| Android 16/API 36 | Device-observed on the connected phone | Current runtime target; baseline session recorded below |
| Emulator/test fakes | Not run | UI/permission/gating logic only, not proof of real hardware features |

Before a phone run, record Android release/API, build identity, selected camera
ID, its reported level and relevant capability snapshot. Ask for camera module
name/version if needed; do not execute root changes to investigate it.

## Host/logic checks

| ID | Test | Pass condition |
| --- | --- | --- |
| H-01 | Gradle scaffold/build | Compatible dependencies resolve and debug APK builds |
| H-02 | Static checks/lint | No relevant unresolved errors; warnings assessed explicitly |
| H-03 | Capability/config validation | Invalid keys/ranges/streams/FPS/profiles rejected with correct reasons |
| H-04 | State machine | Cancellation, stale callbacks and duplicate closure cannot corrupt state |
| H-05 | RAW pairing | Correct result/image matching, bounded queues, timeout cleanup |
| H-06 | Preferences | Persistence, migration, invalid values and missing lenses handled |
| H-07 | Log retention/export | Filtering, bounds, redaction and error paths work |
| H-08 | Compose UI | Actions, unavailable semantics, navigation, large text and touch targets |

## Physical-device acceptance matrix

| ID | Area | Required checks |
| --- | --- | --- |
| D-01 | Permissions/eligibility | Camera denial/regrant, no-level-3 state, actual qualifying camera selection |
| D-02 | Preview | Orientation, no stretch, front mirroring, aspect/crop, focus coordinate mapping |
| D-03 | JPEG | Real saved image, expected dimensions/aspect, orientation/EXIF and gallery readability |
| D-04 | RAW/DNG | Valid DNG in a reader/editor, matching metadata, actual sensor dimensions; combined RAW+JPEG |
| D-05 | Manual exposure | Requested/effective ISO/shutter, frame-duration limits, visible capture effect |
| D-06 | Focus/WB/metering | Supported controls genuinely alter results; fixed-focus/unavailable cases handled |
| D-07 | Flash/processing | Real flash/torch and supported processing; correct RAW/processed distinction |
| D-08 | Zoom/lenses | Actual field-of-view/crop behavior, lens availability, switching and capability revalidation |
| D-09 | Portrait/Night | Genuine extension discovery/session/capture and appropriate unavailable states |
| D-10 | Standard video | Playable MP4, dimensions, orientation, codec, frame cadence and actual stop/finalization |
| D-11 | Audio | Permission behavior, audible synchronized track, silent mode, actual supported routing |
| D-12 | Pro Video | Supported manual controls alter recording/results; incompatible controls unavailable |
| D-13 | HDR/10-bit | Real profile/bit-depth/transfer metadata and correct HDR-aware playback, not SDR substitution |
| D-14 | High FPS | Supported size/range, correct session path, measured capture/output cadence |
| D-15 | Stabilization | Effective modes and compatibility at selected FPS/profile; visual/result evidence |
| D-16 | Multi-camera | Exposed topology, valid physical/concurrent configurations, clean switching/failure handling |
| D-17 | Storage | Naming, collisions, selected folder, revoked grants, full/unwritable destination |
| D-18 | Lifecycle | Background/resume, screen-off behavior, camera contention, rapid repeated operations |
| D-19 | UI | Actual portrait/insets/cutout/navigation, large text, accessibility and reference review |
| D-20 | Diagnostics | Actual capabilities, log filters/live viewer and exported report opened by another app |

Unavailable features are recorded as unavailable with query/config evidence, not
counted as passing implementations. A failure must be repaired or explicitly
reported before delivery.

## Stress and performance

- Repeat shutter/lens/mode operations and record start/stop under normal usage.
- Run longer recordings at validated settings; observe storage, temperature,
  dropped frames, memory and cleanup. Choose/document durations based on safe
  device conditions rather than claiming arbitrary targets were tested.
- Exercise interrupted configuration and cancellation while surfaces change.
- Check logs/analysis cannot starve capture or create unbounded queues.
- Measure startup, session-switch and capture durations with monotonic time.
- Inspect actual frame timestamps; fixed/variable FPS choices and low-light
  auto-exposure may differ. Do not present a maximum rate as guaranteed cadence.

## Media inspection

JPEG: dimensions, crop/aspect, orientation, metadata and actual image quality.
DNG: valid RAW data/metadata pairing, not a renamed JPEG. Video: track count,
codec/profile, dimensions, bit depth/color metadata, timestamps/duration, actual
cadence, orientation and A/V sync. Use a compatible analyzer/viewer when available
and record its name/output; no such tool/output is assumed to exist now.

## Documentation checks

DOC-01: all expected Markdown files exist. DOC-02: local links resolve. DOC-03:
handoff is compact and distinguishes planned work from implemented/verified
work. DOC-04: platform/device constraints agree across canonical files.

## Execution record

| Date | Check | Outcome | Evidence |
| --- | --- | --- | --- |
| 2026-11-18 | Workspace/tool inspection | Completed | Empty workspace; Java/SDK/ADB found by read-only host commands |
| 2026-11-18 | `adb devices -l` | No connected device | Recorded command output contained only the device-list header |
| 2026-11-18 | DOC-01 / DOC-02 | Passed | Final PowerShell inventory/link check: 20 Markdown files, 18 under docs, 61 local links, 0 validation issues, 0 non-Markdown files |
| 2026-11-18 | DOC-03 | Passed | Final handoff length: 69 lines, below 150; read-back confirms planned versus verified distinctions |
| 2026-11-18 | DOC-04 | Passed, automated check and review | Five canonical files checked for minSdk 35 / LEVEL_3; recovery docs read back; proposals and user reports labeled separately |
| 2026-11-18 | ADB recheck | No connected device | `adb devices -l` again returned only the list header |
| 2026-11-18 | Gradle wrapper verification | Passed | Official Gradle 8.13 JAR SHA256 matched; distribution checksum pinned |
| 2026-11-18 | Remote inspection | Empty public target | GitHub API size=0; `git ls-remote` returned no repository refs |
| 2026-11-18 | H-01 / H-02 / policy tests, initial checkpoint | Pending then | Workflow/test source existed; subsequent successful results are below |
| 2026-11-18 | First CI infrastructure run | Failed before compile | [Run 44235279035](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235279035), commit 9e4686c: sdkmanager missing, exit127; explicit SDK setup added, rerun pending |
| 2026-11-18 | Second CI run / H-01 | Passed | [Run 44235722163](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163), commit 9174a63: tests/lint/assembleDebug and both uploads passed; build job 3m39s, Gradle 2m53s |
| 2026-11-18 | Model policy tests / partial H-03 | Passed | Downloaded Gradle HTML report: 9 tests, 0 failures, 0 ignored; no camera/platform execution |
| 2026-11-18 | H-02 / lint | Passed with warnings | Downloaded lint report: 0 errors, 17 warnings; four Kotlin compiler warnings; assessment in P-010 |
| 2026-11-18 | APK artifact inspection | Passed, host-observed | Downloaded APK; apksigner v2 signature verification passed, aapt2 confirmed dev.camera3.app / minSDK35 / targetSDK36 |
| 2026-11-18 | ADB resume recheck | No connected device | Explicit SDK platform-tools adb devices -l returned only the device-list header |
| 2026-11-18 | DOC-01/02/03 final build checkpoint | Passed | 20 Markdown files, 56 local links, 0 heading/link issues; 72-line handoff; git diff --check passed |
| 2026-11-18 | ADB device query | Connected and authorized | `adb devices -l` listed the phone; `getprop` read release 16 / SDK 36, a35x board with SM-S711B model string and mixed fingerprint (spoofed props, P-001 closed) |
| 2026-11-18 | APK install | Passed | `adb install -r` on the verified APK returned "Performing Streamed Install / Success" |
| 2026-11-18 | D-01 permissions/eligibility | Passed | User granted camera permission; discovery log: all four IDs LEVEL_3, `eligible=true, baselineCamera=0`; no fallback UI shown |
| 2026-11-18 | D-02 preview | Passed | Portrait 4:3 fitted preview, rule-of-thirds grid overlay, front preview mirrored correctly, zoom pill/slider visible; screenshots 02/08 |
| 2026-11-18 | D-03 JPEG capture/storage | Passed | Four photos in `DCIM/Camera3`; pulled samples 4080×3060 (12.48 MP), EXIF orientation 6, model string, real ISO 152–222; screenshots and pulled JPEGs saved under `app/build/ci/device-testing/` |
| 2026-11-18 | D-07 torch | Passed | Torch on/off driven on ID 0; `Controls` log lines + user-confirmed visible LED; no error state |
| 2026-11-18 | D-08 zoom/lens switching | Passed | Zoom 1.0→8.0 applied repeatedly without rejection; all four camera chips opened with close-before-reopen sequencing (epoch 9→16 in log); "Closed epoch" before each "Opening ID" |
| 2026-11-18 | D-18 lifecycle | Passed | Repeated background/resume cycles clean (`Lifecycle` foreground lines paired); no crash; observed process restart was a normal user close, not an ANR/force-stop |
| 2026-11-18 | D-19/D-20 settings + diagnostics UI | Partially passed | Settings render/persist (quality 100%, grid ON after user changes); diagnostics render real per-lens data (levels, zoom ranges, extension IDs 1/2, session ISO/exposure); Logs screen and share/export intent not yet exercised |
| 2026-11-18 | JPEG quality setting effect | Consistent (not isolated) | Same 12.48 MP output across captures grew 1.79 → 4.66 → 9.38 MB while the user moved the slider to 100%; final large files consistent with max-quality encoding; no per-capture quality log line exists |
| 2026-11-18 | Local fix compile | Passed | `:core:camera:compileDebugKotlin` BUILD SUCCESSFUL in 1m17s after P-011/P-012 fixes; corrupt bundletool cache removed first (P-013); CI rerun remains the authoritative gate |

Android `testDebugUnitTest` tasks were `NO-SOURCE` in all Android modules. Only
the nine `:core:model:test` tests executed; do not describe this as camera engine,
media, DataStore, log, Compose or instrumentation test coverage. Automated
coverage is unchanged: H-04 through H-08 unverified, D-04/05/09 through D-17
untested. The first D-series checks ran manually on-device on 2026-11-18 and are
recorded above with screenshots and pulled media; manual results do not create
regression tests.

## First verified APK and reports

- Run: [44235722163](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163),
  full source commit `9174a634e8fdaa38cdedf3745909f718157e65b3`.
- Command: `bash ./gradlew :core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug --stacktrace`.
- [camera3-debug-apk artifact](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163/artifacts/14397496809):
  ID `14397496809`, compressed archive 16,592,329 bytes; expires
  `2026-12-02T04:15:57Z`. GitHub sign-in may be required. Unzip `app-debug.apk`.
- [verification-reports artifact](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163/artifacts/14396794423):
  ID `14396794423`, expires `2026-12-02T04:15:59Z`.
- Local APK: `app/build/ci/44235722163/apk/app-debug.apk`;
  62,170,037 bytes (59.29 MiB), SHA256
  `3ad2038f304eb4cd9d961ce018ec8ffe59c5027018f21d8bf37615da5a59d491`.
- Local reports: `app/build/ci/44235722163/reports/`; full downloaded CI log:
  `app/build/ci/44235722163/actions.log`. All generated downloads are ignored by Git.
- Using installed Android SDK build tools 36.0.0, `apksigner.bat verify --verbose`
  passed (one signer, v2 signature), and `aapt2.exe dump badging` passed:
  `dev.camera3.app`, versionCode1 / `0.1.0-dev`, minSDK35 / targetSDK36,
  Camera3 label, debuggable, `dev.camera3.app.MainActivity` launcher.
- This is a runner-debug-signed build, not production release signing, install
  evidence or proof of preview/capture/saved-media behavior. Fresh CI keys can
  require uninstall/reinstall when changing builds; preserve needed app data first.

## First device baseline session (2026-11-18)

Environment: user's phone connected over USB with debugging authorized; Camera3
`0.1.0-dev` debug APK installed from the verified artifact. App property
observation: Android 16 / API 36 with spoofed Samsung props (a35x board,
SM-S711B model string, mixed fingerprint) consistent with the user-reported
Magisk/modified-camera-library environment. Treat all HAL-reported levels and
capabilities as platform claims (P-004), backed up here by session, result and
saved-file evidence.

Verified during the session:

- Permission, LEVEL_3 gate and baseline camera selection behaved as designed.
- Preview, grid overlay, torch, zoom, all four lens chips, capture+storage,
  background/resume and settings persistence worked with no crash.
- Saved JPEGs are real 4080×3060 4:3 photos with correct EXIF orientation 6 and
  genuine per-shot ISO metadata; the quality slider visibly changes file size.
- App persistent logs (`camera-0.log`) record discovery, eligibility, epoch
  transitions, controls, captures and storage; pulled for evidence.

Deferred to the next install (user was actively using the phone; adb input is
suspended while the phone is in use, verified foreground before any tap):

- Logs screen render, log filter/clear actions and the export/share intent
  (D-20 completion).
- "View full capabilities" detail screen render.
- One fresh capture with the quality-100 setting to isolate the quality effect.
- Re-verify the P-011 close-log fix and the P-012 capability labels after the
  rebuilt APK is installed.

Process note: before any `adb shell input tap`, confirm the target app is
foreground (`topResumedActivity`) and re-check after navigation; during this
session the user multitasked, and two taps landed in unrelated apps (Play
Store, Instagram) with no data changed. Screenshots are stored under ignored
`app/build/ci/device-testing/`.
