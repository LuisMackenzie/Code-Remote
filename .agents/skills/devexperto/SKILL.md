---
name: devexperto
description: Guía de buenas prácticas para desarrollo Android con arquitectura CLEAN, principios SOLID, MVVM con Jetpack Compose y modularización. TRIGGER cuando el usuario pregunta sobre arquitectura Android, CLEAN architecture, SOLID, MVVM, inyección de dependencias, testing, o cómo implementar nuevas features en Android. Usa esta skill para generar código siguiendo los patrones del curso de DevExperto.
---

# DevExperto - Guía de Arquitectura Android

Esta skill contiene los patrones y buenas prácticas del curso de arquitecturas Android con principios SOLID y arquitectura CLEAN usando Jetpack Compose.

## Cuándo Usar Esta Skill

Activa esta skill cuando:
- El usuario pregunta sobre arquitectura Android
- Necesitas crear código siguiendo CLEAN Architecture
- El usuario pregunta sobre principios SOLID aplicados a Android
- Necesitas implementar una nueva feature siguiendo los patrones del curso
- El usuario pregunta sobre MVVM con Jetpack Compose
- Necesitas configurar inyección de dependencias (Hilt/Koin)
- El usuario pregunta sobre testing en Android

---

## 1. Arquitectura CLEAN (Visión Pragmática)

### Objetivo Principal
Desacoplar las diferentes unidades de código de manera organizada para que la aplicación sea más fácil de entender, modificar y testear.

### Capas de la Arquitectura

| Capa | Responsabilidad | Contenido |
|------|-----------------|-----------|
| **Presentación** | Interfaz de usuario | UI (Activities, Fragments, Compose), ViewModels |
| **Casos de Uso** | Lógica de aplicación | Interactors, acciones del usuario |
| **Dominio** | Reglas de negocio | Entidades, modelos de negocio |
| **Datos** | Abstracción de datos | Interfaces de Data Sources, Repositorios |
| **Framework** | Implementaciones concretas | Room, Retrofit, librerías externas |

### Organización Pragmática (Recomendada)
Para evitar sobre-ingeniería, agrupar:
- **Presentación + Framework** → Un módulo (ambos dependen de Android)
- **Repositorios + Data Sources** → Una única capa "Data"
- **Casos de Uso** → Módulo separado
- **Dominio** → Módulo separado

### Regla de Dependencia
Las dependencias **siempre** apuntan de afuera hacia adentro. Las capas internas (Dominio, Casos de Uso) no deben saber nada sobre las capas externas (Presentación, Framework).

### Inversión de Dependencias (DIP)
Para permitir que capas internas interactúen con externas:
1. La capa interna define la **interfaz abstracta**
2. La capa externa proporciona la **implementación concreta**
3. La dependencia real se inyecta desde el exterior (IoC)

**Ejemplo:**
```kotlin
// Capa de Datos (interna) - Define abstracción
interface DataPersistence {
    suspend fun save(data: Entity)
    suspend fun get(id: String): Entity?
}

// Capa de Framework (externa) - Implementa
class RoomDatabase : DataPersistence {
    override suspend fun save(data: Entity) { /* Room implementation */ }
    override suspend fun get(id: String): Entity? { /* Room implementation */ }
}
```

### Mapeo de Datos en las Fronteras
Cada fuente de datos tiene su propio modelo:
- **Modelo de Servidor** (ServerMovie)
- **Modelo de Base de Datos** (DBMovie)
- **Modelo de Dominio** (Movie)

Los mappers se realizan **siempre** en las fronteras de la capa más externa:
```kotlin
// En Framework - Mapper
fun DBMovie.toDomainModel(): Movie = Movie(
    id = this.id,
    title = this.title,
    // ...
)

fun Movie.toDBModel(): DBMovie = DBMovie(
    id = this.id,
    title = this.title,
    // ...
)
```

---

## 2. Principios SOLID Aplicados

### S - Single Responsibility Principle (SRP)
Un objeto/clase debe tener **una única responsabilidad** y **un solo motivo para cambiar**.

**Violación común en Android:**
```kotlin
// ❌ INCORRECTO - Activity con múltiples responsabilidades
class MainActivity : AppCompatActivity() {
    fun loadData() { /* lógica de datos */ }
    fun saveToDatabase() { /* persistencia */ }
    fun formatForDisplay(): String { /* presentación */ }
}
```

**Solución arquitectónica (MVVM):**
```kotlin
// ✅ CORRECTO - Responsabilidades separadas
class MovieViewModel(
    private val getMoviesUseCase: GetMoviesUseCase
) : ViewModel() {
    // Solo maneja estado de UI
}

class GetMoviesUseCase(private val repository: MovieRepository) {
    // Solo lógica de aplicación
}

class MovieRepositoryImpl(
    private val localDataSource: MovieLocalDataSource,
    private val remoteDataSource: MovieRemoteDataSource
) : MovieRepository {
    // Solo orquestación de datos
}
```

### O - Open/Closed Principle (OCP)
Abierto a la **extensión**, cerrado a la **modificación**. Usar polimorfismo.

**Ejemplo de violación:**
```kotlin
// ❌ INCORRECTO - Modificar para cada nuevo tipo
fun draw(vehicle: Vehicle) {
    when (vehicle.type) {
        VehicleType.CAR -> drawCar()
        VehicleType.MOTORBIKE -> drawMotorbike()
        // ❌ Hay que modificar aquí para cada nuevo tipo
    }
}
```

**Solución con polimorfismo:**
```kotlin
// ✅ CORRECTO - Extensible sin modificación
interface Vehicle {
    fun draw()
}

class Car : Vehicle {
    override fun draw() { /* dibujar coche */ }
}

class Motorbike : Vehicle {
    override fun draw() { /* dibujar moto */ }
}

// Añadir nuevos tipos sin modificar código existente
class Truck : Vehicle {
    override fun draw() { /* dibujar camión */ }
}

fun draw(vehicle: Vehicle) {
    vehicle.draw() // No necesita modificación
}
```

### L - Liskov Substitution Principle (LSP)
Las subclases deben poder sustituir a sus clases padre sin romper el comportamiento esperado.

**Ejemplo de violación:**
```kotlin
// ❌ INCORRECTO
open class Animal {
    open fun walk() { /* caminar */ }
    open fun jump() { /* saltar */ }
}

class Elephant : Animal() {
    override fun jump() {
        throw UnsupportedOperationException("Elephants can't jump")
    }
}

// El programa falla inesperadamente
fun makeAnimalJump(animal: Animal) {
    animal.jump() // ❌ Rompe con Elephant
}
```

**Solución:**
```kotlin
// ✅ CORRECTO - Segregar interfaces
interface Walkable {
    fun walk()
}

interface Jumpable {
    fun jump()
}

class Elephant : Walkable {
    override fun walk() { /* caminar */ }
    // Elephant no implementa Jumpable
}

class Cat : Walkable, Jumpable {
    override fun walk() { /* caminar */ }
    override fun jump() { /* saltar */ }
}
```

### I - Interface Segregation Principle (ISP)
Ninguna clase debe verse obligada a depender de métodos que no usa. Evitar "interfaces gordas".

**Ejemplo de violación:**
```kotlin
// ❌ INCORRECTO - Interface gorda
interface Product {
    val name: String
    val price: Double
    val recommendedAge: Int // ❌ No todos los productos tienen edad recomendada
}

class DVD : Product {
    override val recommendedAge: Int get() = 12
}

class MusicCD : Product {
    override val recommendedAge: Int
        get() = throw UnsupportedOperationException("Music has no age rating")
}
```

**Solución:**
```kotlin
// ✅ CORRECTO - Interfaces segregadas
interface Product {
    val name: String
    val price: Double
}

interface AgeAware {
    val recommendedAge: Int
}

class DVD : Product, AgeAware {
    override val name: String = "Inception"
    override val price: Double = 15.0
    override val recommendedAge: Int = 12
}

class MusicCD : Product {
    override val name: String = "Thriller"
    override val price: Double = 10.0
    // No implementa AgeAware - no forzado a tener recommendedAge
}
```

### D - Dependency Inversion Principle (DIP)
Las clases de alto nivel no deben depender de clases de bajo nivel. Ambas deben depender de abstracciones.

**Ejemplo de violación:**
```kotlin
// ❌ INCORRECTO - Dependencia directa de implementación
class ShoppingBasket {
    private val database = SqlDatabase() // ❌ Acoplado a implementación concreta
    private val payment = CreditCard()   // ❌ Acoplado a implementación concreta
}
```

**Solución:**
```kotlin
// ✅ CORRECTO - Depender de abstracciones
interface Persistence {
    suspend fun save(data: Any)
}

interface PaymentMethod {
    suspend fun pay(amount: Double)
}

class ShoppingBasket(
    private val persistence: Persistence,  // Inyectado
    private val payment: PaymentMethod      // Inyectado
) {
    suspend fun checkout(amount: Double) {
        payment.pay(amount)
        persistence.save(/* ... */)
    }
}
```

---

## 3. Patrón MVVM con Jetpack Compose

### Arquitectura del Patrón

```
┌─────────────────┐
│      UI         │  (Compose)
│  @Composable    │
└────────┬────────┘
         │ Events (up)
         │ State (down)
         ▼
┌─────────────────┐
│   ViewModel     │
│  StateFlow      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Use Cases     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Repositories   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Data Sources   │
└─────────────────┘
```

### Flujo de Datos Unidireccional (UDF)

1. **Eventos** fluyen hacia arriba (UI → ViewModel)
2. **Estado** fluye hacia abajo (ViewModel → UI)

### Modelado del Estado

```kotlin
// Opción 1: Data class simple
data class MovieUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val movies: List<Movie> = emptyList()
)

// Opción 2: Sealed class para estados distintos
sealed interface MovieState {
    object Loading : MovieState
    data class Error(val message: String) : MovieState
    data class Success(val movies: List<Movie>) : MovieState
}
```

### ViewModel con StateFlow

```kotlin
@HiltViewModel
class MovieViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    // Estado privado mutable
    private val _state = MutableStateFlow(MovieUiState())

    // Estado público inmutable
    val state: StateFlow<MovieUiState> = _state.asStateFlow()

    init {
        loadMovies()
    }

    fun loadMovies() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            getMoviesUseCase()
                .catch { error ->
                    _state.update { it.copy(loading = false, error = error.message) }
                }
                .collect { movies ->
                    _state.update { it.copy(loading = false, movies = movies) }
                }
        }
    }

    fun onFavoriteClicked(movieId: String) {
        viewModelScope.launch {
            toggleFavoriteUseCase(movieId)
        }
    }
}
```

### UI en Jetpack Compose

```kotlin
@Composable
fun MovieScreen(
    viewModel: MovieViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    when {
        state.loading -> {
            CircularProgressIndicator()
        }
        state.error != null -> {
            ErrorMessage(state.error!!)
        }
        else -> {
            MovieList(
                movies = state.movies,
                onFavoriteClick = viewModel::onFavoriteClicked
            )
        }
    }
}

@Composable
fun MovieList(
    movies: List<Movie>,
    onFavoriteClick: (String) -> Unit
) {
    LazyColumn {
        items(movies, key = { it.id }) { movie ->
            MovieItem(
                movie = movie,
                onFavoriteClick = { onFavoriteClick(movie.id) }
            )
        }
    }
}
```

### Rol de los Casos de Uso

Los Use Cases:
1. **Representan acciones del usuario** (activas o implícitas)
2. **Encapsulan lógica de aplicación**
3. **Actúan como puente** entre ViewModels y Repositorios
4. **Gestionan hilos de ejecución** (corrutinas)

```kotlin
class GetMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    operator fun invoke(): Flow<List<Movie>> = repository.movies
        .flowOn(Dispatchers.IO)

    // O con suspend fun
    suspend operator fun invoke(): Result<List<Movie>> {
        return try {
            Result.success(repository.getMovies())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 4. Inyección de Dependencias

### Frameworks Soportados

| Framework | Características |
|-----------|----------------|
| **Koin** | Runtime, Kotlin-friendly, KMP compatible, fácil de empezar |
| **Hilt** | Compile-time, oficial de Google, recomendado para Android |
| **Dagger** | Compile-time, potente, mucho boilerplate |

### Organización de Módulos por Capa

**Regla clave:** Cada módulo de Gradle define su propio módulo de DI.

#### Capa de Dominio
```kotlin
// Koin
val domainMovieModule = module {
    factoryOf(::GetMoviesUseCase)
    factoryOf(::ToggleFavoriteUseCase)
    singleOf(::MovieRepositoryImpl) { bind<MovieRepository>() }
}

// Hilt
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    @Binds
    abstract fun bindRepository(impl: MovieRepositoryImpl): MovieRepository
}

// Use Case con Hilt
class GetMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    operator fun invoke(): Flow<List<Movie>> = repository.movies
}
```

#### Capa de Framework/Data
```kotlin
// Koin
val frameworkMovieModule = module {
    // Singletons de infraestructura
    single { Room.databaseBuilder(get(), AppDatabase::class.java, "app.db").build() }
    single { get<AppDatabase>().movieDao() }

    // Data Sources con visibilidad internal
    singleOf(::MovieRoomDataSource) { bind<MovieLocalDataSource>() }
    singleOf(::MovieServerDataSource) { bind<MovieRemoteDataSource>() }
}

// Hilt
@Module
@InstallIn(SingletonComponent::class)
object FrameworkModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()
    }

    @Provides
    fun provideMovieDao(database: AppDatabase): MovieDao {
        return database.movieDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    internal abstract fun bindLocalDataSource(impl: MovieRoomDataSource): MovieLocalDataSource

    @Binds
    internal abstract fun bindRemoteDataSource(impl: MovieServerDataSource): MovieRemoteDataSource
}
```

#### Capa de Presentación (Features)
```kotlin
// Koin
val featureHomeModule = module {
    viewModelOf(::HomeViewModel)
}

// Hilt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase
) : ViewModel() {
    // ...
}

// En Compose
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    // ...
}
```

#### Aplicación Principal
```kotlin
// Koin - En la clase Application
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            modules(
                domainMovieModule,
                frameworkMovieModule,
                featureHomeModule,
                featureDetailModule
            )
        }
    }
}

// Hilt - Solo anotar la clase Application
@HiltAndroidApp
class MyApp : Application()
```

### Ocultación de Implementaciones

```kotlin
// En framework:data - Visibilidad internal
internal class MovieRoomDataSource(
    private val dao: MovieDao
) : MovieLocalDataSource {
    // Solo accesible desde el mismo módulo
}

// El módulo de DI expone solo la interfaz
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    internal abstract fun bindLocalDataSource(
        impl: MovieRoomDataSource
    ): MovieLocalDataSource // Expone la interfaz, no la implementación
}
```

---

## 5. Estructura de Módulos y Paquetes

### Estructura de Módulos Gradle

```
project/
├── app/                          # Módulo principal
│   ├── build.gradle.kts
│   └── src/main/
│       ├── MyApp.kt              # @HiltAndroidApp
│       └── MainActivity.kt        # NavHost
│
├── build-logic/                  # Convention Plugins
│   ├── convention/
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/
│   │       ├── android-feature.gradle.kts
│   │       ├── jvm-library.gradle.kts
│   │       └── di-library.gradle.kts
│
├── feature/
│   ├── home/                     # feature:home
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/
│   │       └── com/app/home/
│   │           ├── ui/
│   │           │   ├── HomeScreen.kt
│   │           │   ├── HomeViewModel.kt
│   │           │   └── HomeUiState.kt
│   │           └── di/
│   │               └── HomeModule.kt
│   │
│   ├── detail/                   # feature:detail
│   └── common/                   # feature:common (UI compartida)
│
├── domain/
│   └── movie/                    # domain:movie
│       ├── build.gradle.kts
│       └── src/main/kotlin/
│           └── com/app/domain/movie/
│               ├── Movie.kt                    # Entidad
│               ├── MovieRepository.kt          # Interface repositorio
│               ├── MovieLocalDataSource.kt     # Interface
│               ├── MovieRemoteDataSource.kt    # Interface
│               └── usecases/
│                   ├── GetMoviesUseCase.kt
│                   └── ToggleFavoriteUseCase.kt
│
└── framework/
    └── movie/                    # framework:movie
        ├── build.gradle.kts
        └── src/main/kotlin/
            └── com/app/framework/movie/
                ├── database/
                │   ├── DBMovie.kt              # @Entity
                │   ├── MovieDao.kt              # @Dao
                │   └── MovieRoomDataSource.kt    # internal
                ├── remote/
                │   ├── MovieService.kt          # Retrofit
                │   ├── MovieServerDataSource.kt # internal
                │   └── ServerMovie.kt            # @Serializable
                ├── mapper/
                │   └── Mappers.kt
                ├── di/
                │   └── FrameworkMovieModule.kt
                └── MovieRepositoryImpl.kt
```

### Dependencias entre Módulos

```
┌─────────────┐
│     app     │ ──► feature:*, domain:*, framework:*
└─────────────┘
       │
       ▼
┌─────────────┐
│   feature   │ ──► domain
└─────────────┘
       │
       ▼
┌─────────────┐
│   domain    │ (sin dependencias de Android)
└─────────────┘
       ▲
       │
┌─────────────┐
│  framework  │ ──► domain (implementa interfaces)
└─────────────┘
```

### Convenciones de Nomenclatura

| Tipo | Convención | Ejemplo |
|------|------------|---------|
| **Entidades de Dominio** | Sustantivo simple | `Movie`, `User`, `Location` |
| **Data Sources (Interface)** | `{Entity}{Location}DataSource` | `MovieLocalDataSource`, `MovieRemoteDataSource` |
| **Data Sources (Implementación)** | `{Entity}{Tech}DataSource` | `MovieRoomDataSource`, `MovieServerDataSource` |
| **Repositorios (Interface)** | `{Entity}Repository` | `MovieRepository` |
| **Repositorios (Implementación)** | `{Entity}RepositoryImpl` | `MovieRepositoryImpl` |
| **Use Cases** | `{Action}{Entity}UseCase` | `GetMoviesUseCase`, `FindMovieByIdUseCase` |
| **ViewModels** | `{Screen}ViewModel` | `HomeViewModel`, `DetailViewModel` |
| **Estados UI** | `{Screen}UiState` o `{Screen}State` | `HomeUiState`, `DetailState` |
| **Funciones Composable** | PascalCase | `HomeScreen`, `MovieItem`, `LoadingIndicator` |
| **Modelos DB** | Prefijo `DB` | `DBMovie`, `DBUser` |
| **Modelos Server** | Prefijo `Server` o `Remote` | `ServerMovie`, `RemoteUser` |
| **Mappers** | Función de extensión `to{Target}Model` | `fun DBMovie.toDomainModel(): Movie` |
| **Tests** | Sufijo `Test` | `HomeViewModelTest`, `GetMoviesUseCaseTest` |

---

## 6. Testing

### Tipos de Tests

| Tipo | Velocidad | Cobertura | Dónde ejecutar |
|------|-----------|-----------|----------------|
| **Unitarios** | Rápidos | Una entidad aislada | JVM local |
| **Integración** | Moderados | Varias entidades juntas | JVM local + Fakes |
| **UI** | Lentos | Comportamiento visual | Emulador/Dispositivo |

### Tests de Use Cases

```kotlin
class GetMoviesUseCaseTest {
    private val repository: MovieRepository = mock()
    private val useCase = GetMoviesUseCase(repository)

    @Test
    fun `invoke returns movies from repository`() = runTest {
        // Given
        val expectedMovies = listOf(
            Movie(id = "1", title = "Test Movie")
        )
        whenever(repository.movies).thenReturn(flowOf(expectedMovies))

        // When
        val result = useCase().first()

        // Then
        assertEquals(expectedMovies, result)
    }

    @Test
    fun `invoke calls repository toggleFavorite`() = runTest {
        // Given
        val movieId = "123"
        whenever(repository.toggleFavorite(movieId)).thenReturn(Unit)

        // When
        toggleFavoriteUseCase(movieId)

        // Then
        verify(repository).toggleFavorite(movieId)
    }
}
```

### Tests de ViewModel

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    // Regla para sustituir MainDispatcher
    @get:Rule
    val dispatcherRule = StandardTestDispatcher()

    private val getMoviesUseCase: GetMoviesUseCase = mock()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        whenever(getMoviesUseCase()).thenReturn(flowOf(emptyList()))
        viewModel = HomeViewModel(getMoviesUseCase)
    }

    @Test
    fun `loadMovies emits loading then success`() = runTest {
        // Given
        val movies = listOf(Movie(id = "1", title = "Test"))
        whenever(getMoviesUseCase()).thenReturn(flowOf(movies))

        // When
        viewModel.loadMovies()

        // Then - usando Turbine para verificar estados
        viewModel.state.test {
            // Estado inicial
            assertEquals(HomeUiState(loading = false), awaitItem())

            // Iniciar carga
            runCurrent()

            // Verificar estados emitidos
            val loadingState = awaitItem()
            assertTrue(loadingState.loading)

            val successState = awaitItem()
            assertFalse(successState.loading)
            assertEquals(movies, successState.movies)
        }
    }
}
```

### Tests de Repositorios

```kotlin
class MovieRepositoryTest {
    private val localDataSource: MovieLocalDataSource = mock()
    private val remoteDataSource: MovieRemoteDataSource = mock()
    private val repository = MovieRepositoryImpl(localDataSource, remoteDataSource)

    @Test
    fun `getMovies fetches from remote when local is empty`() = runTest {
        // Given
        val movies = listOf(Movie(id = "1", title = "Test"))
        whenever(localDataSource.isEmpty()).thenReturn(true)
        whenever(remoteDataSource.getMovies()).thenReturn(movies)
        whenever(localDataSource.save(movies)).thenReturn(Unit)
        whenever(localDataSource.getMovies()).thenReturn(movies)

        // When
        val result = repository.getMovies()

        // Then
        verify(remoteDataSource).getMovies()
        verify(localDataSource).save(movies)
        assertEquals(movies, result)
    }
}
```

### Tests de UI Compose

```kotlin
@Test
fun `HomeScreen shows loading indicator when loading`() {
    composeTestRule.setContent {
        HomeScreen(
            state = HomeUiState(loading = true),
            onFavoriteClick = {}
        )
    }

    composeTestRule.onNodeWithText("Loading").assertIsDisplayed()
    // O usando semántica
    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
}

@Test
fun `HomeScreen shows movie list on success`() {
    val movies = listOf(Movie(id = "1", title = "Inception"))

    composeTestRule.setContent {
        HomeScreen(
            state = HomeUiState(loading = false, movies = movies),
            onFavoriteClick = {}
        )
    }

    composeTestRule.onNodeWithText("Inception").assertIsDisplayed()
}
```

---

## 7. Implementar Nueva Feature (Paso a Paso)

### Flujo: De Adentro hacia Afuera

1. **Dominio** → 2. **Framework** → 3. **Presentación** → 4. **App**

### Paso 1: Crear Módulos

```kotlin
// settings.gradle.kts
include(":domain:movie")
include(":framework:movie")
include(":feature:home")

// domain:movie/build.gradle.kts
plugins {
    id("architectcoders.jvm.library") // Convention plugin
}

// framework:movie/build.gradle.kts
plugins {
    id("architectcoders.android.library")
    id("architectcoders.di.library")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":domain:movie"))
    implementation(libs.room.runtime)
    implementation(libs.retrofit)
}

// feature:home/build.gradle.kts
plugins {
    id("architectcoders.android.feature")
    id("architectcoders.di.library.compose")
}

dependencies {
    implementation(project(":domain:movie"))
    implementation(libs.compose.navigation)
}
```

### Paso 2: Capa de Dominio

```kotlin
// domain:movie/src/main/kotlin/.../Movie.kt
data class Movie(
    val id: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val isFavorite: Boolean = false
)

// domain:movie/src/main/kotlin/.../MovieLocalDataSource.kt
interface MovieLocalDataSource {
    val movies: Flow<List<Movie>>
    suspend fun save(movies: List<Movie>)
    suspend fun findById(id: String): Movie?
}

// domain:movie/src/main/kotlin/.../MovieRemoteDataSource.kt
interface MovieRemoteDataSource {
    suspend fun getMovies(): List<Movie>
    suspend fun findById(id: String): Movie?
}

// domain:movie/src/main/kotlin/.../MovieRepository.kt
interface MovieRepository {
    val movies: Flow<List<Movie>>
    suspend fun toggleFavorite(movieId: String)
}

// domain:movie/src/main/kotlin/.../usecases/GetMoviesUseCase.kt
class GetMoviesUseCase(private val repository: MovieRepository) {
    operator fun invoke(): Flow<List<Movie>> = repository.movies
}

// domain:movie/src/main/kotlin/.../usecases/ToggleFavoriteUseCase.kt
class ToggleFavoriteUseCase(private val repository: MovieRepository) {
    suspend operator fun invoke(movieId: String) {
        repository.toggleFavorite(movieId)
    }
}
```

### Paso 3: Capa de Framework

```kotlin
// framework:movie/src/main/kotlin/.../database/DBMovie.kt
@Entity(tableName = "movies")
data class DBMovie(
    @PrimaryKey val id: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val isFavorite: Boolean
)

// framework:movie/src/main/kotlin/.../database/MovieDao.kt
@Dao
interface MovieDao {
    @Query("SELECT * FROM movies")
    fun getAll(): Flow<List<DBMovie>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<DBMovie>)

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun findById(id: String): DBMovie?

    @Query("UPDATE movies SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)
}

// framework:movie/src/main/kotlin/.../database/Mappers.kt
fun DBMovie.toDomainModel(): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    isFavorite = isFavorite
)

fun Movie.toDBModel(): DBMovie = DBMovie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    isFavorite = isFavorite
)

fun List<DBMovie>.toDomainModels(): List<Movie> = map { it.toDomainModel() }
fun List<Movie>.toDBModels(): List<DBMovie> = map { it.toDBModel() }

// framework:movie/src/main/kotlin/.../database/MovieRoomDataSource.kt
internal class MovieRoomDataSource(
    private val dao: MovieDao
) : MovieLocalDataSource {

    override val movies: Flow<List<Movie>> = dao.getAll()
        .map { it.toDomainModels() }

    override suspend fun save(movies: List<Movie>) {
        dao.insertAll(movies.toDBModels())
    }

    override suspend fun findById(id: String): Movie? {
        return dao.findById(id)?.toDomainModel()
    }
}

// framework:movie/src/main/kotlin/.../remote/MovieServerDataSource.kt
internal class MovieServerDataSource(
    private val service: MovieService
) : MovieRemoteDataSource {

    override suspend fun getMovies(): List<Movie> {
        return service.getMovies().results.map { it.toDomainModel() }
    }

    override suspend fun findById(id: String): Movie? {
        return service.getMovieById(id)?.toDomainModel()
    }
}

// framework:movie/src/main/kotlin/.../MovieRepositoryImpl.kt
class MovieRepositoryImpl(
    private val localDataSource: MovieLocalDataSource,
    private val remoteDataSource: MovieRemoteDataSource
) : MovieRepository {

    override val movies: Flow<List<Movie>> = localDataSource.movies

    override suspend fun toggleFavorite(movieId: String) {
        localDataSource.toggleFavorite(movieId)
    }

    suspend fun refreshMovies() {
        val remoteMovies = remoteDataSource.getMovies()
        localDataSource.save(remoteMovies)
    }
}

// framework:movie/src/main/kotlin/.../di/FrameworkMovieModule.kt
@Module
@InstallIn(SingletonComponent::class)
object FrameworkMovieModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "movies.db")
            .build()
    }

    @Provides
    fun provideMovieDao(database: AppDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    @Singleton
    fun provideRetrofit(): MovieService {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MovieService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    internal abstract fun bindLocalDataSource(
        impl: MovieRoomDataSource
    ): MovieLocalDataSource

    @Binds
    internal abstract fun bindRemoteDataSource(
        impl: MovieServerDataSource
    ): MovieRemoteDataSource
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRepository(impl: MovieRepositoryImpl): MovieRepository
}
```

### Paso 4: Capa de Presentación

```kotlin
// feature:home/src/main/kotlin/.../ui/HomeUiState.kt
data class HomeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val movies: List<Movie> = emptyList()
)

// feature:home/src/main/kotlin/.../ui/HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadMovies()
    }

    private fun loadMovies() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            getMoviesUseCase()
                .catch { error ->
                    _state.update { it.copy(loading = false, error = error.message) }
                }
                .collect { movies ->
                    _state.update { it.copy(loading = false, movies = movies) }
                }
        }
    }

    fun onFavoriteClicked(movieId: String) {
        viewModelScope.launch {
            toggleFavoriteUseCase(movieId)
        }
    }

    fun onRetryClicked() {
        loadMovies()
    }
}

// feature:home/src/main/kotlin/.../ui/HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onMovieClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    when {
        state.loading -> {
            LoadingContent()
        }
        state.error != null -> {
            ErrorContent(
                message = state.error!!,
                onRetry = viewModel::onRetryClicked
            )
        }
        else -> {
            MovieList(
                movies = state.movies,
                onMovieClick = onMovieClick,
                onFavoriteClick = viewModel::onFavoriteClicked
            )
        }
    }
}

@Composable
fun MovieList(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(movies, key = { it.id }) { movie ->
            MovieItem(
                movie = movie,
                onClick = { onMovieClick(movie.id) },
                onFavoriteClick = { onFavoriteClick(movie.id) }
            )
        }
    }
}

@Composable
fun MovieItem(
    movie: Movie,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = movie.posterPath,
                contentDescription = movie.title,
                modifier = Modifier.size(100.dp, 150.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.body2,
                    maxLines = 3
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite"
                )
            }
        }
    }
}
```

### Paso 5: Cablear en la App Principal

```kotlin
// app/src/main/kotlin/.../MyApp.kt
@HiltAndroidApp
class MyApp : Application()

// app/src/main/kotlin/.../MainActivity.kt
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = HomeRoute
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onMovieClick = { movieId ->
                            navController.navigate(DetailRoute(movieId))
                        }
                    )
                }
                composable<DetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<DetailRoute>()
                    DetailScreen(
                        movieId = route.movieId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// app/src/main/kotlin/.../navigation/Routes.kt
@Serializable
object HomeRoute

@Serializable
data class DetailRoute(val movieId: String)

// app/build.gradle.kts
dependencies {
    implementation(project(":domain:movie"))
    implementation(project(":framework:movie"))
    implementation(project(":feature:home"))
    implementation(project(":feature:detail"))
}
```

---

## 8. Checklist para Nueva Feature

- [ ] **Dominio**
  - [ ] Crear módulo `domain:feature_name`
  - [ ] Definir entidad de dominio (data class pura)
  - [ ] Crear interfaces de Data Sources
  - [ ] Crear interfaz de Repositorio
  - [ ] Implementar Casos de Uso

- [ ] **Framework**
  - [ ] Crear módulo `framework:feature_name`
  - [ ] Crear modelos de DB y Server
  - [ ] Implementar mappers (toDomainModel, toDBModel, etc.)
  - [ ] Implementar Data Sources con visibilidad `internal`
  - [ ] Crear módulo de DI (Hilt Module)

- [ ] **Presentación**
  - [ ] Crear módulo `feature:feature_name`
  - [ ] Definir UiState (data class o sealed class)
  - [ ] Implementar ViewModel con StateFlow
  - [ ] Crear Composables de UI
  - [ ] Inyectar dependencias

- [ ] **App Principal**
  - [ ] Añadir dependencias de módulos en `app/build.gradle.kts`
  - [ ] Configurar navegación
  - [ ] Probar integración completa

- [ ] **Testing**
  - [ ] Tests unitarios para Use Cases
  - [ ] Tests de ViewModel
  - [ ] Tests de integración (opcional)
  - [ ] Tests de UI Compose (opcional)

---

## Referencia Rápida

### Archivos Necesarios por Capa

**Dominio (5-6 archivos):**
- `Entity.kt` - Entidad de dominio
- `EntityRepository.kt` - Interfaz repositorio
- `EntityLocalDataSource.kt` - Interfaz
- `EntityRemoteDataSource.kt` - Interfaz
- `usecases/GetEntitiesUseCase.kt` - Caso de uso
- `usecases/OtherActionUseCase.kt` - Más casos de uso

**Framework (8-10 archivos):**
- `database/DBEntity.kt` - Entidad Room
- `database/EntityDao.kt` - DAO
- `database/EntityRoomDataSource.kt` - Implementación internal
- `remote/ServerEntity.kt` - Modelo servidor
- `remote/EntityService.kt` - Retrofit interface
- `remote/EntityServerDataSource.kt` - Implementación internal
- `mapper/Mappers.kt` - Funciones de extensión
- `di/FrameworkModule.kt` - Módulo Hilt

**Presentación (3-4 archivos):**
- `ui/FeatureUiState.kt` - Estado
- `ui/FeatureViewModel.kt` - ViewModel
- `ui/FeatureScreen.kt` - Composable principal
- `di/FeatureModule.kt` - (opcional, si necesita)

---

## Notas Importantes

1. **Siempre de adentro hacia afuera**: Dominio → Framework → Presentación
2. **Visibilidad internal**: Los Data Sources concretos deben ser `internal`
3. **Mappers en la frontera**: Siempre en la capa más externa
4. **StateFlow inmutable**: Público `StateFlow<T>`, privado `MutableStateFlow<T>`
5. **Use Cases con invoke**: Permite llamarlos como funciones
6. **Convenciones de nomenclatura**: Seguir consistentemente
7. **Tests con Turbine**: Para verificar estados de StateFlow
8. **TestDispatcher**: Requerido para tests de ViewModel con corrutinas