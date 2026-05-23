# Работа с данными: API, локальная база и синхронизация

## Цель

Обеспечить надёжную работу приложения в условиях нестабильного интернет-соединения на складе. Пользователь должен видеть данные мгновенно, а не ждать ответа от сервера. При этом данные должны оставаться актуальными.

---

## Серверная часть (Backend)

Приложение взаимодействует с REST API на базе **FastAPI** (Python).

**Адрес сервера:** `http://213.21.252.154:8000/`

### Группы эндпоинтов

#### Товары (`/products/`)

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/products/` | Список товаров с пагинацией и фильтрами |
| `GET` | `/products/categories` | Список категорий |
| `GET` | `/products/{id}` | Карточка товара |
| `POST` | `/products/` | Создать товар |
| `PUT` | `/products/{id}` | Обновить товар |
| `DELETE` | `/products/{id}` | Удалить товар |

Параметры GET `/products/`:
- `skip`, `limit` — пагинация
- `category` — фильтр по категории
- `low_stock_only` — только товары с низким остатком

#### Движения (`/movements/`)

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/movements/` | Список движений с фильтрами |
| `GET` | `/movements/{id}` | Одно движение |
| `POST` | `/movements/` | Зафиксировать движение |

Параметры GET `/movements/`:
- `product_id` — движения конкретного товара
- `movement_type` — IN / OUT / TRANSFER / INVENTORY
- `date_from`, `date_to` — временной диапазон

#### Аналитика (`/analytics/`)

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/analytics/summary` | Сводка по складу |
| `GET` | `/analytics/low-stock` | Товары с низким остатком |
| `GET` | `/analytics/movements-chart` | График движений за 30 дней |
| `GET` | `/analytics/top-products` | Топ-10 товаров |

---

## Сетевой слой (Retrofit + OkHttp)

### Настройка клиента

```kotlin
// NetworkModule.kt
@Provides @Singleton
fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}

@Provides @Singleton
fun provideRetrofit(okHttpClient: OkHttpClient, @BaseUrl baseUrl: String): Retrofit {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
```

`HttpLoggingInterceptor` с уровнем `BODY` выводит в Logcat полное содержимое запросов и ответов — удобно при отладке интеграции с новым API.

### DTO-классы

DTO (Data Transfer Object) — классы, точно отражающие структуру JSON-ответов API:

```kotlin
// ProductOutDto.kt
data class ProductOutDto(
    @SerializedName("id")            val id: Int,
    @SerializedName("name")          val name: String,
    @SerializedName("sku")           val sku: String,
    @SerializedName("category")      val category: String,
    @SerializedName("unit")          val unit: String,
    @SerializedName("price")         val price: Double,
    @SerializedName("description")   val description: String?,
    @SerializedName("min_stock")     val minStock: Int,
    @SerializedName("current_stock") val currentStock: Int,
    @SerializedName("created_at")    val createdAt: String,
    @SerializedName("updated_at")    val updatedAt: String
)

// MovementCreateDto.kt — тело запроса при создании движения
data class MovementCreateDto(
    @SerializedName("product_id")     val productId: Int,
    @SerializedName("movement_type")  val movementType: String,  // "IN", "OUT", "TRANSFER", "INVENTORY"
    @SerializedName("quantity")       val quantity: Int,
    @SerializedName("comment")        val comment: String?
)

// WarehouseSummaryDto.kt
data class WarehouseSummaryDto(
    @SerializedName("total_products")    val totalProducts: Int,
    @SerializedName("total_stock_value") val totalStockValue: String,
    @SerializedName("low_stock_count")   val lowStockCount: Int,
    @SerializedName("movements_today")   val movementsToday: Int
)
```

`@SerializedName` обеспечивает маппинг snake_case полей API на camelCase поля Kotlin без дополнительной настройки.

---

## Локальная база данных (Room)

### Устройство базы

Room — SQLite-обёртка с поддержкой Kotlin Coroutines. База данных хранится в файле `sklad.db` на устройстве.

**Инициализация:**

```kotlin
@Database(
    entities = [ProductEntity::class, MovementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun movementDao(): MovementDao
}
```

`fallbackToDestructiveMigration()` — при изменении версии схемы база пересоздаётся. Для учебного проекта это приемлемо; в продакшне потребовались бы миграции.

### Сущности

**ProductEntity** (таблица `products`):

```kotlin
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val sku: String,
    val category: String,
    val unit: String,
    val price: Double,
    val description: String?,
    @ColumnInfo(name = "min_stock")     val minStock: Int,
    @ColumnInfo(name = "current_stock") val currentStock: Int,
    @ColumnInfo(name = "created_at")    val createdAt: String,
    @ColumnInfo(name = "updated_at")    val updatedAt: String
)
```

**MovementEntity** (таблица `movements`):

```kotlin
@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "product_id")       val productId: Int,
    @ColumnInfo(name = "product_name")     val productName: String,
    @ColumnInfo(name = "product_sku")      val productSku: String,
    @ColumnInfo(name = "movement_type")    val movementType: String,
    val quantity: Int,
    @ColumnInfo(name = "quantity_before")  val quantityBefore: Int,
    @ColumnInfo(name = "quantity_after")   val quantityAfter: Int,
    val comment: String?,
    @ColumnInfo(name = "created_at")       val createdAt: String
)
```

### DAO-запросы

```kotlin
@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    // Полнотекстовый поиск по имени и SKU
    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
    suspend fun searchProducts(query: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE current_stock <= min_stock")
    suspend fun getLowStockProducts(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}
```

`OnConflictStrategy.REPLACE` при `insertAll` — если товар с таким `id` уже есть, он обновляется. Это ключевой механизм синхронизации: при каждом sync мы просто вставляем все данные с сервера, заменяя устаревшие.

---

## Маппинг данных

Данные проходят три уровня представления:

```
API JSON → DTO → Entity (Room) → Domain Model → UI
```

Все преобразования сосредоточены в **Mappers.kt**:

```kotlin
// DTO → Entity
fun ProductOutDto.toEntity(): ProductEntity = ProductEntity(
    id = id,
    name = name,
    sku = sku,
    // ...
)

// Entity → Domain Model
fun ProductEntity.toDomain(): Product = Product(
    id = id,
    name = name,
    sku = sku,
    currentStock = currentStock,
    minStock = minStock,
    // ...
)

// Domain Model → DTO (при отправке на сервер)
fun Movement.toCreateDto(): MovementCreateDto = MovementCreateDto(
    productId = productId,
    movementType = type.name,
    quantity = quantity,
    comment = comment
)
```

Разделение на DTO и Entity необходимо: структура API может не совпадать со структурой локальной базы. Например, API возвращает `product_name` вместе с движением (для удобства отображения), а в БД это поле денормализовано в `movements`.

---

## Стратегия offline-first

### Принцип работы

```
Пользователь открывает экран
       │
       ▼
  Запрос в Room (мгновенно)
       │
       ▼
  UI отображает данные из кэша
       │
       │   (параллельно)
       ▼
  Запрос к API
       │
       ├── Успех → обновляем Room → UI обновляется (LiveData/Flow)
       │
       └── Ошибка → UI остаётся с кэшированными данными
                  → пользователь видит данные (пусть и не самые свежие)
```

### Реализация в репозитории

```kotlin
// ProductRepositoryImpl.kt
override suspend fun getProducts(): Result<List<Product>> {
    return try {
        // 1. Пробуем получить с сервера
        val dtos = apiService.getProducts()
        // 2. Сохраняем в локальную БД
        val entities = dtos.map { it.toEntity() }
        productDao.deleteAll()
        productDao.insertAll(entities)
        // 3. Возвращаем domain-модели
        Result.Success(entities.map { it.toDomain() })
    } catch (e: Exception) {
        // 4. При ошибке сети — отдаём данные из БД
        val cached = productDao.getAllProducts()
        if (cached.isNotEmpty()) {
            Result.Success(cached.map { it.toDomain() })
        } else {
            Result.Error("Нет данных и нет соединения", e)
        }
    }
}
```

### Метод syncProducts (для WorkManager)

```kotlin
override suspend fun syncProducts(): Result<Unit> {
    return try {
        val dtos = apiService.getProducts(limit = 1000)
        val entities = dtos.map { it.toEntity() }
        productDao.deleteAll()
        productDao.insertAll(entities)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Sync failed", e)
    }
}
```

Отличие от `getProducts`: возвращает `Result<Unit>` — синхронизация не возвращает данные, только сохраняет их в базу. UI обновится через собственный запрос.

---

## Обработка ошибок

### Утилита safeApiCall

```kotlin
suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    return try {
        Result.Success(apiCall())
    } catch (e: HttpException) {
        Result.Error("Ошибка сервера: ${e.code()}", e)
    } catch (e: IOException) {
        Result.Error("Нет подключения к интернету", e)
    } catch (e: Exception) {
        Result.Error("Неизвестная ошибка: ${e.message}", e)
    }
}
```

Использование:

```kotlin
override suspend fun createMovement(movement: Movement): Result<Movement> {
    return safeApiCall {
        val dto = apiService.createMovement(movement.toCreateDto())
        dto.toEntity().let { entity ->
            movementDao.insert(entity)
            entity.toDomain()
        }
    }
}
```

### UiState в ViewModel

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

Во Fragment:

```kotlin
viewModel.state.observe(viewLifecycleOwner) { state ->
    when (state) {
        is UiState.Loading  -> binding.progressBar.isVisible = true
        is UiState.Success  -> {
            binding.progressBar.isVisible = false
            adapter.submitList(state.data)
        }
        is UiState.Error    -> {
            binding.progressBar.isVisible = false
            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }
}
```

---

## Поиск товаров с debounce

В `ProductListViewModel` реализован поиск с задержкой, чтобы не делать запрос к базе после каждого нажатия клавиши:

```kotlin
private var searchJob: Job? = null

fun onSearchQueryChanged(query: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        delay(300)  // ждём 300 мс после последнего символа
        val result = productRepository.searchProducts(query)
        _products.value = result
    }
}
```

Debounce 300 мс — стандартное значение для поля поиска. Если пользователь набирает быстро, промежуточные запросы отменяются.

---

## Типы движений товара

```kotlin
enum class MovementType(val displayName: String) {
    IN("Приход"),
    OUT("Расход"),
    TRANSFER("Перемещение"),
    INVENTORY("Инвентаризация")
}
```

При создании движения сервер автоматически пересчитывает `currentStock` у товара. Приложение после этого запускает синхронизацию через WorkManager — чтобы обновлённое значение остатка появилось в локальной базе.

---

## Итоговая схема потока данных

```
┌─────────────────────────────────────────────────────────────┐
│                      FastAPI Backend                         │
│              http://213.21.252.154:8000/                     │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/JSON
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Retrofit + OkHttp                          │
│              ApiService (DTO-классы)                         │
└────────────────────────┬────────────────────────────────────┘
                         │ DTO
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              Repository (ProductRepositoryImpl,              │
│                        MovementRepositoryImpl)               │
│         offline-first: сначала кэш, потом сеть              │
└──────┬────────────────────────────────────────┬─────────────┘
       │ Entity                                  │ Domain Model
       ▼                                         ▼
┌─────────────┐                        ┌──────────────────────┐
│    Room     │                        │     ViewModel         │
│  sklad.db   │ ◄── SyncWorker ──────► │  LiveData<UiState>   │
└─────────────┘                        └──────────┬───────────┘
                                                   │ observe
                                                   ▼
                                        ┌──────────────────────┐
                                        │      Fragment / UI    │
                                        │  RecyclerView / Views │
                                        └──────────────────────┘
```
