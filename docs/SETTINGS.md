# Settings inventory and behavior

Last updated: 2026-11-18. Initial settings exist; the comprehensive inventory below
is still mostly planned. High-resolution preference is build-verified, not device-verified.

## Implemented subset

- `grid`: boolean, default false, persisted globally; draws rule-of-thirds lines
  only over the real fitted preview.
- `jpeg_quality`: integer 50..100, default 95, persisted globally; UI uses 5-point
  steps; next still request sets `JPEG_QUALITY` from the validated preference.
- `raw`: boolean, default false, persisted globally; only accepted when the active
  standard/maximum-resolution photo configuration has a real compatible RAW stream.
- `high_resolution`: boolean, default false, persisted globally; shown as HI-RES
  only when the active lens exposes a writable sensor-pixel-mode key and a real
  maximum-resolution JPEG. It reconfigures the session and never relabels baseline
  JPEG. Returning to standard mode atomically clears RAW too when no compatible
  standard RAW configuration exists, avoiding an invalid intermediate preference.
- Actual system app-permissions action, diagnostics/log navigation and build info.
- MediaStore destination is fixed/read-only `DCIM/Camera3` in this baseline.
- Lens selection/zoom/torch are transient hardware-aware controls. LUT/Log/HDR,
  RAW-frame video and audio-monitoring settings remain absent until their pipelines exist.

## Functional-settings contract

Every editable setting needs an ID, type, default policy, available values,
capability condition, persistence scope, actual effect, conflict handling and
acceptance test. A preference stored in DataStore is not itself functionality.

For camera-related settings, separate requested, validated and currently
effective values. Revalidate on camera/lens/mode/resolution/FPS/profile changes.
If reconfiguration is required, apply atomically or show the error; do not leave
the UI claiming a value that the pipeline rejected.

## Inventory

| Category | Requested options | Actual effect / availability |
| --- | --- | --- |
| Photo quality | Format, resolution, JPEG quality, aspect, RAW/RAW+JPEG preferences | Output stream and metadata/writer configuration; RAW combinations checked together |
| Video quality | Resolution, FPS, bitrate, codec and dynamic range | Encoder, surfaces and session configuration agree; inspect saved output |
| RAW/HDR | Per-mode defaults and valid profiles | Genuine RAW/DNG or supported HDR pipeline, not cosmetic processing |
| Processing | Noise reduction, sharpening/edge, tone and supported color correction | Advertised request keys/modes; explain RAW versus processed output |
| Exposure | Auto/manual defaults, compensation, metering and supported locks | Actual AE/manual request policy; reflect result data |
| Focus | AF mode, tap metering, manual focus and supported locks | Actual AF requests; fixed-focus cameras get no fake focus slider |
| White balance | AWB presets/lock and real manual controls | Valid correction modes/gains; calibrated temperature only if implemented |
| Stabilization | OIS/EIS and supported video smoothness choices | Valid advertised modes and compatible output configuration |
| Lens correction | Distortion/lens shading where exposed | Actual request keys and appropriate coordinate transforms |
| Lens/camera | Facing, accessible lenses, default camera | Real camera selection, per-lens capabilities; concurrency separate |
| Audio | Enable audio, input/source, supported rate/channels/quality | AudioRecord/encoder config; microphone routing only if selectable and verified |
| Storage | MediaStore destination, selected folder, naming and format policy | Real writable destination; collision-safe names and failure reporting |
| Interface | Grid, real level, controls/overlay preferences and haptics | Viewfinder/sensor/UI behavior, persisted user preferences |
| Diagnostics | Camera report, available keys, active/effective config | Read-only actual data; never fabricated numeric defaults |
| Advanced Camera2 | Safe publicly documented supported controls | Typed validation, no arbitrary vendor-key writes |
| Logging | Severity threshold, live view, export and clear | Real logger/viewer behavior with bounded retention |
| Permissions | Camera, microphone and optional selected storage access | System permission/status UI and relevant Settings intent |
| App information | App/build version, libraries/licenses, relevant device info | Generated/current metadata; final identity not chosen yet |

## Persistence proposal

- DataStore holds a versioned preference schema.
- Global UI preferences are separate from camera/mode-specific preferences.
- Stable camera IDs may be used for per-camera preferences, but detect removed
  IDs and revalidate after firmware/library changes.
- Avoid storing Android object instances or URI permissions that are not valid.
- Validate imported/old preferences before applying; migrations preserve user
  intent without enabling unsupported values.
- Defaults favor robust auto capture and valid SDR recording, not a hardcoded
  maximum resolution/FPS for every camera.

## Application behavior

- Small repeating-request changes may apply without recreating the session.
- Stream size/profile/surface changes normally require safe session recreation.
- Recording-critical settings stay locked while recording unless changing them
  is explicitly supported by the implemented pipeline.
- When switching lenses, present incompatibility reasons and effective values.
- Explicitly notify the user when a recovery requires reducing a chosen quality
  setting. Do not silently disable requested RAW/HDR and report success.

## Permission/storage rules

Photo does not require microphone access. Silent video remains possible when
audio is off or permission is denied. Request microphone access when enabling
audio/recording, with a clear reason. User-created MediaStore files do not require
broad all-files access. SAF destinations require a real grant, writable check and
graceful handling of revocation/full storage.

## Deferred settings

LUT import, software Portrait/Night parameters, focus peaking and settings
backup/import remain deferred until their pipelines exist. They must not be
editable placeholders in the first settings screen.

## Required tests

Persist/restart, invalid values, schema migration, camera disappearance, lens
switch conflicts, settings changed during recording, permission denial and
storage grant revocation. Check actual media/request effects, not only switches.
