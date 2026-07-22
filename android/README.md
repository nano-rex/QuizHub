# QuizHub Android

This is the Android companion app for QuizHub. It follows the Java, single-`app`-module layout used by the other nano-rex Android projects and reads the canonical JSON question banks from the repository root during the Gradle build.

## Build without Android Studio

From this directory, with a JDK, Android SDK, and Gradle available:

```sh
./gradlew :app:assembleDebug
```

The Gradle task copies active root-level files from `../question-banks/` into Android assets before compiling. Reference extractions are intentionally excluded from the APK.

The initial Android screen provides a random objective quiz from the shared banks. Settings, multilingual display, subjective answers, search, image support, and multi-step mathematics will be added incrementally while keeping the JSON schema shared with the web app.
