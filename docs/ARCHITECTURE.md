# Architecture

Last updated: 2026-11-18. Six proposed modules now exist. Initial Photo engine,
settings and logger compiled in Actions run 44235722163; device verification is pending.

## Current implementation

- `app/src/main/kotlin/dev/camera3/app/CameraViewModel.kt`: MVVM coordination;
  application-scoped services; lifecycle-aware Compose state collection.
- `core/camera/.../CameraEngine.kt`: handler-confined device/session/request state,
  per-run epochs, stale-callback checks, close-before-reopen and explicit retry.
- UI owns SurfaceView/holder lifetime; the engine never releases a borrowed
  Surface. Native preview rotation plus fitted aspect; mirror mode AUTO.
- ImageReader is retained until camera closure and outstanding image IO complete;
  acquired images close in `finally`. Byte copies and MediaStore writes run on IO.
- `core/settings`: persisted grid/quality. `core/diagnostics`: bounded actor-style
  log writer. `core/model`: Android-independent capability/configuration policy.
- No encoder, microphone, DNG, extension session or image-analysis owner yet.

The complete state machine and future pipeline contracts below remain targets;
current lifecycle correctness still needs stress tests on a physical device.

## Proposed module boundaries

| Module | Ownership |
| --- | --- |
| `:app` | Application/activity, Compose screens, navigation, ViewModels and dependency wiring |
| `:core:model` | Shared immutable camera/options/settings/error models, no UI ownership |
| `:core:camera` | Discovery, capability validation, request building, camera device and session lifecycle |
| `:core:media` | Image saving, DNG pairing, encoding, microphone, muxing, storage and media metadata |
| `:core:settings` | DataStore persistence, migrations and camera-aware validation |
| `:core:diagnostics` | Structured logging, bounded storage, hardware reports and export preparation |

Keep the first implementation small. Split feature UI into additional Gradle
modules only when there is a demonstrated benefit. Record actual modules in
[PROJECT-STRUCTURE.md](PROJECT-STRUCTURE.md).

Dependencies point from `app` to core modules. Core modules share model types and
logging abstractions as needed; they must not depend on Compose screens or
ViewModels. Avoid circular dependencies. FileProvider/user-share launch belongs
to the app boundary; diagnostics prepares data for it.

## User action to real effect

```text
Compose interaction
  -> ViewModel intent
  -> capability/settings validation
  -> serialized camera/media operation
  -> configured request or session
  -> callback / CaptureResult / encoded output
  -> immutable UI state and diagnostic event
```

A control is not complete merely because it changes a ViewModel value. The
associated request/pipeline must run, errors must be surfaced, and supported
effective values should be reflected from results.

## Camera state machine

Proposed states:

```text
PermissionRequired -> Discovering -> UnsupportedDevice | Idle
Idle -> Opening -> Configuring -> Previewing
Previewing -> Capturing -> Previewing
Previewing -> PreparingRecording -> Recording -> Finalizing -> Previewing
Any active state -> Closing -> Idle
Recoverable failure -> Error -> bounded recovery or user retry
```

- Serialize device/session/request changes on a dedicated executor or confined
  coroutine dispatcher, with explicit callback handoff.
- Assign an operation/session generation to discard stale callbacks after lens
  switches, cancellation, closure or surface replacement.
- Make open/close/finalize operations idempotent where possible.
- Never mutate an already-closed session in response to a late callback.
- Bound retries and retain the original failure evidence.
- Use cancellation-safe wrappers around callback APIs; cleanup must still run.

## Ownership and lifecycle

- The camera engine owns CameraDevice and sessions, not composables.
- UI owns a preview surface lifecycle and informs the engine when it is usable
  or destroyed. A ViewModel must not retain an Activity or dead View reference.
- ImageReader images are closed after saving/analysis, including error paths.
- RAW result/image pairing uses capture IDs/timestamps, bounded queues and a
  timeout. A mismatched result must not produce an allegedly valid DNG.
- Media pipeline owns codecs, audio input, muxer and output destination lifetime.
- Stop camera producers before releasing surfaces/encoders they target.
- Gracefully finalize recording on stop/background where possible, then release
  resources. Background recording is not currently a committed feature.
- Sensor listeners for level/UI follow the foreground lifecycle.

## State and settings

Keep separate models for requested configuration, validated configuration,
effective capture values and UI state. A lens/configuration change revalidates
preferences before request submission. Track explicit reasons for unavailable
options; do not silently substitute RAW/HDR or another lens.

Persistence must not store transient sessions or Android objects. DataStore
values are camera-aware defaults/preferences, not proof of current availability.

## Threads and performance

Main thread: UI and short coordination only. Camera callbacks: lightweight state
updates. IO: storage/logging. Dedicated media work: encode/drain/muxing. Analysis:
bounded latest-frame work with explicit backpressure.

Do not analyze full-resolution frames on every preview tick. Avoid Compose
recomposition for every sensor/capture callback; throttle monitoring state while
keeping capture/recording correctness intact. Never add analysis streams without
checking their effect on the complete session.

## Errors

Use structured errors with operation, camera/session identity, platform code and
recovery action. Preserve technical details in logs while showing actionable
user messages. Distinguish unavailable hardware, incompatible settings,
permission denial, camera contention, storage failure and implementation failure.

## Testing seams

Abstract capability source, camera commands, storage destination, time source and
diagnostic sink. Pure configuration and request-policy logic can be tested
without hardware. A fake backend may be used inside automated tests only; never
ship it as a working camera screen.
