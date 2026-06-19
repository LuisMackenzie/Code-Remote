---
name: ArquitectoMacK
description: "Trigger: Android feature, nueva pantalla, nuevo módulo, nueva capa, nueva clase, nueva feature Android, Clean Architecture Android, MVVM Compose, Hilt module, Room datasource, use case, repository, ViewModel, Composable. Enforce the exact module structure, class templates, naming conventions, and code patterns of this project."
license: Apache-2.0
metadata:
  author: "mackenzie"
  version: "1.0"
---

## Activation Contract

Apply this skill whenever generating or modifying ANY Kotlin/Android code in this project.
Non-negotiable: every new class, interface, function, or file must follow the patterns below exactly.
If the request contradicts these rules, explain why and propose the correct approach before writing a single line.

---

## Hard Rules

### 1. Module Ownership — where code lives

| What you are creating | Module | Sub-package |
|---|---|---|
| Domain entity, Error sealed interface, enum | `:domain` | `com.mackenzie.<app>.domain[.<feature>]` |
| DataSource interface | `:data` | `com.mackenzie.<app>.data.datasource` |
| Repository concrete class | `:data` | `com.mackenzie.<app>.data` |
| Error helpers (`tryCall`, `tryGet`, `trySave`, `toError`) | `:data` (`:app/data/extensions.kt`) | `com.mackenzie.<app>.data` |
| Use case class | `:usecases` | `com.mackenzie.<app>.usecases[.<feature>]` |
| Room DB, DAOs, DbItems, Room DataSource impl | `:app` | `com.mackenzie.<app>.data.db[.dao|.datasources]` |
| Retrofit services, remote DataSource impl, server models | `:app` | `com.mackenzie.<app>.data.server[.mapper|.models]` |
| ViewModel, Composable Route, Composable UI | `:app` | `com.mackenzie.<app>.ui.<screen>[.ui]` |
| DI modules | `:app` | `com.mackenzie.<app>.di` |
| Navigation | `:app` | `com.mackenzie.<app>.ui.common.nav` |
| Shared test fixtures | `:testShared` | `com.mackenzie.testshared` |
| Fakes and test helpers | `:app/src/testShared` | `com.mackenzie.<app>.ui.fakes` / `.helpers` |

**ZERO Android/Retrofit/Room/Hilt imports in `:domain` or `:data`.**
`:domain` compiles to a plain JAR. `:data` is a pure JVM library.

### 2. Domain Layer rules

- Entities are immutable `data class` values only.
- `Error` is a `sealed interface` with exactly three subtypes: `Server(code: Int)`, `Connectivity`, `Unknown(message: String)`.
- No mappers, no adapters, no annotations inside `:domain`.

```kotlin
// domain/Error.kt
sealed interface Error {
    class Server(val code: Int) : Error
    object Connectivity : Error
    class Unknown(val message: String) : Error
}
```

### 3. Data Layer rules

**DataSource interfaces** — one interface per concern (local / remote), one per bounded context:

```kotlin
// data/datasource/FeatureLocalDataSource.kt
interface FeatureLocalDataSource {
    val items: Flow<List<FeatureItem>>
    suspend fun isEmpty(): Boolean
    suspend fun save(items: List<FeatureItem>): Error?
    suspend fun saveOne(item: FeatureItem): Error?
    suspend fun deleteAll(): Error?
    fun findById(id: Int): Flow<FeatureItem>
}
```

**Repository** — cache-first, Room is SSoT, never returns raw exceptions:

```kotlin
// data/FeatureRepository.kt
class FeatureRepository @Inject constructor(
    private val localDataSource: FeatureLocalDataSource,
    private val remoteDataSource: FeatureRemoteDataSource
) {
    val savedItems: Flow<List<FeatureItem>> = localDataSource.items

    fun findById(id: Int): Flow<FeatureItem> = localDataSource.findById(id)

    suspend fun requestItems(param: String): Error? {
        if (localDataSource.isEmpty()) {
            val result = remoteDataSource.fetchItems(param)
            result.fold(ifLeft = { return it }) { localDataSource.save(it) }
        }
        return null
    }

    suspend fun requestMoreItems(param: String): Error? {
        val result = remoteDataSource.fetchItems(param)
        result.fold(ifLeft = { return it }) { localDataSource.save(it) }
        return null
    }

    suspend fun switchFavorite(item: FeatureItem): Error? {
        val updated = item.copy(isFavorite = !item.isFavorite)
        return localDataSource.saveOne(updated)
    }
}
```

**Error helpers** live in `:app/data/extensions.kt`:

```kotlin
fun Throwable.toError(): Error = when (this) {
    is IOException  -> Error.Connectivity
    is HttpException -> Error.Server(code())
    else            -> Error.Unknown(message ?: "")
}

suspend fun <T> tryCall(action: suspend () -> T): Either<Error, T> = try {
    action().right()
} catch (e: Exception) { e.toError().left() }

suspend fun <T> tryGet(action: suspend () -> T): Error? = try {
    action(); null
} catch (e: Exception) { e.toError() }
```

### 4. Use Case rules

One class, one responsibility, `operator fun invoke()`:

```kotlin
// usecases/feature/GetFeatureItemsUseCase.kt
class GetFeatureItemsUseCase @Inject constructor(private val repo: FeatureRepository) {
    operator fun invoke(): Flow<List<FeatureItem>> = repo.savedItems
}

// usecases/feature/RequestFeatureItemsUseCase.kt
class RequestFeatureItemsUseCase @Inject constructor(private val repo: FeatureRepository) {
    suspend operator fun invoke(param: String): Error? = repo.requestItems(param)
}
```

Naming: `VerbNounUseCase` — `GetFeatureUseCase`, `RequestFeatureUseCase`, `ClearFeatureUseCase`, `FindFeatureUseCase`, `SwitchFeatureFavoriteUseCase`.

### 5. ViewModel rules

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    getItemsUseCase: GetFeatureItemsUseCase,
    private val requestItemsUseCase: RequestFeatureItemsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getItemsUseCase()
                .catch { cause -> _state.update { it.copy(error = cause.toError()) } }
                .collect { items -> _state.update { UiState(items = items) } }
        }
    }

    fun onRequest(param: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val error = requestItemsUseCase(param)
            _state.update { it.copy(isLoading = false, error = error) }
        }
    }

    data class UiState(
        val isLoading: Boolean? = null,
        val items: List<FeatureItem>? = null,
        val error: Error? = null,
    )
}
```

Rules:
- `UiState` is ALWAYS a nested `data class` inside the ViewModel — never external.
- `_state` is always `private MutableStateFlow`; public surface is `StateFlow` via `.asStateFlow()`.
- State transitions use ONLY `_state.update { it.copy(...) }`.
- `init {}` collects long-running Flows. Named methods handle one-shot events.
- Use `sealed interface` UiState ONLY for discrete state-machine screens (AI/streaming).

### 6. Composable Screen rules

**Route file** (`FeatureScreenContentRoute.kt`) — owns ViewModel injection and state collection:

```kotlin
@Composable
internal fun FeatureScreenContentRoute(
    param: String,
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigate: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FeatureScreenContent(
        state = state,
        onItemClicked = { onNavigate(it.id) },
        onRequest = { viewModel.onRequest(param) }
    )
}
```

**Content file** (`ui/FeatureScreenContent.kt`) — pure function of state, zero ViewModel references:

```kotlin
@Preview(showBackground = true)
@Composable
fun FeatureScreenContent(
    state: FeatureViewModel.UiState = FeatureViewModel.UiState(),
    onItemClicked: (FeatureItem) -> Unit = {},
    onRequest: () -> Unit = {}
) {
    state.items?.let { items ->
        // render list
    }
    state.isLoading?.let { if (it) LoadingAnimation(modifier = Modifier.fillMaxSize()) }
    state.error?.let { error -> LoadingErrorView(error = error.toString()) }
}
```

Rules:
- NEVER use `collectAsState()` — always `collectAsStateWithLifecycle()`.
- Composables NEVER write to state directly. They only call ViewModel methods.
- UI components live in a `ui/` sub-package under the feature package.

### 7. Room DataSource implementation rules

```kotlin
// app/data/db/datasources/RoomFeatureDataSource.kt
class RoomFeatureDataSource @Inject constructor(private val dao: FeatureDao) : FeatureLocalDataSource {

    companion object {
        private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val nestedAdapter = moshi.adapter(NestedType::class.java)
    }

    override val items: Flow<List<FeatureItem>> = dao.getAll().map { it.toDomainModel() }
    override suspend fun isEmpty(): Boolean = dao.count() == 0
    override fun findById(id: Int): Flow<FeatureItem> = dao.findById(id).map { it.toDomainModel() }

    override suspend fun save(items: List<FeatureItem>): Error? = tryCall {
        dao.insertAll(items.fromDomainModel())
    }.fold(ifLeft = { it }, ifRight = { null })

    override suspend fun deleteAll(): Error? = tryCall {
        dao.deleteAll()
    }.fold(ifLeft = { it }, ifRight = { null })
}

// Private file-level mappers — NEVER inside entity classes
private fun List<FeatureDbItem>.toDomainModel(): List<FeatureItem> = map { it.toDomainModel() }
private fun FeatureDbItem.toDomainModel(): FeatureItem = FeatureItem(id, name, url, isFavorite)
private fun FeatureItem.fromDomainModel(): FeatureDbItem = FeatureDbItem(id, name, url, isFavorite)
fun List<FeatureItem>.fromDomainModel(): List<FeatureDbItem> = map { it.fromDomainModel() }
```

Rules:
- Complex fields (lists, objects) → serialize as JSON with Moshi adapter in `companion object`.
- DB entity suffix: `FeatureDbItem` (never the same name as the domain entity).
- Mappers are private file-level extension functions — NEVER inside the entity or data class.

### 8. Hilt DI rules

Two separate classes in `di/AppModule.kt`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // All @Provides — DB, DAOs, Retrofit, OkHttpClient, Moshi, ApiUrl
    @Provides @Singleton fun provideDatabase(app: Application) = FeatureDataBase.getDatabase(app)
    @Provides @Singleton fun provideFeatureDao(db: FeatureDataBase) = db.featureDao()
    @Provides @Singleton fun provideOkHttpClient(): OkHttpClient = ...
    @Provides @Singleton fun provideMoshi(): Moshi = ...
    @Provides @Singleton fun provideApiUrl(): ApiUrl = ApiUrl()
    @Provides @Singleton fun provideFeatureService(apiUrl: ApiUrl, client: OkHttpClient, moshi: Moshi): RemoteConnect { ... }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppDataModule {
    // All @Binds — interface → concrete class
    @Binds abstract fun bindLocalDataSource(impl: RoomFeatureDataSource): FeatureLocalDataSource
    @Binds abstract fun bindRemoteDataSource(impl: ServerFeatureDataSource): FeatureRemoteDataSource
    @Binds abstract fun bindPermissionChecker(impl: AndroidPermissionChecker): PermissionChecker
}
```

### 9. Navigation rules

```kotlin
// app/ui/common/nav/NavItem.kt
sealed class NavItem(
    internal val baseRoute: String,
    private val navArgs: List<NavArg> = emptyList()
) {
    object FeatureScreen : NavItem("feature_screen", listOf(NavArg.FeatureId)) {
        fun createRoute(id: Int) = "$baseRoute${File.separator}$id"
    }

    val route = listOf(baseRoute).plus(navArgs.map { "{${it.key}}" }).joinToString(File.separator)
    val args  = navArgs.map { navArgument(it.key) { type = it.navType } }
}

enum class NavArg(val key: String, val navType: NavType<*>) {
    FeatureId("featureId", NavType.IntType),
}
```

- URL special characters must be encoded before navigating.
- Splash screens pop with `inclusive = true`.

### 10. BuildSrc conventions

All library coordinates live in `buildSrc/`:
- `AppConfig.kt` — SDK versions, app ID, versionCode.
- `Libs.kt` — all library coordinates as `object Libs { object Category { const val lib = "group:artifact:_" } }`.
- `Modules.kt` — all module path constants (`:app`, `:data`, `:domain`, `:usecases`, `:testShared`).
- `ClassPath.kt`, `Plugins.kt`, `Constants.kt`.

Use `_` as version placeholder for dependency management — never hardcode versions in `build.gradle.kts`.

---

## Decision Gates

| Situation | Correct action |
|---|---|
| New screen needed | Create Route file + ViewModel + `ui/` content composable |
| New data type needed | Domain entity first, then interfaces, then repo, then use cases |
| Need to call network | Remote DataSource interface in `:data`, implementation in `:app/server` |
| Need to persist data | Local DataSource interface in `:data`, Room impl in `:app/db` |
| Error from network/db | Always map to `domain.Error` via `tryCall`/`toError` — never throw raw |
| Multiple Retrofit base URLs | Separate Retrofit instances sharing one OkHttpClient + Moshi, bundled in `RemoteConnect` |
| Hilt can't inject ViewModel param | Use ViewModel factory pattern, do not use `@HiltViewModel` |
| Testing a ViewModel | `@RunWith(MockitoJUnitRunner)` + `CoroutinesTestRule` + Turbine |
| Testing a Repository | `@RunWith(MockitoJUnitRunner)` + mock DataSource interfaces |
| Integration test | Real use cases + fake DataSource implementations (in-memory state) |

---

## Execution Steps

When adding a new feature (e.g., `Photo`):

1. **`:domain`** — create `PhotoItem.kt` (immutable `data class`).
2. **`:data/datasource`** — create `PhotoLocalDataSource.kt` (interface) + `PhotoRemoteDataSource.kt` (interface).
3. **`:data`** — create `PhotoRepository.kt` (depends only on interfaces, never on Room/Retrofit).
4. **`:usecases/photo`** — create one class per operation: `GetPhotosUseCase`, `RequestPhotosUseCase`, etc.
5. **`:app/db/dao`** — create `PhotoDao.kt`.
6. **`:app/db`** — create `PhotoDbItem.kt` entity.
7. **`:app/db/datasources`** — create `RoomPhotoDataSource.kt` implementing the local interface.
8. **`:app/server`** — create `PhotoService.kt` (Retrofit interface) + `ServerPhotoDataSource.kt`.
9. **`:app/di`** — add `@Provides` for DAO and `@Binds` for both DataSources.
10. **`:app/ui/photo`** — create `PhotoViewModel.kt` + `PhotoScreenContentRoute.kt` + `ui/PhotoScreenContent.kt`.
11. **`NavItem`** — add `PhotoScreen` object + args if needed.
12. **Tests** — use case unit test, repository unit test, ViewModel unit+integration test.

Do NOT skip steps. Do NOT collapse layers.

---

## Output Contract

Every generated file must include:
- Correct package declaration matching the module and sub-package rules above.
- Only the imports valid for that layer.
- Mappers as file-level private extension functions (never inside classes or entities).
- `UiState` as a nested `data class` inside the ViewModel.
- No raw exceptions crossing layer boundaries.
- `@Preview` on every `@Composable` screen content function.

Return the complete, compilable file — no stubs, no TODOs unless explicitly flagged.

---

## References

- `AGENTS.md` — full architecture reference guide for this project
- `app/src/main/java/com/mackenzie/waifuviewer/ui/main/WaifuImViewModel.kt` — canonical ViewModel example
- `data/src/main/java/com/mackenzie/waifuviewer/data/WaifusImRepository.kt` — canonical Repository example
- `data/src/main/java/com/mackenzie/waifuviewer/data/datasource/WaifusImLocalDataSource.kt` — canonical DataSource interface
- `app/src/main/java/com/mackenzie/waifuviewer/data/db/datasources/RoomImDataSource.kt` — canonical Room DataSource impl
- `app/src/main/java/com/mackenzie/waifuviewer/di/AppModule.kt` — canonical DI module
- `app/src/main/java/com/mackenzie/waifuviewer/ui/common/nav/NavItem.kt` — canonical Navigation
- `app/src/main/java/com/mackenzie/waifuviewer/data/extensions.kt` — canonical error helpers
- `buildSrc/src/main/kotlin/Libs.kt` — dependency catalog

Base directory for this skill: file:///Users/mackenzie/.config/opencode/skills/ArquitectoMacK
Relative paths in this skill (e.g., assets/, references/) are relative to this base directory.
