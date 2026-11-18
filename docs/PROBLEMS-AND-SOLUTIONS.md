# Problems, investigations and solutions

Last updated: 2026-11-18. This is an evidence ledger, not a list of imagined app
bugs. The earlier baseline Photo session passed; the installed RAW+Video APK
fails before camera open (P-016). Source corrections are locally build-verified;
functional retest of the correction is still pending.

## P-001: No connected physical device

- **Status:** resolved, device-verified 2026-11-18.
- **Observed:** recorded `adb devices -l` commands returned no device rows until
  the user connected and authorized the phone.
- **Impact:** had blocked all physical-device verification.
- **Resolution evidence:** authorized device listed; `getprop` read Android 16 /
  API 36 with spoofed Samsung props (a35x board, SM-S711B model string, mixed
  fingerprint) matching the user-reported Magisk/modified-library environment;
  verified APK installed (`install -r` Success) and the baseline device session
  recorded in TESTING/CAMERA-CAPABILITIES. Device serial is deliberately kept
  out of documentation.

## P-002: There is no existing Android project

- **Status:** resolved, build-verified by Actions run 44235722163 at `9174a63`.
- **Observed:** workspace read/forced directory listing showed zero entries
  before documentation creation.
- **Impact:** nothing can be built, installed or modified as an existing app.
- **Approach chosen:** document scope, then create a reproducible modular Android
  project incrementally, beginning with working Photo.
- **Completed mitigation:** wrapper, six Gradle modules, baseline Photo/settings/
  diagnostics source and model policy tests now exist. CI compilation passed.
- **Resolution evidence:** full debug APK build, nine passing model tests,
  uploaded/downloaded artifacts and verified package/signature (TESTING).

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
- **Observed so far (2026-11-18):** all four cameras report LEVEL_3 with RAW/
  manual/burst/reprocessing flags, and the baseline JPEG path produces valid
  12.48 MP files with genuine per-shot result metadata on this modified stack,
  so the reported flags are functional for the Photo path. Advanced pipelines
  (RAW/manual/extensions/recording) remain unverified until implemented and run.
- **Resolution evidence required:** per-camera characteristics plus relevant
  functioning sessions/results/files for each advanced pipeline. Never solve by
  hardcoding the report.

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

- **Status:** corrected before build, 2026-11-18; full compilation subsequently
  passed in run 44235722163 at `9174a63`.
- **Finding:** Kotlin 2.2.21's official fully supported AGP range stops at 8.11.1;
  initial scaffold uses AGP 8.13.2. No runtime/build failure was fabricated.
- **Fix:** use Kotlin/Compose compiler 2.3.21, whose documented range covers
  AGP 8.13.2 / Gradle 8.13. See D-013 and SOURCES.

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
- **Status:** resolved by run 44235722163 at `9174a63`: SDK steps, model tests,
  lint, APK compilation and both artifact uploads all passed.

## P-010: Successful-build warnings and maintenance follow-ups

- **Observed:** run 44235722163 succeeded, but lint reports 0 errors/17 warnings;
  compiler reports four warnings, and Actions reports three maintenance annotations.
- **Lint assessment:** 15 notices concern deliberately pinned dependency/tool
  versions or API36 targeting (D-013/014). Keep the tested compatible baseline;
  plan/test upgrades separately. One `UseKtx` suggestion is stylistic. One
  `DataExtractionRules` warning needs explicit Android12+ cloud/device-transfer
  exclusions for private settings/logs; existing `allowBackup=false` is not
  equivalent across all device-transfer implementations. This is a policy
  follow-up, not a demonstrated data leak or runtime failure.
- **Compiler assessment:** three unnecessary safe calls on display and the
  deprecated non-AutoMirrored ReceiptLong icon are nonblocking cleanup items.
- **Actions assessment:** Node20 actions are forced onto Node24; setup-java v4
  is deprecated; gradlew has Git mode 100644 and setup-gradle repairs execution
  permission automatically. Migrate to verified supported action SHAs and set
  executable mode in a separate tested maintenance change.
- **Status:** open, nonblocking for debug compilation; warnings not suppressed.
  Full reports and log are downloaded under `app/build/ci/44235722163/`.

## P-011: Duplicate close exception logs during camera shutdown

- **First observed:** 2026-11-18 device session, `Camera3/Close` logcat lines.
- **Affected:** `dev.camera3.app` 0.1.0-dev at `9174a63` on the connected phone;
  harmless to function, noisy in logs.
- **Reproduction:** close/reopen or background the app; every `reconcile()` that
  runs while a run is closing re-calls `close(run)`, and each call retries
  `stopRepeating()` on the already-closed session, logging a caught
  `IllegalStateException` with stack trace per repetition.
- **Expected:** at most one teardown attempt per session instance.
- **Fix:** `CameraEngine.close()` now nulls `run.session` while tearing it down
  (handler-confined, after `closing = true`; both `updateRepeating()` and
  `capture()` guard on `run.closing`, so no null dereference). Device closing
  remains re-entrant so an in-flight `onOpened` still closes a still-opening
  device.
- **Status:** fix compile-verified locally (`:core:camera:compileDebugKotlin`
  BUILD SUCCESSFUL); device re-verification of clean logs pending the next
  install.

## P-012: Unmapped capability constants in diagnostics

- **First observed:** 2026-11-18 device session; diagnostics and discovery logs
  showed `CAPABILITY_5` and `CAPABILITY_19` on all four cameras.
- **Affected:** `dev.camera3.app` 0.1.0-dev at `9174a63`.
- **Root cause:** `CameraDiscovery.capabilityName()` mapped only a subset of
  `REQUEST_AVAILABLE_CAPABILITIES` constants.
- **Verification method:** read constant values with `javap -constants` against
  the compile-time `android-36/android.jar` (not from memory): 5 =
  READ_SENSOR_SETTINGS, 19 = STREAM_USE_CASE, and the complete API-36 set
  (16 ULTRA_HIGH_RESOLUTION_SENSOR, 17 REMOSAIC_REPROCESSING, 18
  DYNAMIC_RANGE_TEN_BIT, 20 COLOR_SPACE_PROFILES) was mapped; unknown future
  values still fall through to `CAPABILITY_n`.
- **Impact on findings:** the device capability list is now fully named; no
  behavior change beyond labeling.
- **Status:** fix compile-verified locally; device re-verification pending the
  next install.

## P-013: Corrupt Gradle cache blocked the local fix compile

- **First observed:** 2026-11-18 during the local compile of the P-011/P-012
  fixes on this host.
- **Observed:** first `:core:camera:compileDebugKotlin` run failed after 4m14s:
  `Could not download bundletool-1.18.1.jar ... Tag mismatch` (checksum
  verification of the cached artifact).
- **Fix:** deleted
  `%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.android.tools.build\bundletool`
  and reran; re-download succeeded and the task reported BUILD SUCCESSFUL in
  1m17s (27 tasks). The harness surfaced a contradictory `exit code 1` on the
  successful run; treated as a reporting artifact, noted here honestly, and CI
  remains the authoritative build gate.
- **Status:** resolved for this host/session; no project file changed.

## P-014: DngCreator API mismatch in the first RAW implementation

- **First observed:** 2026-11-18 during the local compile of the RAW pipeline.
- **Affected:** this session's draft `CameraEngine.tryProcessRaw()` only (never
  committed).
- **Observed:** `:core:camera:compileDebugKotlin` failed with
  `No value passed for parameter 'p0'/'p1'` at the `DngCreator()` construction
  and `Unresolved reference 'setResult'` (CameraEngine.kt:371/373).
- **Verification:** `javap -classpath <sdk>/platforms/android-36/android.jar
  android.hardware.camera2.DngCreator` — the API-36 class has one constructor,
  `DngCreator(CameraCharacteristics, CaptureResult)`, and no `setResult`
  method; metadata is bound at construction. The earlier working note that
  assumed a no-arg constructor plus `setResult` was wrong.
- **Fix:** construct `DngCreator(manager.getCameraCharacteristics(cameraId),
  result)` on the still capture's `TotalCaptureResult`; `setOrientation` and
  `writeImage` unchanged. Recorded in D-016 so the design stays javap-verified.
- **Status:** fixed; full touched-module compile passed.

## P-015: "Missing return statement" in a plain function with a try/catch tail

- **First observed:** 2026-11-18 during the same local compile, module
  `:core:media`, `PhotoStore.insert()` written as a plain function whose body
  ended in a bare `try { … uri } catch { … throw }` tail.
- **Observed:** Kotlin compiler error `Missing return statement` at the
  function's closing brace (PhotoStore.kt:51:5), although the pre-refactor
  code compiled with the same shape as the tail expression of a
  `withContext { }` lambda.
- **Fix:** added an explicit `return` before the try/catch; the module then
  compiled. The root cause in the K2 frontend was not investigated further;
  the fix is behavior-neutral.
- **Status:** resolved; recorded because the failing shape looked idiomatic.

## P-016: Camera remains paused after RAW+Video reinstall

- **Status:** source correction local build-verified; CI/runtime retest pending.
  P-016 is **NOT device-resolved**; current acceptance remains blocked.
- **First observed / affected:** 2026-11-18, installed RAW+Video APK from run
  44251120868 / `409c5d9`, `dev.camera3.app` `0.1.0-dev`, on the connected
  Android 16 / API 36 phone with the user-reported modified camera stack.
- **Reproduction / actual:** launch/relaunch and return to the camera screen;
  MainActivity is resumed, but the SurfaceView is black and status says
  "Camera paused" instead of opening the selected camera and showing real preview.
- **Discovery / eligibility:** three complete passes, each reporting four
  LEVEL_3 IDs and `eligible=true, baselineCamera=0`. This proves those gate
  observations only, not a working session or RAW/Video output.
- **Open evidence:** zero `Opening ID` entries in the saved app log. The captured
  `media.camera` dump has `Active Camera Clients: []` and no Camera3 CONNECT
  since reinstall; older CONNECT entries confirm the earlier baseline app did
  connect. CAMERA and RECORD_AUDIO are both granted; no Camera3 crash, ANR or
  Camera2 error was found in the captured buffers.
- **Evidence:** `app/build/ci/device-testing/debug-report-01/` — `camera-0.log`,
  `screen.png`, `ui-dump.xml`, `top-activity*.txt`, `permissions.txt`,
  `media-camera.txt`, `events-camera3.txt`, `logcat*.txt`, `crash-buffer.txt`.
- **Background only:** Samsung Freecess freezes the process later, after screen
  off; that chronology is not proof of the preview failure's root cause. The
  Nov-17 vendor-provider SIGSEGV in `camera.s5e8835.so` is historical context,
  not an error from this attempt.
- **Investigation:** initial wiring review found no defect; fresh parallel review
  found the Video change's `Application.display` lookup immediately before
  `setSource` in both modes. Local SDK `ContextImpl.getDisplay()` throws for
  non-display contexts; SurfaceView catches surface callback exceptions. This
  explains a possible no-source/no-fatal-crash path, but the saved logcat window
  starts after startup, so the original exception was not captured.
- **Selected source correction (D-018):** pass rotation from the real SurfaceView
  display into attachment, rather than query Application. Added bounded P-016
  callback/attachment/foreground/reconcile logs with explicit gate reasons.
  Also restored complete active-configuration preview keys for RAW/microphone
  changes and independent Photo availability while Video is selected.
- **Remaining uncertainty:** attachment rejection/replay and device surface
  validity/foreground ordering need runtime logs if preview still fails. No
  source review or build alone establishes P-016's actual runtime resolution.
- **Local verification:** full model/Android unit-test, lint and debug-APK command
  succeeded in 4m12s. Reports: 12 model tests (task up-to-date) and 5 new app
  state tests, 0 failures/errors/skips; lint 0 errors / 16 warnings. Other
  Android-module tests remain NO-SOURCE; no physical retest performed.
  Final-source full command passed again in 49s after busy-state UI alignment.
- **Verification required:** fresh foreground startup with gate/open/session
  evidence, then D-02 preview and D-04/D-10/D-11 RAW/media/audio checks after an
  evidence-backed fix. No passing retest exists yet.

### Future issue template

For each real issue add an ID, status, first observed date, affected version/
device/configuration, reproduction, expected/actual behavior, evidence, working
hypotheses, attempted fixes and their outcomes, selected fix, regression tests and
remaining risks. Keep failed attempts; do not rewrite history as an immediate fix.

Hypothetical risks belong in architecture/capability/testing docs until observed.
When an investigation grows, move detail to a focused linked Markdown file and
retain this ledger's concise summary.
