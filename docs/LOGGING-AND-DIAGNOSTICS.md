# Logging and diagnostics

Last updated: 2026-11-18. Logger, viewer, report and sharing source is build-verified;
on-device diagnostics screens/log capture supported the baseline and P-016 work.
Viewer filtering, exported-share opening, retention and load acceptance remain open.
Development issues are recorded in [PROBLEMS-AND-SOLUTIONS.md](PROBLEMS-AND-SOLUTIONS.md).

## Implemented baseline

`AppLogger.kt` writes on IO through a 256-command channel; viewer buffer is 500
events; files rotate at 1 MiB across three files. Messages are limited to 6000
characters and queue overflow is reported. DEBUG/INFO/WARNING/ERROR, Logcat,
severity/search filtering, live-follow pause on drag, confirmed clear, user-led
FileProvider/Android Sharesheet export and actual capability reports exist.
No serials/media content or automatic uploads. Persistent logs export in
chronological file order. Device logs and capability reports have been collected;
load/retention/privacy/share tests remain pending;
no measured performance guarantee is claimed.

## Runtime log contract

Required levels: **DEBUG, INFO, WARNING, ERROR**. Present Android Logcat WARN as
WARNING in the app if that is the chosen UI vocabulary; do not introduce a fake
extra severity.

Proposed event fields:

- Wall-clock timestamp for reading; monotonic elapsed time for durations.
- Severity, subsystem/tag, operation and concise message.
- Camera ID, session/operation generation and capture/recording correlation ID
  when applicable.
- Relevant validated configuration and structured error code/cause.
- Throwable detail on errors, without repeated identical unbounded stack traces.

Keep fields structured before display/export. A text-only logger loses useful
filtering/correlation. Do not log raw frame buffers or microphone samples.

## Event coverage

| Subsystem | Events |
| --- | --- |
| App/permissions | Foreground/background transitions, permission state changes and access failures |
| Discovery | Camera IDs, reported hardware levels, capability/range/profile detection and unavailable reasons |
| Device | Open/close attempts, successful opens, disconnects, contention and platform error codes |
| Session | Requested output config, validation, configured/failed/closed callbacks and duration |
| Requests | Meaningful user-setting changes, requested/effective values and rejected keys/configurations |
| Capture | Capture start/result/image pairing, processing/saving, successful file metadata and errors |
| Recording | Encoder/audio/session setup, start/stop/finalization, codec/profile/FPS, drain/mux failures |
| Storage | Destination failures, revoked grants, insufficient space and collision handling |
| Performance | Sampled frame cadence, dropped analysis work, queue pressure, memory/thermal observations where measured |
| Export | User export request, generated report and export failure |

Do not write a verbose event for every preview frame. Sample or aggregate
telemetry and reserve per-capture details for actual captures/debug sessions.

## Storage/performance proposal

- Bounded in-memory recent-event buffer for the viewer.
- Asynchronous app-private rotating files with explicit byte/count limits.
- Configurable severity threshold; useful default INFO, richer DEBUG when enabled.
- Backpressure policy prevents logs from blocking capture or growing indefinitely.
- Preserve ERROR events where possible; expose any dropped-log counter.
- Camera callbacks submit lightweight events, not synchronous disk writes.

Exact retention sizes will be chosen and measured during implementation; no
unmeasured performance numbers are promised here.

## In-app viewer

- All/Debug/Info/Warning/Error filters and text/tag search.
- Chronological list with severity semantics and selectable/copyable detail.
- Live follow switch; pause automatic scrolling when examining older events.
- Export/share action and explicit clear confirmation.
- Clear affects app logs, not system-wide Logcat, user photos or recordings.
- A slow viewer must not slow the camera engine.

## Hardware diagnostics

Show actual reported level per camera, logical/physical topology, sensor/focal
info, output sizes/FPS, RAW/manual modes, profiles/encoder compatibility,
available request/result keys and selected/effective session config.

Separate reported support from implemented/configured/tested support. Include
recorded session failures with reproducible configuration details. For a device
without an eligible camera, diagnostics remains available while capture is
disabled by the LEVEL_3 gate.

## Export/privacy

Prepare a user-initiated report containing app/build identity, relevant Android
version, capability snapshot, selected config, failures and bounded recent logs.
Share through Android's Sharesheet with a content URI and temporary read grant.
Do not upload automatically.

Exclude serial numbers, credentials, unrelated installed apps, media contents,
precise location and raw audio. Sanitize user-specified folder/file names when
they reveal unnecessary personal details. Review build identifiers and module
information for relevance before including them. Never claim export exists
until a real report can be shared and opened by another app.

## Verification

Test severity/search/live behavior, bounded retention, clear confirmation,
export readability, URI grants, privacy redaction, concurrent logging and camera
load. Include injected errors in automated tests but identify them as synthetic
test events, not observed physical-device failures.
