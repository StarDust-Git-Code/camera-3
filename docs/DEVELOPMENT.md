# Development environment and commands

Last updated: 2026-11-18. Windows host; six-module application and repeatable
local/CI validation exist. GitHub Actions remains the publication build path.

## Observed environment

- Workspace: `C:\Users\irsha\Desktop\camera3`.
- Workspace was empty before this documentation set. Git now exists, with
  `origin/main` pointing to the user's Camera3 repository.
- JDK 21.0.6 from Eclipse Adoptium is on PATH (`java`, `javac`).
- Android SDK: `C:\Users\irsha\AppData\Local\Android\Sdk`.
- ADB: `C:\Users\irsha\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Platforms 35, 36 and 36.1 and relevant build tools are installed.
- SDK command-line tools are installed, but `sdkmanager` was not on PATH.
- Standalone `gradle` and `kotlinc` were not on PATH. This does not prevent a
  properly bootstrapped wrapper/Kotlin Gradle build.
- Wrapper, Android source, model/app unit tests and verified debug APKs exist;
  production release signing is not configured. Git is installed. `gh` is not
  globally on PATH, but a verified portable GitHub CLI is available in the
  approved temporary directory. Git Credential Manager supplies authentication
  without writing credentials into the project.

Recheck these observations when resuming; do not assume the user's host is
unchanged.

## Device connection

The A35 was previously connected and authorized. Baseline Photo passed; the
verified P-016 correction restored preview/session opening, and RAW/Video gained
partial runtime evidence. No phone access is authorized during the current
implementation phase; do not reconnect, install or send device input until the
user permits another physical test session.

Read-only PowerShell checks:

```powershell
adb devices -l
```

After an authorized device is present, query Android independently of One UI:

```powershell
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.model
```

With multiple devices, select explicitly using `adb -s <serial>` rather than
accidentally testing another target. Avoid copying serial numbers into shared
documents or exports.

Camera service diagnostics may provide useful context:

```powershell
adb shell dumpsys media.camera
```

This is not a replacement for querying CameraCharacteristics inside the actual
app process or testing an output pipeline. Saved baseline/P-016 reports under
ignored `app/build/ci/device-testing/` contain actual device queries; see TESTING.

## Application commands

The wrapper now exists. Run from the workspace or CI:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :core:model:test
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:installDebug
.\gradlew.bat connectedDebugAndroidTest
```

The workflow has passed repeatedly, most recently for published correction
`cc73c69` in run 44259956383. It uses Bash/Ubuntu to run
`:core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug` and uploads
`camera3-debug-apk` plus `verification-reports`. Local SDK location may come from
`ANDROID_HOME`/`ANDROID_SDK_ROOT` (both re-verified on this host) or ignored
`local.properties`; that file is currently absent and is not required here.

## GitHub Actions

- Remote: `https://github.com/StarDust-Git-Code/camera-3.git`, branch `main`.
- `.github/workflows/android.yml`: push/PR/manual dispatch, JDK21/SDK36,
  checksum-validated Gradle wrapper, tests/lint/APK, 14-day artifacts.
- Follow the run at `https://github.com/StarDust-Git-Code/camera-3/actions`.
- Success/download/install evidence must be recorded in TESTING/PROGRESS.
- Never publish local.properties, keystores, device identifiers or raw test media.
- First CI run lacked `sdkmanager` on PATH; explicitly set up Android command-line
  tools using the pinned setup-android action before installing SDK packages.
- Verified portable GitHub CLI is available in the approved temp directory (see
  HANDOFF); use Git Credential Manager via process memory, never source/token files.
- Fresh CI debug signing keys are disposable; updates from different runs may
  require uninstall/reinstall. Stable signing is a separate future decision.
- Verified APKs/reports are stored by run ID under ignored `app/build/ci/`;
  [TESTING.md](TESTING.md) is authoritative for paths, hashes and inspection.
- SDK build tools `36.0.0/apksigner.bat verify --verbose <apk>` and
  `36.0.0/aapt2.exe dump badging <apk>` passed. See TESTING for hash/metadata.
- CI annotations about deprecated action versions/Node20 and wrapper executable
  permissions are maintenance follow-ups, not a failed build (P-010).

## Repeatable workflow

1. Read the handoff and inspect current source/changes.
2. Choose a small roadmap exit criterion and record decisions as needed.
3. Implement and validate the relevant logic/build before installing.
4. Test on the explicitly selected phone; collect real capability/results/media
   evidence and record unsupported features.
5. Repair failures with focused changes rather than broad unproven upgrades.
6. Update canonical docs, progress and the compact handoff.

## Tooling setup constraints

Select compatible AGP/Gradle/JDK and Kotlin/Compose versions using official
references before creating the wrapper. A cached Gradle version is not a reason
to choose it blindly. Keep local SDK paths out of shared build files.

Do not modify the user's Magisk/camera module, flash the phone, install root
helpers, alter bootloader state or change system libraries as part of ordinary
app development. Ask separately if an actual device change is needed.

Do not run destructive cleanup of unrelated directories or discard user work.
Scaffold missing project files incrementally. No cloud deployment, account,
signing-key setup or app publication is authorized by this command list.
