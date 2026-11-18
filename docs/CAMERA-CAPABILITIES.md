# Camera capabilities and availability policy

Last updated: 2026-11-18. Baseline, P-016 correction and partial RAW/Video
device probes completed; maximum-resolution discovery is implemented but has
passed CI at `d0ccb40` but has not run on the phone. Evidence and limitations are
in TESTING.

## Implemented baseline discovery / policy

`CameraDiscovery.kt` reads public IDs, levels, regular and maximum-resolution
stream maps, physical IDs, capabilities, keys, AF/flash/zoom, extension IDs and
diagnostic ranges.
`CameraPolicy.kt` enforces LEVEL_3 eligibility, prefers a qualifying rear camera,
and selects matching JPEG/preview aspect with a modest baseline size.
The engine checks complete session support when the HAL implements that query,
then requires configuration success. Zoom/torch check exposed keys/modes;
unsupported combinations enter an explicit error/retry state, not fake preview.
Lower-level auxiliary lenses are allowed only on an eligible phone (D-003).
RAW/DNG and first-cut MediaRecorder video pipelines exist; manual controls,
extensions, GPU color processing and routed audio monitoring do not. First real
device findings are in the table below.

## LEVEL_3-only startup requirement

1. Request required camera access and enumerate public CameraManager IDs.
2. Read `INFO_SUPPORTED_HARDWARE_LEVEL` for accessible image-producing cameras.
3. Require at least one camera reporting
   `INFO_SUPPORTED_HARDWARE_LEVEL_3` before enabling capture operation.
4. If none qualifies, expose permission/retry/diagnostics/help states, not a
   FULL/LIMITED capture fallback disguised as LEVEL_3.
5. Prefer a qualifying rear main camera when available. Do not hardcode camera
   ID `0` as the main lens.

This is a device eligibility gate. Current baseline policy permits other publicly
accessible lenses on an eligible phone, with their own restrictions; they need
not all report LEVEL_3. If the user requires LEVEL_3-only lens selection too,
change this policy explicitly before implementation.

The reported level is a platform claim, not proof that every feature or output
combination works. Modified camera libraries can misreport flags or limits.
Configuration, results and output files provide additional evidence; they cannot
universally establish the physical authenticity of a reported hardware level.

## Capability model

For each logical/public camera, collect facing, hardware level, sensor geometry,
orientation, focal lengths, physical IDs, stream maps, available request/result
keys, ranges/modes, extension modes and dynamic-range profiles. Inspect accessible
physical camera characteristics as permitted by public APIs; do not assume every
physical ID can be independently opened.

Availability combines:

```text
OS API + exposed hardware + request keys/modes + selected lens
+ mode + resolution + FPS + surfaces + encoder + session success
```

Keep reported support separate from implementation and runtime evidence:

- Reported supported, but no implemented pipeline: diagnostics only.
- Hardware/API unavailable: explain the reason.
- Incompatible with current mode/configuration: explain the conflict.
- Implemented and validated configuration: may be selectable.
- Configuration/runtime failure: retain evidence, exclude the failed combination
  and allow an explicit safe recovery action.

## Feature inventory and control mapping

| Feature | Detection / real implementation requirement |
| --- | --- |
| JPEG/resolution | StreamConfigurationMap output sizes and valid complete session |
| Maximum-resolution JPEG/RAW | Maximum-resolution stream map, writable sensor pixel mode, pixel-mode-aware outputs/request and a valid complete session; label actual output MP, never marketing resolution |
| RAW/DNG | RAW capability, RAW_SENSOR output, matching CaptureResult, DngCreator and valid saved DNG |
| RAW+JPEG | Simultaneous outputs supported for the chosen sizes; never assume separate support implies combined support |
| Manual ISO/shutter | Manual sensor capability, keys/ranges, AE-off policy, exposure/frame-duration relationship and result feedback |
| Exposure compensation | AE compensation range/step; not a functional substitute for manual exposure |
| Metering/AE lock | Advertised metering-region limits, AE modes and lock availability |
| Autofocus/manual focus | AF modes, focus distance range/calibration and fixed-focus detection |
| White balance | Supported AWB presets/lock; manual correction modes/gains where available |
| WB temperature | No fictitious Kelvin slider; require a defined calibrated conversion if implemented |
| Flash/torch | Flash availability, supported request policy and capture/recording restrictions |
| Zoom | Zoom-ratio range where available or crop-region limits; correct preview/metering transforms |
| Portrait | Supported Camera2 BOKEH extension plus valid extension sizes/requests/session |
| Night | Supported Camera2 NIGHT extension plus valid extension sizes/requests/session |
| Still HDR | Genuine available extension/scene mode and its real restrictions; separate from HDR video |
| Video resolution/FPS | Camera output duration/ranges, encoder size/rate/profile limits and working session |
| High-speed video | High-speed capability and size/range map; constrained session and generated repeating request list where required |
| HDR/10-bit video | Dynamic-range capability/profile, compatible output surfaces, encoder profile and color metadata |
| OIS/EIS | Advertised modes plus active session/FPS/output restrictions; no assumed simultaneous compatibility |
| Noise/edge/tone controls | Advertised modes/keys and their valid session policy; processed output differs from RAW |
| Lens correction | Distortion/lens-shading modes and corresponding coordinate/performance consequences |
| Multi-camera | Logical/physical topology, physical request keys and supported output/concurrent-camera combinations |
| Histogram/monitoring | Real sampled data and a compatible bounded analysis/rendering pipeline |

Other publicly reported capabilities such as burst, reprocessing, depth or
maximum-resolution sensor modes must be inventoried in diagnostics. They become
usable features only after their own output/control pipeline and acceptance
tests exist. Listing a flag is not implementing that feature.

## Mode policy

- Photo uses actual still capture; manual options are exposed only when usable.
- Portrait/Night use genuine supported Camera2 extension pipelines initially.
  Their availability is independent of LEVEL_3.
- Video currently offers only a complete 1920×1080 fixed-60-fps configuration:
  real MediaRecorder size/timing, `[60,60]` range, writable request, AVC encoder,
  matching preview and valid regular or constrained high-speed session.
- Pro Video offers actual supported manual recording controls; manual values may
  be unavailable in constrained high-speed or extension configurations.
- Software Portrait/Night fallbacks are deferred ideas, not silent substitutions.

## Important output distinctions

- A 10-bit-capable sensor does not prove a supported 10-bit encoder/profile.
- HLG/PQ are defined transfer functions, not interchangeable branding.
- A recorded SDR video with an HDR toggle selected is a failed feature.
- RAW_SENSOR still buffers do not imply RAW video support.
- A UI crop/aspect label must describe what is saved, not only preview geometry.
- Extra lenses exposed by a modification do not prove concurrent operation.
- Do not combine high FPS, RAW, HDR, physical outputs and analysis merely because
  each is individually advertised.

## Advanced request safety

Use typed, validated controls for publicly documented writable keys. Inspect
advertised key availability and value ranges before submission. Unknown/private
vendor keys may be shown read-only with their provenance; never blindly write
them or use hidden API bypasses.

Store failed-combination evidence with the software/build/config identity.
Revalidate after a camera-library or OS change; do not permanently blacklist an
entire lens based on one stale session failure.

## Current device findings (2026-11-18, device-observed)

Observed through the app's public Camera2 discovery on the connected phone.
Build identity (spoofed properties noted): Android 16 / API 36, model string
SM-S711B on an a35x device board; fingerprint mixes `a35x` with `S711B...`,
consistent with the user's Magisk/modified-library report. Serial is excluded
from documentation. File evidence: TESTING execution record.

| Item | Observed value |
| --- | --- |
| Public cameras | 4: ID 0 BACK, ID 1 FRONT, ID 2 BACK, ID 3 FRONT |
| Reported hardware level | LEVEL_3 on all four (platform claim, not a pipeline proof) |
| Eligibility log | `LEVEL_3 device eligible=true, baselineCamera=0` |
| Capabilities (all four) | BACKWARD_COMPATIBLE, MANUAL_SENSOR, READ_SENSOR_SETTINGS, MANUAL_POST_PROCESSING, BURST_CAPTURE, YUV_REPROCESSING, RAW, STREAM_USE_CASE |
| Extra on ID 0 | CONSTRAINED_HIGH_SPEED_VIDEO (labeled HIGH_SPEED_VIDEO in UI) |
| Baseline JPEG (policy pick, 4:3) | ID 0 4080×3060, ID 1 1920×1440, ID 2 3264×2448, ID 3 3408×2556 |
| Preview (policy pick) | 1440×1080 on every lens |
| Zoom ratio range | ID 0/ID 2: 1.0…8.0; ID 1/ID 3: 1.0…4.0 |
| Extension IDs (all four) | 1 = BEAUTY/FACE_RETOUCH, 2 = BOKEH; NIGHT(4), HDR(3), AUTOMATIC(0) not advertised |
| Physical IDs | None reported on any camera (no sub-lens topology exposed) |
| Regular RAW outputs | ID 0: 4080×3060; ID 1: 4128×3096; ID 2 reports one RAW size but its dimensions were not captured here; ID 3 reports none |
| RAW+JPEG runtime | Correction-build logs show configured three-stream sessions and paired JPEG+DNG saves on IDs 0 and 1; files were not pulled/opened, so DNG validity/rotation remain unverified |
| Standard Video runtime | Correction-build log shows ID 0 1920×1080 silent Video session configured; user reports FHD/30 recording works. File tracks, cadence, orientation and A/V sync were not independently inspected |
| 1080p60 source | Capability-gated regular/constrained-session implementation is locally build-verified only; the phone's exact ranges, session acceptance and saved cadence are unknown |
| Stabilization | User reports OIS does not work in current video. The app does not yet select/verify OIS or EIS modes, so stabilization remains unimplemented rather than a passed feature |

Current implementation consequences (re-verify per new build/configuration):

- Portrait has a genuine BOKEH (ID 2) extension path on this device.
- The Camera2 NIGHT extension (ID 4) is not advertised, so the Night tab has no
  honest extension pipeline on this device; keep it disabled with that reason
  unless a different real pipeline is implemented later.
- READ_SENSOR_SETTINGS/STREAM_USE_CASE/RAW advertise a manual/RAW path; key
  ranges must be revalidated per lens before manual controls are exposed.
- The installed correction predates maximum-resolution-map discovery. Actual
  maximum-resolution JPEG/RAW maps, sensor-pixel-mode writability, full-session
  acceptance and saved dimensions are unknown. Existing 4080×3060 output is
  approximately 12.5 MP and must never be labeled 50 MP.
- The installed correction also predates D-021. ID 0's reported high-speed
  capability alone does not prove 1080p60; exact ranges, encoder gate, successful
  new session and saved-frame timestamps must all be checked later.

Populate additional facts here only after actual queries/tests, and link
evidence from [TESTING.md](TESTING.md) and [PROGRESS.md](PROGRESS.md).
