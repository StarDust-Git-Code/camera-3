# Incremental roadmap

Last updated: 2026-11-18. M1/M2 baseline code is build-verified; device checks are pending. Checkboxes
represent exit criteria, not optimistic completion.

## M0: Inspect and establish continuity

- [x] Inspect workspace and host Android tooling.
- [x] Record Android 15+/LEVEL_3-only scope and user-reported test device.
- [x] Create project-local Markdown and root agent instructions.
- [x] Validate the documentation links and update the checkpoint with the result.

M0 verification: 20 Markdown files, 61 local links checked, no broken links or
top-level-heading issues; recovery/constraint review completed. Evidence is in
[TESTING.md](TESTING.md) and [PROGRESS.md](PROGRESS.md).

No app implementation is implied by M0.

## M1: Reproducible scaffold and eligibility diagnostics

- [x] Pin compatible AGP, Gradle, Kotlin and Compose versions (D-013; CI verified).
- [x] Create wrapper/build files and modules with `minSdk = 35`.
- [x] Add camera permissions and minimal native navigation/theme.
- [x] Implement discovery and the LEVEL_3 device gate (not yet device-verified).
- [x] Implement initial capability diagnostics and lifecycle logs (runtime pending).
- [x] Build debug APK and run JVM checks (run 44235722163; nine model tests passed).
- [ ] Install/test on the Samsung phone when connected.

Exit: a buildable app, reliable permission/eligibility states and an actual device
capability report. Scaffolding/unit work can proceed without the phone; the device
part cannot be marked done without it.

## M2: Working Photo baseline

- [ ] Implement lifecycle-safe device/session/surface ownership.
- [ ] Correct preview orientation, mirroring/aspect and metering transforms.
- [ ] Implement JPEG capture, metadata/orientation, saving and gallery access.
- [ ] Add real focus, exposure and flash controls as available.
- [ ] Exercise capture, background/resume, contention and error recovery.

Exit: correct preview and saved photo on the phone, not a simulated viewfinder.
Preserve failure evidence before adding advanced stream combinations.

M2 source now includes close-before-reopen Camera2 ownership, fitted SurfaceView,
JPEG request/orientation, IO MediaStore saving and last-photo gallery access.
Supported automatic AF/AE/AWB, torch, zoom and camera switching exist. M2 boxes
remain open until physical lifecycle/orientation/capture tests substantiate
the behavior; tap metering and capture AF/AE locking remain unimplemented.

## M3: Capability-aware advanced stills/lenses

- [ ] Add RAW_SENSOR/result pairing and valid DNG saving.
- [ ] Validate RAW+JPEG size/stream combinations.
- [ ] Add supported ISO/shutter/focus/WB and processing controls with feedback.
- [x] Add real zoom and accessible lens switching (baseline zoom 1.0–8.0 and
  all four lens chips device-verified 2026-11-18).
- [ ] Implement Portrait/Night extension sessions where exposed. Device facts:
  this HAL advertises BOKEH(2) and BEAUTY(1) on all four lenses but not
  NIGHT(4), so Portrait has a genuine extension path while Night must stay
  disabled with that reason unless an alternative real pipeline is implemented.
- [ ] Report unavailable modes and failed configurations accurately.

Exit: every enabled still control affects actual capture; files and capture
results substantiate support. No extension support means those modes remain
unavailable, not substituted by ordinary Photo.

## M4: Robust standard Video

- [ ] Surface encoder, microphone/AAC when enabled, timestamps and muxing.
- [ ] Validated resolution/FPS/bitrate/codec choices.
- [ ] Real record/stop/timer/finalization and playable output.
- [ ] Silent recording when mic is disabled/unavailable.
- [ ] Safe repeated start/stop, lifecycle stop and storage/error cleanup.

Exit: playable, correctly oriented recording with validated A/V synchronization
and no leaked camera/audio/codec resources in repeated tests.

## M5: Pro Video, HDR and high FPS

- [ ] Manual recording controls supported by the active configuration.
- [ ] Compatible HDR/10-bit profiles with correct encoder/color metadata.
- [ ] Valid high-speed size/FPS sessions and generated burst requests.
- [ ] Supported stabilization/physical/concurrent camera configurations.
- [ ] Exclude incompatible combinations and verify actual output cadence/profile.

Exit: enabled advanced recording features have file/result evidence. A selected
toggle, advertised flag or encoded nominal frame rate alone is insufficient.

## M6: Comprehensive settings and diagnostics

- [ ] Complete only functional inventory items from [SETTINGS.md](SETTINGS.md).
- [ ] DataStore preferences, migrations and per-camera revalidation.
- [ ] Searchable settings and actual permission/storage actions.
- [ ] Dedicated real log viewer, bounded logging and shareable export.
- [ ] Hardware report and safe advanced public request controls.

Exit: every editable setting has an observable effect; unsupported/read-only
items cannot mislead; diagnostics can reproduce a real failure.

Logging infrastructure and basic diagnostics start in M1; M6 is completion, not
the first time logging is introduced.

## M7: Reference UI and performance polish

- [ ] Refine camera/settings/diagnostic screens against the reference.
- [ ] Validate approximately 20:9 portrait layout and system insets on device.
- [ ] Large-text, TalkBack, contrast, touch-target and narrow-screen checks.
- [ ] Real grid/level and optional real monitoring within valid stream budgets.
- [ ] Measure startup, mode-switch, capture, recording and resource behavior.

Exit: responsive premium black native UI without sacrificing media correctness
or pretending unsupported reference labels are actual features.

## M8: Physical-device validation and delivery

- [ ] Execute the full relevant test matrix and fix reproduced failures.
- [ ] Record OS/build/camera-library environment and final capability inventory.
- [x] Produce a signed debug APK and document exact verified artifact path (TESTING).
- [ ] Record build/test results, unavailable features and remaining risks.
- [ ] Update all canonical docs and the handoff to the delivered state.

Do not mark M8 complete until actual physical-device testing is documented.
Release signing/publishing is a separate explicit action, not implied by a debug
APK or this roadmap.
