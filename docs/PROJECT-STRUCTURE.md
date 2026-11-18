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

The tree above records the documentation bootstrap. Android source, build files,
wrapper and unit tests now exist as described below. No APK has been verified yet.

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
and module-specific packages. Model policy tests live in `core/model/src/test/`.
`.github/workflows/android.yml` is the APK CI entry point. Instrumentation tests
are not implemented yet. Machine-local configuration/build outputs are ignored.

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

Future build outputs belong to Gradle build directories. Future diagnostic/test
artifacts need named locations and privacy review; none exist now. Avoid storing
APK binaries, raw camera images, device serials, signing keys or large log dumps
inside Markdown. Link only to real, reviewed artifacts.
