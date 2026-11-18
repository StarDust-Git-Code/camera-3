# Testing and evidence

Last updated: 2026-11-18. P-016 correction `cc73c69` is CI-green and its verified
APK restored preview/session opening on-device. Logs show JPEG+DNG save completion
on IDs 0/1; the user reports FHD/30 recording works and OIS does not. The current
maximum-resolution/smooth-zoom source is CI build-verified only; no phone access is
authorized for this implementation phase. Commit `d0ccb40` passed CI run
44298287574; its artifact is verified but not installed. The focused D-021
1080p60-only source passed local and CI gates; device evidence is pending.

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
| D-21 | Maximum-resolution still | Separate map/key exposed, complete pixel-mode session accepted, saved JPEG/RAW dimensions and truthful MP label; baseline upscaling/relabeling forbidden |
| D-22 | Advanced color/RAW/audio | Defined LUT/Log/HDR output, bounded RAW throughput/drop evidence, stereo channel verification and effective wired/USB monitoring route |

Unavailable features are recorded as unavailable with query/config evidence, not
counted as passing implementations. A failure must be repaired or explicitly
reported before delivery.

## Stress and performance

### P-016 correction retest order (planned, not results)

1. Assistant installs the verified correction when the phone is reconnected and
   unlocked; user performs UI actions. Keep mic permission ungranted initially
   if reinstall reset it. Stop if preview remains black and collect fresh logs.
2. Confirm live Photo preview, switch lenses and return to ID 0. Assistant checks
   attachment → valid target → open → configured session logs; save one JPEG.
3. On a RAW-capable lens, toggle RAW, wait for preview, capture a JPEG+DNG pair,
   then toggle RAW off. Inspect actual session streams, files and DNG metadata.
4. Switch Photo → Video → Photo → Video. Record a short silent clip and stop;
   inspect MP4 playback/orientation/video-only tracks before granting mic.
5. Grant microphone in Settings, return, record a spoken clip; verify actual
   audio track/playback/sync. Repeat start/stop, then background during a take
   and verify finalization plus preview recovery. No untested step is a pass.

- Repeat shutter/lens/mode operations and record start/stop under normal usage.
- Run longer recordings at validated settings; observe storage, temperature,
  dropped frames, memory and cleanup. Choose/document durations based on safe
  device conditions rather than claiming arbitrary targets were tested.
- Exercise interrupted configuration and cancellation while surfaces change.
- After P-016, stress rapid RAW→capture and repeated mode/lens changes to check
  delayed preference writes and late callbacks; availability must reflect busy
  capture/recording state. These cases are planned, not source-review passes.
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
| 2026-11-18 | Fix commit CI run | Passed | Commit `6b8b35f`, [run 44240950175](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44240950175) concluded success (background watch) |
| 2026-11-18 | Updated APK verification | Passed, host-observed | Downloaded `camera3-debug-apk`: 62,170,037 bytes, SHA256 `4f9d9661bc2e1d077090eef0be8e0d274bce2e029a81b1766698295840e30e44`, v2 signature verified, aapt2 confirms dev.camera3.app 0.1.0-dev minSDK35/targetSDK36; device install pending (phone disconnected at attempt) |
| 2026-11-18 | Docs checkpoint push CI | Passed | Commit `6212f3c`, [run 44243301133](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44243301133) concluded success (identical app sources to 6b8b35f); no new artifact downloaded |
| 2026-11-18 | Reinstall of the fix build | Passed | `install -r` failed (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, fresh runner debug key) → uninstall + streamed install Success; system dialog re-granted CAMERA; DataStore reset observed (quality 95%, grid OFF); grid re-enabled in-app; quality left at 95% (user decision); DCIM photos survived |
| 2026-11-18 | RAW pipeline local build | Passed, build-verified | `:core:model:test` 11 tests 0 failures (incl. 2 new RAW pairing tests) + `:core:camera|media|settings:compileDebugKotlin` + `:app:compileDebugKotlin` BUILD SUCCESSFUL; two real compile errors found and fixed (P-014, P-015); CI rerun remains the authoritative gate |
| 2026-11-18 | RAW commit CI run | Passed | Commit `7905a89`, [run 44247877745](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44247877745) concluded success (foreground watch, exit 0) |
| 2026-11-18 | RAW APK verification | Passed, host-observed | Downloaded `camera3-debug-apk`: 62,186,421 bytes, SHA256 `17ab7be0d15ed57b8f57bc56297cb8c50440f8aff74cd6aff4b2a26c1128cd78`, v2 signature verified, aapt2 confirms dev.camera3.app 0.1.0-dev; lint 0 errors/17 warnings; Gradle HTML report shows 11 tests / 0 failures; device install pending (phone disconnected at attempt) |
| 2026-11-18 | Video pipeline local build | Passed, build-verified | `:core:model:test` 12 tests 0 failures (incl. `videoNeedsBoundedWideSizeAndMatchingPreview`) + `:core:camera|media|settings:compileDebugKotlin` + `:app:compileDebugKotlin` BUILD SUCCESSFUL; two compile errors found and fixed in-session (nullable reader target, missing appContext field); CI rerun remains the authoritative gate |
| 2026-11-18 | Video commit CI run | Passed | Commit `409c5d9`, [run 44251120868](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44251120868) concluded success (foreground watch, exit 0); the docs checkpoint `0d40728` run 44248444802 was also CI-green |
| 2026-11-18 | RAW+Video APK verification | Passed, host-observed | Downloaded `camera3-debug-apk`: 62,235,613 bytes, SHA256 `685ad39061ea10f34987651602bafa9b6bd8ba144a868215854ad5d9fcc628ef`, v2 signature verified, aapt2 confirms dev.camera3.app 0.1.0-dev; lint 0 errors/17 warnings; Gradle HTML report shows 12 tests / 0 failures; device install was pending at that checkpoint (phone disconnected) |
| 2026-11-18 | RAW+Video reinstall | Passed, install only (recorded session checkpoint) | `install -r` failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; authorized uninstall + streamed install succeeded. CAMERA re-granted via `pm grant`, microphone user-granted; both grants corroborated by `debug-report-01/permissions.txt` |
| 2026-11-18 | RAW+Video artifact/report read-back | Passed, host-observed | Read-only size/SHA256, `apksigner verify --verbose` v2 signature and `aapt2 dump badging` identity match the checkpoint; local reports show 12 model tests / 0 failures and lint 0 errors / 17 warnings. No new Gradle/device actions performed for this read-back |
| 2026-11-18 | Latest D-01 discovery/eligibility | Passed this gate only | `debug-report-01/camera-0.log`: three passes, four reported LEVEL_3 IDs each, `eligible=true, baselineCamera=0`; not session/output verification |
| 2026-11-18 | Latest D-02 / RAW+Video device acceptance | Preview failed; further checks BLOCKED (P-016) | Resumed camera screen, black SurfaceView / "Camera paused", zero `Opening ID`, no post-reinstall Camera3 CONNECT. [P-016](PROBLEMS-AND-SOLUTIONS.md#p-016-camera-remains-paused-after-rawvideo-reinstall) records the investigation; artifacts: `app/build/ci/device-testing/debug-report-01/`. D-04/D-10/D-11 media checks not exercised |
| 2026-11-18 | P-016 pre-build documentation validation | Passed, host-observed | 21 Markdown files, 59 local links, 0 missing targets/device-identifier issues; HANDOFF 114 lines; `git diff --check` clean. No camera/runtime acceptance implied |
| 2026-11-18 | P-016 correction local test/lint/APK build | Passed, local build-verified | `.\gradlew.bat :core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug --stacktrace --console=plain`: BUILD SUCCESSFUL in 4m12s. XML: 12 model tests (task UP-TO-DATE), 5 new app state tests executed, 0 failures/errors/skips; lint 0 errors/16 warnings. APK at `app/build/outputs/apk/debug/app-debug.apk`; other Android-module tests NO-SOURCE. CI/physical retest pending |
| 2026-11-18 | P-016 final-source revalidation | Passed, local build-verified | Same full command BUILD SUCCESSFUL in 49s after UI busy-state alignment; app tests executed again, model task UP-TO-DATE; no physical retest |
| 2026-11-18 | P-016 prepublication checks | Passed, host-observed | 21 Markdown files / 59 local links / 0 issues, HANDOFF 115 lines, `git diff --check` clean; final XML reports 12 model + 5 app tests, no failures, lint 0 errors / 16 warnings |
| 2026-11-18 | P-016 correction push/CI start | Pushed; CI in progress | `cc73c69ae7383c979a6cee2c6682fd66ae64957d` pushed to `main`; [run 44259956383](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44259956383) in progress, `gh run watch --exit-status` active. No correction APK downloaded/installed yet |
| 2026-11-18 | P-016 correction CI/artifact verification | Passed, host-observed | Run 44259956383 completed successfully. Downloaded APK is 62,251,997 bytes, SHA256 `33430BEB11A683E0944002CC2CD608E7AF358F7AE780085F007C4A80F0768F1E`; v2 signature and package/API identity passed. CI reports: 12 model + 5 app tests, 0 failures; lint 0 errors/17 warnings |
| 2026-11-18 | P-016 correction install/retest | Passed for preview/session regression | Verified correction APK installed. `p016-retest/fix-startup-camera-0.log` records valid Surface attachment, `Opening ID 0`, configured Photo/Video sessions, Photo↔Video transitions, lens switch and background close/relaunch; black paused state did not recur. P-016 is device-resolved for this reproduction |
| 2026-11-18 | RAW+JPEG correction-build probe | Partial device pass | Log records ID 0 JPEG 4080×3060 + RAW 4080×3060 and ID 1 JPEG 1920×1440 + RAW 4128×3096 sessions, followed by JPEG and DNG save completion for each. Files were not pulled/opened; DNG validity, metadata and rotation remain unverified |
| 2026-11-18 | Standard Video correction-build probe | Partial, user-reported | Log proves ID 0 1920×1080 silent Video session configuration; user reports FHD/30 recording works but OIS does not. MP4 tracks, orientation, cadence, A/V sync and stabilization request/result state were not independently inspected |
| 2026-11-18 | Maximum-resolution/smooth-zoom targeted tests | Passed, local build-verified | 14 model + 6 app state tests passed after added policy/configuration-identity coverage; camera/app Kotlin compilation passed. Synthetic tests are not hardware/map/session/file or perceptual zoom evidence |
| 2026-11-18 | Maximum-resolution/smooth-zoom final local gate | Passed, local build-verified | Full command `:core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug` BUILD SUCCESSFUL in 36s. XML: 14 model + 6 app tests, 0 failures/errors/skips; lint 0 errors/16 warnings. Local APK 62,568,619 bytes, SHA256 `5E01DBFA7AFFBE77D3B3D89C4FAAB0948BB4B6ED173768FCF98637F362942D13`; v2 signature and package identity checked. No phone test |
| 2026-11-18 | Maximum-resolution tranche documentation/diff check | Passed, host-observed | 21 Markdown files, 59 local links / 0 broken targets, 93-line HANDOFF; `git diff --check` clean before publication review |
| 2026-11-18 | Maximum-resolution/smooth-zoom publication CI | Passed, CI/host-observed | Commit `d0ccb4003d45836f45df8ceeaeb7428becdf7145`, [run 44298287574](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44298287574), completed in 2m4s. Reports: 14 model + 6 app tests, 0 failures/ignored; lint 0 errors/17 warnings. APK and report uploads passed; Node/action/runtime and wrapper-executable annotations remain P-010 maintenance, not failures |
| 2026-11-18 | Focused 1080p60 policy and local full gate | Passed, local build-verified | `:core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug` BUILD SUCCESSFUL in 1m12s. Reports: 15 model + 6 app tests, 0 failures/ignored; lint 0 errors/16 warnings. APK 62,568,739 bytes, SHA256 `244A1F0C4A2BA29E590BFC535DC92C6A6F9F195AB93B72279C2CD982C517E533`; v2 signature, package `dev.camera3.app`, minSdk35/targetSdk36 verified. Synthetic checks are not phone/session/cadence evidence |
| 2026-11-18 | Focused 1080p60 documentation/diff check | Passed, host-observed | 21 Markdown files, 60 local links, 0 broken targets, 99-line HANDOFF; `git diff --check` clean before publication |
| 2026-11-18 | Focused 1080p60 publication CI | Passed, CI/host-observed | Commit `c78ae3a21e65bec93f4a1be9fbb5efee279390c9`, [run 44303969928](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44303969928), completed in 2m00s. Reports: 15 model + 6 app tests, 0 failures/ignored; lint 0 errors/17 warnings; APK and report uploads passed. Downloaded APK: 62,284,765 bytes, SHA256 `A8EC60F269D357D1455301154E21A2539FE434A0FE4593871AF662EDC34FF0DC`; v2 signature, Camera3 / `dev.camera3.app` / minSdk35 / targetSdk36 verified. Not installed; no physical cadence claim |
| 2026-11-18 | Focused 1080p60 post-CI documentation check | Passed, host-observed | 21 Markdown files, 60 local links, 0 broken targets, 100-line HANDOFF; `git diff --check` clean before docs checkpoint |

Historical CI test reports cover only `:core:model:test` (nine initially,
12 in RAW+Video CI). P-016 added five CI-green app state tests; the current local
tranche adds a sixth. These do not test Camera2, media, DataStore, logging,
Compose or instrumentation. H-04 through H-08 remain incomplete; advanced
physical checks remain partial or untested. The manual D-series passes do not
create automated regression coverage.

D-021 acceptance requires a saved 1920×1080 MP4 whose frame timestamps measure
approximately 60 fps without systematic drops, plus successful session, playback,
orientation and A/V checks. A `[60,60]` request or nominal track rate alone is not a pass.

## Verified APK builds and reports

All builds are runner-debug-signed, not production release signing, and none
implies device verification beyond what the execution record states. Fresh CI
keys can require uninstall/reinstall between builds; preserve needed app data
first. All generated downloads are ignored by Git.

### Focused 1080p60 build (verified; not installed)

- Run: [44303969928](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44303969928),
  source commit `c78ae3a21e65bec93f4a1be9fbb5efee279390c9`; workflow success.
- Local artifact: `app/build/ci/44303969928/camera3-debug-apk/app-debug.apk`;
  62,284,765 bytes, SHA256
  `A8EC60F269D357D1455301154E21A2539FE434A0FE4593871AF662EDC34FF0DC`.
- `apksigner` verified v2 signing. `aapt2` verified Camera3 /
  `dev.camera3.app` / `0.1.0-dev` / minSdk35 / targetSdk36.
- Reports show 15 model + 6 app tests, no failures/ignored; lint 0 errors/17 warnings.
- Installed on the connected phone (`RFCX20PKXWB`, SM-S711B/a35x) after uninstalling
  the prior runner-signed build; DataStore reset as expected. Actual capability,
  session acceptance, cadence and saved-file evidence still need your physical run.
  This artifact does not prove the phone exposes/accepts the required configuration
  or records 60 fps.

### Maximum-resolution/smooth-zoom build (verified; not installed)

- Run: [44298287574](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44298287574),
  source commit `d0ccb4003d45836f45df8ceeaeb7428becdf7145`; workflow success.
- Local artifact: `app/build/ci/44298287574/camera3-debug-apk/app-debug.apk`;
  62,268,381 bytes, SHA256
  `69A5B0F6186DC5B3BF9C72A845B47CCB7CFA77921E64FD39B765B9C1A2317910`.
- `apksigner` verified v2 signing. `aapt2` verified Camera3 /
  `dev.camera3.app` / `0.1.0-dev` / minSdk35 / targetSdk36.
- Downloaded reports show 14 model + 6 app tests, no failures/ignored; lint
  0 errors/17 warnings.
- Not installed or device-tested by instruction. This artifact does not prove
  maximum-resolution map availability, output dimensions or smooth-zoom feel.

### P-016 correction build (installed; regression resolved, media partial)

- Run: [44259956383](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44259956383),
  source commit `cc73c69`; workflow completed successfully.
- Local APK: `app/build/ci/44259956383/camera3-debug-apk/app-debug.apk`;
  62,251,997 bytes, SHA256
  `33430BEB11A683E0944002CC2CD608E7AF358F7AE780085F007C4A80F0768F1E`.
- Build-tools verification: v2 signature passed; `aapt2` confirmed Camera3 /
  `dev.camera3.app` / `0.1.0-dev` / targetSDK36.
- CI reports under `app/build/ci/44259956383/verification-reports/`: 12 model
  + 5 app tests, no failures; lint 0 errors/17 warnings.
- Installed and device-retested. Preview/session opening and Photo↔Video
  reconfiguration passed; JPEG+DNG save completion has log evidence on IDs 0/1.
  Saved DNG/MP4 inspection and complete media acceptance remain open.

### RAW+Video build (historical P-016 reproduction)

- Run: [44251120868](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44251120868),
  source commit `409c5d9` (RAW + Video). CI-success conclusion and both artifact
  upload steps independently re-queried read-only on 2026-11-18.
- Local APK: `app/build/ci/44251120868/camera3-debug-apk/app-debug.apk`;
  62,235,613 bytes, SHA256
  `685ad39061ea10f34987651602bafa9b6bd8ba144a868215854ad5d9fcc628ef`.
  Size and hash independently rechecked read-only on 2026-11-18.
- Build-tools 36.0.0 checks re-run: `apksigner verify --verbose` passed with
  v2 signing; `aapt2 dump badging` confirmed `dev.camera3.app`, versionCode 1 /
  `0.1.0-dev`, Camera3 label, targetSDK36.
- Local reports: `app/build/ci/44251120868/verification-reports/`; read-back
  confirmed 12 model tests / 0 failures / 0 ignored, lint 0 errors / 17 warnings.
  This is CI artifact inspection, not a new build or physical-camera test.
- **Installed** via the authorized reinstall recorded above; both camera and
  microphone permissions are granted in the saved device report.
- This installed build reproduced P-016 and is not the current baseline. The
  correction build above resolved that startup reproduction.

### RAW build (historical; superseded install candidate)

- Run: [44247877745](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44247877745),
  source commit `7905a89` (RAW/DNG pipeline + docs); a foreground watch
  observed conclusion success.
- Local APK: `app/build/ci/44247877745/camera3-debug-apk/app-debug.apk`;
  62,186,421 bytes, SHA256
  `17ab7be0d15ed57b8f57bc56297cb8c50440f8aff74cd6aff4b2a26c1128cd78`.
- Local reports: `app/build/ci/44247877745/verification-reports/` (lint +
  11 model tests).
- Build-tools checks passed: `apksigner verify` (v2 scheme) and
  `aapt2 dump badging`: `dev.camera3.app`, versionCode 1 / `0.1.0-dev`,
  Camera3 label, debuggable.
- Device install was pending (phone disconnected at the attempt); the installed
  RAW+Video build above supersedes this candidate. At that checkpoint the runner
  debug key differed from the installed build, so the expected path was
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → uninstall/reinstall, with a settings
  reset (quality 95%, grid OFF) on first launch.
- Device verification plan for this build (D-016): RAW chip visibility per
  lens, toggle → close/reopen → 3-stream session probe outcome, capture saving
  a JPEG+DNG pair, DNG opening/rotation, honest failure path, plus the
  deferred UI checks (Logs/share, full-caps detail, P-011 close logs, P-012
  capability labels).

### Fix build (superseded install candidate)

- Run: [44240950175](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44240950175),
  source commit `6b8b35f` (P-011/P-012 fixes + device-session docs); a
  background watch observed conclusion success.
- Local APK: `app/build/ci/44240950175/apk/app-debug.apk`; 62,170,037 bytes,
  SHA256 `4f9d9661bc2e1d077090eef0be8e0d274bce2e029a81b1766698295840e30e44`.
- Local reports: `app/build/ci/44240950175/reports/` (lint + nine model tests).
- Build-tools 36.0.0 checks passed: `apksigner verify --verbose` (v2 signature)
  and `aapt2 dump badging`: `dev.camera3.app`, versionCode 1 / `0.1.0-dev`,
  minSDK35 / targetSDK36, Camera3 label, debuggable, launcher MainActivity.
- Installation was pending at the first attempt, then succeeded via the
  uninstall/reinstall recorded above. Superseded by the installed RAW+Video
  build; deferred UI checks are still unverified.

### First build (provenance)

- Run: [44235722163](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163),
  full source commit `9174a634e8fdaa38cdedf3745909f718157e65b3`.
- Command: `bash ./gradlew :core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug --stacktrace`.
- [camera3-debug-apk artifact](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163/artifacts/14397496809):
  ID `14397496809`, compressed archive 16,592,329 bytes; expires
  `2026-12-02T04:15:57Z`. GitHub sign-in may be required. Unzip `app-debug.apk`.
- [verification-reports artifact](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163/artifacts/14396794423):
  ID `14396794423`, expires `2026-12-02T04:15:59Z`.
- Local APK: `app/build/ci/44235722163/apk/app-debug.apk` (59.29 MiB, SHA256
  `3ad2038f304eb4cd9d961ce018ec8ffe59c5027018f21d8bf37615da5a59d491`); this is
  the build that passed the first device baseline session.
- Local reports + CI log: `app/build/ci/44235722163/reports/`,
  `app/build/ci/44235722163/actions.log`.

## First device baseline session (2026-11-18)

Environment: user's phone connected over USB with debugging authorized; Camera3
`0.1.0-dev` debug APK installed from the first verified artifact (`9174a63`).
This section is historical, not evidence that the latest RAW+Video build works.
App property observation: Android 16 / API 36 with spoofed Samsung props (a35x
board, SM-S711B model string, mixed fingerprint) consistent with the user-reported
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
