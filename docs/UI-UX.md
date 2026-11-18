# UI/UX direction

Last updated: 2026-11-18. Initial native Compose camera/settings/diagnostics/log
screens compile successfully. Runtime and visual comparison on a device are pending.

## Implemented reference adaptation

Black background, quiet panels, white centered shutter, real JPEG/MP summary,
portrait fitted live preview, actual camera-ID chips, grid, and simple settings
rows. The attachment was re-supplied with the build request; it is still not a
local image asset. No fake system bars, scenery, hardware figures or Log profiles.
Night/Portrait/Video/Pro Video tabs are disabled and the screen explicitly says
this development build supports Photo only. Larger-text/accessibility review
and phone screenshot comparison remain outstanding.

## Reference provenance

The user attached an image showing eight camera/settings/diagnostic phone
screens. It is the primary visual reference. It is available in the conversation
but is not a local project image. Do not invent an asset filename or claim a
pixel-level screenshot comparison has been performed.

## Visual tokens: proposed from the reference

| Token | Proposed value | Role |
| --- | --- | --- |
| Base | `#000000` | Main camera and screen background |
| Surface | `#171917` | Quiet settings groups and control surfaces |
| Divider | `#343734` | Thin meaningful separation |
| Foreground | `#F5F5F2` | Primary text and selected photo controls |
| Muted | `#A5AAA4` | Supporting labels, never sole disabled explanation |
| Recording | `#FF453A` | Record button and recording status |
| Diagnostic success | `#8BC34A` | Verified/healthy state, with text/icon equivalents |
| Level alignment | `#E0C547` | Alignment indicator, not a global decoration color |

Use native Android sans-serif typography with a deliberate small hierarchy:
screen title, main control value, setting label, supporting explanation. Use
monospaced text for logs/diagnostic values only where it aids scanning. No fake
iOS status bar, branded stock-camera font or system navigation bar rendering.

## Primary portrait composition

```text
System cutout/status-safe region
Top camera controls: flash, supported quality/RAW/HDR, settings
Dominant correctly proportioned viewfinder
  Optional grid / real level / focus indicator
  Camera-aware zoom rail
  Optional genuine monitoring overlays
Optional Pro Video control strip
Mode selector
Gallery thumbnail | photo shutter or video record | camera switch
System gesture/navigation-safe region
```

Photo's portrait preview may use a 4:3 sensor view; Video may use a 16:9 output
view. The screen itself being 20:9 does not mean saved media is 20:9. Preserve
the selected output aspect and expose any intentional crop. Never stretch the
image to fill remaining space.

## Layout and accessibility

- Use dp/sp and measured window dimensions, not hardcoded 1080 x 2400 pixels.
- Use Compose edge-to-edge and WindowInsets for controls. Background may extend
  behind bars; critical actions remain clear of cutouts/navigation gestures.
- At least 48 dp interactive targets, even for visually small icons/pills.
- Center the shutter; keep thumb-reachable controls near the lower screen.
- Permit horizontal mode/control scrolling when labels do not fit.
- Settings and diagnostics scroll vertically; keyboard search respects IME insets.
- Test larger system text and TalkBack descriptions, checked/disabled semantics,
  contrast, focus order and non-color status indicators.
- Keep animations brief and user-triggered. Do not animate decorative overlays
  continuously or waste camera performance on chrome.

## Screen behavior

### Camera

Real preview, hardware-driven top controls, supported mode selection, gallery
access and actual shutter/record actions. Disable lens/mode reconfiguration during
recording unless the implemented pipeline explicitly supports it. Show real
elapsed recording time and meaningful capture/finalization progress.

### Pro Video and color

ISO, shutter, EV, white balance and focus open functional selectors/sliders only
where supported. Display requested versus effective values when possible.
Resolution/FPS/bitrate/codec/stabilization summaries must match the active config.

The reference's Log/S-Log3/LUT screen is inspiration, not authority to label an
unimplemented or nonexistent Log pipeline as real. Show supported SDR/HDR
profiles only; deferred monitoring/LUT features remain unavailable.

### Settings

Quiet grouped rows, clear labels, concise current values, search and back
navigation. Unsupported settings are absent or visibly unavailable with a reason.
Read-only hardware facts do not use switches.

### Camera diagnostics

Actual hardware-level card, selected camera ID/facing, physical camera topology,
capabilities/ranges/output limits, effective configuration and failure details.
LEVEL_3 success must come from a query, never a permanent design label.

### Logs

Severity chips, search, chronological events, live following, export and explicit
clear confirmation. Avoid auto-scroll while the user is inspecting older events.

## Error and unavailable states

Permission request, no accessible LEVEL_3 camera, camera in use, failed
configuration, microphone denied and storage failure each need a specific next
action. Modified-camera failures should retain enough diagnostics to reproduce
them. Never replace a broken real preview with a sample landscape.
