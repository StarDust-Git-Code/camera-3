# Technology stack

Last updated: 2026-11-18. Dependency pins and the Photo/RAW/MediaRecorder source
are build-verified. The app-owned GPU/MediaCodec/AudioRecord stack remains planned.

## Initial pinned build

- AGP 8.13.2, Gradle 8.13, Kotlin and Compose compiler plugin 2.3.21.
- Compose BOM 2025.11.01, Activity 1.11.0, Lifecycle 2.9.4, Core 1.17.0.
- Coroutines 1.10.2, DataStore 1.1.7, JUnit 4.13.2.
- `compileSdk = targetSdk = 36`, `minSdk = 35`; JVM bytecode target 17.
- JDK 21 for host/CI execution, compatible with Gradle 8.13.
- Wrapper JAR checked against Gradle's official SHA256; distribution SHA256 is
  pinned in `gradle/wrapper/gradle-wrapper.properties`.
- Deliberately retain an API-36-compatible Compose line; current Compose 1.12
  requires API 37 / AGP 9 and is not a blind upgrade for this scaffold.
- GitHub Actions is the user's requested APK compilation path (D-014).

The AGP, Kotlin plugin and BOM POM endpoints returned HTTP success; this is
dependency-existence evidence, not a passing application build.
Initial Kotlin 2.2.21 selection was corrected to 2.3.21 after inspecting Kotlin's
compatibility matrix: 2.2.21 fully supports AGP only through 8.11.1, while 2.3.21
covers AGP 8.13.2 and Gradle 8.13. See D-013 for the correction history.
The full pinned set subsequently compiled successfully at `9174a63`, with nine
passing model tests and no lint errors; newer-version warnings are recorded in
[TESTING.md](TESTING.md), not suppressed or treated as automatic upgrade orders.

## Required by the user

| Area | Required choice |
| --- | --- |
| Platform | Android mobile, Android 15+ (`minSdk = 35`) |
| Language | Kotlin |
| UI | Jetpack Compose |
| Camera | Public Android Camera2 APIs, LEVEL_3-capable device gate |
| Architecture | Modular MVVM |
| Concurrency | Kotlin Coroutines |

Do not substitute CameraX as the capture engine. The low-level Camera2 controls
are intentional in this project.

## Proposed implementation choices

| Area | Proposal | Reason / verification needed |
| --- | --- | --- |
| Build | Gradle wrapper, Android Gradle Plugin, Kotlin/Compose plugins, version catalog | Reproducible compatible versions; pin after checking official compatibility |
| Compile/target | API 36 initially, subject to verified tooling compatibility | Stable SDK installed locally; minimum remains API 35 |
| UI state | ViewModel, immutable state, StateFlow, lifecycle-aware collection | Survives UI recomposition without duplicating camera ownership |
| Dependency wiring | Explicit constructor injection initially | Small, understandable object graph without unnecessary framework overhead |
| Preview | SurfaceView through Compose AndroidView interop | Direct camera surface, low-copy preview; verify layering/orientation/HDR behavior |
| Photo output | ImageReader, JPEG and RAW_SENSOR, DngCreator, ExifInterface | Actual sensor output and corresponding metadata |
| Baseline video | MediaRecorder | Historical FHD/30 has partial device evidence; current source permits only capability-gated 1080p60 via regular or constrained high-speed Camera2 sessions |
| Advanced video | GPU surface processing, MediaCodec and MediaMuxer | Planned codec/profile control, LUT/Log/HDR and output inspection |
| Advanced audio | AudioRecord, AAC encoding and routed AudioTrack monitor | Planned stereo verification, explicit timestamps and Type-C IEM monitoring |
| Storage | MediaStore and Storage Access Framework | Scoped storage and user-selected destinations without broad storage access |
| Settings | Jetpack DataStore | Persisted typed preferences and safe schema evolution |
| Log storage | Bounded asynchronous rotating app-private files + in-memory viewer buffer | No unbounded memory growth or main-thread file writes |
| Sharing | Android Sharesheet and FileProvider | User-controlled export with temporary URI grants |
| Tests | JVM unit tests, Android instrumentation, Compose UI tests, lint | Logic, platform interactions and visual/semantic checks |

Implemented subset: SurfaceView, ViewModel/StateFlow, JPEG/RAW ImageReaders,
DngCreator/MediaStore, first-cut MediaRecorder with optional microphone audio,
DataStore, asynchronous bounded logging and FileProvider sharing. SAF, app-owned
GPU/codec/audio processing and instrumentation/advanced device tests remain open.

## Observed host tooling

- Windows workspace: `C:\Users\irsha\Desktop\camera3`.
- Java and javac on PATH: Eclipse Adoptium JDK 21.0.6.
- Android SDK: `C:\Users\irsha\AppData\Local\Android\Sdk`.
- Installed platforms include API 35, 36 and 36.1, plus older platforms.
- ADB is on PATH. Standalone Gradle, sdkmanager and kotlinc were not on PATH.
- SDK command-line tools are installed under the SDK despite sdkmanager not being
  directly on PATH. A Gradle distribution cache exists; it is not a selected
  project Gradle version.
- The workspace was empty at initial inspection; the current six-module project
  and its build evidence supersede that historical starting point.

## Version policy

1. Check Android Gradle Plugin, Gradle and JDK compatibility before scaffolding.
2. Check the current Kotlin/Compose plugin relationship and a compatible Compose
   BOM. Avoid guessing newest-version combinations.
3. Use a checked-in wrapper and a central version catalog.
4. Record exact chosen versions, source and successful command evidence here.
5. Do not upgrade several major dependencies while diagnosing an unrelated bug.
6. Keep machine-specific SDK paths in local configuration, not shared build logic.

## App privileges and distribution

Request camera permission and microphone permission only for the functionality
that needs them. The app can take photos and record silent video without a mic
grant. MediaStore-created media does not justify broad all-files permission.
No root shell, firmware modification, hidden API or network dependency is part
of this proposed stack. Release identity/signing remains open.
