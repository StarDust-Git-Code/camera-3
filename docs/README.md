# Documentation index

Recorded: 2026-11-18. These documents are persistent project memory, not evidence
that the planned application has already been built.

## Minimal resume sequence

Read [HANDOFF.md](HANDOFF.md), [PROGRESS.md](PROGRESS.md),
[REQUIREMENTS.md](REQUIREMENTS.md) and [DECISIONS.md](DECISIONS.md). Then load only
the subsystem documents needed for the task. Read root
[AGENTS.md](../AGENTS.md) if it has not already been loaded.

## Canonical documents

| Document | Responsibility | Update when |
| --- | --- | --- |
| [REQUIREMENTS.md](REQUIREMENTS.md) | User constraints, scope and acceptance | The user changes or clarifies scope |
| [GOALS-AND-IDEAS.md](GOALS-AND-IDEAS.md) | Product goals, design ideas and deferred ideas | An idea is accepted, rejected or deferred |
| [TECH-STACK.md](TECH-STACK.md) | Required stack, proposed dependencies, version policy | Technology/version choices change |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Boundaries, state, data flow and resource ownership | A design changes or code establishes a boundary |
| [PROJECT-STRUCTURE.md](PROJECT-STRUCTURE.md) | Actual and proposed repository layout | Files/modules are introduced or moved |
| [UI-UX.md](UI-UX.md) | Reference interpretation and native layout behavior | Screens/interactions change |
| [CAMERA-CAPABILITIES.md](CAMERA-CAPABILITIES.md) | Detection, LEVEL_3 gate, controls and session rules | A capability is discovered, implemented or tested |
| [SETTINGS.md](SETTINGS.md) | Functional settings inventory and persistence | A setting is added, removed or validated |
| [LOGGING-AND-DIAGNOSTICS.md](LOGGING-AND-DIAGNOSTICS.md) | Runtime logging, diagnostics and privacy | Diagnostic behavior or evidence changes |
| [ROADMAP.md](ROADMAP.md) | Ordered implementation milestones and exit checks | Sequencing/scope changes or a milestone passes |
| [TESTING.md](TESTING.md) | Test strategy, device matrix and evidence requirements | A test is added, run or yields a result |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Host setup, device setup and repeatable commands | Tooling/setup/commands are verified |
| [PROBLEMS-AND-SOLUTIONS.md](PROBLEMS-AND-SOLUTIONS.md) | Observed issues, investigations and fixes | A real problem or attempted fix occurs |
| [DECISIONS.md](DECISIONS.md) | Accepted constraints and architecture decisions | An approach is chosen or superseded |
| [PROGRESS.md](PROGRESS.md) | Latest task status and historical evidence | Meaningful work or validation completes |
| [HANDOFF.md](HANDOFF.md) | Compact recovery point and next action | Work pauses or current state changes |
| [SOURCES.md](SOURCES.md) | Authoritative sources inspected and research limits | A relevant source is actually inspected |

## Evidence vocabulary

- **User-reported:** stated by the user, not independently checked.
- **Host-observed:** found through local tools or workspace inspection.
- **Planned:** intended design, not implemented.
- **Implemented:** source exists, with its paths recorded.
- **Build-verified:** the relevant build/check completed successfully.
- **Device-verified:** behavior was exercised on the identified physical device.
- **Unavailable:** hardware/API/session does not expose the feature.
- **Failed:** an attempted operation did not pass; include reproduction evidence.

Do not use an unchecked box, proposal or mockup as implementation evidence. Do not
copy full logs into every file; keep a short summary and link to an appropriate
artifact if one exists. Never invent an artifact path.

## Maintenance and context recovery

1. Write factual progress and failed attempts as work happens, not only at the
   end of a long session.
2. Keep the current handoff small. Move history into the appropriate record.
3. Store goals once in requirements; link to them from other documents.
4. Store a new architectural decision with an ID and explain alternatives.
5. When implementation diverges from a proposal, document the reason and actual
   paths before calling the change complete.
6. After a restart, verify current files and device connection. Historical host
   observations are not guarantees about the present environment.

No OpenCode configuration file or external documentation directory was created.
The continuity mechanism here is a root `AGENTS.md` plus explicit Markdown reads
and updates, not an automatic background synchronization service.

## Reference material

- [REFERENCE-PROJECTS.md](REFERENCE-PROJECTS.md) — open-source camera projects
  the user shared (OpenCineCam, CinemaCamera, PhotonCamera, Open Camera), with
  license implications and borrowable ideas. Not a canonical document.
