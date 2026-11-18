# Decision record

Last updated: 2026-11-18. Accepted user constraints and proposed engineering
choices are distinguished explicitly. Initial scaffold/Photo code now exists;
the older proposals below are retained as design history.

## D-001: Native low-level Android stack

- **Status:** accepted user constraint.
- **Choice:** Kotlin, Jetpack Compose, Camera2, modular MVVM and Coroutines.
- **Reason:** user's explicit engineering request and actual low-level control.
- **Alternative:** CameraX is not the requested capture engine; no substitution.
- **Consequence:** own session/request/resource correctness and test it carefully.

## D-002: Minimum Android version

- **Status:** accepted user constraint; older proposal superseded.
- **Current choice:** Android 15+ only, `minSdk = 35`.
- **History:** Android 10/API 29 was initially proposed, then the user explicitly
  narrowed the target to Android 15 and later. Do not restore the older baseline.
- **Consequence:** no need for pre-35 compatibility code, but later OS and actual
  hardware/session restrictions still require checks.

## D-003: LEVEL_3-only device scope

- **Status:** user constraint accepted; per-lens interpretation proposed.
- **Choice:** require at least one accessible Camera2 camera reporting LEVEL_3
  before enabling capture. Prefer a qualifying rear camera.
- **Current per-lens proposal:** eligible phones may use other exposed lenses
  with independent capability gating even if those lenses report lower levels.
- **Alternative:** only permit LEVEL_3 lenses; use if the user requests that
  stricter interpretation. Do not silently reinterpret device eligibility.
- **Consequence:** diagnostics/permission/help may operate on an unsupported
  device, but a FULL/LIMITED-only camera app fallback is not the product.

## D-004: Trust evidence, not unlocked-camera flags alone

- **Status:** planned engineering policy consistent with the user's no-fake rule.
- **Choice:** separate reported capabilities, implemented pipelines, session
  success, effective capture results and media-file verification.
- **Reason:** Magisk/modified camera software can expose incomplete/incompatible
  capabilities; even ordinary hardware flags do not prove combined streams work.
- **Rejected:** hardcoding LEVEL_3, RAW, 10-bit, high FPS or lens availability.

## D-005: No app root dependency or system modification

- **Status:** planned scope boundary.
- **Choice:** normal app permissions and public Camera2/media APIs.
- **Reason:** root is the user's environment, not authorization to alter it.
- **Consequence:** no Magisk/camera-library/firmware changes, hidden API bypass or
  privileged camera assumptions without a separate explicit request.

## D-006: Portrait/Night availability

- **Status:** proposed initial implementation.
- **Choice:** genuine public Camera2 Bokeh/Night extension sessions when exposed.
- **Alternative:** real software depth/segmentation or multi-frame processing,
  deferred until algorithm/quality/performance work is explicitly planned.
- **Rejected:** renaming regular Photo as Portrait/Night to keep tabs usable.
- **Consequence:** requested modes can be unavailable on an otherwise LEVEL_3
  phone, with a clear explanation. LEVEL_3 does not imply extensions.

## D-007: Recording pipeline

- **Status:** proposed, not built/version-pinned.
- **Choice:** Camera2 output surfaces, MediaCodec, AudioRecord/AAC when enabled,
  and MediaMuxer.
- **Reason:** explicit recording profile, bitrate, timestamps and HDR/color
  control with inspectable output.
- **Alternative:** MediaRecorder can simplify a standard baseline, but mixing
  pipelines adds complexity. Revisit with evidence if needed.
- **Consequence:** correctly implement encoder drain, synchronization,
  finalization and cancellation; never advertise features only because codecs
  exist.

## D-008: Preview/UI geometry

- **Status:** proposed.
- **Choice:** SurfaceView/Compose interop and dp/sp/window-inset-aware layout.
- **Reason:** native direct camera surface and responsive approximately 20:9
  portrait phone UX.
- **Alternatives:** TextureView/GL pipeline if transforms/HDR/monitoring require
  it; document measured tradeoffs before switching.
- **Consequence:** 1080 x 2400 is a reference, not layout coordinates. Saved media
  aspect is independent of display aspect.

## D-009: Modular boundaries and persistence

- **Status:** proposed.
- **Choice:** app plus core model/camera/media/settings/diagnostics modules,
  StateFlow/ViewModels, explicit dependency wiring and DataStore.
- **Reason:** separate hardware/resource ownership from UI while avoiding an
  unnecessarily large feature-module framework.
- **Consequence:** actual layout/version choices must be recorded when created;
  no circular core dependencies or editable settings without real effects.

## D-010: Project-local persistent Markdown

- **Status:** accepted user requirement; documentation files created.
- **Choice:** root `AGENTS.md`, root entry README and focused `docs/*.md` with a
  compact handoff, progress/decision/problem records and explicit maintenance.
- **History:** user rejected an external Plan directory and switched agents so
  project-local files could be written within harness permissions.
- **Reason:** preserve meaningful facts across context limits and new sessions.
- **Rejected:** filler files, duplicate entire plans, false completion evidence,
  or claiming that documentation enlarges the model context window.
- **Consequence:** load minimum recovery docs first, task-specific detail later;
  update after material changes and before pausing/compaction.

## D-011: Dependency version selection

- **Status:** proposed; exact versions not selected.
- **Choice:** pin a compatible wrapper/AGP/Kotlin/Compose set from official
  compatibility documentation; tentatively compile/target installed API 36.
- **Reason:** reproducibility; a cached distribution alone is not compatibility.
- **Consequence:** record versions and build evidence in TECH-STACK before
  treating the scaffold as verified.

## D-012: Feature completeness and delivery claims

- **Status:** accepted scope interpretation.
- **Choice:** working pipelines with physical-device evidence define completion;
  UI, declared support, source and successful builds are intermediate states.
- **Reason:** user explicitly requested a fully functional app, actual device
  testing and no UI-only stopping point.
- **Consequence:** report unavailable, implemented-unverified and failing items
  honestly. Documentation-only completion does not complete the app request.

## D-013: Initial development identity and baseline implementation

- **Status:** implemented engineering choice, 2026-11-18; not device-verified.
- **Choice:** Camera3 / `dev.camera3.app` / `0.1.0-dev`; six planned modules;
  AGP 8.13.2, Gradle 8.13, Kotlin/Compose compiler 2.3.21, Compose BOM 2025.11.01,
  compile/target 36 and minSdk 35. This pins D-008/D-009/D-011 for the baseline.
- **Reason:** a reproducible API-36-compatible set rather than adopting the
  current API-37/AGP-9-only Compose line during first implementation.
- **Correction before build:** Kotlin 2.2.21 was initially pinned, then replaced
  by 2.3.21 when the official KGP matrix showed the former's fully supported AGP
  maximum is 8.11.1. 2.3.21 covers the chosen AGP/Gradle versions.
- **Baseline:** SurfaceView fits the stream aspect and uses native camera/display
  rotation; automatic Photo JPEG, optional torch/zoom, MediaStore, DataStore,
  real diagnostics/logging. No simulated scenery or advanced-mode fallbacks.
- **Limits:** no AF/AE lock sequence, tap metering, manual controls, RAW/DNG,
  extension sessions or recording yet; orientation/lifecycle need device tests.
- **Branding/signing:** development identity is not final branding or release
  signing authorization.

## D-014: GitHub Actions APK compilation

- **Status:** explicit user instruction, 2026-11-18; workflow implemented,
  remote execution not yet verified.
- **Repository:** `https://github.com/StarDust-Git-Code/camera-3` (public;
  API reports size zero and `git ls-remote` returned no refs before publishing).
- **Choice:** `.github/workflows/android.yml`, Ubuntu 24.04/JDK 21, pinned action
  commit SHAs, Gradle wrapper validation, model/Android unit tests, lint and debug
  APK compilation; upload APK and reports as artifacts, no release publication.
- **Consequence:** only a successful Actions run proves CI compilation. A debug
  runner-generated signing key can change between runs; stable signing is future
  explicit setup. No device test is implied by CI success.

### New-decision template

Add the next ID with status, date, problem, chosen approach, inspected sources/
evidence, alternatives, consequences and affected files/tests. If superseded,
retain the earlier entry and link the new decision. Never promote a proposal to
"accepted by user" merely because it was mentioned in an assistant plan.
