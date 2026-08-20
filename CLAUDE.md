# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Go4Lunch (OpenClassrooms Project 7) — an Android app (Java) to help colleagues decide where to eat lunch. The project is in an early/scaffold stage: single `MainActivity`, no feature code yet beyond template boilerplate, with Firebase and Google Maps/Places dependencies wired in ahead of implementation.

## Language & code conventions

- **Java only** — no Kotlin (explicit requirement of this project).
- **All code must be in English**: variable names, method names, comments, commit messages. No French anywhere in the codebase or Git history.
- The build must compile with **zero warnings and zero errors**.

## Architecture

- **MVVM is mandatory**: View (Activity/Fragment) → ViewModel → Repository → data sources (Firebase, Retrofit for Places API).
- Reference projects to follow as style/pattern examples (not to copy verbatim):
  - [CatFactsGenerator](https://github.com/OpenClassrooms-Student-Center/CatFactsGenerator) — Retrofit usage pattern.
  - [FreelanceFinder](https://github.com/OpenClassrooms-Student-Center/FreelanceFinder) — Firestore usage pattern.

## Build & run

This is a standard single-module Gradle Android project (module: `app`, package `com.jeanotto.go4lunch`). Use the Gradle wrapper, not a global `gradle`.

```
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build and install on a connected device/emulator
./gradlew test                   # run JVM unit tests (app/src/test)
./gradlew connectedAndroidTest    # run instrumented tests on a device/emulator (app/src/androidTest)
./gradlew testDebugUnitTest --tests "com.jeanotto.go4lunch.ExampleUnitTest"   # run a single unit test class
./gradlew lint                   # Android lint
```

On Windows use `gradlew.bat` in place of `./gradlew` when running outside a POSIX shell.

- Java/Kotlin source/target compatibility: Java 11.
- `compileSdk`/`targetSdk` 37, `minSdk` 23.
- Version catalog for dependencies lives at `gradle/libs.versions.toml` — add new library versions/aliases there rather than hardcoding coordinates in `app/build.gradle.kts`, following the existing entries.

## Secrets / local configuration

- `MAPS_API_KEY` is defined in the (gitignored) root `local.properties` and injected into `AndroidManifest.xml` via the `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` (`${MAPS_API_KEY}` placeholder, `com.google.android.geo.API_KEY` meta-data). A fresh checkout needs this key added to `local.properties` before the app can use Maps/Places.
- Firebase is configured via `app/google-services.json` (Auth, Firestore, Cloud Messaging enabled through the Firebase BoM in `app/build.gradle.kts`). Note this file is currently committed to the repo even though it's listed in `.gitignore`.

## Testing

- Unit tests required with **Mockito**, targeting **~70% line coverage** on:
  - the ViewModel layer
  - the Repository layer
- No coverage requirement on Activities/Fragments/Adapters (UI layer).

## Green code constraint (critical, from client requirements)

- The restaurant **list** API call must run **exactly once**, at app startup.
- The restaurant **detail** API call must only run on demand (user opens a restaurant detail screen, or an autocomplete prediction is selected).
- Never poll or refetch the list on every screen navigation.

## Auth & platform constraints

- **Google Sign-In only** — no Facebook OAuth (Play Store policy conflict for this project).
- Support **Android 6.0 (API 23) and above**, portrait orientation only, all screen sizes.
- Release builds must be **obfuscated** (R8/ProGuard enabled for the `release` build type).
- Firestore security rules must restrict all reads/writes to **authenticated users only**.

## Localization

- App must support **French and English** at minimum (`values/` and `values-en/` or `values-fr/` resource sets).

## Git workflow

- Fine-grained, atomic commits with clear English messages.
- Hosted on GitHub, pushed regularly.

## Known TODOs

- **R8/ProGuard is currently disabled** for the `release` build type (`app/build.gradle.kts`, `optimization.enable = false`), even though release builds are required to be obfuscated (see Auth & platform constraints). This is intentional for now — re-enable it once core features are implemented and keep-rules for Firebase/Gson have been written, before final delivery.
- **No automatic overnight reset of restaurant interest data**: `restaurants/{placeId}.interestedUserIds` and `users/{uid}.todayChoicePlaceId` (written by `RestaurantRepository.setUserChoice`) have no date field, so a user's lunch choice persists until they explicitly pick another restaurant rather than resetting daily. To fix later via a scheduled Cloud Function or a date-based staleness check.
