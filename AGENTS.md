# Camera3 project instructions

## Read before working

1. Read [docs/HANDOFF.md](docs/HANDOFF.md) for current state and next actions.
2. Read [docs/PROGRESS.md](docs/PROGRESS.md) for completed work and evidence.
3. Read [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) for the user's constraints.
4. Read [docs/DECISIONS.md](docs/DECISIONS.md) before changing an approach.
5. Use [docs/README.md](docs/README.md) to find task-specific documentation. Load
   those files when needed; do not load the entire documentation set by default.

These files are the project's persistent working memory. Re-read the short
handoff after context compaction or a session restart. Documentation supplements
the source and test evidence; it does not establish that a feature works.

## Non-negotiable product constraints

- Native Android mobile camera app: Kotlin, Jetpack Compose, public Camera2 APIs.
- Android 15 and later only: `minSdk = 35`.
- LEVEL_3-only device requirement. See the startup gate and per-lens policy in
  [docs/CAMERA-CAPABILITIES.md](docs/CAMERA-CAPABILITIES.md).
- Main test device: user-reported Samsung Galaxy A35, One UI 8.5, Magisk,
  unlocked bootloader and modified/unlocked camera library. This environment is
  not independently verified yet. Do not infer the Android API from One UI.
- Hardware and session capabilities must drive available controls. No simulated
  camera preview, dummy settings, fake lens labels or fictional recording modes.
- Use the user's attached camera UI reference: minimalist black design adapted
  to a portrait, approximately 20:9 phone. The image is not yet a local asset.
- A working camera, media files, settings and diagnostics are required, not only
  a UI mockup. Do not label untested work complete.
- Root is part of the test environment, not an app requirement. Do not request
  root, change Magisk modules, flash firmware or modify camera libraries without
  a separate, explicit user request.

## Keep Markdown current

The user explicitly requested project-local Markdown for goals, ideas, plans,
stack, structure, decisions, problems, solutions and continuity.

After each meaningful requirement change, decision, implementation milestone,
bug investigation, build/test run or blocker change:

- Update the relevant canonical document.
- Update `docs/PROGRESS.md` with accurate status and supporting evidence.
- Update `docs/HANDOFF.md` with the current task, blockers and exact next action.
- Record architectural choices in `docs/DECISIONS.md`; do not erase superseded
  decisions. Record real issues and attempted fixes in
  `docs/PROBLEMS-AND-SOLUTIONS.md`.
- Before pausing, handing off or approaching a context limit, checkpoint the
  current state. Include changed paths, unverified work and commands to resume.
- Do not create filler files or repeat long sections across documents. Add a
  focused document when a new subsystem or substantial investigation needs one.
- Keep the handoff short enough to restore context cheaply, ideally under
  150 lines. Move older detail to the progress/decision/problem records.

Distinguish **user-reported**, **host-observed**, **planned**, **implemented**,
**build-verified** and **device-verified**. Never invent a command result, fix,
capability, file, measurement or successful test.

## Engineering and verification

- Inspect current files before editing; preserve work that is not yours.
- Use modular boundaries, MVVM, Coroutines and lifecycle-aware state collection.
- Serialize camera state mutations and handle stale asynchronous callbacks.
- Keep encoding, image processing, storage and log writes off the main thread.
- Revalidate settings when lens, mode, resolution, FPS or dynamic range changes.
- Close every owned session, image, reader, surface, codec, microphone and file.
- A capability flag is not proof of a working output, especially with a modified
  camera library. Check configuration, capture results and saved media.
- Validate changes incrementally. Follow [docs/TESTING.md](docs/TESTING.md).
- Gradle project/wrapper now exist. Use [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)
  and the GitHub Actions workflow for commands; report only actual run outcomes.
- No subagent delegation is authorized by this file; follow the current user's
  and harness's delegation rules.

## Documentation authority

Latest explicit user instructions override older project proposals. Harness
permissions and agent modes remain authoritative. If Plan mode is activated
again, respect its write restrictions instead of following this file's update
routine into a forbidden directory. Record a needed checkpoint when allowed.
