# Progress and evidence

Last updated: 2026-11-18. Initial Photo implementation exists. User requested
GitHub Actions APK compilation; first remote build is pending.

## Current summary

| Area | Status | Evidence / limitation |
| --- | --- | --- |
| Workspace inspection | Host-observed complete | Empty directory before documentation |
| Host Java/SDK/ADB | Host-observed available | Read-only tool/path/component inspection |
| Device description | User-reported | A35, One UI 8.5, Magisk, unlocked BL/camera library |
| Physical device connection | Not available in recorded checks | `adb devices -l` returned no device rows |
| Requirements/design | Documented | REQUIREMENTS, DECISIONS and subsystem docs |
| Project-local Markdown | Created | Root AGENTS/README and 18 documents under docs |
| Documentation consistency | Host-verified / reviewed | 20 Markdown files, 61 valid local links, no validation issues, 69-line handoff and constraint review |
| Android scaffold | Implemented, uncompiled | Six modules, API35 minimum/API36 target, verified wrapper JAR |
| Photo/settings/logger | Implemented, unverified | Real Camera2/JPEG/MediaStore, grid/quality DataStore, diagnostics/logs/share source |
| Recording/RAW/extensions/manual | Not implemented | Disabled or diagnostics-only, no fake pipeline |
| GitHub Actions | Workflow implemented | Target repository inspected empty; first push/run pending |
| App builds/tests/device validation | Not run | Eight policy unit tests exist but have not executed; no physical evidence |
| APK/delivery | Not available | No build artifact |

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

1. Commit/push source and `.github/workflows/android.yml` to the specified empty
   repository without force; run CI and repair compiler/lint failures.
2. Record the actual Actions run/artifact/test result, not expected success.
3. Connect/authorize the phone for baseline OS/capability/media/lifecycle tests.
4. Continue the roadmap only after assessing baseline failures; advanced modes
   are not implemented in this first build.

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

## Active blockers

- **P-001:** no connected device; blocks physical verification.
- **P-004:** camera claims unverified; resolve through app queries and outputs.
- Development identity/dependencies are pinned (D-013); final branding and release
  signing remain open. No successful build/device result has been claimed.

## Update policy

After material changes update this summary and append concise dated evidence.
Link longer investigations instead of duplicating them. A passing test needs an
actual outcome. A feature's state must advance through implementation/build/
device evidence, not from a roadmap checkbox alone.
