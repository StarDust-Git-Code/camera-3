# Development environment and commands

Last updated: 2026-11-18. Windows host; six-module application scaffold exists.
User requested GitHub Actions as the APK compilation path.

## Observed environment

- Workspace: `C:\Users\irsha\Desktop\camera3`.
- Workspace was empty before this documentation set; no Git repository detected
  by the supplied environment/current layout.
- JDK 21.0.6 from Eclipse Adoptium is on PATH (`java`, `javac`).
- Android SDK: `C:\Users\irsha\AppData\Local\Android\Sdk`.
- ADB: `C:\Users\irsha\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Platforms 35, 36 and 36.1 and relevant build tools are installed.
- SDK command-line tools are installed, but `sdkmanager` was not on PATH.
- Standalone `gradle` and `kotlinc` were not on PATH. This does not prevent a
  properly bootstrapped wrapper/Kotlin Gradle build.
- Wrapper, Android source and model unit tests now exist. No verified APK or
  release signing yet. Git is installed; `gh` is not on PATH. Git Credential
  Manager lists the repository-owner account, without exposing credentials.

Recheck these observations when resuming; do not assume the user's host is
unchanged.

## Device connection

The user needs to connect the A35, enable Developer options/USB debugging, and
accept this computer's debugging authorization. Cable, USB mode and Windows
driver may matter. Last recorded ADB checks found no device at all, not an
"unauthorized" entry.

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
app process or testing an output pipeline. No connected-device property/dumpsys
command has run yet.

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

No application build has been verified yet. The workflow uses Bash/Ubuntu to run
`:core:model:test testDebugUnitTest :app:lintDebug :app:assembleDebug` and uploads
`camera3-debug-apk` plus `verification-reports`. Local `local.properties` must
point to the SDK if local checks are needed; it is ignored and not published.

## GitHub Actions

- Remote: `https://github.com/StarDust-Git-Code/camera-3.git`, branch `main`.
- `.github/workflows/android.yml`: push/PR/manual dispatch, JDK21/SDK36,
  checksum-validated Gradle wrapper, tests/lint/APK, 14-day artifacts.
- Follow the run at `https://github.com/StarDust-Git-Code/camera-3/actions`.
- Success/download/install evidence must be recorded in TESTING/PROGRESS.
- Never publish local.properties, keystores, device identifiers or raw test media.
- Fresh CI debug signing keys are disposable; updates from different runs may
  require uninstall/reinstall. Stable signing is a separate future decision.

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
