# TwiCast TV — Техническое задание и архитектура (v2)

**Проект:** TwiCast TV — лёгкий и быстрый клиент Twitch для Android TV. Открытый проект, публикуется на GitHub.

**Главные цели:**
1. **Плавность** — 60 FPS при навигации пультом, мгновенный отклик на D-Pad, без лагов и подтормаживаний **даже на слабых ТВ-приставках** (ориентир: 2 ГБ RAM, 4-ядерный ARM, Android TV 9+).
2. **Чистый, читаемый код** — проект должен легко читаться и поддерживаться: понятная структура, явные границы между слоями, никаких «умных» трюков ради трюков.
3. **Современный премиальный UI** — дизайн на холсте <ССЫЛКА НА ХОЛСТ «TwiCast TV — дизайн»> + спецификация `docs/design/DESIGN.md` (светлая и тёмная темы).

**Статус:** MVP. Всё, что помечено «после MVP», сейчас не делаем.

---

## 1. Стек

| Задача | Технология | Примечание |
|---|---|---|
| Язык | Kotlin 2.x | K2-компилятор, официальный code style |
| UI | **Compose for TV** (`androidx.tv:tv-material`) + Material 3 | Ряды и сетки — стандартные `LazyRow` / `LazyVerticalGrid` (не устаревшие `TvLazy*`) |
| Архитектура | **MVVM + UDF** | ViewModel → `StateFlow<UiState>`; экран stateless |
| DI | **Hilt** | |
| Сеть | **Retrofit + OkHttp** | Интерсепторы для заголовков, логирование только в debug |
| JSON | **kotlinx.serialization** | `ignoreUnknownKeys = true` |
| Картинки | **Coil 3** | Размер загрузки = размер карточки |
| Асинхронность | Coroutines + Flow | |
| Хранилище | **DataStore** | Токены и настройки |
| Навигация | **Navigation Compose** с type-safe маршрутами (`@Serializable`) | |
| Пагинация | **Paging 3** | Курсоры Helix (`pagination.cursor`) |
| QR | **ZXing** (core) | Экран входа |
| Видео | **Официальный Twitch embed-плеер в WebView** | Неофициальные API не используем |
| Сборка | Gradle Kotlin DSL, version catalog, convention plugins (`build-logic`) | |
| Качество | detekt, ktlint, Android Lint | Обязательны в CI |
| Тесты | JUnit, Turbine, kotlinx-coroutines-test, фейки вместо моков | |
| Производительность | Baseline Profiles, Macrobenchmark, JankStats | |

**minSdk 26, target/compile — последний стабильный.**

---

## 2. Архитектура

### 2.1 Модули

```
twicast-tv/
├─ build-logic/              # convention plugins: android-application, android-library, android-feature, compose
├─ app/                      # точка входа, NavHost, сборка графа Hilt, оболочка с сайдбаром
├─ core/
│  ├─ model/                 # чистые Kotlin-модели: Stream, Game, Channel, User, AuthState (без Android-зависимостей)
│  ├─ network/               # Retrofit-сервисы, DTO, интерсепторы, маппинг ошибок в AppError
│  ├─ datastore/             # хранение токенов и настроек
│  ├─ data/                  # интерфейсы и реализации репозиториев, кэш, маппинг DTO → model
│  ├─ designsystem/          # тема (цвета, типографика, размеры, моушн), базовые компоненты
│  ├─ ui/                    # общие UI-состояния: Loading / Error / Empty, скелетоны
│  └─ testing/               # фейковые репозитории, тестовые данные, правила для тестов
└─ feature/
   ├─ home/
   ├─ categories/            # сетка категорий + стримы категории
   ├─ search/
   ├─ auth/                  # Device Flow
   ├─ following/
   ├─ profile/
   └─ player/                # WebView + embed
```

### 2.2 Правила зависимостей

```
app ─► feature:* ─► core:data ─► core:network, core:datastore
          │             └──────► core:model
          └──► core:designsystem, core:ui, core:model
```

- `feature`-модули **не зависят друг от друга**. Каждый экспортирует свой маршрут и функцию `NavGraphBuilder.xxxScreen(...)`; связывает экраны только `app`.
- `feature` **не видит** `core:network` и DTO — только интерфейсы репозиториев (`core:data`) и модели (`core:model`).
- `core:model` — чистый Kotlin без Android.
- Всё, что не нужно снаружи модуля, — `internal`.

### 2.3 Структура одного feature

```
feature/home/
├─ HomeRoute.kt        // берёт ViewModel, собирает state, передаёт колбэки
├─ HomeScreen.kt       // stateless: HomeScreen(state, onEvent)
├─ HomeViewModel.kt
├─ HomeUiState.kt      // sealed: Loading / Content / Error
├─ HomeEvent.kt        // действия пользователя (если их больше 2–3)
├─ components/         // компоненты, которые нужны только этому экрану
└─ navigation/HomeNavigation.kt
```

### 2.4 Поток данных

```
TwitchApi (DTO) → Repository (маппинг в модели, кэш, AppError) → ViewModel (StateFlow<UiState>) → Route → Screen
```

- Репозитории возвращают `Result<T>` / `Flow<T>` с ошибками типа `sealed interface AppError` (`Network`, `Unauthorized`, `RateLimited`, `NotFound`, `Unknown`). UI не знает о HTTP-кодах и исключениях.
- ViewModel держит **один** `StateFlow<UiState>` на экран, наружу — только неизменяемое состояние. Коллекция в UI — `collectAsStateWithLifecycle()`.
- `AuthRepository` отдаёт `Flow<AuthState>` (`Guest` / `Authorized(user)`); главная перестраивает ряды реактивно при входе/выходе.
- Кэш ответов в памяти с TTL ~60 с для главной и топов (беречь rate limit Helix ~800 очков/мин).

### 2.5 Сеть и авторизация

- OkHttp-интерсептор добавляет `Client-Id` и `Authorization: Bearer <token>`.
- `Authenticator` обрабатывает 401: сброс токена → `AuthState.Guest`.
- **MVP:** `clientId`/`clientSecret` — в `local.properties` → `BuildConfig`, **никогда не коммитить** (в репозитории — `local.properties.example`). **После MVP:** убрать секрет из клиента (public client + Device Flow или прокси).
- Эндпоинты Helix:
  - `GET /helix/streams` — топ стримов (`language`, `first`, `after`)
  - `GET /helix/games/top` — топ категорий
  - `GET /helix/streams?game_id=` — стримы категории
  - `GET /helix/search/channels`, `GET /helix/search/categories` — поиск
  - `GET /helix/users` — профиль текущего пользователя
  - `GET /helix/streams/followed?user_id=` — подписки в эфире (scope `user:read:follows`)
  - `GET /helix/channels/followed?user_id=` — все подписки (`/helix/users/follows` удалён — не использовать)
- «Рекомендации» — в Helix такого эндпоинта нет; MVP-эвристика: топ стримов в категориях из подписок пользователя.

---

## 3. Требования к коду (читаемость и поддержка)

1. **Понятность важнее краткости.** Код читается как текст: говорящие имена (`loadPopularStreams`, а не `load2`), без сокращений, кроме общепринятых (`id`, `url`).
2. **Маленькие единицы.** Composable-функция — одна ответственность, ориентир до ~60–80 строк; длиннее — выделять подкомпоненты. Класс — одна задача; «God»-ViewModel и «Utils»-свалки запрещены.
3. **Состояние явно и неизменяемо.** UiState — `data class` / `sealed interface` с `val`; списки — `ImmutableList` (kotlinx.collections.immutable). Никакого изменяемого состояния, разбросанного по Composable.
4. **Без магических чисел.** Цвета, размеры, радиусы, длительности и easing — только из `core:designsystem` (`TwiColors`, `TwiDimens`, `TwiMotion`, `TwiTypography`).
5. **Без `!!`**, без `lateinit` во ViewModel, без пустых `catch`. Ошибки — через `AppError`, а не исключения в UI.
6. **Зависимости через конструктор** (Hilt). Никаких синглтонов-объектов с состоянием.
7. **Комментарии объясняют «почему», а не «что».** KDoc — для публичного API `core`-модулей.
8. **Единый стиль:** ktlint + detekt без предупреждений; Android Lint без новых ошибок.
9. **Тесты** для ViewModel и репозиториев (фейки из `core:testing`, Turbine для Flow). Каждый новый экран — минимум тест на состояния Loading → Content / Error.
10. **Коммиты** — conventional commits (`feat:`, `fix:`, `refactor:`, `perf:`); один PR — одна логическая задача.

---

## 4. Производительность (обязательные правила)

Цель: **60 FPS** при навигации и скролле, **без пропуска кадров при смене фокуса**, на слабой приставке.

### 4.1 Compose
- **Анимации — только через `graphicsLayer`** (`scaleX/Y`, `alpha`, `translationX`) в лямбда-форме `Modifier.graphicsLayer { … }`. **Запрещено** анимировать размер, padding, ширину — это перекомпоновка и relayout каждый кадр.
- Часто меняющиеся значения читать в фазе отрисовки (лямбда-модификаторы), а не в композиции.
- Модели для UI — стабильные (`@Immutable` / неизменяемые коллекции), чтобы работал skipping.
- В `LazyRow` / `LazyVerticalGrid` — всегда `key` и `contentType`.
- Никаких аллокаций и тяжёлых вычислений в теле Composable; производные значения — `remember` / `derivedStateOf`.
- Фокус: `clipChildren`/запас padding у рядов, чтобы увеличенная карточка не обрезалась и не вызывала перекомпоновку соседей.

### 4.2 Картинки
- Превью Twitch содержат шаблон `{width}x{height}` — запрашивать **ровно размер карточки** (например, 320×180), не 1080p.
- Coil: размер декодирования = размер слота, `crossfade` ≤ 150 мс, общий `ImageLoader` с memory cache.

### 4.3 Потоки и старт
- На главном потоке — только UI. Маппинг больших списков — `Dispatchers.Default`.
- `Application.onCreate` — без тяжёлой инициализации; ленивые зависимости.
- **Baseline Profile** для старта и основных путей (главная → скролл → открыть стрим).

### 4.4 Плеер
- Один WebView, создаётся при входе на экран плеера, уничтожается при выходе (`destroy()`), без утечек Activity-контекста.

### 4.5 Как проверяем
| Метрика | Цель | Чем меряем |
|---|---|---|
| Холодный старт до первого кадра | ≤ 1.5 с на целевой приставке | Macrobenchmark `StartupTimingMetric` |
| Кадры при скролле рядов | p95 ≤ 16 мс, jank < 1% | Macrobenchmark `FrameTimingMetric`, JankStats, `adb shell dumpsys gfxinfo` |
| Память в работе | без роста при долгом листании | Android Studio Profiler |

Проверка — **на реальной слабой приставке после каждого этапа**, а не в конце.

---

## 5. UI, фокус и анимации

Дизайн: холст <ССЫЛКА НА ХОЛСТ «TwiCast TV — дизайн»> (все экраны) и `docs/design/DESIGN.md` (токены, размеры, компоненты). Все цвета — через тему (подготовка к тёмной теме).

1. **Сайдбар.** Свёрнут — 88dp (иконки). При фокусе слева — раскрывается до 264dp **поверх контента** со скримом (`translationX`/`alpha`, 250 мс, `FastOutSlowIn`), подписи — fade-in 150 мс с задержкой 80 мс. Ширина контента не меняется. Компонент — `ModalNavigationDrawer` из tv-material или собственный с теми же правилами.
2. **Фокус карточки.** `scale 1.0 → 1.06`, 200 мс, Decelerate; рамка 3dp `primary` + мягкая тень; обратно — 200 мс. (1.08 из v1 отменено: соседние карточки перекрываются.)
3. **D-Pad.** Предсказуемые переходы `Сайдбар ⇄ Контент`, фокус не теряется «в пустоту»; при возврате на экран фокус восстанавливается на последнем элементе.
4. **Переходы между экранами** — fade 150–200 мс, без белых вспышек (фон окна = `background` темы).
5. **Состояния:** загрузка — скелетоны формы карточек; пусто; ошибка сети с кнопкой «Повторить».

---

## 6. План этапов

| Этап | Что | Результат |
|---|---|---|
| 0 | **Спайк плеера** | Экран, который играет один стрим через официальный embed в WebView. Не работает — стоп, пересматриваем подход |
| 1 | Каркас и дизайн-система | Модули, convention plugins, CI, тема, компоненты (StreamCard, Sidebar, кнопки, чипы), экран-витрина |
| 2 | Сетевой слой | Retrofit-сервис Helix, интерсепторы, AppError, репозитории, кэш, тесты |
| 3 | Гостевой режим | Главная (3 ряда), Категории → Стримы категории, Поиск |
| 4 | Вход (Device Flow) | Код + QR, polling, сохранение токена, `AuthState` |
| 5 | Авторизованный режим | Главная (подписки в эфире, рекомендации, популярное), Подписки (онлайн/офлайн), Профиль, выход |
| 6 | Производительность и релиз | Baseline Profile, бенчмарки на приставке, README со скриншотами, v0.1 |

---

## 7. Промпты для ИИ по этапам

> **Общее правило для всех промптов:** соблюдай разделы 2–5 этого документа (архитектура, требования к коду, производительность, UI). После каждого шага — собрать, установить на ТВ (`adb install -r`), сделать скриншот и показать рядом с макетом.

**Этап 0 — плеер**
> «Сделай в `feature:player` экран, который проигрывает стрим канала по логину через официальный Twitch embed-плеер в WebView (локальная HTML-обёртка + `parent`). Один WebView, корректное создание и уничтожение, обработка Back. Проверь на ТВ и опиши ограничения.»

**Этап 1 — каркас и дизайн-система**
> «Создай многомодульный проект по разделу 2 (build-logic с convention plugins, version catalog, CI на GitHub Actions: build + ktlint + detekt + unit-тесты). В `core:designsystem` реализуй тему и токены из `docs/design/DESIGN.md` (TwiColors, TwiTypography с Plus Jakarta Sans, TwiDimens, TwiMotion) и компоненты: StreamCard и CategoryCard с фокусом (scale 1.06 через graphicsLayer, рамка, тень), Sidebar (88/264dp, overlay, скрим), кнопки, чипы, бейджи LIVE/зрители, скелетоны. Собери экран-витрину компонентов.»

**Этап 2 — сеть и данные**
> «В `core:network` опиши Retrofit-сервис Helix (эндпоинты из 2.5) с kotlinx.serialization, интерсептор Client-Id/Authorization, Authenticator для 401, маппинг ошибок в AppError. В `core:data` — репозитории Streams, Games, Search с маппингом DTO → `core:model`, кэшем TTL 60 с и Paging 3 для списков. Покрой репозитории тестами на фейках.»

**Этап 3 — гостевой режим**
> «Реализуй `feature:home` (ряды: популярное в регионе, топ категорий, мировой топ), `feature:categories` (сетка категорий → стримы категории с фильтром языка) и `feature:search` (ввод с пульта, голосовой ввод, результаты: каналы и категории) по макетам с холста дизайна. Структура Route/Screen/ViewModel/UiState, ключи в lazy-списках, превью нужного размера.»

**Этап 4 — вход**
> «Реализуй `feature:auth`: Twitch Device Authorization Grant (`/oauth2/device`, polling с учётом `interval` и истечения кода), экран с кодом по ячейкам и QR (ZXing), сохранение токена в DataStore, `AuthRepository` с `Flow<AuthState>`.»

**Этап 5 — авторизованный режим**
> «Добавь `GET /helix/users`, `/streams/followed`, `/channels/followed`. Главная реактивно переключается по `AuthState` (подписки в эфире, рекомендации по категориям подписок, популярное). `feature:following` — табы В эфире/Офлайн, `feature:profile` — аватар, ник, счётчики, выход (очистка токенов и состояния).»

**Этап 6 — производительность и релиз**
> «Добавь модуль baselineprofile и Macrobenchmark (старт, скролл главной, открытие стрима). Прогони на целевой приставке, исправь всё, что не укладывается в цели раздела 4.5. Подготовь README.»

---

## 8. Публичный репозиторий

- **README:** описание, GIF/скриншоты с ТВ, схема модулей, стек, «как запустить» (Client ID в `local.properties`), дорожная карта.
- **Лицензия:** Apache-2.0.
- **Секреты:** только в `local.properties` / GitHub Secrets; `local.properties.example` в репозитории.
- **CI:** сборка, ktlint, detekt, unit-тесты на каждый PR; бейдж статуса в README.
- **Twitch:** не использовать логотип и торговые знаки Twitch в названии и иконке; в README — «Проект не аффилирован с Twitch Interactive, Inc.» и ссылка на правила для разработчиков Twitch.

---

## 9. После MVP
Тёмная тема · чат · безопасное хранение секретов / прокси · экран канала (VOD, клипы) · настройки качества · тесты скриншотов (Roborazzi).
