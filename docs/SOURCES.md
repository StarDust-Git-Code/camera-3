# Inspected sources and provenance

Last updated: 2026-11-18. Sources below were actually fetched during this
conversation. They inform plans; they do not verify the user's physical phone.

## Primary product authority

- User's original camera application requirements and attached eight-screen
  UI/UX reference.
- User's clarification: Android 15 and above; LEVEL_3-capable mobile devices only.
- User-reported Samsung A35, One UI 8.5, Magisk, unlocked bootloader and unlocked
  camera library. Reported, not hardware/software query evidence.
- User's instruction to keep extensive Markdown in the project folder and
  maintain it as decisions/progress change.

## Official Android references inspected

| Source | Plan consequence |
| --- | --- |
| [Camera enumeration](https://developer.android.com/media/camera/camera2/camera-enumeration) | Enumerate real cameras/capabilities; do not hardcode lens IDs or defaults |
| [Camera2 Extensions API](https://developer.android.com/media/camera/camera2/extensions-api) | Query per-camera extension availability and configure real extension sessions |
| [HDR video capture](https://developer.android.com/media/camera/camera2/hdr-video-capture) | Camera dynamic-range support and encoder/color/output configuration must agree |
| [Multi-camera](https://developer.android.com/media/camera/camera2/multi-camera) | Logical/physical topology and valid combined outputs matter; OEM privilege is not public access |
| [CameraConstrainedHighSpeedCaptureSession](https://developer.android.com/reference/android/hardware/camera2/CameraConstrainedHighSpeedCaptureSession) | High-speed sessions have explicit size/range/request-list/surface restrictions |
| [DngCreator](https://developer.android.com/reference/android/hardware/camera2/DngCreator) | RAW_SENSOR data and corresponding result/characteristics produce DNG; close resources |
| [Compose system insets](https://developer.android.com/develop/ui/compose/system/insets) | Edge-to-edge does not remove the need to protect controls from system bars/cutouts |

The official docs link sample Camera2 extensions/video projects. Those sample
source trees were not inspected in this checkpoint, so no code-specific claim
about them is recorded as verified.

## OpenCode continuity guidance inspected

- [OpenCode V2 documentation index](https://opencode.ai/docs/llms.txt).
- [OpenCode V2 instructions](https://opencode.ai/docs/instructions/): root
  `AGENTS.md` provides project guidance; explicit instructions can ask the agent
  to read/update additional files.

An older `/docs/rules/` response was marked V1 and was not used as the authority
for V2-specific behavior. No OpenCode configuration or context-limit override was
created. Root instructions and explicit re-reading are the continuity mechanism.

## Community research limitation

DevRelay discovery returned no available connector namespace. Therefore no DEV
article, comment thread, community consensus or event match was verified or cited.
Do not invent those sources in a future summary.

## P-016 platform source inspection

- Installed SDK `sources/android-36.1/android/app/ContextImpl.java`, lines
  3284–3306: `getDisplay()` throws `UnsupportedOperationException` for a context
  not associated with a display; ordinary Application is not a visual context.
- `sources/android-36.1/android/view/SurfaceView.java`, lines 1386–1438:
  surface-created/changed callbacks run within a catch that logs
  "Exception configuring surface". A callback failure need not be a fatal crash.
- These sources were read locally during the P-016 investigation. They support
  the D-018 source correction, not an assertion that the modified phone emitted
  a particular exception. Original startup logcat was no longer in the capture.

## Future research record

## Build / preview sources inspected during implementation

- [AGP 8.13 release notes](https://developer.android.com/build/releases/past-releases/agp-8-13-0-release-notes):
  Gradle 8.13, API through 36.1, JDK minimum 17; 8.13.2 supports Kotlin 2.3.
- [Gradle 8.13 compatibility](https://docs.gradle.org/8.13/userguide/compatibility.html):
  JDK 21 is supported for execution.
- [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html):
  use 2.3.21 rather than initial 2.2.21 for the chosen AGP range.
- [Compose compiler setup](https://developer.android.com/develop/ui/compose/compiler)
  and [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping):
  compiler plugin matches Kotlin; newer Compose 1.12 requires API37/AGP9.
- [Camera2 preview](https://developer.android.com/media/camera/camera2/camera-preview):
  SurfaceView handles sensor/display rotation, but the app must fit aspect ratio;
  implement a DisplayListener for 180-degree rotation.
- Official Gradle 8.13 wrapper JAR/scripts were downloaded from the Gradle GitHub
  tag and checked against `services.gradle.org` checksums.
- GitHub repository API and `git ls-remote` showed the target was public and
  empty before publishing. Action v4 commit IDs were resolved by `git ls-remote`,
  not guessed. DevRelay discovery again returned zero tools.

### Future research policy

Before pinning build versions, inspect current official AGP/Gradle/JDK and
Kotlin/Compose compatibility references and record chosen versions, URLs and
successful build evidence in TECH-STACK. Before using an unfamiliar advanced
Camera2 key, inspect its public reference and actual advertised support.

Add sources only after inspection. Distinguish facts stated by documentation,
inferences/design decisions, user reports and measurements on the device.
