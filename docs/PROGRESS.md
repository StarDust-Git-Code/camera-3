# Progress and evidence

Last updated: 2026-11-18. Initial Photo implementation exists. First debug APK
build-verified and installed on the user's phone; baseline device session
completed with fixes for two runtime findings awaiting the next CI build.

## Current summary

| Area | Status | Evidence / limitation |
| --- | --- | --- |
| Workspace inspection | Host-observed complete | Empty directory before documentation |
| Host Java/SDK/ADB | Host-observed available | Read-only tool/path/component inspection |
| Device description | User-reported | A35, One UI 8.5, Magisk, unlocked BL/camera library |
| Physical device connection | Device-observed available | Authorized over USB; Android 16/API 36 with spoofed Samsung props; serial excluded from docs |
| Device baseline session | Device-verified (partial) | Permission/gate, preview, capture+storage, torch, zoom, 4-lens switching, lifecycle, settings persistence passed; Logs/share/full-caps/quality-isolation deferred (TESTING) |
| Requirements/design | Documented | REQUIREMENTS, DECISIONS and subsystem docs |
| Project-local Markdown | Created | Root AGENTS/README and 18 documents under docs |
| Documentation consistency | Host-verified / reviewed | Final build checkpoint: 20 Markdown files, 56 local links, 0 issues, 72-line handoff; history in TESTING |
| Android scaffold | Build-verified | Six modules compiled in run 44235722163 at 9174a63; API35 minimum/API36 target |
| Photo/settings/logger | Device-verified for Photo path | Real Camera2/JPEG/MediaStore verified on-device with saved media; log screen/share intent and quality isolation still to exercise |
| Runtime findings | Fixed, re-verify pending | P-011 close-log noise and P-012 unmapped capability labels fixed; local compile passed; next CI/device run confirms |
| Recording/RAW/extensions/manual | Not implemented | Disabled or diagnostics-only, no fake pipeline |
| GitHub Actions | Passed | `9174a63`, run 44235722163: tests/lint/APK + both artifact uploads passed |
| App builds/tests/device validation | Build/model tests passed; device untested | Nine model tests passed; lint 0 errors/17 warnings; Android test tasks NO-SOURCE |
| APK/delivery | Debug artifact verified | Artifact 14397496809 downloaded; signature/package checked locally; exact path/hash in TESTING |

## Completed record

### 2026-11-18: Initial inspection and scope clarification

- Inspected `C:\Users\irsha\Desktop\camera3`; no existing project files.
- Found JDK 21.0.6, Android SDK platforms including 35/36/36.1 and ADB.
- Checked attached devices more than once during the conversation; no connected
  device was reported.
- Recorded the user's narrowed Android 15+ and LEVEL_3-only scope.
- Recorded the user-reported A35/One UI/Magisk/camera modification environment
  without asserting actual camera support.
- Inspected official Camera2 discovery, extensions, HDR, multi-camera,
  high-speed, DNG and Compose insets references.
- Tool discovery found no available DevRelay connector; no community claims
  were fabricated.

### 2026-11-18: Documentation bootstrap

- User requested persistent Markdown for decisions, goals, stack, structure,
  problems/solutions, plans and context recovery.
- User requested those files inside the project and switched out of Plan mode.
- Created `AGENTS.md`, root `README.md` and a linked 18-file `docs/` set.
- Added compact resume/maintenance rules, evidence vocabulary, issue ledger,
  implementation roadmap and actual-versus-proposed layout.
- Consulted OpenCode V2 instructions for root project guidance. No OpenCode
  config, application source, dependencies, device modifications or external
  plan documents were created.
- Final check verified all 20 Markdown files, 18 under docs, with 61 local links
  and zero validation issues. No non-Markdown files were created.
- Read back the required recovery docs and reviewed the Android 15+/LEVEL_3
  constraints and clear separation of proposals, reports and implementation.
- Automated checks confirmed minSdk 35 / LEVEL_3 in five canonical files and a
  69-line handoff below its 150-line budget. Recorded DOC-01 to DOC-04 results in
  TESTING and completed documentation roadmap M0.

## Next tasks

1. Push the P-011/P-012 fixes + device-session docs, watch CI, download and
   verify the new APK, then reinstall (fresh CI debug keys may require
   uninstall/reinstall).
2. Finish the deferred device checks: Logs screen + share/export intent,
   "View full capabilities" detail, one fresh quality-100 capture, and clean
   close-log observation (P-011/P-012 re-verification) — only while the user
   is not using the phone, verifying foreground before each tap.
3. Address reviewed maintenance warnings separately (P-010), without blanket
   suppression or untested major upgrades.
4. Plan M3 with the observed per-lens facts: BOKEH (2) exists, NIGHT (4) is not
   advertised; RAW/manual flags need per-lens key-range revalidation before UI.

### 2026-11-18: Implementation bootstrap (not yet compiled)

- Created six Gradle modules, public Camera2 discovery/LEVEL_3 selection policy,
  handler-confined engine and SurfaceView preview. Old device closure precedes
  reopening; stale run callbacks are checked; JPEG images close after IO copies.
- Added JPEG MediaStore transaction/cleanup, request orientation, last-photo view,
  conditional zoom/torch, actual camera chips, immutable ViewModel state.
- Added persisted grid/quality, actual settings/permissions, capability diagnostics,
  bounded logs/search/live/clear/export/share. No advanced-mode dummy controls.
- Added eight Android-independent policy tests (not executed yet).
- Verified official wrapper JAR checksum; pinned distribution checksum. Corrected
  checksum-response decoding and initial Kotlin compatibility selection (P-007/008).
- Rechecked ADB: no devices. `gh` unavailable; Git Credential Manager lists the
  intended owner account. Remote API reports public/empty; no refs from ls-remote.
- User supplied the UI reference again and requested GitHub Actions compilation;
  workflow pins verified action SHAs and uploads debug APK + verification reports.

### 2026-11-18: First push and CI infrastructure failure

- Published 58 source/document/build files to the inspected empty repository,
  `main`, commit `9e4686c`; no force push. Commit author explicitly OpenCode Agent.
- Portable official GitHub CLI v2.105.0 downloaded into approved temp storage;
  release SHA256 verified. Git credential used only in process memory for Actions
  inspection; not printed or persisted in source.
- Run 44235279035 failed at SDK installation: `sdkmanager` not on PATH, exit127.
  Model tests and app compilation did not run. Corrected explicit SDK setup (P-009).
- Independent review corrected rotation expression precedence, torch writable-key
  gating, added a ninth model test and `.gitattributes`. Rerun pending.

### 2026-11-18: SDK setup fix rerun

- Committed and pushed SDK setup/rotation/torch corrections as `9174a63`.
- [Run 44235722163](https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235722163)
  passed Android SDK setup and reached the combined Gradle test/lint/APK step.
  At that checkpoint the background watcher was active; result recorded below.
- Resume inspection confirmed the latest commit and no uncommitted source changes.
  Documentation now records the running build rather than an unpushed fix.

### 2026-11-18: First successful CI APK and local inspection

- Run 44235722163 at `9174a63` completed successfully: job 3m39s, Gradle 2m53s;
  nine model policy tests passed, lint 0 errors/17 warnings, both artifacts uploaded.
- Android module unit-test tasks were NO-SOURCE; no camera/platform/UI tests
  executed. Four compiler warnings and three CI annotations assessed in P-010.
- Downloaded APK and reports with the verified portable CLI, auth only in process
  memory. APK under ignored `app/build/ci/44235722163/apk/app-debug.apk`, 59.29 MiB.
  Build-tools 36.0.0 verified v2 signature and expected package/API35 minimum/API36
  target. Artifact IDs, expiration and SHA256 are canonical in TESTING.
- ADB resume recheck still had no device. Physical preview/capture/media/lifecycle
  verification is blocked, not silently marked complete.
- Updated canonical documentation from compilation-pending to build-verified;
  device-unverified/advanced-pipeline limitations remain explicit. Remote source
  stays at the verified build commit; documentation checkpoint is a docs-only push.

### 2026-11-18: First device baseline session (user's phone)

- User connected and authorized the phone; ADB query read Android 16 / API 36
  with spoofed Samsung props (a35x board, SM-S711B model string), matching the
  user-reported Magisk environment; serial excluded from docs.
- Installed the verified APK (`install -r` Success). The user then exercised the
  app themselves: granted permission, captured, toggled torch, dragged zoom,
  switched all four lenses, changed settings.
- Device-verified: four real 4080×3060 JPEGs in `DCIM/Camera3` (EXIF
  orientation 6, model string, true ISO 152–222); preview/grid/mirroring; zoom
  1.0–8.0 without rejection; close-before-reopen across dozens of lens switches
  (epoch 9→16); clean background/resume; settings persistence; diagnostics
  screens render real per-lens data; persistent `camera-0.log` captured the
  whole session. No crash; screenshots + pulled JPEGs + log copy under ignored
  `app/build/ci/device-testing/`.
- Discovery facts: all four IDs LEVEL_3 with RAW/MANUAL_SENSOR/
  READ_SENSOR_SETTINGS/MANUAL_POST_PROCESSING/BURST_CAPTURE/YUV_REPROCESSING/
  STREAM_USE_CASE (two showed as `CAPABILITY_5/19` before the label fix); ID 0
  adds HIGH_SPEED_VIDEO; baseline JPEGs 4080×3060/1920×1440/3264×2448/3408×2556;
  zoom ranges 1.0–8.0 (rear) and 1.0–4.0 (front); extension IDs 1 (BEAUTY) and
  2 (BOKEH) on all lenses; NIGHT(4) not advertised; no physical sub-IDs.
- Findings recorded: P-011 (duplicate close ISE logs), P-012 (unmapped
  capability constants), P-013 (corrupt bundletool cache). Both code fixes
  compile-verified locally; P-001 closed; P-004 advanced to observed evidence.
- Remaining UI checks (Logs screen, share intent, full-caps detail, isolated
  quality-100 capture) deferred because the user was actively using the phone;
  adb input stopped and foreground verification noted in TESTING.

## Active blockers

- **P-004:** advanced-pipeline claims still need per-pipeline runtime evidence.
- **P-010:** reviewed nonblocking warnings; backup/transfer policy and action
  runtime/executable-mode maintenance remain follow-ups.
- **P-011/P-012:** fixes await the next CI build + device re-verification.
- No hard blocker; the user controls phone availability for deferred checks.

## Update policy

After material changes update this summary and append concise dated evidence.
Link longer investigations instead of duplicating them. A passing test needs an
actual outcome. A feature's state must advance through implementation/build/
device evidence, not from a roadmap checkbox alone.
