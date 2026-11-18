# Reference projects

Recorded: 2026-11-18, host-observed (READMEs fetched and read; no source
cloned, no code copied). These are open-source Android camera projects the
user shared as reference material for Camera3.

## Survey

| Project | License | What it is | Relevance to Camera3 |
| --- | --- | --- | --- |
| [OpenCineCam](https://github.com/LibreStatic/opencinecam) | Apache-2.0 | Shipped (Google Play) native cinema camera: Kotlin + Compose + public Camera2 only, honest capability gating, RAW/DNG, OCLog2 LOG + HLG10, real 120/240 fps high-speed, peaking/zebra/false color/waveform/LUTs | Closest philosophical match: "nothing is shown as supported until the device proves it" mirrors our constraints. Module split (app/camera/media/core-model) resembles ours |
| [OpenMotionFX/OpenCamera "CinemaCamera"](https://github.com/OpenMotionFX/OpenCamera) | MIT | RAW_SENSOR debayered on-GPU in C++ (NDK/GLES3), constant-frame-rate video via manual exposure locking, flat-log HEVC, Gyroflow `.gcsv`, cinema UI, browser remote | Reference for a GPU RAW/RAW-video path (M5) and true-CFR recording; works on any RAW_SENSOR-capable Camera2 device |
| [PhotonCamera](https://github.com/bjzhou/PhotonCamera) | Apache-2.0 | Shipped (Google Play) stills-focused camera: Compose + Camera2 (minSdk 30), multi-format LUT engine (.cube/.png/.xmp, real-time shader preview), color recipes, Motion Photos with multi-vendor adaptation (incl. Samsung), unlimited high-speed burst, multi-frame synthesis/super-resolution, midas-v2 depth bokeh, "Phantom mode" via the system camera | Strong reference for the photo side: LUT pipeline design (our deferred LUT idea), computational Night/Portrait paths, Motion Photo format handling for Samsung devices. Note: Phantom mode delegates capture to the system camera — does not fit our public-Camera2-only constraint; AI color features use a cloud model |
| [Open Camera](https://opencamera.sourceforge.io) | GPLv3+ | Mature general camera app: Camera2 manual controls, RAW/DNG, HDR, bracketing, vendor extensions | Behavior/technique reference only — see license warning below |

## License implications

- Apache-2.0 / MIT: permissive. Code may be adapted with attribution (NOTICE +
  license text) if that is ever explicitly decided; studying architecture is
  unrestricted.
- **Open Camera is GPLv3 or later: its code must NOT be copied into Camera3**
  unless the entire app is relicensed GPL. Reading it to understand Camera2
  behavior is fine; reimplementing from observation is the limit.

## Borrowable ideas (mapped to our roadmap; each needs a DECISIONS entry)

- **Strict/Adaptive recording policy** (OpenCineCam): stop a take with an
  explicit reason when storage/heat/frame timing leave the safe range, vs keep
  recording with reduced monitoring. Fits our honest-failure philosophy.
- **Foreground service keeps an active recording alive across UI recreation**
  (OpenCineCam): our current design finalizes (publish/discard) on close;
  surviving recreation is an M4+ enhancement requiring service permissions.
- **FPS lock** (OpenCineCam): hold shutter at 1/fps for constant exposure
  cadence, emulated with manual exposure where the HAL ignores the request.
- **Storage benchmark** (OpenCineCam): verify a destination sustains the
  configured data rate before recording.
- **Capability report export, redacted by default** (OpenCineCam): matches our
  planned one-tap diagnostic export.
- **On-GPU RAW debayer / RAW video** (CinemaCamera): the honest path to RAW
  video after our still-DNG pipeline; large scope, NDK required.
- **CFR via manual exposure lock** (CinemaCamera): relevant if MediaRecorder
  VFR becomes a verified problem in M4 device testing.
- **Gyroflow `.gcsv` sidecar** (both cinema projects): opt-in IMU log per take.
- **LUT engine design** (PhotonCamera): .cube/.png/.xmp import applied by a
  real-time shader in preview, burst, and saved output — a concrete model for
  our deferred LUT idea; preview-affecting vs output-affecting must stay
  declared separately.
- **Motion Photo vendor adaptation** (PhotonCamera): multi-vendor (incl.
  Samsung) still+clip embedding; a possible M6+ still feature.
- **Multi-frame synthesis / super-resolution** (PhotonCamera): the computational
  path our deferred software-Night idea needs, and a benchmark for its scope.
- **AI depth bokeh** (PhotonCamera, midas-v2 local model): candidate approach
  for the deferred software-Portrait idea (local model, on-device only).

## Use rules

- Features enter via GOALS-AND-IDEAS and the ROADMAP with capability gating;
  no idea is "supported" until device evidence exists (same rule as ours).
- Re-check licenses before copying anything; record provenance in DECISIONS.
