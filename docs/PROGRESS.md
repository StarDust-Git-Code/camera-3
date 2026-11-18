# Progress and evidence

Last updated: 2026-11-18. Correction `cc73c69` is CI-green and device-retested:
preview/session opening works, with partial RAW/Video evidence. Genuine
maximum-resolution still discovery and smooth video zoom are implemented and
CI build-verified but device-unverified. A focused 1080p60-only Video policy is
also CI build-verified; physical cadence evidence is pending.

## Current summary

| Area | Status | Evidence / limitation |
| --- | --- | --- |
| Workspace inspection | Host-observed complete | Empty directory before documentation |
| Host Java/SDK/ADB | Host-observed available | Read-only tool/path/component inspection |
| Device description | User-reported | A35, One UI 8.5, Magisk, unlocked BL/camera library |
| Physical device connection | Historical device evidence; current access unavailable by instruction | Android 16/API 36 with spoofed Samsung props; serial excluded from docs |
| Device baseline session | Device-verified (partial) | Permission/gate, preview, capture+storage, torch, zoom, 4-lens switching, lifecycle, settings persistence passed; Logs/share/full-caps/quality-isolation deferred (TESTING) |
| Requirements/design | Documented | REQUIREMENTS, DECISIONS and subsystem docs |
| Project-local Markdown | Created | Root AGENTS/README and 18 documents under docs |
| Documentation consistency | Host-verified / reviewed | Current check: 21 Markdown files, 60 local links, 0 broken targets, 100-line handoff; history in TESTING |
| Android scaffold | Build-verified | Six modules compiled in run 44235722163 at 9174a63; API35 minimum/API36 target |
| Photo/settings/logger | Device-verified for Photo path | Real Camera2/JPEG/MediaStore verified on-device with saved media; log screen/share intent and quality isolation still to exercise |
| Runtime findings | P-016 device-resolved for reproduced startup path | Correction log shows valid attachment/open/configure across Photo/Video/lens/lifecycle; P-011/P-012 checks remain |
| Recording/RAW | Implemented / partial device evidence | JPEG+DNG save logs on IDs 0/1; user reports FHD/30 recording works. Saved DNG/MP4 inspection remains |
| 1080p60-only Video | Implemented / CI build-verified | `c78ae3a`, run 44303969928; capability-gated regular or constrained sessions. Device cadence pending |
| Maximum-resolution/smooth zoom | Implemented / CI build-verified | `d0ccb40`, run 44298287574; no installed build has queried maximum-resolution maps or assessed zoom feel |
| GPU color/RAW-frame/stereo monitoring | Planned | D-020 architecture only; no controls exposed |
| Extensions/manual | Not implemented | Disabled or diagnostics-only, no fake pipeline |
| GitHub Actions | 1080p60 tranche passed | `c78ae3a`, run 44303969928 success; 15 model + 6 app tests, lint and both artifact uploads passed |
| App builds/tests/device validation | Current tranche CI build-verified | CI: 21 tests, no failures/ignored; lint 0 errors/17 warnings. No D-021 phone test |
| APK/delivery | D-021 CI artifact verified; now installed | New APK 62,284,765 bytes, SHA256 `A8EC60F2…4FF0DC`, v2/package/SDK verified; installed on RFCX20PKXWB; DataStore reset on first launch |

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

1. When device testing is separately authorized, inspect the exact 1080p60
   capability/session and saved MP4 timestamps before calling it achieved on-device.

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
- Pushed fixes + docs as `6b8b35f`; CI run 44240950175 passed (background
  watch). Fix-build APK verified locally (SHA256, v2 signature, package/API)
  with reports downloaded to `app/build/ci/44240950175/`; device install
  pending because the phone was disconnected at the attempt.
- Docs checkpoint `6212f3c` pushed and CI-green (run 44243301133); its ledger
  row rides with the next batch to avoid a docs-push loop.

### 2026-11-18: Fix build installed; RAW/DNG pipeline implemented

- The fix build was installed on the phone: `install -r` rejected it because
  the fresh runner re-signed the APK (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`);
  uninstall + streamed install succeeded, the system dialog re-granted CAMERA,
  and the DataStore reset (quality 95%, grid OFF) was confirmed — the grid was
  re-enabled in-app and the quality slider was deliberately left at 95%
  (user decision; no more slider work). DCIM photos survived the reinstall.
- User direction for this phase: work on RAW first, then Video; stop spending
  time on the quality slider (95% accepted).
- RAW/DNG pipeline implemented per D-016 across model/policy/tests, discovery,
  engine, PhotoStore, settings, ViewModel and the camera UI: RAW chip only on
  lenses advertising RAW with real sizes, aspect-matched pairing (±0.03) with
  no invented streams, dual-surface still capture, result/image pairing with
  re-arming watchdog, `DngCreator` with javap-verified metadata/EXIF handling
  (P-014), streamed MediaStore DNG save, honest per-output completion messages.
- Local verification: `:core:model:test` 11 tests 0 failures (two new RAW
  pairing tests) and all touched modules compile (P-014/P-015 recorded).
  CI remains the authoritative gate; nothing is device-verified yet.
- Device verification for RAW (chip per lens, session-probe outcome, saved DNG
  + rotation) and the deferred UI checks (Logs/share, full-caps detail, clean
  close logs, capability labels) ride with the next device session.
- Video (M4) planned items were scoped during the RAW wait: MediaRecorder
  surface pairing, record/stop state machine, orientation hint, silent mode
  without mic grant, session reconfiguration.
- RAW commit `7905a89` pushed; CI run 44247877745 passed (foreground watch).
  RAW-build APK downloaded and verified locally (SHA256, v2 signature, package
  identity; 11 tests / 0 failures in CI); phone was disconnected at the install
  attempt, so RAW device verification is pending the next connection.
- Video (M4) first cut implemented per D-017 while the phone is away: bounded
  16:9 ≤1080p MediaRecorder policy with aspect-matched preview (12/12 model
  tests), plan-driven sessions (`StreamPlan.Photo`/`Video`) that close/reopen
  the camera on mode switch, recorder prepared before `openCamera` with a
  pending MediaStore fd, record start adds the recorder surface to the
  repeating request, shared finalize (publish if frames recorded, discard
  otherwise) used by user stop, backgrounding and `close()`, silent mode
  without `RECORD_AUDIO` (Settings Grant row), REC timer UI, and honest
  "Video unavailable on this lens" copy when no qualifying pairing exists.
- User shared four open-source camera projects as reference; surveyed and
  recorded in REFERENCE-PROJECTS.md (license implications included: Open
  Camera is GPLv3 — concepts only, no code copying). Borrowable ideas noted
  for M5/M6 (recording policy, foreground-service recording survival, FPS
  lock, storage benchmark, GPU RAW path) and the deferred photo ideas (LUT
  engine, Motion Photos, multi-frame Night, AI depth bokeh — PhotonCamera).

### 2026-11-18: Parallel P-016 investigation and startup corrections

- User authorized multiple subagents. Independent static audit, bounded-log
  instrumentation, host/CI/APK verification and issue/test documentation completed;
  separate implementation and regression-test passes followed.
- Host independently re-queried success for `409c5d9` / 44251120868 and
  `0d40728` / 44248444802; installed RAW+Video APK bytes/hash/v2 signature,
  package/API35 minimum/API36 target and 12-test/lint reports match TESTING.
- Fresh audit found `Application.display` before `setSource` in both modes;
  installed SDK source confirms non-display-context exceptions and SurfaceView
  callback exception handling. The original startup logcat is outside the saved
  window, so this is a demonstrated source defect, not captured runtime causality.
- D-018 source corrections implemented: rotation from SurfaceView, independent
  Photo availability, full active-configuration preview key. Existing engine
  gates/resource logic unchanged; P-016 DEBUG logs expose callback/attachment and
  gate outcomes. Added five synthetic CameraUiState regression tests; none proves
  Compose callback behavior or physical output. Local full validation passed
  (`:core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug`,
  BUILD SUCCESSFUL in 4m12s); model task was up-to-date, new app tests ran.
  XML reports: 12 model + 5 app tests, no failures/errors/skips; lint 0 errors /
  16 warnings. Other Android-module unit tests remain NO-SOURCE.
- Independent integrated-source review found no blocking wiring defect; noted
  queued RAW-update/late-callback stress cases. UI lens/mode/RAW availability
  now follows model busy state during both capture and recording; final-source
  full local revalidation passed (BUILD SUCCESSFUL in 49s). Independent review
  and compilation are not a physical retest; no device inputs were performed.
- ADB foreground query found the test phone disconnected; no installs or inputs
  performed. DevRelay namespace discovery again returned no tools; no community
  or event claims fabricated.
- Pre-build documentation check: 21 Markdown files, 59 local links, 0 missing
  targets/identifier issues, 114-line handoff; `git diff --check` clean.
- Final prepublication checks also passed: 21 Markdown files / 59 local links,
  115-line handoff, 0 issues; final reports still 12 model + 5 app tests with
  no failures, lint 0 errors / 16 warnings.
- Committed/pushed corrections, regression tests and reference/issue/test docs as
  `cc73c69`; CI run 44259956383 is in progress, background watcher active.
  Post-push evidence updates remain local for the next meaningful commit batch.

### 2026-11-18: P-016 retest and maximum-resolution/smooth-zoom tranche

- Run 44259956383 completed successfully. Its APK/reports were downloaded and
  verified: 62,251,997 bytes, SHA256 `33430BEB…F1E`, v2 signed; 12 model + 5 app
  tests passed; lint reported 0 errors/17 warnings.
- Installed correction restored real Surface attachment, camera opening and
  configured Photo/Video sessions. Photo↔Video, ID 0↔1 and background/relaunch
  sequencing completed without the paused-state reproduction; P-016 is resolved
  for that path.
- Device log records JPEG+DNG save completion on ID 0 (4080×3060 pair) and ID 1
  (1920×1440 JPEG + 4128×3096 RAW). Files were not pulled/opened, so DNG validity,
  metadata and rotation are still unverified.
- User reports FHD/30 recording works and OIS does not. The app currently does
  not request/verify OIS/EIS, so stabilization remains unimplemented; MP4 track,
  orientation, cadence and A/V inspection are still open.
- Added D-019 maximum-resolution discovery/policy/output/request handling,
  persisted HI-RES gating with actual-MP labels and logarithmic smoothstep Video
  zoom. Android docs confirm the map/key may exist without the ultra-high-resolution
  capability flag. No installed build has queried these maps.
- Added D-020 for a future app-owned GPU/MediaCodec/AudioRecord pipeline covering
  LUT, defined Log/HDR, bounded RAW-frame storage, stereo and verified wired/USB
  monitoring. It remains planned with no UI toggles.
- Initial full local gate passed with 14 model + 5 app tests. After added
  configuration-identity coverage and atomic standard-mode fallback, the final
  full gate passed in 36s: 14 model + 6 app tests, no failures/errors/skips;
  lint 0 errors/16 warnings; APK assembled, v2 signature/package checked.
- Committed/pushed as `d0ccb40`. CI run 44298287574 passed in 2m4s; reports show
  14 model + 6 app tests, 0 failures/ignored, and lint 0 errors/17 warnings.
  Downloaded APK is 62,268,381 bytes, SHA256 `69A5B0F6…317910`; v2 signature,
  package `dev.camera3.app`, minSdk35 and targetSdk36 were verified. It was not
  installed because phone access is not authorized for this phase.

### 2026-11-18: Focused 1080p60-only tranche

- User limited the task to 1920×1080 at 60 fps only and requested all CI checks pass.
- Discovery now records AE FPS ranges, per-MediaRecorder-size minimum frame
  durations, per-size high-speed ranges and AVC encoder 1080p60 support.
- Policy exposes Video only for a complete fixed-60 configuration. Regular mode
  is preferred; constrained high-speed is accepted only with explicit `[60,60]`
  support and same-size preview. No 30 fps or alternate-resolution fallback remains.
- Engine sets `[60,60]`, records H.264 at 60 fps, and uses the constrained-session
  generated repeating-burst API when required. UI truthfully labels 1080p60.
- Final local full gate passed in 1m12s: 15 model + 6 app tests, no failures/ignored;
  lint 0 errors/16 warnings; APK assembled and v2/package/SDK verified. No phone
  access was used, so actual capability, session acceptance and cadence remain unknown.
- Committed/pushed as `c78ae3a`. CI run 44303969928 passed in 2m00s; 15 model +
  6 app tests had no failures/ignored, lint had 0 errors/17 warnings, and both
  artifacts uploaded. Downloaded APK is 62,284,765 bytes, SHA256
  `A8EC60F269D357D1455301154E21A2539FE434A0FE4593871AF662EDC34FF0DC`;
  v2 signature, package, minSdk35 and targetSdk36 verified. It was not installed.

## Active blockers

- **P-004:** advanced-pipeline claims still need per-pipeline runtime evidence;
  RAW has save-log evidence but not file inspection.
- **P-010:** reviewed nonblocking warnings; backup/transfer policy and action
  runtime/executable-mode maintenance remain follow-ups.
- **P-011/P-012:** fix build is installed; clean close logs and capability
  labels still need the next device session.
- **P-013:** resolved. No hard blocker; the user controls phone availability.
- **P-016:** resolved for the reproduced startup path by correction-build device
  retest; retain evidence and reopen if the paused state recurs.
- **P-017:** current standard Video does not request/verify OIS/EIS; user reports
  no OIS. Stabilization remains an explicit implementation/acceptance gap.
- Maximum-resolution maps/session/files, smooth-zoom feel, advanced color/RAW
  throughput, stereo channels and wired/USB routing are blocked on later device
  access. No phone access is authorized during the current implementation phase.

## Update policy

After material changes update this summary and append concise dated evidence.
Link longer investigations instead of duplicating them. A passing test needs an
actual outcome. A feature's state must advance through implementation/build/
device evidence, not from a roadmap checkbox alone.
