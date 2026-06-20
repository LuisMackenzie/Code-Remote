# AGENTS.md

## Project shape

- Multi-module Android project under namespace `dev.mackenzie.coderemote`:
  - `:domain` — pure Kotlin/JVM module: domain models (`Session`, `Message`, `Part`, `SseEvent`, `ServerConfig`, `SessionStatus`, `ToolState`). No Android dependencies.
  - `:data` — pure Kotlin/JVM module: repository implementations (`ServerRepository`, `SettingsRepository`, `DraftRepository`, `EventReducer`), datasource interfaces (`OpenCodeApi`, `SseClient`, `PtySocket`), DTOs, and platform abstractions (`AppLogger`, `KeyValueStorage`, `FileStorage`, `ServerConnection`). No Android dependencies.
  - `:usecases` — pure Kotlin/JVM module: use cases grouped by feature under `usecases/{server,session,message,permission,question,provider,config,pty,file,project,draft}/`, each with `operator fun invoke()`.
  - `:app` — Android application module: Hilt DI, Compose UI, ViewModels, Ktor implementations (`KtorOpenCodeApi`, `KtorSseClient`, `KtorPtySocket`), storage implementations (`DataStoreKeyValueStorage`, `FileStorageImpl`, `AndroidLogger`), and `local/LocalServerManager.kt` (Termux orchestration).
- Toolchain from Gradle files: Gradle 8.6, Android Gradle Plugin 8.4.0, Kotlin 2.0.21, Java 17, KSP `2.0.21-1.0.28`, Hilt 2.51 (via KSP, not kapt).
- Main entrypoints: `OpenCodeApp` for Hilt, `MainActivity` for the single-activity Compose shell, `ui/navigation/NavGraph.kt` for routes, and `service/OpenCodeConnectionService.kt` for foreground SSE connections.
- Dependency direction: `:app` → `:usecases` → `:data` → `:domain`. Inner modules never import from outer modules.

## Android skills to prioritize

Use these skills first for Android work in this repository:

1. `ArquitectoMacK` — project-specific Android feature structure, MVVM Compose, Hilt, repositories, use cases, and naming conventions.
2. `devexperto` — Clean Architecture, SOLID, MVVM, dependency injection, testing, and feature implementation guidance.
3. `android-kotlin-core` — Kotlin idioms, nullability, sealed types, extension functions, and safe collection pipelines.
4. `kotlin-specialist` — advanced Kotlin, coroutines, Flow, Compose, Ktor, and type-safe DSL patterns.
5. `android-coroutines-flow` — structured concurrency, dispatchers, Flow pipelines, and cancellation-safe async work.
6. `mobile-android-design` — Material Design 3 and Jetpack Compose UI patterns.
7. `android-di-hilt` — Hilt modules, scopes, injection boundaries, and test overrides.
8. `android-testing-unit` — focused unit tests for reducers, use cases, repositories, and state holders.
9. `android-gradle-build-logic` — Gradle, Android plugin, version catalog, and toolchain compatibility work.
10. `android-networking-retrofit-okhttp` — networking contracts, interceptors, API resilience, and error handling.

## Commands agents usually need

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run local unit tests
./gradlew :app:testDebugUnitTest

# Run one local unit test class or method
./gradlew :app:testDebugUnitTest --tests 'dev.mackenzie.coderemote.ExampleTest'

# Android lint for debug work
./gradlew :app:lintDebug

# Device/emulator instrumentation tests only when a device is available
./gradlew :app:connectedDebugAndroidTest

# Install the debug APK after assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Source boundaries

- `:domain/model/` holds pure Kotlin data models and sealed types (only `kotlinx.serialization`).
- `:data/api/` owns datasource interfaces (`OpenCodeApi`, `SseClient`, `PtySocket`), `ServerConnection`, request/response DTOs, and SSE exceptions.
- `:data/repository/` owns app persistence, local runtime orchestration, and event reduction; uses `AppLogger`, `KeyValueStorage`, `FileStorage` abstractions instead of Android APIs.
- `:usecases/<feature>/` groups one class per operation, each with `operator fun invoke()`. Use cases wrap `OpenCodeApi` and repository calls.
- `:app/di/` provides Hilt modules: `NetworkModule` (@Provides for `HttpClient`, `Json`, `DataStore`) and `DataModule` (@Binds for `OpenCodeApi`, `SseClient`, `AppLogger`, `KeyValueStorage`, `FileStorage`). Ktor/storage implementations live here.
- `:app/local/LocalServerManager.kt` owns Termux/Intent/PackageManager orchestration (100% Android, cannot live in `:data`).
- `:app/ui/screens/` contains Compose screens; shared navigation stays in `ui/navigation/`.
- ViewModels that still inject `OpenCodeApi` directly (e.g. `ChatViewModel`): pending migration to use cases. New ViewModels should inject use cases only.

## Localization

- English source strings live in `app/src/main/res/values/strings.xml`.
- Translated `values-*` resources and `lokit.lock` are managed via `lokit.yaml`; keep them in sync when adding, renaming, or deleting source strings.
- Do not leave locale-only keys in translated XML. Stale locale-only keys have previously broken release lint.

## Local runtime script

- The Termux setup script is `scripts/opencode-local-setup.sh`.
- If that script changes, update `scripts/opencode-local-setup.sha256` in the same change.

## Release/signing notes

- Debug builds use application id suffix `.debug` and label `OC Remote Dev`.
- Release signing is optional and only activates when `app/keystore/signing.properties` exists; `app/keystore/` is gitignored and must not be committed.

## Repo facts

- No repo-local CI workflow or OpenCode config was present when this file was written; rely on the Gradle commands above for local verification.
