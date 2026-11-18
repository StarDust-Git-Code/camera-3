# Testing and evidence

Last updated: 2026-11-18. Initial source and eight model unit tests exist. No
application build, automated application test or physical-device test has run yet.

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
| Samsung Galaxy A35 | User-reported; not connected/queried in recorded ADB checks | Primary camera/runtime/quality validation |
| One UI 8.5 / modified camera stack | User-reported, exact build/module unknown | Validate exposed versus effective Camera2 behavior |
| Android 15/API 35 | Required minimum, actual phone API not yet queried | Minimum-platform compatibility |
| Later Android versions | Supported target range, untested | API compatibility and runtime restrictions |
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
| 2026-11-18 | H-01 / H-02 / policy tests | Pending | GitHub Actions workflow and test source exist; execution not claimed |

No H-series or D-series test has run. Update this table with actual outcomes,
not expected results.
