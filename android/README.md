# QuizHub Android

This is the Android companion app for QuizHub. It follows the Java, single-`app`-module layout used by the other nano-rex Android projects and reads the canonical JSON question banks from the repository root during the Gradle build.

## Build without Android Studio

From this directory, with a JDK, Android SDK, and Gradle available:

```sh
./gradlew :app:assembleDebug
```

The verified local build used JDK 17, Gradle 8.2, Android platform 34, and Android build-tools 34.0.0. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The Gradle task copies active root-level files from `../question-banks/` into Android assets before compiling. Reference extractions are intentionally excluded from the APK.

The Android screen currently provides a random objective quiz from the shared banks, plus Settings and Statistics activities. Question count, language, and dark-mode preferences are stored locally on the device. Completed quiz scores and subject/topic breakdowns are also stored locally; no account or network connection is required.
