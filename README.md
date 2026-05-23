# Pro Sklad — Мобильная система управления складом

Android-приложение для складского учёта: движения товаров, инвентаризация, сканирование штрихкодов, синхронизация с сервером.

---

## Требования

| | Минимум |
|-|---------|
| Android Studio | Hedgehog (2023.1.1) или новее |
| JDK | 11 |
| Android SDK | API 30 (Android 11) |
| Устройство / эмулятор | Android 11+ |

---

## Запуск

### 1. Клонировать репозиторий

```bash
git clone <url-репозитория>
cd pro_sklad
```

### 2. Открыть в Android Studio

**File → Open** → выбрать папку `pro_sklad`

Android Studio автоматически загрузит Gradle и все зависимости. Дождитесь завершения синхронизации (прогресс-бар внизу).

### 3. Убедиться, что сервер доступен

Приложение работает с REST API по адресу:

```
http://213.21.252.154:8000/
```

Адрес задан в `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BASE_URL", "\"http://213.21.252.154:8000/\"")
```

Чтобы сменить адрес — изменить строку выше и пересобрать проект.

### 4. Запустить приложение

- Подключить Android-устройство по USB (с включённой отладкой по USB) **или** запустить эмулятор
- Нажать **Run → Run 'app'** (Shift+F10)

---

## Сборка APK

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Готовый файл появится по пути:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Структура проекта

```
app/src/main/java/com/tornus/pro_sklad/
├── di/             # Hilt-модули (DI)
├── data/           # Room, Retrofit, репозитории
├── domain/         # Модели, интерфейсы репозиториев
├── presentation/   # Fragments, ViewModels, адаптеры
├── worker/         # SyncWorker (фоновая синхронизация)
└── util/           # Result, Mappers
```

Подробнее — в папке [`docs/`](docs/):
- [`docs/00_requirements.md`](docs/00_requirements.md) — Техническое задание
- [`docs/01_architecture.md`](docs/01_architecture.md) — Архитектура приложения
- [`docs/02_features.md`](docs/02_features.md) — Реализованные функции
- [`docs/03_data_and_api.md`](docs/03_data_and_api.md) — API и работа с данными

---

## Разрешения

Приложение запрашивает:
- `CAMERA` — для сканирования штрихкодов
- `INTERNET` — для синхронизации с сервером
- `ACCESS_NETWORK_STATE` — для проверки наличия сети перед синхронизацией
