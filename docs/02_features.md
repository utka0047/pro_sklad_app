# Реализованные функциональные возможности

## Обзор задач

В рамках доработки приложения были реализованы четыре независимые задачи:

1. **Адаптация под безрамочные экраны** — корректное отображение UI под системными панелями (статусбар, навигационная панель)
2. **Смена валюты и округление суммы** — замена тенге (₸) на рубли (₽), отображение стоимости остатков как целого числа
3. **Исправление работы фонарика** — устранение бага, из-за которого фонарик не включался при сканировании
4. **Фоновая синхронизация данных** — автоматическая и событийная синхронизация с сервером через WorkManager

---

## Задача 1. Адаптация под безрамочные экраны

### Проблема

На современных Android-смартфонах (с выемками, закруглёнными углами, динамическими жестами) система отображает панели поверх контента приложения. Без специальной обработки верхняя часть UI уходила под статусбар, а нижняя — под навигационную панель или жестовую зону.

Это называется **edge-to-edge режим** — контент рисуется под системными панелями, а приложение самостоятельно управляет отступами (insets).

### Решение

**Шаг 1 — включить edge-to-edge режим в `MainActivity`:**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)  // <-- отключаем автоматические отступы
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    // ...
}
```

`WindowCompat.setDecorFitsSystemWindows(window, false)` говорит системе: «не добавляй автоматические отступы для системных панелей — я сам разберусь».

**Шаг 2 — применить insets вручную:**

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    binding.navHostFragment.updatePadding(top = systemBars.top)
    binding.bottomNav.updatePadding(bottom = systemBars.bottom)
    insets
}
```

Логика распределения отступов:
- **Верхний отступ** → контейнер фрагментов (`NavHostFragment`). Это сдвигает весь контент вниз от статусбара.
- **Нижний отступ** → `BottomNavigationView`. Это поднимает навигационную панель над жестовой зоной / кнопками системы.

### Результат

UI корректно отображается на всех типах устройств:
- Телефоны с жестовой навигацией (Android 10+)
- Телефоны с кнопками (три кнопки внизу)
- Устройства с вырезом/каплей под фронтальную камеру

---

## Задача 2. Смена валюты и округление суммы

### Проблема

Приложение изначально использовало казахстанский тенге (₸). Поскольку реальные пользователи работают с рублями, требовалась замена символа. Дополнительно: стоимость остатков (`totalStockValue`) приходила с сервера в виде строки вида `"1234567.89"` — нужно было отображать целое число без копеек.

### Решение

**Карточка товара** (`ProductDetailFragment`):

```kotlin
// Было:
binding.tvPrice.text = "${product.price} ₸"

// Стало:
binding.tvPrice.text = "${product.price} ₽"
```

**Главный экран** (`HomeFragment`) — стоимость остатков с округлением:

```kotlin
// Было:
binding.tvStockValue.text = "${summary.totalStockValue} ₸"

// Стало:
val stockValue = summary.totalStockValue.toDoubleOrNull()?.toLong()
binding.tvStockValue.text = if (stockValue != null) {
    "$stockValue ₽"
} else {
    "${summary.totalStockValue} ₽"
}
```

**Почему `toDoubleOrNull()?.toLong()`:**
- `toDoubleOrNull()` безопасно парсит строку (если строка некорректна — возвращает `null` вместо исключения)
- `.toLong()` отсекает дробную часть (не округляет, а усекает: `1234567.89 → 1234567`)
- Fallback — если строка не парсится, показываем её как есть (но уже с символом ₽)

### Результат

Все денежные значения в приложении отображаются с символом ₽. Стоимость складских остатков показывается как целое число рублей.

---

## Задача 3. Исправление фонарика при сканировании

### Проблема

В `ScannerFragment` была кнопка включения фонарика, но она не работала. Анализ показал причину: код пытался управлять фонариком через старый `Camera` API (из пакета `android.hardware.camera2`), хотя всё остальное в фрагменте использовало **CameraX**. Объект `Camera` из CameraX не сохранялся в поле класса — после вызова `bindToLifecycle()` доступа к нему не было.

### Исправление

**Шаг 1 — добавить поле для хранения `Camera`:**

```kotlin
private var camera: Camera? = null
```

**Шаг 2 — сохранить объект при запуске камеры:**

```kotlin
private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, barcodeAnalyzer) }

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(  // <-- сохраняем Camera
            viewLifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
        )
    }, ContextCompat.getMainExecutor(requireContext()))
}
```

**Шаг 3 — правильно переключать фонарик через CameraControl:**

```kotlin
binding.btnFlashlight.setOnClickListener {
    val isOn = camera?.cameraInfo?.torchState?.value == TorchState.ON
    camera?.cameraControl?.enableTorch(!isOn)
}
```

**Шаг 4 — освобождать ссылку при уничтожении View:**

```kotlin
override fun onDestroyView() {
    super.onDestroyView()
    camera = null
    // ...
}
```

### Почему это работает

CameraX предоставляет интерфейс `Camera`, который включает:
- `Camera.cameraInfo` — информация о камере (в т.ч. `torchState: LiveData<Int>`)
- `Camera.cameraControl` — управление (фокус, зум, вспышка)

`enableTorch(true/false)` — официальный CameraX API для управления фонариком, который работает корректно с жизненным циклом фрагмента.

### Результат

Кнопка фонарика корректно включает и выключает вспышку при сканировании штрихкодов. Состояние фонарика читается из `torchState`, поэтому кнопка всегда отражает реальное состояние (а не предполагаемое).

---

## Задача 4. Фоновая синхронизация данных

### Проблема

Приложение работает в офлайн-первом режиме: данные хранятся локально в Room. Но без механизма синхронизации пользователь видел бы устаревшие данные. Требовалось:
- Периодически синхронизировать данные с сервером в фоне
- Запускать синхронизацию сразу после важных операций (движение товара, инвентаризация)
- Синхронизировать только при наличии сети
- Механизм должен пережить перезагрузку телефона и принудительное закрытие приложения

### Выбор инструмента: WorkManager

`WorkManager` — компонент Android Jetpack для гарантированного выполнения фоновых задач. В отличие от `Service` или `AsyncTask`, он:
- Учитывает Doze mode (режим сна устройства)
- Переживает перезагрузку телефона
- Поддерживает ограничения (выполнять только при наличии Wi-Fi или любой сети)
- Интегрируется с Hilt через `HiltWorkerFactory`

### Реализация

#### SyncWorker — рабочий класс

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val productRepository: ProductRepository,   // инжектируется через Hilt
    private val movementRepository: MovementRepository  // инжектируется через Hilt
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val productsResult = productRepository.syncProducts()
        if (productsResult is com.tornus.pro_sklad.util.Result.Error) return Result.retry()

        val movementsResult = movementRepository.syncMovements()
        if (movementsResult is com.tornus.pro_sklad.util.Result.Error) return Result.retry()

        return Result.success()
    }

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_ONE_TIME  = "sync_one_time"
    }
}
```

Логика работы `doWork()`:
1. Синхронизируем товары — запрашиваем API, обновляем Room
2. Синхронизируем движения — запрашиваем API, обновляем Room
3. Если любой шаг завершился ошибкой — возвращаем `Result.retry()` (WorkManager повторит попытку)
4. При успехе — `Result.success()`

#### Интеграция Hilt + WorkManager

По умолчанию WorkManager создаёт Worker-классы через собственную фабрику и не умеет внедрять зависимости через Hilt. Чтобы это исправить:

**Шаг 1 — Отключить автоматическую инициализацию WorkManager в `AndroidManifest.xml`:**

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

**Шаг 2 — Предоставить кастомную конфигурацию в `SkladApplication`:**

```kotlin
@HiltAndroidApp
class SkladApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    // ...
}
```

`HiltWorkerFactory` знает, как создать любой `@HiltWorker` класс с нужными зависимостями.

#### Периодическая синхронизация (каждые 3 часа)

Запускается при старте приложения в `SkladApplication.onCreate()`:

```kotlin
private fun schedulePeriodicSync() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // только при наличии сети
        .build()

    val request = PeriodicWorkRequestBuilder<SyncWorker>(3, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        SyncWorker.WORK_NAME_PERIODIC,
        ExistingPeriodicWorkPolicy.KEEP,   // если уже запланировано — не трогаем
        request
    )
}
```

`ExistingPeriodicWorkPolicy.KEEP` — если задача уже существует (например, после перезапуска приложения), не создавать новую. Это предотвращает дублирование задач.

#### Событийная синхронизация (после операций)

После успешного движения товара (`MovementFragment`) и после инвентаризации (`InventoryFragment`) запускается одноразовая синхронизация:

```kotlin
private fun enqueueSync() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(requireContext()).enqueueUniqueWork(
        SyncWorker.WORK_NAME_ONE_TIME,
        ExistingWorkPolicy.REPLACE,   // заменяем предыдущую одноразовую задачу, если она ещё не выполнилась
        request
    )
}
```

Вызов в обработчике успешной операции:

```kotlin
if (state.success) {
    Snackbar.make(binding.root, "Операция выполнена", Snackbar.LENGTH_SHORT).show()
    viewModel.clearSuccess()
    enqueueSync()          // <-- запускаем синхронизацию
    findNavController().popBackStack()
}
```

### Схема работы синхронизации

```
Запуск приложения
    └─→ schedulePeriodicSync() → WorkManager
            └─→ каждые 3 часа (при наличии сети)
                    └─→ SyncWorker.doWork()
                            ├─→ productRepository.syncProducts()   → Room update
                            └─→ movementRepository.syncMovements() → Room update

Пользователь делает движение / инвентаризацию
    └─→ enqueueSync() → WorkManager (one-time)
            └─→ сразу (при наличии сети), или при появлении сети
                    └─→ SyncWorker.doWork() (то же самое)
```

### Результат

- Данные на устройстве актуальны в пределах трёх часов без действий пользователя
- После каждой важной операции немедленно запускается синхронизация
- Синхронизация не теряется даже если устройство было выключено в момент операции — WorkManager сохраняет задачу в собственной базе данных
- При отсутствии сети задача ставится в очередь и выполняется при появлении соединения
