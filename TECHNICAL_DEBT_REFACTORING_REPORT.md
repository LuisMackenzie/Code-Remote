# Technical Debt & Refactoring Report

**Project**: Code Remote (OC Remote) — `dev.mackenzie.coderemote`  
**Date**: 2026-06-19  
**Module**: Single `:app` module  
**Stack**: Kotlin 2.0.21, AGP 8.4.0, Compose, Hilt, Ktor, Coroutines/Flow  

---

## 1. Executive Summary

The app is a functional, feature-rich OpenCode mobile client with SSE streaming, multi-server support, terminal emulation, and rich chat UI. However, it carries **significant structural debt** that will slow future development and increase regression risk:

- **Zero automated tests** — no unit, integration, or UI tests exist.
- **Single monolithic module** — all code lives in `:app` with no Clean Architecture boundaries.
- **God Objects** — `ChatScreen.kt` (6,973 lines), `OpenCodeApi.kt` (1,146 lines), `ChatViewModel.kt` (1,263 lines), `OpenCodeConnectionService.kt` (906 lines).
- **No DataSource interfaces or Use Cases** — ViewModels call the API client directly, violating SRP and DIP.
- **Credentials in navigation arguments** — passwords are URL-encoded into Compose Navigation routes.
- **No domain error model** — exceptions are caught ad-hoc with `Log.e` and swallowed.

The highest-impact refactors are: (1) introducing tests around the EventReducer and critical ViewModel logic, (2) extracting DataSource interfaces to enable testability, and (3) breaking up the ChatScreen mega-composable.

---

## 2. Methodology

### Areas Inspected

| Area | Files Inspected |
|------|----------------|
| Build & tooling | `build.gradle.kts` (root + app), `settings.gradle.kts`, `gradle.properties` |
| DI | `di/NetworkModule.kt` |
| API / Networking | `data/api/OpenCodeApi.kt`, `data/api/SseClient.kt` |
| Repositories | `data/repository/ServerRepository.kt`, `SettingsRepository.kt`, `DraftRepository.kt`, `EventReducer.kt`, `LocalServerManager.kt` |
| Domain models | `domain/model/SseEvent.kt`, `Message.kt`, `Part.kt`, `Session.kt`, `SessionStatus.kt`, `ServerConfig.kt`, `ToolState.kt` |
| Service | `service/OpenCodeConnectionService.kt` |
| ViewModels | `ChatViewModel.kt`, `HomeViewModel.kt`, `SessionListViewModel.kt`, `SettingsViewModel.kt`, `ServerSettingsViewModel.kt` |
| UI / Compose | `ChatScreen.kt`, `HomeScreen.kt`, `SessionListScreen.kt`, `SettingsScreen.kt`, `ServerSettingsScreen.kt`, `ServerProvidersScreen.kt`, `ServerModelFilterScreen.kt`, `WebViewScreen.kt`, `AboutScreen.kt`, `TerminalEmulator.kt`, `ServerTerminalWorkspace.kt` |
| Navigation | `ui/navigation/NavGraph.kt`, `ui/navigation/Screen.kt` |
| App entry | `OpenCodeApp.kt`, `MainActivity.kt` |
| Tests | `app/src/test/`, `app/src/androidTest/` — **both empty** |

### Architecture Standards Applied

- **ArquitectoMacK** — Module ownership, DataSource interfaces, Use Cases, Route/Content separation, `collectAsStateWithLifecycle`, nested `UiState`, error sealed interface.
- **devexperto** — Clean Architecture layers, SOLID principles, MVVM UDF, DI module boundaries, testing strategy.
- **AGENTS.md** — Project-specific source boundaries (`data/api/`, `data/repository/`, `domain/model/`, `ui/screens/`, `di/`).

---

## 3. Prioritized Technical Debt List

### 🔴 CRITICAL — Blocks Testability & Correctness

#### TD-01: Zero Automated Tests

| | |
|---|---|
| **Evidence** | `app/src/test/` and `app/src/androidTest/` are empty. No `.kt` test files exist anywhere in the project. |
| **Why it matters** | Every refactor is a blind leap. The EventReducer alone has 20+ event handlers with non-trivial state mutation logic. Regressions are caught only at runtime. This violates the most basic quality engineering practice. |
| **Proposed refactor** | Introduce test infrastructure: JUnit 4 + kotlinx-coroutines-test + Turbine + MockK. Start with the highest-value targets: EventReducer, ChatViewModel state derivation, SseClient.parseEventByType, ServerRepository CRUD. |
| **First slice** | Add `EventReducerTest` covering `handleSessionCreated`, `handleSessionUpdated`, `handleSessionDeleted`, `handleMessagePartDelta`, and `clearForServer`. |
| **Risk/Effort** | Low risk, medium effort (2-3 days for initial infrastructure + first batch). |

---

#### TD-02: No DataSource Interfaces — ViewModels Depend Directly on API Client

| | |
|---|---|
| **Evidence** | `ChatViewModel` injects `OpenCodeApi` directly (line 95). `SessionListViewModel` injects `OpenCodeApi` directly (line 10). `ServerSettingsViewModel` injects `OpenCodeApi` directly (line 82). `HomeViewModel` injects `OpenCodeApi` directly (line 85). |
| **Why it matters** | Violates DIP (SOLID). ViewModels cannot be unit-tested without mocking the entire HTTP client. ArquitectoMacK requires DataSource interfaces in the data layer with implementations bound via Hilt `@Binds`. AGENTS.md states `data/api/` owns HTTP access — ViewModels should never import from it. |
| **Proposed refactor** | (A) Create `SessionRemoteDataSource` interface with methods like `listSessions()`, `listMessages()`, `promptAsync()`, etc. (B) Make `OpenCodeApi` implement it (or wrap it). (C) Inject the interface into ViewModels. (D) Bind via Hilt `@Binds`. |
| **First slice** | Extract `SessionRemoteDataSource` interface for the 5 API methods used by `SessionListViewModel`, inject the interface instead of `OpenCodeApi`. |
| **Risk/Effort** | Medium risk (interface extraction is mechanical), high effort (many call sites across 4+ ViewModels). |

---

#### TD-03: No Use Cases — ViewModels Contain Business Logic

| | |
|---|---|
| **Evidence** | `ChatViewModel` contains: message pagination logic (lines 442-497), model resolution from history (lines 284-302), agent resolution (lines 305-313), cost/token aggregation (lines 320-329), file search with debounce (lines 634-671), draft persistence (lines 694-733), export with notifications (lines 924-980), revert/undo logic (lines 983-1046). |
| **Why it matters** | Violates SRP. The ViewModel has 15+ responsibilities. Business logic is untestable in isolation. ArquitectoMacK and devexperto both require Use Cases as the single-responsibility bridge between ViewModels and Repositories. |
| **Proposed refactor** | Extract Use Cases: `LoadMessagesUseCase`, `SendMessageUseCase`, `ResolveModelUseCase`, `ExportSessionUseCase`, `RevertMessageUseCase`, `SearchFilesUseCase`. Each is a single class with `operator fun invoke()`. |
| **First slice** | Extract `LoadMessagesUseCase` with pagination + OOM retry logic from `ChatViewModel.loadMessages()`. |
| **Risk/Effort** | Low risk per Use Case, high cumulative effort (5-8 Use Cases needed). |

---

#### TD-04: Credentials Passed via Navigation Arguments

| | |
|---|---|
| **Evidence** | `Screen.Chat.createRoute()` encodes `password` as a URL query parameter (`Screen.kt:56`). `NavGraph.kt` decodes it at lines 467-471. Passwords appear in navigation backstack, log output, and potentially in saved instance state. |
| **Why it matters** | Security risk. Passwords in navigation args are visible in: (1) `NavController` backstack dumps, (2) process death restoration bundles, (3) any crash reporting that captures navigation state. This is a CWE-532 (Insertion of Sensitive Information into Log File) adjacent issue. |
| **Proposed refactor** | Store active `ServerConnection` objects in a scoped Hilt `@ActivityRetained` component or a simple in-memory registry keyed by `serverId`. Navigation routes carry only `serverId` + `sessionId`. ViewModels resolve the connection from the registry. |
| **First slice** | Create `ServerConnectionRegistry` (singleton), register connections on `connect()`, and have `ChatViewModel` look up by `serverId` instead of decoding from SavedStateHandle. |
| **Risk/Effort** | Medium risk (navigation flow changes), medium effort (2-3 days). |

---

### 🟠 HIGH — Architectural Erosion

#### TD-05: ChatScreen Mega-Composable (6,973 lines)

| | |
|---|---|
| **Evidence** | `ui/screens/chat/ChatScreen.kt` is 6,973 lines — likely the largest single file in the project. |
| **Why it matters** | Violates SRP and the ArquitectoMacK Route/Content separation. The file likely contains: input handling, message rendering, tool card rendering, permission dialogs, question dialogs, model selector, agent selector, terminal UI, file mention autocomplete, markdown rendering, and more. This makes any UI change risky and prevents preview/testability. |
| **Proposed refactor** | Decompose into: `ChatRoute` (state collection), `ChatContent` (layout), `ChatInputBar`, `ChatMessageList`, `ChatMessageBubble`, `ToolCard`, `PermissionDialog`, `QuestionDialog`, `ModelSelector`, `AgentSelector`, `FileMentionPopup`. Each in its own file under `ui/screens/chat/ui/`. |
| **First slice** | Extract `ChatInputBar` composable (text field + attachment chips + send button) into its own file. |
| **Risk/Effort** | Low risk per extraction, high cumulative effort (week+). |

---

#### TD-06: OpenCodeApi God Object (1,146 lines, 40+ methods)

| | |
|---|---|
| **Evidence** | `data/api/OpenCodeApi.kt` contains: REST client (40+ suspend functions), WebSocket handling (`PtySocket`), DTO definitions (30+ `@Serializable` data classes), PTY response parsing with fallback heuristics, streaming export with raw OkHttp, and Base64 auth encoding. |
| **Why it matters** | Violates SRP and ISP. The class has too many reasons to change. DTOs mixed with API logic violate the ArquitectoMacK rule that server models live in `data/server/models/`. The inline OkHttp client creation in `exportSessionToStream` (line 475) bypasses the DI-managed Ktor client. |
| **Proposed refactor** | (A) Move all DTOs to `data/api/dto/` or `data/api/models/`. (B) Split API into focused interfaces: `SessionApi`, `ProviderApi`, `PtyApi`, `FileApi`, `PermissionApi`. (C) Move `PtySocket` to its own file. (D) Use the shared Ktor client for streaming export. |
| **First slice** | Move all `@Serializable` DTOs out of `OpenCodeApi.kt` into `data/api/dto/ApiDtos.kt`. |
| **Risk/Effort** | Low risk (file moves), medium effort. |

---

#### TD-07: OpenCodeConnectionService God Object (906 lines)

| | |
|---|---|
| **Evidence** | `service/OpenCodeConnectionService.kt` handles: SSE connection management, auto-reconnect with backoff, WakeLock management, notification channels (4 channels), persistent notification, event notifications (task complete, permission, question, error), notification grouping, deep-link intent building, locale configuration, auto-connect, and local server detection. |
| **Why it matters** | Violates SRP. The service is untestable. Notification logic alone is 300+ lines that should be in a dedicated `NotificationHelper`. SSE reconnection logic should be in a `SseConnectionManager`. |
| **Proposed refactor** | Extract: (A) `NotificationHelper` for all notification building/posting. (B) `SseConnectionManager` for reconnect logic. (C) `ServerConnectionRegistry` for tracking active connections. The service becomes a thin orchestrator. |
| **First slice** | Extract `NotificationHelper` class with all `show*Notification()` and `create*Notification()` methods. |
| **Risk/Effort** | Medium risk (service lifecycle is delicate), medium effort. |

---

#### TD-08: No Domain Error Model

| | |
|---|---|
| **Evidence** | `OpenCodeApi.promptAsync()` throws `RuntimeException` (line 538). `SseClient` throws `SseAuthException` and `SseConnectionException` (custom, non-domain). ViewModels catch `Exception` generically: `catch (e: Exception) { Log.e(TAG, "...", e) }`. No sealed `Error` interface exists in `domain/model/`. |
| **Why it matters** | ArquitectoMacK mandates `sealed interface Error { Server, Connectivity, Unknown }` in the domain layer. Without it, error handling is inconsistent, UI cannot render meaningful error states, and error mapping is scattered across ViewModels. |
| **Proposed refactor** | (A) Create `domain/model/Error.kt` with the sealed interface. (B) Create `data/extensions.kt` with `Throwable.toError()`, `tryCall`, `tryGet`. (C) Have DataSources return `Either<Error, T>` or `Error?`. (D) ViewModels display `Error` in `UiState`. |
| **First slice** | Create `Error.kt` and `extensions.kt`, then convert `ServerRepository.checkHealth()` to return `Error?` instead of `Result`. |
| **Risk/Effort** | Low risk, medium effort (gradual migration). |

---

### 🟡 MEDIUM — Maintainability & Scalability

#### TD-09: EventReducer as Global Mutable Singleton

| | |
|---|---|
| **Evidence** | `EventReducer` is `@Singleton` with 10 `MutableStateFlow` fields (lines 33-64), mutated by `processEvent()` from the service. `clearAll()` directly sets `.value` (lines 388-400) instead of using `.update {}`. |
| **Why it matters** | The reducer is a global mutable state container. Any component can read/write. Race conditions are possible if events arrive from multiple servers simultaneously on different coroutines. The `.value =` assignments in `clearAll()` bypass the atomic `update {}` and can lose concurrent mutations. |
| **Proposed refactor** | (A) Replace `.value =` with `.update {}` in `clearAll()` and `handleVcsBranchUpdated`/`handleProjectUpdated`. (B) Consider scoping state per-server instead of globally. (C) Add thread-safety documentation. |
| **First slice** | Replace all `.value =` assignments with `.update { }` for atomicity. |
| **Risk/Effort** | Low risk, low effort. |

---

#### TD-10: SettingsRepository Monolith (513 lines, 30+ settings)

| | |
|---|---|
| **Evidence** | `SettingsRepository.kt` has 30+ preference keys, 30+ `Flow` properties, and 30+ setter methods. Every new setting adds ~15 lines of boilerplate. |
| **Why it matters** | Violates SRP and OCP. The class grows linearly with every feature flag. `SettingsViewModel` mirrors it with 30+ `stateIn()` calls and 30+ setter methods. |
| **Proposed refactor** | (A) Group settings into domain-specific repositories: `ChatSettings`, `NotificationSettings`, `LocalServerSettings`, `AppearanceSettings`. (B) Or use a code-generated settings DSL. (C) At minimum, extract `LocalServerSettings` since it's a cohesive group of 8+ settings. |
| **First slice** | Extract `LocalServerSettings` class with the 8 local-server-related preferences. |
| **Risk/Effort** | Low risk, medium effort. |

---

#### TD-11: Navigation Routes Use String Concatenation Instead of Type-Safe API

| | |
|---|---|
| **Evidence** | `Screen.kt` builds routes via string concatenation with manual `URLEncoder.encode()`. `NavGraph.kt` decodes with `URLDecoder.decode()` at every composable destination. The `server_settings`, `server_providers`, and `server_model_filter` routes are defined inline in `NavGraph.kt` (lines 268-338) instead of using `Screen.*` sealed class routes. |
| **Why it matters** | Fragile — a missing encode/decode causes crashes with special characters in URLs or passwords. Compose Navigation 2.7+ supports type-safe navigation with `@Serializable` routes, which eliminates this entire class of bugs. |
| **Proposed refactor** | Migrate to type-safe navigation using `kotlinx.serialization` and `composable<Screen.Chat>` syntax. This eliminates manual encoding/decoding entirely. |
| **First slice** | Migrate `Screen.Home`, `Screen.Settings`, and `Screen.About` (parameterless routes) to type-safe API as proof of concept. |
| **Risk/Effort** | Medium risk (navigation changes need careful testing), medium effort. |

---

#### TD-12: No Route/Content Separation in Composables

| | |
|---|---|
| **Evidence** | `ChatScreen`, `HomeScreen`, `SessionListScreen`, `SettingsScreen` — none follow the ArquitectoMacK pattern of `*Route` (ViewModel injection + `collectAsStateWithLifecycle`) + `*Content` (pure state → UI function with `@Preview`). |
| **Why it matters** | Without separation: (A) Composables cannot be previewed in Android Studio without a running ViewModel. (B) UI testing requires Hilt test infrastructure. (C) State collection and UI rendering are coupled. |
| **Proposed refactor** | For each screen, create: `XxxRoute` (injects ViewModel, collects state) and `XxxContent` (pure `@Composable` with `@Preview`). |
| **First slice** | Refactor `SettingsScreen` into `SettingsRoute` + `SettingsContent` (it's the simplest screen). |
| **Risk/Effort** | Low risk, low effort per screen. |

---

#### TD-13: `collectAsState()` Instead of `collectAsStateWithLifecycle()`

| | |
|---|---|
| **Evidence** | `MainActivity.kt` line 19: `import androidx.compose.runtime.collectAsState`. ArquitectoMacK rule: "NEVER use `collectAsState()` — always `collectAsStateWithLifecycle()`." |
| **Why it matters** | `collectAsState()` continues collecting when the activity is in the background, wasting resources and potentially causing crashes from UI updates on stopped activities. `collectAsStateWithLifecycle()` is lifecycle-aware and pauses collection when the activity is not STARTED. |
| **Proposed refactor** | Replace all `collectAsState()` imports/usages with `collectAsStateWithLifecycle()` from `androidx.lifecycle.runtime.compose`. |
| **First slice** | Fix `MainActivity.kt` and add `androidx.lifecycle:lifecycle-runtime-compose` dependency if not already present. |
| **Risk/Effort** | Very low risk, very low effort. |

---

#### TD-14: Hardcoded Dispatchers

| | |
|---|---|
| **Evidence** | `OpenCodeApi.exportSessionToStream()` line 484: `withContext(Dispatchers.IO)`. `ChatViewModel.exportSession()` line 925: `launch(Dispatchers.IO)`. `OpenCodeConnectionService` line 108: `CoroutineScope(SupervisorJob() + Dispatchers.IO)`. |
| **Why it matters** | Hardcoded dispatchers prevent test substitution. The android-coroutines-flow skill recommends injecting dispatchers via constructor. `Dispatchers.IO` in production is fine, but tests need `StandardTestDispatcher`. |
| **Proposed refactor** | Add `@Inject` dispatcher parameters (with `@dagger.hilt.android.qualifiers.ApplicationContext` or a custom `@IoDispatcher` qualifier) to classes that need IO work. |
| **First slice** | Add `ioDispatcher: CoroutineDispatcher = Dispatchers.IO` parameter to `ChatViewModel` constructor (with a Hilt `@Provides` for the default). |
| **Risk/Effort** | Low risk, low effort. |

---

#### TD-15: No Version Catalog — Dependencies Hardcoded

| | |
|---|---|
| **Evidence** | `app/build.gradle.kts` has 40+ dependency declarations with inline version strings: `"io.ktor:ktor-client-core:2.3.11"`, `"com.google.dagger:hilt-android:2.51"`, etc. No `libs.versions.toml`, no `buildSrc`. |
| **Why it matters** | Version updates require searching through the build file. No centralized version management. ArquitectoMacK specifies `buildSrc/` with `Libs.kt` for coordinates. Modern Android projects use `gradle/libs.versions.toml`. |
| **Proposed refactor** | Create `gradle/libs.versions.toml` with all versions, libraries, and plugins. Migrate `build.gradle.kts` to use `libs.*` accessors. |
| **First slice** | Create `libs.versions.toml` with the Ktor, Hilt, and Compose BOM versions. |
| **Risk/Effort** | Very low risk, low effort. |

---

#### TD-16: DraftRepository Thread Safety

| | |
|---|---|
| **Evidence** | `DraftRepository.kt` uses a `MutableMap<String, Draft>` cache (line 49) with lazy loading via `ensureLoaded()`. The `persist()` method writes the entire map to disk synchronously. No synchronization primitives are used. |
| **Why it matters** | If two coroutines call `saveDraft()` concurrently (e.g., two ChatViewModels being cleared simultaneously), the map can be corrupted or one write can overwrite another. The synchronous `file.writeText()` on the calling thread can block. |
| **Proposed refactor** | (A) Add a `Mutex` for concurrent access. (B) Use `withContext(Dispatchers.IO)` for disk writes. (C) Consider using DataStore or Room for draft persistence instead of raw file I/O. |
| **First slice** | Add `Mutex` around `saveDraft()`, `clearDraft()`, and `ensureLoaded()`. |
| **Risk/Effort** | Low risk, low effort. |

---

#### TD-17: Inline OkHttp Client in Export

| | |
|---|---|
| **Evidence** | `OpenCodeApi.exportSessionToStream()` lines 475-478: Creates a new `OkHttpClient.Builder()` with hardcoded timeouts, bypassing the DI-managed Ktor client entirely. |
| **Why it matters** | The new client has no shared connection pool, no auth interceptors, no logging. It duplicates configuration. If the Ktor client's OkHttp engine is already configured, this creates a second, unmanaged HTTP client. |
| **Proposed refactor** | Extract the underlying OkHttp client from the Ktor engine configuration and reuse it, or implement streaming via Ktor's `bodyAsChannel()` which already supports true streaming without buffering. |
| **First slice** | Replace the inline OkHttp usage with Ktor `prepareGet { }.execute { response.bodyAsChannel() }` for streaming. |
| **Risk/Effort** | Medium risk (streaming behavior must be preserved), low effort. |

---

### 🟢 LOW — Code Quality & Polish

#### TD-18: Excessive `Log.d`/`Log.e` Usage Without Abstraction

| | |
|---|---|
| **Evidence** | 200+ `Log.d`/`Log.e`/`Log.w`/`Log.i` calls across the codebase. Many guarded by `BuildConfig.DEBUG` but inconsistently. |
| **Why it matters** | No structured logging. Cannot redirect to crash reporting. Cannot filter by severity in production. Timber is the Android standard. |
| **Proposed refactor** | Replace with Timber: `Timber.d()`, `Timber.e()`. Plant a `DebugTree` in debug builds and a crash-reporting tree in release. |
| **First slice** | Add Timber dependency, plant `DebugTree` in `OpenCodeApp.onCreate()`, replace `Log.*` in one file as proof. |
| **Risk/Effort** | Very low risk, low effort. |

---

#### TD-19: WebViewScreen and Legacy WebView Path

| | |
|---|---|
| **Evidence** | `NavGraph.kt` line 69: `val useNativeUi = true` — hardcoded. The entire WebView navigation path (lines 211-229) and `WebViewScreen.kt` are dead code. The `webkit` dependency (`androidx.webkit:webkit:1.11.0`) is listed as "kept for legacy". |
| **Why it matters** | Dead code increases maintenance burden and APK size. The `webkit` dependency is unused. |
| **Proposed refactor** | Remove `WebViewScreen.kt`, the WebView navigation path in `NavGraph.kt`, `Screen.WebView` from `Screen.kt`, and the `webkit` dependency. |
| **First slice** | Delete `WebViewScreen.kt` and remove the `webkit` dependency from `build.gradle.kts`. |
| **Risk/Effort** | Very low risk, very low effort. |

---

#### TD-20: `combine()` with 20 Flows Uses Array Casting

| | |
|---|---|
| **Evidence** | `ChatViewModel.kt` lines 211-382: `combine()` with 20 flows, using `args[0] as List<Session>`, `args[1] as Map<String, List<Message>>`, etc. with `@Suppress("UNCHECKED_CAST")`. |
| **Why it matters** | Extremely fragile — reordering flows breaks silently. No compile-time type safety. Difficult to read and maintain. `combine` with more than 5 parameters uses the vararg overload which loses type information. |
| **Proposed refactor** | Split into multiple smaller `combine()` calls that produce intermediate data classes, then combine those. Or use `combine()` with 2-3 flows at a time, building up a typed state object incrementally. |
| **First slice** | Extract a `ChatSessionData` intermediate class from the first 6 flows (sessions, messages, parts, statuses, permissions, questions). |
| **Risk/Effort** | Low risk, medium effort. |

---

#### TD-21: TerminalEmulator (1,224 lines) and ServerTerminalWorkspace (440 lines)

| | |
|---|---|
| **Evidence** | `TerminalEmulator.kt` is 1,224 lines implementing a VT100/xterm terminal emulator from scratch. `ServerTerminalWorkspace.kt` manages PTY tabs. |
| **Why it matters** | Terminal emulation is a complex domain that has well-tested open-source implementations (e.g., Terminal-Emulator by Jack Palevich, or Termux's terminal emulator). A custom implementation is a maintenance burden and likely has rendering/escape-sequence bugs. |
| **Proposed refactor** | Evaluate replacing with an existing library. If keeping custom, at minimum split into: `TerminalEmulator` (core state machine), `TerminalBuffer` (scrollback + screen), `TerminalRenderer` (ANSI parsing), `TerminalView` (Compose canvas). |
| **First slice** | Extract `TerminalBuffer` class from `TerminalEmulator`. |
| **Risk/Effort** | High risk (terminal emulation is subtle), high effort. |

---

## 4. Cross-Cutting Refactoring Roadmap

### Phase 0: Foundation (Week 1)
**Goal**: Enable safe refactoring with minimal tests and remove dead code.

1. **TD-13**: Fix `collectAsState` → `collectAsStateWithLifecycle` (1 hour)
2. **TD-19**: Remove WebView dead code + `webkit` dependency (1 hour)
3. **TD-15**: Create `libs.versions.toml` version catalog (2 hours)
4. **TD-09**: Fix `.value =` → `.update {}` in EventReducer (1 hour)
5. **TD-01 (slice)**: Add test infrastructure + `EventReducerTest` (1 day)

### Phase 1: Testability (Weeks 2-3)
**Goal**: Make the core logic testable and add critical test coverage.

1. **TD-08**: Create `domain/model/Error.kt` + `data/extensions.kt` (1 day)
2. **TD-02 (slice)**: Extract `SessionRemoteDataSource` interface (1 day)
3. **TD-01**: Add `ChatViewModelTest` for state derivation (2 days)
4. **TD-01**: Add `SseClientTest` for event parsing (1 day)
5. **TD-14**: Inject dispatchers into `ChatViewModel` (2 hours)
6. **TD-16**: Add `Mutex` to `DraftRepository` (2 hours)

### Phase 2: Decomposition (Weeks 4-6)
**Goal**: Break up God Objects into focused, testable units.

1. **TD-06 (slice)**: Move DTOs out of `OpenCodeApi.kt` (1 day)
2. **TD-07 (slice)**: Extract `NotificationHelper` from service (1 day)
3. **TD-05 (slice)**: Extract `ChatInputBar` from `ChatScreen` (1 day)
4. **TD-12**: Refactor `SettingsScreen` into Route + Content (1 day)
5. **TD-03 (slice)**: Extract `LoadMessagesUseCase` (1 day)
6. **TD-10 (slice)**: Extract `LocalServerSettings` (1 day)

### Phase 3: Architecture Alignment (Weeks 7-10)
**Goal**: Align with Clean Architecture and project conventions.

1. **TD-04**: Server connection registry (remove passwords from nav) (3 days)
2. **TD-11**: Type-safe navigation migration (2 days)
3. **TD-02**: Full DataSource interface extraction (3 days)
4. **TD-03**: Remaining Use Cases (3 days)
5. **TD-06**: Split `OpenCodeApi` into focused API interfaces (2 days)
6. **TD-17**: Replace inline OkHttp with Ktor streaming (1 day)

### Phase 4: Polish (Ongoing)
1. **TD-18**: Migrate to Timber (1 day)
2. **TD-20**: Decompose the 20-flow `combine()` (2 days)
3. **TD-21**: Evaluate terminal emulator library replacement (research)
4. **TD-05**: Continue ChatScreen decomposition (ongoing)

---

## 5. Testing Strategy for Refactors

### Test Pyramid

| Layer | What to Test | Tools | Priority |
|-------|-------------|-------|----------|
| **Unit** | EventReducer state transitions, Use Case logic, error mapping, model resolution, draft persistence | JUnit 4, kotlinx-coroutines-test, Turbine, MockK | **P0** |
| **Integration** | DataSource + Repository flow, SSE parsing → EventReducer pipeline | JUnit 4, FakeDataSource, TestServer | **P1** |
| **ViewModel** | State derivation from mocked Use Cases, pagination, optimistic updates | JUnit 4, Turbine, MockK, `StandardTestDispatcher` | **P1** |
| **UI (Compose)** | Content composables with fixed state, Route wiring | Compose Test Rule, `createComposeRule()` | **P2** |
| **Instrumented** | DataStore persistence, Hilt DI graph, Service lifecycle | AndroidX Test, Hilt Testing | **P3** |

### Testing Conventions

- Test files go in `app/src/test/kotlin/` mirroring the source package structure.
- Use `@OptIn(ExperimentalCoroutinesApi::class)` with `StandardTestDispatcher` for all coroutine tests.
- Use Turbine's `test { }` block for `StateFlow` assertions.
- MockK for mocking (Kotlin-idiomatic, works with final classes).
- Every Use Case gets a test class. Every ViewModel gets a test class.
- EventReducer tests should cover every `handle*` method.

---

## 6. Acceptable Debt — Do Not Refactor Yet

| Item | Why It's Acceptable | Revisit When |
|------|-------------------|--------------|
| **Single module** (`:app` only) | The app is a single-developer project. Multi-module adds Gradle complexity without proportional benefit at current scale. | Team grows to 2+ developers or build times exceed 3 minutes. |
| **`LocalServerManager` using `HttpURLConnection`** | Simple health check, works fine, not on critical path. | If local server management grows beyond health checks. |
| **`SettingsRepository` dual-write for locale** (DataStore + SharedPreferences) | Required for synchronous `attachBaseContext` locale read. Documented in code. | If AndroidX adds synchronous DataStore reads. |
| **Custom terminal emulator** | Works for the app's specific use case. Replacing with a library is high-risk, high-effort. | If terminal rendering bugs become frequent or escape sequence support needs expand. |
| **`HomeViewModel` as `AndroidViewModel`** | Needs `Application` context for service binding and `LocalServerManager`. Acceptable tradeoff. | If `Application` context dependency causes testing issues. |
| **`SessionDeepLink` as `MutableSharedFlow` with replay=1** | Pragmatic solution for cold-start deep links. The `resetReplayCache()` call prevents replay. | If deep link handling grows more complex. |

---

## 7. Dependency Graph Summary

```
MainActivity
  └── NavGraph (navigation, deep links, share picker)
        ├── HomeScreen → HomeViewModel
        │     ├── ServerRepository (DataStore)
        │     ├── OpenCodeApi (direct — TD-02)
        │     ├── LocalServerManager
        │     └── OpenCodeConnectionService (bound)
        │           ├── SseClient → EventReducer (global state)
        │           ├── OpenCodeApi
        │           └── NotificationManager
        ├── SessionListScreen → SessionListViewModel
        │     ├── OpenCodeApi (direct — TD-02)
        │     └── EventReducer
        ├── ChatScreen → ChatViewModel
        │     ├── OpenCodeApi (direct — TD-02)
        │     ├── EventReducer
        │     ├── DraftRepository
        │     ├── SettingsRepository
        │     └── ServerTerminalWorkspace → PtySocket
        ├── SettingsScreen → SettingsViewModel
        │     └── SettingsRepository
        └── ServerSettingsScreen → ServerSettingsViewModel
              ├── OpenCodeApi (direct — TD-02)
              └── SettingsRepository
```

---

## Appendix: File Size Summary

| File | Lines | Concern |
|------|------:|---------|
| `ChatScreen.kt` | 6,973 | TD-05 |
| `OpenCodeApi.kt` | 1,146 | TD-06 |
| `ChatViewModel.kt` | 1,263 | TD-03 |
| `TerminalEmulator.kt` | 1,224 | TD-21 |
| `HomeScreen.kt` | 1,463 | — |
| `SessionListScreen.kt` | 1,449 | — |
| `SettingsScreen.kt` | 1,277 | — |
| `OpenCodeConnectionService.kt` | 906 | TD-07 |
| `NavGraph.kt` | 757 | TD-11 |
| `SettingsRepository.kt` | 513 | TD-10 |
| `EventReducer.kt` | 454 | TD-09 |
| `ServerTerminalWorkspace.kt` | 440 | TD-21 |
| `SseClient.kt` | 421 | — |
| `HomeViewModel.kt` | 797 | — |
| `ServerSettingsViewModel.kt` | 460 | — |
| `SessionListViewModel.kt` | 431 | — |
| `SettingsViewModel.kt` | 368 | — |
