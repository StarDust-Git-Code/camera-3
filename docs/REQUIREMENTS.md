# Requirements

Last updated: 2026-11-18. Authority: the user's requests in this conversation.
Implementation status is tracked separately in [PROGRESS.md](PROGRESS.md).

## R-01: Platform and device scope

- Native Android app for mobile phones, with portrait as the primary layout.
- Android 15 and above only: `minSdk = 35`.
- Devices must provide Camera2 LEVEL_3 support. Do not quietly support a
  FULL/LIMITED-only device as if it met this requirement.
- The current planned interpretation is a device with at least one accessible
  LEVEL_3 camera; each other lens has its own capability restrictions. See
  [CAMERA-CAPABILITIES.md](CAMERA-CAPABILITIES.md).
- Primary test phone: Samsung Galaxy A35. User reports One UI 8.5, Magisk,
  unlocked bootloader and an unlocked/modified camera library.
- Device software, camera IDs, levels and capabilities remain unverified until
  the phone is connected and queried. The One UI version is not an Android API
  measurement.

## R-02: Required engineering stack

- Kotlin and Jetpack Compose.
- Android Camera2, including LEVEL_3 features available to public APIs.
- Modular architecture, MVVM and Kotlin Coroutines.
- Proper ownership and cleanup of camera/media resources.
- Stability, performance, image quality and clean, maintainable code.

## R-03: Visual direction

- The attached multi-screen image is the primary UI/UX reference.
- Minimalist, premium black aesthetic; compact camera controls and quiet chrome.
- Adapt for a 6.6-inch portrait phone, approximately 20:9; 1080 x 2400 is a visual
  reference, not a fixed layout coordinate system.
- Responsive layout, correct display cutout/system-bar handling and edge-to-edge
  rendering. Real camera content replaces sample scenery from the reference.
- Android accessibility and touch-target requirements take precedence over
  copying tiny controls or simulated iOS system bars from the image.

## R-04: Camera modes

- Photo.
- Portrait.
- Night.
- Video.
- Pro Video.

All five belong to the requested product. A mode is selectable only when its
actual pipeline works on the chosen camera/configuration. Portrait/Night must
not be renamed ordinary Photo captures. An unavailable mode may be shown as
disabled with a clear explanation, never as a usable feature.

## R-05: Camera capabilities

Implement hardware-supported public Camera2 features, including:

- RAW/DNG still-image capture.
- Manual ISO, shutter/exposure time, focus and white balance.
- Automatic/manual exposure, metering and supported locks.
- HDR/10-bit video, supported high frame rates and codecs.
- Stabilization, zoom and multi-camera/lens support.
- Supported image processing, noise reduction and lens correction.
- Advanced Camera2 controls and hardware diagnostics.

Capabilities are dynamic, per lens and per complete session configuration.
Advertised flags alone do not make an option available. Encoder support,
resolution/FPS limits, extension restrictions and output compatibility matter.
Never expose a fake control or claim a private OEM function is publicly usable.
RAW/DNG here means stills; RAW video is not implicitly promised.

## R-06: Settings

A comprehensive settings page must cover photo/video quality; resolution, aspect
ratio and FPS; RAW/HDR; processing/noise reduction; focus/exposure;
stabilization/lens correction; audio/microphone; storage/naming; grid/level/UI;
hardware diagnostics; advanced Camera2; permissions and app information.

Every editable setting must have an implemented effect, validation and persisted
state. See [SETTINGS.md](SETTINGS.md). Read-only capability information must not
look like an editable control.

## R-07: Logging and diagnostics

- DEBUG, INFO, WARNING and ERROR levels.
- Initialization, lifecycle and capability detection logs.
- Capture, recording and session-configuration errors.
- Useful bounded performance/debugging logs.
- Dedicated in-app log viewer.
- Export/share diagnostic logs.

Logs must remain useful without blocking camera work or disclosing unnecessary
personal/device identifiers. See
[LOGGING-AND-DIAGNOSTICS.md](LOGGING-AND-DIAGNOSTICS.md).

## R-08: Delivery and validation

- Inspect the existing workspace first; this was done and it was empty.
- Implement incrementally, beginning with working camera capture.
- Test on the actual Samsung phone, discover unsupported features and resolve
  build/runtime errors.
- Deliver working application source and a tested installable build.
- Do not stop at a UI mockup or report emulator tests as physical-hardware tests.
- Report implemented, untested, unavailable and failing features separately.

## R-09: Persistent documentation

- Keep Markdown in the project folder, not an external plan directory.
- Record goals, ideas, plans, technology, problems, attempted solutions,
  architecture/structure, decisions, testing, progress and resume context.
- Maintain the records as material facts change.
- Use a short handoff and a documentation index so context can be recovered
  without loading every historical detail.
- These notes cannot enlarge the agent's context limit. They preserve facts for
  explicit re-reading across sessions/compaction.

## R-10: Build infrastructure

- User requested GitHub Actions to build/compile the APK using
  `https://github.com/StarDust-Git-Code/camera-3` on 2026-11-18.
- Publish the project/workflow without overwriting existing remote work; retain
  tests/lint and downloadable debug APK artifacts.
- CI compilation is not evidence of physical-device camera functionality.

## R-11: Added high-resolution, processing and audio scope

The user added the following requirements on 2026-11-18:

- Capability-gated high-resolution still capture, including a 50 MP label only
  when a real public maximum-resolution output of that size is exposed. Never
  upscale or relabel the existing approximately 12 MP stream.
- High-resolution RAW+JPEG only when the maximum-resolution stream map exposes a
  compatible real RAW_SENSOR output; an ordinary 12 MP DNG is not 50 MP RAW.
- Smooth video zoom.
- LUT preview/recording, software HDR and a genuine documented Log pipeline;
  never call an ordinary SDR transform "Log" or use a LUT toggle as proof.
- A bounded RAW sensor-frame recording/storage mode with honest throughput and
  dropped-frame reporting; do not promise unbounded RAW video.
- Stereo audio recording and live monitoring through Type-C earphones/IEM, only
  when the selected input/output routes and channel configuration are real and
  verified. Monitoring must not silently route to the speaker.

The user requested no phone access during the current implementation phase.
Source/build work may proceed, but maximum-resolution availability, zoom feel,
RAW throughput, stereo channels and USB routing remain device-unverified until
the user later permits a physical test session.

## Non-goals unless explicitly added

- Modifying Magisk modules, camera libraries, bootloader state or firmware.
- Root-required capture, hidden-API bypasses or stock-camera feature parity.
- Fabricated RAW, simulated HDR, fixed fake zoom lenses or fictional Log profiles.
- Cloud accounts, media uploads, analytics, ads or social publishing.
- Tablets/desktop UI as primary targets.
- Installing a release signing key or publishing the app without approval.

## Open product details

- Final app name and application ID; "Camera3" is a workspace/working name.
- Exact installed Android release/API and active camera modification/module.
- Whether any lower-level auxiliary lens should be excluded despite the phone
  meeting the LEVEL_3 device gate. Current proposal permits capability-gated use.
- Additional software Portrait/Night processing if OEM extensions are absent.
  No such fallback is currently committed or implemented.
