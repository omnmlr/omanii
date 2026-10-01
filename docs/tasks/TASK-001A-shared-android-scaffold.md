# TASK 001A — Shared Android Build Scaffold

**Status:** READY after Bootstrap Task 001 merges
**Type:** Integration foundation

## Objective

Create exactly one minimal native Android project that Tasks 002, 003 and the Android portion of Task 004 can share.

This task exists to prevent parallel agents from independently creating incompatible Gradle, manifest, package and Compose scaffolds.

It must create infrastructure only. No omanii product feature may be implemented.

## Product reason

Tasks 002 and 003 both require an Android application foundation. A single integration-owned scaffold prevents immediate build-system and manifest conflicts while preserving parallel feature development.

## Dependencies

- Bootstrap Task 001 approved and merged to `main`.
- ADR-001 accepted.
- `AGENTS.md` and `ARCHITECTURE.md` canonical.

## Owned paths

- `android/**`
- root build-support files only where explicitly required for the Android project
- `docs/evidence/TASK-001A/**`

No protocol, score, product or ADR semantics may be changed.

## Required context

Read:

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/adr/ADR-001-android-native-stack.md`
- this task packet

## Implementation requirements

Create the smallest conventional Android project supporting subsequent work.

Use:

- Kotlin
- Jetpack Compose
- Gradle wrapper
- one initial `app` module
- standard Android source/test layout
- package boundaries compatible with the architecture
- unit-test wiring
- lint wiring

Choose and document:

- application/package ID
- minimum SDK
- target/compile SDK
- Java/Kotlin toolchain versions
- Compose configuration

The choices should favor broad realistic device support while remaining compatible with the Android/ARCore work planned in Tasks 002 and 003.

Create only a minimal development shell sufficient to prove that the app launches.

Do not add ARCore in this task unless it is strictly required merely to establish the shared build contract. Task 003 owns AR functionality.

Do not add networking libraries unless they are strictly required for the scaffold. Task 004 owns active measurement networking.

## Acceptance criteria

1. Android project opens/builds successfully.
2. Debug APK assembles.
3. Unit-test task passes.
4. Android lint passes.
5. App launches to a minimal blank/development Compose shell.
6. No omanii measurement, scanner, radio, probe, score, Gaming, Boost, Premium, data-usage or consumer feature exists.
7. No unnecessary sensitive permission exists.
8. No unnecessary runtime dependency exists.
9. Tasks 002 and 003 can branch from this commit without needing to recreate the Android project.

## Verification commands

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug


```

Also run from repository root:

```bash
git diff --check
```

## Automated tests

Only the minimal generated/scaffold tests necessary to prove build/test wiring.

Do not manufacture feature tests for functionality that does not exist.

## Physical-device evidence

Install and launch the debug APK on Francis's Android phone.

Evidence only needs to prove:

- installation succeeds;
- app launches;
- no immediate crash;
- no unexpected permission request appears.

## Security / privacy

- No secrets.
- No analytics.
- No advertising SDKs.
- No sensitive permissions.
- No user/network data collection.

## Out of scope

- network/radio logging
- ARCore pose implementation
- measurement probes
- scanner UI
- Omanii Score
- OG Score
- diagnostics
- Boost
- data usage
- Premium
- ads
- website
- backend feature implementation

## Stop / escalation conditions

Stop if scaffold choices require:

- changing ADR-001;
- adding an unexpected sensitive permission;
- adding a major framework;
- introducing multi-module complexity without evidence;
- changing canonical product/protocol semantics.

## Handoff

Report:

- files created/changed
- package/application ID
- SDK/toolchain versions
- dependencies
- permissions
- verification commands/results
- physical launch result
- known limitations
- commit reference

## Recommended agent role

Android build/integration implementer.

A different agent should review Gradle, dependency and manifest choices before Tasks 002 and 003 branch.
