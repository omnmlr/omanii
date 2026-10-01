# TASK-001A acceptance evidence

Status: automated scaffold and physical Redmi 13 install/launch gates passed on 2026-10-01; independent Phase 2 audit verdict: PASS WITH FOLLOW-UP.

Scaffold HEAD reviewed: `74edbc718c4cf7f31f86cbf974c998ba41289385`.
PR: [#1 - chore: add shared Android build scaffold](https://github.com/omnmlr/omanii/pull/1), targeting `main`.

## Files created or changed

- Changed: `android/README.md`.
- Created: `android/.gitignore`, `android/.gitattributes`, `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle.properties`.
- Created: `android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/gradle-wrapper.jar`, `android/gradle/wrapper/gradle-wrapper.properties`.
- Created: `android/app/build.gradle.kts`, `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/values/strings.xml`, `android/app/src/main/java/com/omanii/app/MainActivity.kt`, `android/app/src/test/java/com/omanii/app/ScaffoldTest.kt`.
- Created: this report.
- No root build-support file or other canonical contract changed.

## Build contract

| Setting | Choice |
| --- | --- |
| Application ID and namespace | `com.omanii.app` |
| Modules | One `:app` module |
| Minimum SDK | 24 (Android 7.0) |
| Compile SDK | 37 |
| Target SDK | 36 |
| Android Gradle Plugin | 9.4.0 |
| Gradle wrapper | 9.6.1 binary distribution, SHA-256 pinned |
| Kotlin / Compose compiler plugin | 2.3.21; AGP built-in Kotlin, so no separate Kotlin Android plugin |
| Java source/target and build JDK | 17 |
| Compose BOM | 2026.09.00 |

Compile SDK 37 supports the current Compose library set; target SDK 36 avoids opting into Android 17 runtime changes before device testing. API 24 permits the later optional ARCore path without making AR a requirement. The app currently has only a launcher activity and a centered development label. No future architecture packages were created.

Official version and platform references checked before implementation:

- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes): API 37 maximum, Gradle 9.6.0 minimum, JDK 17.
- [AGP built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin): AGP 9 does not need `org.jetbrains.kotlin.android`.
- [Compose compiler setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler): Compose compiler plugin matches Kotlin; Compose libraries require current compile SDK.
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom): BOM 2026.09.00.
- [Activity releases](https://developer.android.com/jetpack/androidx/releases/activity): `activity-compose` 1.13.0.
- [ARCore optional-app SDK floor](https://developers.google.com/ar/develop/java/enable-arcore): optional AR is supported above API 19 and AR functionality requires API 24.
- [Gradle release checksums](https://gradle.org/release-checksums/): Gradle 9.6.1 distribution and wrapper checksums.

## Dependencies and permissions

- Runtime declarations: Compose BOM (version constraint), `androidx.activity:activity-compose:1.13.0` (activity/Compose bridge), and `androidx.compose.foundation:foundation` (minimal layout and text). The debug APK is 9,121,333 bytes. The transitive AndroidX graphics stack includes `libandroidx.graphics.path.so`; the build packaged it unchanged. These are maintained AndroidX libraries needed for the selected Compose shell; platform views would not meet ADR-001. No ARCore, network, persistence, DI, analytics, advertising, or billing library was added.
- Test-only declaration: JUnit 4.13.2 for one scaffold wiring test. It is not packaged as an app runtime dependency.
- Source manifest declares no `uses-permission`. The merged APK contains only `com.omanii.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a signature-level permission contributed by AndroidX Core for its receiver. No dangerous permission or first-launch permission request is configured. The `DUMP` permission on a transitive profile-installer receiver restricts callers; the app does not request it.
- `android:allowBackup="false"` is set in the source manifest. No secrets, network calls, user data collection, or tracking code exists.

## Verification

Executed from `android/` on Windows using `gradlew.bat` (the Windows equivalent of the task's `./gradlew` command):

```text
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
BUILD SUCCESSFUL in 14m 39s
51 actionable tasks: 51 executed
```

- Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk` exists, 9,121,333 bytes. `aapt dump badging` confirms package `com.omanii.app`, min SDK 24, target SDK 36, compile SDK 37, and launch activity `com.omanii.app.MainActivity`.
- Unit test: `ScaffoldTest` ran 1 test, 0 failures, 0 errors, 0 skipped (`android/app/build/test-results/testDebugUnitTest/TEST-com.omanii.app.ScaffoldTest.xml`).
- Lint: 0 errors and 4 warnings (`android/app/build/reports/lint-results-debug.txt`). Warnings concern target SDK 36 versus 37, newer Gradle availability, Android 12 backup-rule guidance, and the deliberately absent launcher icon. No lint check was disabled or weakened.
- APK permission inspection: `aapt dump permissions` found only the AndroidX signature-level receiver permission above.
- Wrapper JAR SHA-256: `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`, matching Gradle's published 9.6.1 checksum.
- Repository whitespace gate: `git diff --check` and `git diff --cached --check` passed with no whitespace errors. New files were marked intent-to-add so the requested check includes them; `android/gradlew` alone was staged to preserve its Unix executable bit.

## Independent review and limitations

The shell intentionally has no product functionality or custom launcher icon. The Android 12 backup-rule lint warning should be revisited before a task introduces sensitive persisted data; the current app stores none. Runtime behavior on Android 17 is not claimed because target SDK remains 36.

The independent Phase 2 launch audit reviewed the actual scaffold diff, existing build/test/lint artifacts and logs, dependency and manifest choices, and current official toolchain documentation. Verdict: **PASS WITH FOLLOW-UP**. No blocking scaffold implementation defect was found. The audit was read-only and did not rerun builds or physical tests; the Redmi 13 result below is founder-attested.

The audit identified an untracked local `android/gradle/gradle-daemon-jvm.properties` file selecting Java 25, while the documented scaffold build uses Java 17. This local-file issue was resolved outside the repository; the file is no longer present in the checkout. No tracked Android build file changed. The original Java 17 build evidence remains valid.

Follow-ups remain the backup/extraction-rule review before sensitive persistence, the documented target-SDK/launcher-icon limitations, and the Kotlin tested-matrix qualification noted in the independent audit. These do not establish a scaffold failure or validate future radio, ARCore or probe behavior.

Deviations from TASK-001A: none. Contracts changed: none. Privacy/security implication: no sensitive permission or collection was added; backup is disabled in the source manifest. Unresolved questions: no outstanding physical launch question; the non-blocking follow-ups above remain. Commit/PR reference: scaffold HEAD `74edbc718c4cf7f31f86cbf974c998ba41289385`; [PR #1](https://github.com/omnmlr/omanii/pull/1), targeting `main`.

## Physical-device verification

Date: 2026-10-01
Device: Redmi 13

- ADB device connection: PASS
- Debug APK installation: PASS
- Application launch: PASS
- Package: com.omanii.app
- Launch result: displayed "omanii development build"
- Immediate crash: none observed
- Unexpected permission prompt: none observed

TASK-001A physical-device acceptance gate: PASS.
