# Android development scaffold

This directory contains the single native Android app module for TASK-001A. It
currently launches a Compose development shell. Product functionality belongs
to later task packets; see `../ARCHITECTURE.md` for package boundaries.

Use a JDK 17 installation and Android SDK platform 37, then run:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.
