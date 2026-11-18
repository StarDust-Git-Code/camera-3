# Problems, investigations and solutions

Last updated: 2026-11-18. This is an evidence ledger, not a list of imagined app
bugs. Initial implementation exists; compile and physical-runtime checks are pending.

## P-001: No connected physical device

- **Status:** open; blocks physical-device verification, not documentation or
  all host-side implementation.
- **Observed:** recorded `adb devices -l` commands returned no device rows.
- **Impact:** cannot verify Android API, modified-camera behavior, LEVEL_3,
  capabilities, images, recordings or runtime stability on the A35.
- **Actions taken:** checked ADB discovery; documented the required connection.
  No driver/cable/root changes were made.
- **Likely next investigation:** connect USB, enable debugging, authorize this
  computer; recheck. If still absent, inspect cable/USB mode/driver before more
  intrusive actions.
- **Resolution evidence required:** an authorized device entry, correct model/API
  queries and actual application camera tests. Do not close based on a model name.

## P-002: There is no existing Android project

- **Status:** scaffold implemented; successful-build closure pending.
- **Observed:** workspace read/forced directory listing showed zero entries
  before documentation creation.
- **Impact:** nothing can be built, installed or modified as an existing app.
- **Approach chosen:** document scope, then create a reproducible modular Android
  project incrementally, beginning with working Photo.
- **Completed mitigation:** wrapper, six Gradle modules, baseline Photo/settings/
  diagnostics source and model policy tests now exist. CI compilation is pending.
- **Resolution evidence required:** actual build files/source and a successful
  relevant build command.

## P-003: Plan mode initially prevented project-local files

- **Status:** resolved for the current session.
- **Observed:** active harness Plan restrictions allowed plan writes only in an
  external path; user then requested the project folder instead.
- **Action:** did not write into the prohibited project path while restricted;
  requested a mode change.
- **Resolution:** harness explicitly removed Plan restrictions; user confirmed.
  Markdown creation proceeded in `camera3`.
- **Verification:** actual files are in the project, not an external plan folder.
- **Caution:** future Plan mode restrictions still take precedence over the normal
  documentation update routine.

## P-004: Camera claims from the modified stack are unverified

- **Status:** open verification concern, not an observed runtime bug.
- **User report:** LEVEL_3 was unlocked using a modified camera library on a
  Magisk/unlocked-bootloader A35.
- **Risk:** an advertised level/flag may not imply a functioning public API
  pipeline or compatible combined output configuration.
- **Approach:** separate reported support, implementation, session validation,
  capture-result feedback and saved-media evidence.
- **Resolution evidence required:** per-camera characteristics plus relevant
  functioning sessions/results/files. Never solve by hardcoding the report.

## P-005: Community connector unavailable

- **Status:** research limitation recorded; not an app blocker.
- **Observed:** tool discovery found no DevRelay namespace available.
- **Approach:** use inspected official Android documentation; do not invent
  community posts, discussion links or consensus.
- **Follow-up:** research community experience later if the integration becomes
  available, without replacing authoritative Camera2 requirements.

## P-006: Chat reference image has no local asset

- **Status:** open asset-provenance detail; conceptual UI design can proceed.
- **Observed:** the user supplied a chat image, while the workspace was empty.
- **Approach:** document the visible design and provenance in [UI-UX.md](UI-UX.md).
- **Caution:** no local image path or screenshot-comparison artifact exists.
- **Resolution:** if exact visual comparison is needed later, use a real saved
  reference supplied/accessible in the project and record its path.

## P-007: PowerShell checksum response type

- **Status:** resolved, host-verified 2026-11-18.
- **Observed:** wrapper-download script tried `.Content.Trim()`; the checksum
  response was a byte array, producing `System.Byte does not contain Trim`.
- **Fix:** decode byte-array content as UTF-8 before trimming/comparing.
- **Evidence:** downloaded Gradle 8.13 wrapper JAR matches official SHA256
  `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`.
  Distribution checksum was fetched and pinned. Not an app compile result.

## P-008: Initial compiler compatibility selection

- **Status:** corrected before build, 2026-11-18; compilation pending.
- **Finding:** Kotlin 2.2.21's official fully supported AGP range stops at 8.11.1;
  initial scaffold uses AGP 8.13.2. No runtime/build failure was fabricated.
- **Fix:** use Kotlin/Compose compiler 2.3.21, whose documented range covers
  AGP 8.13.2 / Gradle 8.13. See D-013 and SOURCES.

### Investigation template

## P-009: First CI run has no sdkmanager on PATH

- **Observed failure:** Actions run
  `https://github.com/StarDust-Git-Code/camera-3/actions/runs/44235279035`
  at commit `9e4686c`; SDK install step returned exit 127, `sdkmanager: command
  not found`. No Gradle compile or app tests ran in that attempt.
- **Fix implemented:** explicitly run pinned `android-actions/setup-android` v3
  before package installation; request `platform-tools` rather than the obsolete
  default `tools` package. Do not assume runner preinstalled tools are on PATH.
- **Independent source review:** parenthesized rotation formula's inline `if`
  to avoid Kotlin expression precedence; torch now requires both hardware and
  writable flash key; added a ninth policy regression test and line-ending rules.
- **Status:** corrected workflow/source awaiting rerun; not yet a verified fix.

### Future issue template

For each real issue add an ID, status, first observed date, affected version/
device/configuration, reproduction, expected/actual behavior, evidence, working
hypotheses, attempted fixes and their outcomes, selected fix, regression tests and
remaining risks. Keep failed attempts; do not rewrite history as an immediate fix.

Hypothetical risks belong in architecture/capability/testing docs until observed.
When an investigation grows, move detail to a focused linked Markdown file and
retain this ledger's concise summary.
