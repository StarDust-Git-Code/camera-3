# Goals and ideas

Last updated: 2026-11-18. Baseline Photo, RAW/DNG and historical FHD/30 Video have
partial device evidence. Current focused source permits only capability-gated
1080p60; CI/device cadence evidence is pending. Advanced media work remains planned.

## Product goals

1. Make a dependable camera first. A successful photo/recording matters more than
   matching every decorative element in the reference.
2. Make advanced hardware usable without exaggeration. Users should understand
   what their selected lens and recording configuration actually support.
3. Keep the viewfinder dominant. Controls should be legible and reachable, while
   the image remains the main content.
4. Expose useful evidence when modified camera software behaves incorrectly.
5. Preserve decisions and unfinished work so a later session can continue safely.

## Planned ideas

| Idea | Why it fits | Guardrail |
| --- | --- | --- |
| White active-mode pill on black | Closely matches the supplied camera reference | Accessible hit targets, not image-scaled tiny labels |
| Compact manual strip in Pro Video | Makes ISO/shutter/EV/WB/focus immediately visible | Each field comes from an implemented request/control |
| Camera-aware zoom rail | Quick switching/zooming without clutter | Derive labels; distinguish digital zoom from physical lens changes |
| Capability explanation sheet | Makes missing features understandable | Report actual API/session reasons, not generic marketing claims |
| Requested versus effective values | Helpful on modified camera libraries | Display result data only when available; do not invent feedback |
| One diagnostic export action | Simplifies a reproducible bug report | User-initiated, privacy-filtered, size-bounded |
| Searchable functional settings | Covers a large feature set without crowding capture | Only implemented settings can be editable |
| Lightweight histogram and audio meters | Matches the Pro Video reference and improves monitoring | Real data only; disable when stream/performance constraints prevent it |
| Maximum-resolution still mode | Uses genuinely exposed full-sensor JPEG/RAW streams | Actual MP label from the selected output; no upscaling or 50 MP marketing alias |
| Smooth logarithmic video zoom | Makes ratio changes feel even across a wide zoom range | Clamp to the active lens and serialize request updates |
| GPU color pipeline | Shared preview/encode path for LUT, software HDR and defined Log transforms | Declare transfer/gamut/bit depth and verify encoded metadata/output |
| Wired IEM monitoring | Lets the operator hear the real recorded input | Require a routed wired/USB output; never fall back silently to speaker |

## Deferred ideas

- Software Portrait using depth/segmentation: requires a real algorithm, image
  quality evaluation, performance budget and a separate documented decision.
- Software multi-frame Night: requires alignment, merge, motion handling and
  tests. Ordinary long-exposure Photo must not masquerade as a Night algorithm.
- LUT import/preview: now required, but still deferred to the GPU/codec tranche;
  it is not proof of genuine Log and must declare preview/output scope.
- Focus peaking/zebra overlays: only after a valid analysis stream or rendering
  pipeline exists and has been performance-tested.
- Simultaneous multi-camera capture/recording: only supported combinations with
  explicit resource limits; lens switching does not prove concurrent capture.
- Settings backup/import: a later quality-of-life feature with schema validation
  and revalidation against the current camera.

Deferred does not mean available in the UI. Promote an idea only with a decision,
implementation plan and acceptance test.

## Rejected approaches

- Building a static camera mockup as the final deliverable.
- Hardcoding the reference's "12 MP", "4K / 60 / 10-bit" or lens labels.
- Calling a camera LEVEL_3 solely because the phone model is usually capable.
- Treating Magisk/bootloader unlock as automatic access to OEM private APIs.
- Arbitrary S-Log3 branding without a documented, implemented capture pipeline.
- Loading all Markdown into every model turn or creating repeated filler notes.

## Priority order

Working baseline -> trustworthy capability gating -> correct media output ->
advanced controls -> comprehensive settings/diagnostics -> monitoring extras ->
visual/performance polish -> device validation and delivery.

See [ROADMAP.md](ROADMAP.md) for milestone exit criteria. When selecting another
idea, record the choice and tradeoff in [DECISIONS.md](DECISIONS.md).
