# ADR-001 - Android-native stack

**Status:** Accepted for V1 bootstrap

## Context

omanii's first differentiating workflow depends on Android-specific capabilities: foreground camera/AR pose, network callbacks, connected Wi-Fi/radio context, telephony observations, explicit permission states, monotonic timing, lifecycle cancellation and later usage statistics. The product also needs a highly responsive consumer UI, but the difficult part is truthful platform integration rather than cross-platform screen reuse.

The research direction is Android first, Kotlin, Jetpack Compose and ARCore where supported. Basic connectivity testing must still work when ARCore is unavailable.

## Decision

Use native Kotlin for the Android client and Jetpack Compose for consumer UI. Use ARCore only as an optional runtime pose provider for supported spatial sessions.

Prefer one initial Android app module with clear packages. Add Gradle modules only when a measured build/test/ownership benefit appears.

The cross-platform boundary is the language-neutral measurement/protocol semantics, not a UI framework abstraction.

## Consequences

- Direct access to Android lifecycle, connectivity, telephony and permission behavior.
- Easier use of official Android/ARCore samples/documentation when platform behavior is uncertain.
- Less plugin/bridge uncertainty in the measurement-critical path.
- Android-specific code will need a future iOS reimplementation rather than shared UI code.
- ARCore support remains capability-gated; unsupported phones retain non-AR paths.

## Alternatives considered

### Flutter

Viable for broader cross-platform products, but the scanner, telephony, permissions and usage functions would still need native Android integration. The additional bridge/plugin layer is not justified for the first evidence gate.

### React Native

Similar native burden, with additional JavaScript/bridge/runtime considerations around timing-heavy foreground work. Familiarity alone is not enough reason to move the measurement-critical path away from native Android.

### Unity

Suitable for genuinely complex game-like 3D experiences, but too much bundle/lifecycle/platform-integration cost for the first consumer utility. The desired sci-fi feel does not require a game engine.

None of these frameworks is rejected generally. The decision is V1-specific.

## Explicitly undecided

- final renderer for the polished scanner;
- whether a later iOS client uses native SwiftUI or another approach;
- whether profiling later justifies C/C++ or specialized graphics code;
- whether future code sharing is worthwhile outside protocol/test fixtures.
