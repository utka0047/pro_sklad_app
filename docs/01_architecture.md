# Архитектура Android-приложения Pro Sklad

## Цель

Разработать мобильное складское приложение, которое позволяет работникам склада в режиме реального времени управлять остатками товаров, фиксировать движения (приход/расход/перемещение), проводить инвентаризацию и сканировать штрихкоды. Приложение должно работать надёжно даже при нестабильном интернет-соединении.

---

## Общая архитектурная концепция

Приложение построено на двух принципах:

- **Clean Architecture** — разделение на три независимых слоя (data, domain, presentation), где каждый слой знает только о слое ниже
- **MVVM** (Model-View-ViewModel) — отделение логики UI от данных, управление состоянием через `LiveData`

Такой подход обеспечивает:
- Тестируемость каждого слоя в изоляции
- Замену источника данных (например, другой API) без изменения UI
- Чёткое разделение ответственности между классами

---

## Слои архитектуры

```
┌─────────────────────────────────────────────┐
│             PRESENTATION LAYER              │
│  Fragments · ViewModels · Adapters · UI     │
├─────────────────────────────────────────────┤
│               DOMAIN LAYER                  │
│  Repository interfaces · Domain models      │
├─────────────────────────────────────────────┤
│                DATA LAYER                   │
│  Room (local) · Retrofit (remote) · DTOs    │
│  Repository implementations · Mappers       │
└─────────────────────────────────────────────┘
```

---

## Слой Domain

Центр архитектуры. Не зависит ни от Android-фреймворка, ни от конкретных реализаций хранилищ.

### Доменные модели

```
domain/model/
  Product.kt           — товар (id, name, sku, category, price, currentStock, minStock, ...)
  Movement.kt          — движение товара (тип, количество, дата, комментарий)
```

**Product** содержит вычисляемое свойство `isLowStock`:
```kotlin
val isLowStock: Boolean
    get() = currentStock <= minStock
```

**MovementType** — перечисление типов операций:
```kotlin
enum class MovementType { IN, OUT, TRANSFER, INVENTORY }
```

**WarehouseSummary** — агрегированная статистика склада: общее количество товаров, стоимость остатков, количество позиций с низким остатком.

### Интерфейсы репозиториев

```kotlin
interface ProductRepository {
    suspend fun getProducts(): Result<List<Product>>
    suspend fun getProductById(id: Int): Result<Product>
    suspend fun getProductBySku(sku: String): Result<Product?>
    suspend fun syncProducts(): Result<Unit>
    // ...
}

interface MovementRepository {
    suspend fun getMovements(): Result<List<Movement>>
    suspend fun createMovement(movement: Movement): Result<Movement>
    suspend fun getWarehouseSummary(): Result<WarehouseSummary>
    suspend fun syncMovements(): Result<Unit>
    // ...
}
```

Интерфейсы объявлены в domain — реализации в data. Это позволяет при тестировании подменить реальный репозиторий моком.

---

## Слой Data

Отвечает за получение, хранение и синхронизацию данных.

### Локальная база данных — Room

**AppDatabase** содержит две таблицы:

| Таблица | Сущность | Основные поля |
|---------|----------|---------------|
| `products` | `ProductEntity` | id, name, sku, category, unit, price, currentStock, minStock, ... |
| `movements` | `MovementEntity` | id, productId, productName, movementType, quantity, quantityBefore, quantityAfter, createdAt |

**DAO (Data Access Object):**

```kotlin
// ProductDao
@Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
suspend fun searchProducts(query: String): List<ProductEntity>

@Query("SELECT * FROM products WHERE current_stock <= min_stock")
suspend fun getLowStockProducts(): List<ProductEntity>

// MovementDao
@Query("SELECT * FROM movements WHERE product_id = :productId ORDER BY created_at DESC")
suspend fun getMovementsForProduct(productId: Int): List<MovementEntity>
```

### Удалённый источник данных — Retrofit

**ApiService** описывает все эндпоинты REST API:

```kotlin
@GET("products/")
suspend fun getProducts(@Query("skip") skip: Int = 0, @Query("limit") limit: Int = 100): List<ProductOutDto>

@POST("movements/")
suspend fun createMovement(@Body movement: MovementCreateDto): MovementOutDto

@GET("analytics/summary")
suspend fun getWarehouseSummary(): WarehouseSummaryDto
```

**Конфигурация OkHttp:**
- Timeout 30 секунд на подключение и чтение
- `HttpLoggingInterceptor` с уровнем BODY для отладки
- `cleartext` трафик разрешён (HTTP, не HTTPS) — настройка для тестового сервера

### Репозитории — offline-first стратегия

Реализации репозиториев придерживаются следующей логики:

```
Запрос данных
    │
    ├─→ Сначала возвращаем данные из Room (мгновенно, без сети)
    │
    └─→ Параллельно запрашиваем API → при успехе обновляем Room → UI реагирует на изменение
```

Это означает, что пользователь видит актуальные данные сразу, а не ждёт сетевого запроса.

### Маппинг данных

**Mappers.kt** содержит функции-расширения для преобразования между слоями:

```
ProductOutDto  →  ProductEntity  →  Product (domain)
MovementOutDto →  MovementEntity →  Movement (domain)
```

### Утилита Result

Sealed-класс для единообразной обработки результатов операций:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

Вспомогательная функция `safeApiCall` оборачивает любой suspend-вызов и автоматически превращает исключения в `Result.Error`.

---

## Слой Presentation

### ViewModels

Каждый экран имеет свой ViewModel, инжектируемый через Hilt:

| ViewModel | Экран | Ключевая логика |
|-----------|-------|-----------------|
| `HomeViewModel` | Главный экран | Статистика, низкие остатки, ручная синхронизация |
| `ProductListViewModel` | Список товаров | Поиск с debounce 300 мс |
| `ProductDetailViewModel` | Карточка товара | Детали товара + история движений |
| `MovementViewModel` | Операция | Создание движения, валидация |
| `ScannerViewModel` | Сканер | Обработка результата сканирования |
| `InventoryViewModel` | Инвентаризация | Учёт фактических остатков, расчёт отклонений |

Пример паттерна в ViewModel:

```kotlin
private val _state = MutableLiveData<UiState>()
val state: LiveData<UiState> = _state

fun loadData() {
    viewModelScope.launch {
        _state.value = UiState.Loading
        when (val result = repository.getProducts()) {
            is Result.Success -> _state.value = UiState.Success(result.data)
            is Result.Error   -> _state.value = UiState.Error(result.message)
        }
    }
}
```

### Fragments и навигация

Приложение использует **Single Activity Architecture** — одна `MainActivity`, все экраны — Fragments.

Навигация управляется **Navigation Component**:

```
HomeFragment
  ├─→ ScannerFragment → ProductDetailFragment → MovementFragment
  ├─→ MovementFragment
  └─→ InventoryFragment

ProductListFragment (Bottom Nav)
  └─→ ProductDetailFragment → MovementFragment
```

`BottomNavigationView` содержит две вкладки: «Главная» и «Товары».

### RecyclerView Adapters

- **ProductAdapter** — список товаров с подсветкой красным при низком остатке (`isLowStock`)
- **MovementAdapter** — история движений с цветовой маркировкой типа операции (зелёный / красный / синий)
- **InventoryAdapter** — редактируемый список с `TextWatcher` для ввода фактических остатков в реальном времени

---

## Dependency Injection — Hilt

Hilt (надстройка над Dagger 2) управляет созданием и жизненным циклом всех зависимостей.

### Модули

```
di/
  AppModule.kt         — BaseUrl, DataStore
  NetworkModule.kt     — OkHttpClient, Retrofit, ApiService
  DatabaseModule.kt    — AppDatabase, ProductDao, MovementDao
  RepositoryModule.kt  — биндинг интерфейсов к реализациям
```

**RepositoryModule** использует `@Binds`:
```kotlin
@Binds @Singleton
abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
```

Это гарантирует, что во всём приложении существует ровно один экземпляр каждого репозитория.

### HiltAndroidApp

```kotlin
@HiltAndroidApp
class SkladApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    // ...
}
```

---

## Структура пакетов

```
com.tornus.pro_sklad/
├── di/                          # Hilt-модули
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── ProductDao.kt
│   │   │   └── MovementDao.kt
│   │   └── entity/
│   │       ├── ProductEntity.kt
│   │       └── MovementEntity.kt
│   ├── remote/
│   │   ├── ApiService.kt
│   │   └── dto/
│   │       ├── ProductDto.kt
│   │       ├── MovementDto.kt
│   │       └── AnalyticsDto.kt
│   └── repository/
│       ├── ProductRepositoryImpl.kt
│       └── MovementRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── Product.kt
│   │   └── Movement.kt
│   └── repository/
│       ├── ProductRepository.kt
│       └── MovementRepository.kt
│
├── presentation/
│   ├── MainActivity.kt
│   ├── adapter/
│   │   ├── ProductAdapter.kt
│   │   ├── MovementAdapter.kt
│   │   └── InventoryAdapter.kt
│   └── ui/
│       ├── home/
│       ├── products/
│       ├── product/
│       ├── movement/
│       ├── scanner/
│       └── inventory/
│
├── util/
│   ├── Result.kt
│   └── Mappers.kt
│
├── worker/
│   └── SyncWorker.kt
│
└── SkladApplication.kt
```

---

## Ключевые технологические решения

| Задача | Решение | Почему |
|--------|---------|--------|
| DI-контейнер | Hilt 2.52 | Официальный DI для Android, меньше boilerplate чем Dagger |
| Локальная БД | Room 2.6.1 | Типобезопасные запросы, Kotlin Coroutines из коробки |
| Сеть | Retrofit 2.11 + OkHttp 4.12 | Де-факто стандарт для Android REST |
| Асинхронность | Kotlin Coroutines + Flow | Нативный подход в Kotlin, без callback hell |
| Навигация | Navigation Component 2.8.5 | Безопасные аргументы, единый back stack |
| Камера | CameraX 1.4.1 | Современный API, lifecycle-aware |
| Штрихкоды | ML Kit Barcode 17.3 | On-device, быстрый, без облака |
| Фоновые задачи | WorkManager 2.9.1 | Гарантированное выполнение, учёт Doze mode |
