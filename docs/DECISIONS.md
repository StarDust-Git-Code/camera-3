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

- **Status:** explicit user instruction, 2026-11-18; workflow implemented and
  build-verified by run 44235722163 at commit `9174a63`.
- **Repository:** `https://github.com/StarDust-Git-Code/camera-3` (public;
  API reports size zero and `git ls-remote` returned no refs before publishing).
- **Choice:** `.github/workflows/android.yml`, Ubuntu 24.04/JDK 21, pinned action
  commit SHAs, Gradle wrapper validation, model/Android unit tests, lint and debug
  APK compilation; upload APK and reports as artifacts, no release publication.
- **Consequence:** only a successful Actions run proves CI compilation. A debug
  runner-generated signing key can change between runs; stable signing is future
  explicit setup. No device test is implied by CI success.
- **Evidence:** nine model tests passed, lint 0 errors/17 warnings, debug APK and
  verification-report artifacts uploaded; downloaded APK signature and API35
  minimum verified locally. TESTING records exact paths, IDs and hash.
- **Reviewed warnings:** retain the compatible dependency/API36 pins for the
  baseline rather than mixing major upgrades into delivery. Data extraction rules
  and action-runtime maintenance are explicit follow-ups in P-010; no lint
  baseline or blanket warning suppression was added.

## D-015: Capability and extension naming from the compile-time platform

- **Status:** engineering decision, 2026-11-18; local compile passed; device
  re-verification pending (P-011/P-012).
- **Problem:** the first device session rendered `CAPABILITY_5`/`CAPABILITY_19`
  because `CameraDiscovery` mapped only part of the platform's capability
  constants, and extension IDs appeared without names.
- **Approach:** map every named `REQUEST_AVAILABLE_CAPABILITIES` constant of
  compileSdk 36, keep `CAPABILITY_n` fall-through for future/unknown values,
  and show extension IDs as raw API IDs. Constant values are verified with
  `javap -constants` against the local `android-36/android.jar` rather than
  recalled from memory.
- **Device facts locked in:** extension support is BEAUTY(1) and BOKEH(2) on
  all four lenses; NIGHT(4) is not advertised, so the Night tab has no honest
  extension pipeline on this device (M3 must plan an alternative or keep it
  disabled with that reason).
- **Consequence:** naming changes are diagnostic labels only; per-lens key and
  range revalidation still gates any newly exposed control (D-004).

## D-016: RAW (DNG) still-capture pipeline

- **Status:** implemented and CI build-verified, 2026-11-18; correction-build
  logs prove JPEG+DNG save completion on IDs 0/1, but files were not opened.
- **Problem:** M3 requires real RAW_SENSOR output paired with the existing JPEG
  capture, without inventing capability or mislabeling untested work.
- **Approach:**
  - Discovery reads `getOutputSizes(ImageFormat.RAW_SENSOR)` into
    `CameraDescriptor.rawSizes` (defensive query, per-lens detail entry).
  - `supportsRaw` = capability `RAW` advertised **and** real RAW sizes reported;
    the UI RAW chip only appears when this is true.
  - `CameraPolicy.configuration(camera, rawEnabled)` pairs the largest
    aspect-matching (±0.03) RAW size with the chosen JPEG only when RAW is
    advertised; no aspect-compatible RAW size means no configuration rather than
    a fake pairing.
  - The engine adds a second `ImageReader` (`RAW_SENSOR`, maxImages 2) when the
    configuration carries `raw`, targets both JPEG+RAW surfaces in one still
    request, pairs the `TotalCaptureResult` with the RAW `Image` (result and
    frame may arrive in either order), and writes the DNG in the IO scope.
    The capture stays in `CAPTURING` until **both** outputs settle; the
    watchdog re-arms per remaining output (12 s each).
  - `isSessionConfigurationSupported` remains the honest gate for the
    3-stream combination; rejection fails with an explicit message.
  - `PhotoStore.saveDng` streams into MediaStore (`image/x-adobe-dng`,
    `DCIM/Camera3`), pending→publish→cleanup like JPEG.
- **Verified platform facts (javap on android-36/android.jar):** `DngCreator`'s
  constructor is `(CameraCharacteristics, CaptureResult)` and no `setResult`
  exists at API 36 — metadata is bound at construction. `setOrientation` takes
  EXIF 1–8 values (0°→1, 90°→6, 180°→3, 270°→8), not degrees; the JPEG request
  still uses degrees.
- **Limits:** RAW is same-request paired with JPEG (no separate sensor modes);
  no thumbnail is embedded; zoom crop applies to RAW output as the HAL reports.
  A failing JPEG no longer abandons its in-flight DNG.
- **Consequence:** every expected output must settle before the phase returns
  to `PREVIEWING`; partial outcomes are reported per-output in the runtime
  message (e.g. "JPEG saved, DNG failed: …").

## D-017: Video recording pipeline (MediaRecorder, first cut)

- **Status:** implemented and CI build-verified, 2026-11-18; a real 1920×1080
  session configured and the user reports FHD/30 recording works. MP4 inspection,
  repeated-stop cleanup and stabilization remain unverified.
- **Problem:** M4 requires real MP4 recording on the public Camera2 stack with
  honest sizing, orientation and microphone behavior — no fake recorder labels.
- **Approach:**
  - Discovery reads `getOutputSizes(MediaRecorder::class.java)` into
    `CameraDescriptor.videoSizes` (defensive query, per-lens detail entry).
  - `CameraPolicy.videoConfiguration(camera, mic)` picks the largest 16:9
    (±0.03) MediaRecorder size at or under 1080p that has an aspect-matched
    (±0.015) preview stream ≤1080p; without such a pairing the Video mode is
    reported unavailable rather than shown with mismatched framing.
  - Sessions are plan-driven (`StreamPlan.Photo`/`StreamPlan.Video`); the mode
    switch rekeys the preview surface, so the camera is closed and reopened
    with the matching stream set (preview + recorder surface, TEMPLATE_RECORD
    repeating request). `PhotoConfiguration` sessions keep TEMPLATE_PREVIEW.
  - The `MediaRecorder` is prepared **before** `openCamera` (platform
    contract: the recorder surface must exist before session configuration):
    MPEG-4
    container, H.264 video + AAC audio (44.1 kHz/96 kbps) only when
    `RECORD_AUDIO` is granted, 30 fps, ~5 Mbps per megapixel (coerced
    4–24 Mbps), size = policy's video size, `setOrientationHint` with the
    camera/display degrees computed at attach time, output to a pending
    MediaStore video entry opened by `PhotoStore.openVideoOutput()`.
  - Record start re-targets the repeating request with the recorder surface
    added (preview keeps flowing); zoom/torch remain usable while
    `RECORDING` via `CONTROL_PHASES` and re-include the recorder surface.
  - Stop (user tap, backgrounding, or `close()`/`dispose()`) goes through
    `finalizeRecording`: the recorder is stopped and released exactly once,
    the pending entry is **published** if frames were recorded (stop() threw →
    discard as "too short") and **discarded** otherwise; the shared path keeps
    the stop-on-close contract identical to the user stop.
  - UI: Photo/Video tabs drive the plan; the shutter becomes
    record/stop in Video mode with a REC timer; the top row shows
    "MP4 · <height>p" and "· silent" when the mic is not granted; Settings
    carries a microphone Grant row (silent by default).
- **Verified platform facts:** `MediaRecorder(Context)` constructor and
  `setNextPdfFile`-free fd API (`setOutputFile(FileDescriptor)`) exist at API
  36; `setOrientationHint` takes clockwise degrees and must be called before
  `prepare()` (documented contract).
- **Limits (first cut):** no pause/resume, no fps/bit-rate settings surface,
  no high-speed (120 fps+) capture, no per-clip fallback if the HAL rejects
  the 2-stream combination (the session probe path reports it), no autofocus
  trigger on record start, mic grant is only checked at attach time.
- **Consequence:** every `MediaRecorder`/fd/uri lifecycle is owned by the
  engine run and finalized exactly once; a recording cut short by leaving the
  app is still published best-effort.

## D-018: Preview-owned rotation and complete session configuration identity

- **Status:** implemented, CI-green and device-retested, 2026-11-18. Run
  44259956383 passed; the verified APK restored attachment/open/configuration
  through mode/lens/lifecycle transitions. P-016 is resolved for that reproduction.
- **Problem:** Video introduced `Application.display` before `setSource` for
  both modes. A non-display context throws; SurfaceView can catch the callback
  exception, leaving no desired camera source and no fatal app crash. Its
  configuration key also omitted RAW/audio changes, and Photo availability
  depended on already being in Photo mode.
- **Evidence inspected:** local SDK `sources/android-36.1/android/app/ContextImpl.java`
  lines 3284–3306 and `android/view/SurfaceView.java` lines 1395–1438; actual
  source diff and saved P-016 device artifacts. Platform source behavior is not
  proof that this exception was captured on the modified test OS.
- **Choice:** SurfaceView supplies its own display's `Surface.ROTATION_*`
  value to `attachSurface`; the ViewModel converts to degrees without retaining
  a UI context. A genuinely absent view display falls back to rotation 0 with
  that absence explicitly logged. Recording orientation still freezes at attach.
- **Configuration:** the preview subtree is keyed by camera ID, mode and the
  complete active Photo/Video configuration; changing RAW/audio replaces the
  session even when preview dimensions do not change. Runtime telemetry, grid
  and JPEG quality do not change this identity. Inactive configuration getters
  remain null; independent `photoUsable`/`videoUsable` drive mode availability.
  Lens/mode/RAW changes are disabled in the UI whenever the model is busy
  (capturing or recording), rather than show enabled actions the model rejects.
- **Alternatives:** do not conceal an invalid Application context with a blanket
  exception-to-zero fallback or retain Activity in the ViewModel. Live-surface
  rebinding could avoid subtree replacement, but adds ownership/order complexity;
  retained-source replay after a rejected attachment remains a separate follow-up
  only if runtime evidence requires it.
- **Verification:** bounded P-016 callback/attachment/reconcile logs, pure-state
  regression tests, local/CI compilation, then fresh preview, RAW toggles and
  Photo↔Video round trips on the actual phone. State tests cannot prove callback
  ordering, context safety at runtime or working saved outputs.
- **Review caveats:** rapid queued RAW preference updates and late surface
  callbacks still need stress tests; the complete-key fix is not a new proof
  of their ordering. Handler serialization and identity-checked detach remain.

## D-019: Public maximum-resolution stills and perceptual video zoom

- **Status:** implemented and locally build-verified, 2026-11-18; no device test
  authorized for this tranche.
- **Problem:** the user requested genuine 50 MP JPEG/RAW where available and
  smoother video zoom. The installed build only queried the default stream map,
  where ID 0 produced 4080×3060 JPEG/RAW; those outputs cannot be relabeled 50 MP.
- **Maximum-resolution choice:** query
  `SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION` separately and require the
  writable `android.sensor.pixelMode` key plus a real maximum-resolution JPEG.
  Do not require `ULTRA_HIGH_RESOLUTION_SENSOR`: Android documents that the pixel
  mode and map may also exist without that capability. RAW+JPEG additionally
  requires an aspect-compatible RAW_SENSOR from the maximum-resolution map.
- **Session/request choice:** carry `SensorPixelMode` in `PhotoConfiguration`,
  declare maximum-resolution use on the JPEG/RAW `OutputConfiguration`s, keep the
  default-map preview out of the still request, and set
  `SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION` only on the still request. The full
  session support query/configuration callback remains the runtime combination gate.
- **UI/persistence:** persist `high_resolution` off by default, show HI-RES only
  when policy permits it, display the actual megapixel count and `MAX`, and make
  sensor pixel mode part of the preview/session identity. No fixed 50 MP label.
- **Zoom choice:** in Video, interpolate `CONTROL_ZOOM_RATIO` with smoothstep
  timing in logarithmic ratio space at approximately 30 Hz; a new target cancels
  the old ramp. Photo retains direct zoom response. Intermediate steps do not
  produce INFO-log spam.
- **Verification:** final local full gate passed: 14 model + 6 app tests, no
  failures/errors/skips; lint 0 errors/16 warnings; debug APK assembled and v2
  signature/package checked. Actual maximum maps/session acceptance/file
  dimensions and perceived zoom smoothness remain device-unverified.

## D-020: Advanced GPU/codec and routed-audio pipeline

- **Status:** planned architecture, not implemented or device-verified.
- **Problem:** LUT, software HDR, genuine Log, RAW-frame storage, stereo capture
  and live Type-C IEM monitoring cannot be represented honestly by the current
  MediaRecorder-only path.
- **Choice:** build an app-owned media pipeline: Camera2 surface → GPU processing
  for preview/encoder → MediaCodec video → MediaMuxer; AudioRecord PCM feeds the
  AAC encoder and, when explicitly enabled, an AudioTrack monitor. A bounded
  RAW_SENSOR ImageReader/storage worker is separate from encoded video and uses
  explicit queue/drop/stop policy.
- **Color contract:** each Log/HDR mode must define transfer function, gamut,
  bit depth, tone mapping and whether a LUT affects preview, recording or both.
  Encoded profile/color metadata and playback must substantiate the label.
- **Audio contract:** request stereo input only when supported; verify recorded
  channel count/content. Monitoring is available only with an actual selected
  wired/USB sink and must confirm `getRoutedDevice()` while playing. A preferred
  route is not proof of the effective route; never fall back silently to speaker.
- **RAW contract:** report actual format, dimensions, bytes/frame, sustained
  cadence, dropped frames and storage use. At roughly 25 MB for one 12 MP
  16-bit RAW frame on the current device report, continuous unbounded storage is
  infeasible; duration/FPS must be bounded by measured throughput.
- **Consequence:** this is a second tranche with new ownership, synchronization,
  backpressure and acceptance tests. It does not promote any UI toggle today.

### New-decision template

Add the next ID with status, date, problem, chosen approach, inspected sources/
evidence, alternatives, consequences and affected files/tests. If superseded,
retain the earlier entry and link the new decision. Never promote a proposal to
"accepted by user" merely because it was mentioned in an assistant plan.
