# Project structure

Last updated: 2026-11-18. Keep actual layout separate from proposed layout.

## Actual layout at this checkpoint

```text
camera3/
  AGENTS.md
  README.md
  docs/
    README.md
    REQUIREMENTS.md
    GOALS-AND-IDEAS.md
    TECH-STACK.md
    ARCHITECTURE.md
    PROJECT-STRUCTURE.md
    UI-UX.md
    CAMERA-CAPABILITIES.md
    SETTINGS.md
    LOGGING-AND-DIAGNOSTICS.md
    ROADMAP.md
    TESTING.md
    DEVELOPMENT.md
    PROBLEMS-AND-SOLUTIONS.md
    DECISIONS.md
    PROGRESS.md
    HANDOFF.md
    SOURCES.md
```

The tree above records the documentation set. Android source, build files,
wrapper and tests exist as described below. CI downloads and device-test evidence
are kept under ignored `app/build/ci/`; exact run directories, hashes and limits
are recorded in [TESTING.md](TESTING.md). Generated APKs are not committed.

## Implemented Android layout

```text
camera3/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradlew
  gradlew.bat
  gradle/
    libs.versions.toml
    wrapper/
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/kotlin/dev/camera3/app/
      Camera3Application.kt
      CameraViewModel.kt
      MainActivity.kt
      ui/
    src/main/res/
  core/
    model/
    camera/
    media/
    settings/
    diagnostics/
  docs/
```

All six modules have build files. Core source uses `src/main/kotlin/dev/camera3/`
and module-specific packages. Model policy tests live in `core/model/src/test/`;
app state tests live in `app/src/test/`. `.github/workflows/android.yml` is the
APK CI entry point. Instrumentation tests are not implemented. Machine-local
configuration/build outputs are ignored.

## Source organization rules

- Camera API ownership stays in `core/camera`, not scattered among screens.
- Encoding and saving stay in `core/media`.
- Shared models should not import Activity or Compose UI types.
- Settings UI belongs to the app; persistence/validation belongs to core settings.
- Log UI belongs to the app; log storage/reporting belongs to core diagnostics.
- Add tests next to the module whose behavior they cover.
- Keep reusable theme/control primitives separate from hardware logic.

## Documentation growth

For a substantial later investigation, add a focused file such as
`docs/investigations/<issue-id>-<topic>.md` and link it from the problem ledger.
Do not create this directory or placeholder files until there is real content.

When code is present, replace proposed paths with verified actual paths. Preserve
the reason for changes in [DECISIONS.md](DECISIONS.md).

## Artifacts and local state

Build outputs belong to Gradle build directories. Downloaded CI and device-test
artifacts use ignored named directories under `app/build/ci/` and require privacy
review. Avoid storing APK binaries, raw camera images, device serials, signing
keys or large log dumps inside Markdown. Link only to real, reviewed artifacts.
