# Kanata — Task Plan

---

## Pending

### Task 1: Remove debug Log calls from PlayerViewModel

**Description:** `PlayerViewModel.kt` містить `Log.d` та `Log.e` виклики. Видалити їх.

**Files touched:**
- `features/player/PlayerViewModel.kt`

**Acceptance criteria:**
- [ ] Жодного `Log.d` / `Log.e` у `PlayerViewModel.kt`
- [ ] Проєкт компілюється

**Estimated scope:** XS

---

### Task 2: Dynamic Color (Material You)

**Description:** Додати опцію у Settings → Appearance для використання кольорів з шпалер замість фіксованої палітри. Працює лише на Android 12+ (`Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`). Потребує:
- Нова властивість `useDynamicColor: Boolean` у `SettingsManager`
- Новий `MainState.useDynamicColor` + `MainEvent.ToggleDynamicColor`
- `KanataTheme` отримує параметр `dynamicColor: Boolean`
- `MainActivity` передає значення з `state.useDynamicColor`
- Новий `SettingsItem` у Settings → Appearance (показувати лише якщо SDK >= 31)

**Estimated scope:** S

---

### Task 3: Налаштування розміру сітки аніме

**Description:** Додати перемикання між 2 і 3 колонками у `LazyVerticalGrid`. Потребує:
- Нова властивість `gridColumns: Int` (2 або 3) у `SettingsManager`
- Пробрасувати через `MainState` → `MainScreen` → `AnimeGrid`
- Те саме для `FavoritesScreen` та `MoodResultContent`
- Новий `SettingsItem` у Settings → Appearance з іконками сітки

**Estimated scope:** S

---

### Task 4: Очищення кешу зображень

**Description:** Додати кнопку-дію (не toggle) у Settings → нова секція "Storage" або в "Content". При натисканні — очистити Coil memory cache і disk cache, показати `Snackbar` з підтвердженням.

```kotlin
val imageLoader = ImageLoader.get(context)
imageLoader.memoryCache?.clear()
imageLoader.diskCache?.clear()
```

**Estimated scope:** XS

---

### Task 5: Авто-відтворення наступного епізоду

**Description:** Додати опцію у Settings → нова секція "Playback". Коли `Player.Listener.onPlaybackStateChanged` отримує `STATE_ENDED` і є наступний епізод — показати overlay з таймером (5 сек) і кнопкою "Скасувати", після чого автоматично викликати `PlayerEvent.NextEpisode`. Потребує:
- Нова властивість `autoPlayNextEpisode: Boolean` у `SettingsManager`
- Новий `PlayerState.isCountingDown: Boolean` + `PlayerState.countdownSeconds: Int`
- Новий composable `AutoPlayCountdown.kt` у `player/content/`
- `SettingsItem` у Settings → Playback

**Estimated scope:** M

---

### Task 6: Орієнтація плеєра у повноекранному режимі

**Description:** Зараз fullscreen завжди використовує `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`. Додати опцію у Settings → Playback:
- **Sensor** (поточне) — повертається за сенсором
- **Locked** — завжди `SCREEN_ORIENTATION_LANDSCAPE` (без автоповороту)

Потребує нову властивість `lockLandscape: Boolean` у `SettingsManager`, читати у `PlayerScreen` при вході у fullscreen.

**Estimated scope:** XS

---

---

## Feature Plan: Перемикання серій у плеєрі з екрану завантажень

### Проблема

З екрану завантажень передається лише ONE епізод (`path: String, title: String`).
Плеєр переключає серії лише коли отримує повний список. Потрібно передавати всі
завершені завантаження того самого аніме як список.

### Залежності

```
DownloadDao  (новий запит)
    └── DownloadRepository  (новий метод)
            └── GetSiblingDownloadsUseCase  (новий use case)
                    └── DownloadManagerViewModel  (оновлений PlayDownloaded)
                            └── DownloadManagerEvent.NavigateToPlayer  (нова сигнатура)
                                    └── NavGraph  (оновлений callback)
```

### Рішення

- Групування по `animeTitle` (надійніший за `animeId`, який за замовчуванням 0).
- Сортування: `episodeTitle ASC` — відповідає поточному сортуванню в UI.
- Лише `COMPLETED` + `isManga = 0` — лише відеофайли, що вже готові до відтворення.
- `NavigateToPlayer` змінюється: `(localFilePaths: List<String>, titles: List<String>, startIndex: Int)`.

### Task A: DAO query + Repository method

**Files:**
- `data/local/DownloadDao.kt` — додати `suspend fun getCompletedVideosByAnimeTitle(animeTitle: String): List<DownloadEntity>`
- `domain/repository/DownloadRepository.kt` — додати `suspend fun getCompletedVideosByAnimeTitle(animeTitle: String): List<DownloadItem>`
- `data/repository/DownloadRepositoryImpl.kt` — реалізувати метод

**Acceptance criteria:**
- [ ] SQL: `WHERE status = 'COMPLETED' AND animeTitle = :animeTitle AND isManga = 0 ORDER BY episodeTitle ASC`
- [ ] Mapping entity → domain такий же, як у `getCompletedDownloads()`
- [ ] Проєкт компілюється

---

### Task B: Use case + DI

**Files:**
- `domain/usecase/GetSiblingDownloadsUseCase.kt` *(новий файл)*
- `core/di/UseCaseModule.kt` — зареєструвати use case
- `core/di/ViewModelModule.kt` — додати залежність до `DownloadManagerViewModel`
- `features/downloads/DownloadManagerViewModel.kt` — додати параметр конструктора

**Acceptance criteria:**
- [ ] Use case: `operator fun invoke(item: DownloadItem) = repo.getCompletedVideosByAnimeTitle(item.animeTitle)`
- [ ] Koin граф резолвиться без помилок при старті
- [ ] Проєкт компілюється

---

### Task C: Event model + ViewModel logic

**Files:**
- `features/downloads/model/DownloadManagerEvent.kt` — змінити `NavigateToPlayer`
- `features/downloads/DownloadManagerViewModel.kt` — оновити `PlayDownloaded`

**Acceptance criteria:**
- [ ] `NavigateToPlayer(localFilePaths: List<String>, titles: List<String>, startIndex: Int)` — старі поля прибрані
- [ ] `PlayDownloaded`: викликає use case → фільтрує `localFilePath != null` → будує `file://` prefixed lists → знаходить `startIndex` за `item.id` → надсилає подію
- [ ] Fallback: якщо список порожній або item не знайдено → передає список з одного елементу, `startIndex = 0`
- [ ] Проєкт компілюється

---

### Task D: NavGraph callback update

**Files:**
- `navigation/NavGraph.kt` — оновити `onNavigateToPlayer` lambda
- `features/main/MainScreen.kt` *(якщо прокидає callback)* — оновити сигнатуру

**Acceptance criteria:**
- [ ] `onNavigateToPlayer: (List<String>, List<String>, Int) -> Unit` → передає в `PlayerRoute(episodeUrls, episodeTitles, startIndex)`
- [ ] Жодного місця зі старою сигнатурою `(String, String)` не залишилось (compiler enforced)
- [ ] Натиснути на завантажений епізод → плеєр відкривається на правильній серії
- [ ] Кнопки prev/next активні коли є сусідні завантаження того ж аніме

### Checkpoint: Фіча готова

- [ ] Компіляція без помилок
- [ ] Відкрити епізод з екрану завантажень → prev/next переключає серії того ж аніме
- [ ] Аніме з одним завантаженням → немає кнопок переключення (регресія відсутня)
- [ ] Відкрити з EpisodeListScreen → переключення серій працює як раніше

---

## Feature Plan: Відстеження прогресу перегляду (Watch Progress)

### Проблема

Немає способу бачити яку серію дивились останньою і з якого місця продовжити. В списку серій та закачок жодного візуального індикатора перегляду. При повторному відкритті серія починається з початку.

### Ключові рішення архітектури

- **Canonical key** — `episodePageUrl`: для онлайн-серій це `episode.url`, для завантажень це `DownloadItem.episodePageUrl`. Вони однакові. Для мангових розділів — `chapterUrl` (або `file://` шлях для офлайн).
- **`PlayerRoute` отримує `episodePageUrls: List<String>`** (опційний, default `emptyList()`). Коли список порожній — ключем слугує сам `episodeUrls[i]` (онлайн, де URL вже є page URL). Для завантажень — передається паралельний список `episodePageUrl`.
- **Нова таблиця `watch_progress`**: PK = `episodeUrl`, fields: `positionMs`, `durationMs`, `updatedAt`. Manga: `positionMs` = номер сторінки, `durationMs` = загальна кількість сторінок.
- **"Дивились цю"** — виділяється ТА ОДНА серія в списку з найновішим `updatedAt`. Всі переглянуті (positionMs > 0) серії отримують мінімальний прогрес-бар. Серія вважається завершеною якщо `positionMs ≥ durationMs * 0.9`.
- **Resume**: якщо `positionMs < durationMs * 0.9` — при відкритті плеєр seekTo(positionMs).
- **Збереження прогресу**: кожні 5 секунд під час відтворення + при паузі + при зміні серії.
- **DB version**: 9 → 10 (нова таблиця, migration).

### Граф залежностей

```
WatchProgressEntity + WatchProgressDao  (нова таблиця)
    └── WatchProgressRepository (interface + impl)
            └── Save/Get/Observe use cases
                    ├── PlayerViewModel (зберігає позицію)
                    │       └── PlayerScreen (periodic save, seek resume)
                    ├── EpisodeListViewModel (читає прогрес для відображення)
                    │       └── EpisodeCard (border + label + прогрес-бар)
                    ├── DownloadManagerViewModel (читає прогрес для відображення)
                    │       └── CompletedDownloadCard (border + label + прогрес-бар)
                    └── PageReaderViewModel (зберігає сторінку)
                            └── PageReaderScreen (save on page change)
```

---

### Phase 1: Data Layer

#### Task 1: WatchProgressEntity, WatchProgressDao, DB migration

**Files:**
- `data/local/WatchProgressEntity.kt` *(новий)*
- `data/local/WatchProgressDao.kt` *(новий)*
- `data/local/StorageDatabase.kt` — version 10, додати entity+dao+migration

```kotlin
@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)
```

DAO методи:
- `suspend fun upsert(entity: WatchProgressEntity)` (Insert REPLACE)
- `suspend fun getByUrl(url: String): WatchProgressEntity?`
- `fun observeByUrls(urls: List<String>): Flow<List<WatchProgressEntity>>` — Room `WHERE episodeUrl IN (:urls)`

Migration SQL:
```sql
CREATE TABLE IF NOT EXISTS watch_progress (
    episodeUrl TEXT NOT NULL PRIMARY KEY,
    positionMs INTEGER NOT NULL DEFAULT 0,
    durationMs INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0
)
```

**Acceptance criteria:**
- [ ] `StorageDatabase.version = 10`, entity зареєстрована, DAO підключений
- [ ] MIGRATION_9_10 додана в `DatabaseModule`
- [ ] Компіляція без помилок

---

#### Task 2: WatchProgress domain model + Repository + Use cases + DI

**Files:**
- `domain/model/WatchProgress.kt` *(новий)* — `data class WatchProgress(episodeUrl, positionMs, durationMs, updatedAt)`
- `domain/repository/WatchProgressRepository.kt` *(новий)* — interface
- `data/repository/WatchProgressRepositoryImpl.kt` *(новий)* — impl (upsert, getByUrl, observeByUrls + map to domain)
- `domain/usecase/SaveWatchProgressUseCase.kt` *(новий)* — `invoke(url, positionMs, durationMs)`
- `domain/usecase/GetWatchProgressUseCase.kt` *(новий)* — `suspend invoke(url): WatchProgress?`
- `domain/usecase/ObserveWatchProgressUseCase.kt` *(новий)* — `invoke(urls): Flow<Map<String, WatchProgress>>`
- `core/di/DatabaseModule.kt` — додати `WatchProgressDao`
- `core/di/RepositoryModule.kt` — додати `WatchProgressRepository`
- `core/di/UseCaseModule.kt` — зареєструвати 3 use cases

`ObserveWatchProgressUseCase` повертає `Flow<Map<String, WatchProgress>>` (key = episodeUrl) — зручно для VM lookups.

**Acceptance criteria:**
- [ ] Koin граф резолвиться без помилок
- [ ] Компіляція без помилок

---

### Checkpoint Phase 1

- [ ] Компіляція
- [ ] App стартує без Koin errors

---

### Phase 2: Player — збереження і відновлення позиції

#### Task 3: PlayerRoute + PlayerEvent + PlayerState + PlayerViewModel

**Files:**
- `navigation/Route.kt` — додати `val episodePageUrls: List<String> = emptyList()` до `PlayerRoute`
- `features/player/model/PlayerEvent.kt` — додати `data class SaveProgress(val positionMs: Long, val durationMs: Long) : PlayerEvent`
- `features/player/model/PlayerState.kt` — додати `val resumePositionMs: Long = 0L`
- `features/player/PlayerViewModel.kt` — ін'єкція `SaveWatchProgressUseCase` + `GetWatchProgressUseCase`; у `loadStream()` після встановлення URL: читати progress і ставити `resumePositionMs`; у `handleEvent(SaveProgress)`: зберігати progress

Canonical key logic у VM:
```kotlin
private fun episodeKey(index: Int): String =
    episodePageUrls.getOrNull(index)?.takeIf { it.isNotBlank() }
        ?: episodeUrls.getOrElse(index) { "" }
```

- `core/di/ViewModelModule.kt` — додати 2 `get()` до `PlayerViewModel`

**Acceptance criteria:**
- [ ] `PlayerRoute` має `episodePageUrls`
- [ ] VM зберігає progress при `SaveProgress` event
- [ ] VM читає resume position при `loadStream()` і ставить в state
- [ ] Компіляція

---

#### Task 4: PlayerScreen — periodic save + resume seek + pause save

**Files:**
- `features/player/PlayerScreen.kt`

Зміни:
1. У `DisposableEffect(exoPlayer)` — додати в існуючий `Player.Listener`:
   ```kotlin
   override fun onIsPlayingChanged(isPlaying: Boolean) {
       if (!isPlaying) viewModel.handleEvent(
           PlayerEvent.SaveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0))
       )
   }
   ```
2. Periodic save — новий `LaunchedEffect(state.streamUrl)`:
   ```kotlin
   while (true) {
       delay(5_000)
       if (exoPlayer.isPlaying) {
           viewModel.handleEvent(PlayerEvent.SaveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0)))
       }
   }
   ```
3. Resume seek — у існуючий `LaunchedEffect(state.streamUrl, state.streamHeaders)` після `exoPlayer.prepare()`:
   ```kotlin
   if (state.resumePositionMs > 0) exoPlayer.seekTo(state.resumePositionMs)
   ```

**Acceptance criteria:**
- [ ] Після перегляду 30 сек і закриття — позиція збережена в БД
- [ ] Повторне відкриття → плеєр починає з збереженої позиції
- [ ] При паузі → прогрес зберігається
- [ ] Завершена серія (≥90%) → наступного разу починається з початку

---

#### Task 5: NavGraph + Downloads → передати episodePageUrls у PlayerRoute

**Files:**
- `navigation/NavGraph.kt` — оновити AnimeDetailsRoute callback і Downloads callback
- `features/downloads/DownloadManagerViewModel.kt` — передавати `episodePageUrls = siblings.map { it.episodePageUrl }` у `NavigateToPlayer`
- `features/downloads/model/DownloadManagerEvent.kt` — додати `episodePageUrls: List<String>` до `NavigateToPlayer`

**Acceptance criteria:**
- [ ] `PlayerRoute` отримує `episodePageUrls` при запуску з Downloads
- [ ] Прогрес зберігається по `episodePageUrl` (не по `file://...`)
- [ ] Онлайн-перегляд (EpisodeListScreen) не змінюється — `episodePageUrls` передається порожнім, VM використовує `episodeUrls`

---

### Checkpoint Phase 2

- [ ] Компіляція
- [ ] Дивитись серію → вийти → зайти знову → відтворення з того ж місця
- [ ] Завершена серія → відтворення з початку

---

### Phase 3: EpisodeListScreen — відображення прогресу

#### Task 6: EpisodeListState + EpisodeListViewModel — підписка на прогрес

**Files:**
- `features/episodes/model/EpisodeListState.kt` — додати `val watchProgress: Map<String, WatchProgress> = emptyMap()`
- `features/episodes/EpisodeListViewModel.kt` — ін'єкція `ObserveWatchProgressUseCase`; після завантаження списку серій запустити `observeWatchProgress(urls).onEach { _state.update { s -> s.copy(watchProgress = it) } }.launchIn(viewModelScope)`
- `core/di/ViewModelModule.kt` — додати `get()` до `EpisodeListViewModel`

**Acceptance criteria:**
- [ ] `state.watchProgress` оновлюється реактивно
- [ ] Компіляція

---

#### Task 7: EpisodeCard — border + label + прогрес-бар

**Files:**
- `features/episodes/content/EpisodeCard.kt` — додати параметр `watchProgress: WatchProgress? = null`, `isLastWatched: Boolean = false`
- `features/episodes/EpisodeListScreen.kt` — обчислити `lastWatchedUrl` (max updatedAt серед episodes), передати `watchProgress` + `isLastWatched` у `EpisodeCard`

Візуальні зміни в `EpisodeCard`:
- `isLastWatched = true` → `Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CardDefaults.elevatedShape)` на `ElevatedCard`
- `isLastWatched = true` → маленький `Text("Дивились цю", style=labelSmall, color=primary)` під заголовком
- `watchProgress != null && durationMs > 0` → `LinearProgressIndicator(progress = positionMs / durationMs.toFloat())` під заголовком (4dp висота)

**Acceptance criteria:**
- [ ] Остання переглянута серія виділена рамкою і міткою
- [ ] Прогрес-бар видно для серій що не завершені (< 90%)
- [ ] Завершені серії (≥ 90%) — прогрес-бар показує повний (або не показується)

---

### Checkpoint Phase 3

- [ ] Компіляція
- [ ] Подивитись 2 серії → у списку обидві мають прогрес-бар, остання — рамку + "Дивились цю"

---

### Phase 4: DownloadsScreen — відображення прогресу

#### Task 8: DownloadManagerState + DownloadManagerViewModel + CompletedDownloadCard

**Files:**
- `features/downloads/model/DownloadManagerState.kt` — додати `val watchProgress: Map<String, WatchProgress> = emptyMap()`
- `features/downloads/DownloadManagerViewModel.kt` — ін'єкція `ObserveWatchProgressUseCase`; підписка аналогічна EpisodeListViewModel, але ключі — `completedDownloads.map { it.episodePageUrl }`
- `features/downloads/content/CompletedDownloadCard.kt` — додати параметр `watchProgress: WatchProgress? = null`, `isLastWatched: Boolean = false`, та аналогічні візуальні зміни (border, label, progress bar)
- `features/downloads/DownloadManagerScreen.kt` — обчислити `lastWatchedUrl` per anime group, передати параметри в `CompletedDownloadCard`
- `core/di/ViewModelModule.kt` — додати `get()` до `DownloadManagerViewModel`

**Acceptance criteria:**
- [ ] Той самий візуальний стиль що й в EpisodeListScreen
- [ ] "Дивились цю" та рамка — на одному елементі у кожному аніме-групуванні
- [ ] При зміні останньої переглянутої серії — UI оновлюється реактивно

---

### Checkpoint Phase 4

- [ ] Компіляція
- [ ] Закачки: подивитись серію → в закачках видно прогрес + "Дивились цю"

---

### Phase 5: Manga (PageReaderScreen)

#### Task 9: PageReaderScreen/ViewModel — збереження прогресу розділу

**Files:**
- `navigation/Route.kt` — додати `val chapterPageUrls: List<String> = emptyList()` до `PageReaderRoute`
- `navigation/NavGraph.kt` — передавати `chapterPageUrls` з Downloads при відкритті мангового розділу
- `features/pagereader/model/PageReaderEvent.kt` — додати `data class SaveProgress(val currentPage: Int, val totalPages: Int) : PageReaderEvent`
- `features/pagereader/PageReaderViewModel.kt` — ін'єкція `SaveWatchProgressUseCase` + `GetWatchProgressUseCase`; handle `SaveProgress`; завантажити resume page index
- `features/pagereader/model/PageReaderState.kt` — додати `val resumePageIndex: Int = 0`
- `features/pagereader/PageReaderScreen.kt` — `LaunchedEffect(currentPage)` → emit `SaveProgress`; після завантаження сторінок → `LaunchedEffect(state.resumePageIndex)` → `listState.scrollToItem(state.resumePageIndex)`

**Acceptance criteria:**
- [ ] Читати мангу → вийти → зайти → відкривається на тій самій сторінці
- [ ] У закачках (`CompletedDownloadCard` з `isManga=true`) — відображається прогрес читання

---

### Checkpoint Phase 5 — Фіча повністю готова

- [ ] Компіляція без помилок
- [ ] Відео: зберігається позиція, resume, "Дивились цю" в двох місцях
- [ ] Manga: зберігається сторінка, resume, "Дивились цю" в закачках

---

### Ризики

| Ризик | Вплив | Пом'якшення |
|---|---|---|
| `exoPlayer.duration` = `-1` поки медіа не готова | Mid | Використовувати `coerceAtLeast(0)`, не зберігати якщо 0 |
| Room `IN (:urls)` з порожнім списком кидає помилку | Mid | Перевіряти `if (urls.isEmpty()) return emptyMap()` перед викликом |
| SeekTo перед тим як ExoPlayer готовий | Mid | Seek у `LaunchedEffect(state.streamUrl)` після `prepare()` — ExoPlayer коректно обробляє seek у стані BUFFERING |
| Дві різні серії з однаковим `episodePageUrl` (різні source) | Low | Не буває — url прив'язаний до конкретного джерела |

---

## Feature Plan: Діалог "Продовжити перегляд" при запуску

### Проблема

Коли додаток закривається під час перегляду, при наступному запуску немає жодного натяку що можна продовжити. Користувач мусить самостійно шукати серію у списку або закачках.

### Залежності між фічами

Ця фіча **залежить від Watch Progress** (Phase 1 + Phase 2 цього ж плану): вона читає вже збережені дані про прогрес. Реалізовувати в порядку: Watch Progress Phase 1 → Watch Progress Phase 2 (Task 3–4) → ця фіча.

Крім того, ця фіча **розширює** схему `WatchProgressEntity` (додаткові колонки `playbackUrl`, `episodeTitle`, `animeTitle`, `isManga`) і **розширює** `PlayerEvent.SaveProgress` (передає назви). Ці зміни слід вносити паралельно до Watch Progress Phase 1/2, або як їх частину.

### Ключові рішення

- **`WatchProgressEntity` отримує 4 нових колонки**: `playbackUrl` (реальний URL для плеєра, `file://` або page URL), `episodeTitle`, `animeTitle`, `isManga`. Всі — з DEFAULT значеннями, тому migration просте `ALTER TABLE`.
- **Canonical key лишається `episodeUrl`**. `playbackUrl` = те, що передається безпосередньо у `PlayerRoute.episodeUrls[0]`.
- **`getLastWatched()`** — новий DAO suspend метод: `ORDER BY updatedAt DESC LIMIT 1 WHERE positionMs > 0 AND (durationMs = 0 OR positionMs < durationMs * 0.9)`. Повертає найостанніший незавершений перегляд.
- **Один запит при старті** — `MainViewModel.init` викликає `getLastWatched()` один раз. Результат кладеться в `MainState.pendingContinueWatching`. Після закриття діалогу очищається — більше не з'являється до наступного рестарту.
- **Діалог рендериться в `MainActivity`** поруч з наявними (аналітика, оновлення). Доступ до `backStack` для навігації є там само.
- **Manga-aware**: якщо `isManga = true` → навігація до `PageReaderRoute`; `false` → `PlayerRoute`. Resume позиція підтягується автоматично через `GetWatchProgressUseCase` (вже реалізований в Watch Progress фічі).
- **Налаштування `showContinueWatchingDialog`** (default `true`) — у `SettingsManager` / DataStore, відображується у Settings як toggle.

### Граф залежностей

```
WatchProgressEntity (розширена)
    └── WatchProgressDao.getLastWatched()
            └── GetLastWatchedUseCase
                    └── MainViewModel.init → state.pendingContinueWatching
                            └── MainActivity dialog
                                    └── backStack.add(PlayerRoute / PageReaderRoute)

SettingsManager.showContinueWatchingDialog
    └── MainViewModel.observeSettings() → state.showContinueWatchingDialog
            └── MainState → SettingsScreen toggle
```

---

### Phase 1 — Налаштування

#### Task 1: SettingsManager + MainState/Event/VM + SettingsScreen

**Files:**
- `domain/repository/SettingsManager.kt` — додати `val showContinueWatchingDialog: Flow<Boolean>` + `suspend fun setShowContinueWatchingDialog(Boolean)`
- `data/repository/SettingsManagerImpl.kt` — PreferencesKey `"show_continue_watching_dialog"` (default `true`), getter Flow, setter
- `features/main/model/MainState.kt` — додати `val showContinueWatchingDialog: Boolean = true`
- `features/main/model/MainEvent.kt` — додати `data object ToggleContinueWatchingDialog : MainEvent`
- `features/main/MainViewModel.kt` — у `observeSettings()` підписатись на нове налаштування; у `handleEvent` обробити toggle
- `features/settings/SettingsScreen.kt` — новий `SettingsItem` у секції "Перегляд" (нова секція, або додати до найближчої): "Пропонувати продовжити перегляд" з subtitle "Показувати діалог при запуску додатку"

**Acceptance criteria:**
- [ ] Toggle зберігається у DataStore між сесіями
- [ ] `MainState.showContinueWatchingDialog` реактивно оновлюється
- [ ] Компіляція

---

### Phase 2 — Розширення WatchProgressEntity

#### Task 2: Нові колонки + DAO метод + domain model

> **Зміни стосуються WatchProgress Phase 1 (Task 1) і Phase 2 (Task 3). Застосовувати разом з ними або після.**

**Files:**
- `data/local/WatchProgressEntity.kt` — додати поля:
  ```kotlin
  val playbackUrl: String = episodeUrl,
  val episodeTitle: String = "",
  val animeTitle: String = "",
  val isManga: Boolean = false,
  ```
- `data/local/StorageDatabase.kt` — у MIGRATION_9_10 додати 4 `ALTER TABLE watch_progress ADD COLUMN ...` (або одразу в CREATE TABLE якщо міграція ще не застосована)
- `data/local/WatchProgressDao.kt` — додати:
  ```kotlin
  @Query("SELECT * FROM watch_progress WHERE positionMs > 0 AND (durationMs = 0 OR CAST(positionMs AS REAL) / durationMs < 0.9) ORDER BY updatedAt DESC LIMIT 1")
  suspend fun getLastWatched(): WatchProgressEntity?
  ```
- `domain/model/WatchProgress.kt` — додати `playbackUrl`, `episodeTitle`, `animeTitle`, `isManga` до domain model
- `domain/repository/WatchProgressRepository.kt` — додати `suspend fun getLastWatched(): WatchProgress?`
- `data/repository/WatchProgressRepositoryImpl.kt` — реалізація

**Acceptance criteria:**
- [ ] Поля зберігаються при upsert
- [ ] `getLastWatched()` повертає найсвіжіший незавершений перегляд
- [ ] Компіляція

---

#### Task 3: PlayerEvent.SaveProgress + PageReaderEvent.SaveProgress — передають назви

> **Доповнення до Watch Progress Task 3 (PlayerViewModel) і Task 9 (PageReaderViewModel).**

**Files:**
- `features/player/model/PlayerEvent.kt` — `SaveProgress` отримує `episodeTitle: String` і `animeTitle: String`
- `features/player/PlayerViewModel.kt` — при `handleEvent(SaveProgress)` передавати `episodeTitles[currentIndex]` і `animeTitle` у `SaveWatchProgressUseCase`; `playbackUrl = episodeUrls[currentIndex]`
- `domain/usecase/SaveWatchProgressUseCase.kt` — параметри розширюються або `invoke` приймає `WatchProgress` напряму
- `features/pagereader/model/PageReaderEvent.kt` — `SaveProgress` отримує `chapterTitle: String`, `animeTitle: String`, `chapterUrl: String`, `isManga = true`
- `features/pagereader/PageReaderViewModel.kt` — аналогічно; `playbackUrl = chapterUrls[currentChapterIndex]`

**Acceptance criteria:**
- [ ] Після перегляду в `watch_progress` є `episodeTitle`, `animeTitle`, `playbackUrl`
- [ ] Компіляція

---

### Phase 3 — Діалог і навігація

#### Task 4: GetLastWatchedUseCase + MainViewModel + Dialog у MainActivity

**Files:**
- `domain/usecase/GetLastWatchedUseCase.kt` *(новий)* — `suspend operator fun invoke(): WatchProgress? = repo.getLastWatched()`
- `core/di/UseCaseModule.kt` — зареєструвати use case
- `features/main/model/MainState.kt` — додати `val pendingContinueWatching: WatchProgress? = null`
- `features/main/model/MainEvent.kt` — додати:
  ```kotlin
  data object DismissContinueWatching : MainEvent
  ```
- `features/main/MainViewModel.kt` — ін'єкція `GetLastWatchedUseCase` + `SettingsManager.showContinueWatchingDialog`; у `init`:
  ```kotlin
  viewModelScope.launch {
      if (settingsManager.showContinueWatchingDialog.first()) {
          val last = getLastWatched()
          if (last != null) _state.update { it.copy(pendingContinueWatching = last) }
      }
  }
  ```
  Обробка `DismissContinueWatching` → `_state.update { it.copy(pendingContinueWatching = null) }`
- `core/di/ViewModelModule.kt` — додати `get()` до `MainViewModel`
- `MainActivity.kt` — у `setContent` поруч з analytics/update dialogs:
  ```kotlin
  val progress = state.pendingContinueWatching
  if (progress != null && state.showContinueWatchingDialog) {
      ContinueWatchingDialog(
          progress = progress,
          onContinue = {
              mainViewModel.handleEvent(MainEvent.DismissContinueWatching)
              if (progress.isManga)
                  backStack.add(PageReaderRoute(listOf(progress.playbackUrl), listOf(progress.episodeTitle), 0))
              else
                  backStack.add(PlayerRoute(listOf(progress.playbackUrl), listOf(progress.episodeTitle), 0, episodePageUrls = listOf(progress.episodeUrl)))
          },
          onDismiss = { mainViewModel.handleEvent(MainEvent.DismissContinueWatching) },
      )
  }
  ```
- `features/main/content/ContinueWatchingDialog.kt` *(новий composable)* — AlertDialog:
  - Title: "Продовжити перегляд?"
  - Text: `"${progress.episodeTitle} · ${progress.animeTitle}"` + якщо `durationMs > 0` — рядок з позицією (`mm:ss / mm:ss`)
  - Confirm: "Продовжити"
  - Dismiss: "Не зараз"

**Acceptance criteria:**
- [ ] При першому запуску після перегляду — з'являється діалог з правильною назвою серії і аніме
- [ ] "Продовжити" → відкривається плеєр на збереженій позиції (resume через WatchProgress)
- [ ] "Не зараз" → діалог зникає, більше не з'являється до наступного рестарту
- [ ] Якщо серія вже завершена (≥ 90%) — діалог не з'являється
- [ ] Налаштування вимкнено → діалог ніколи не з'являється

---

### Checkpoint: Фіча готова

- [ ] Запустити додаток після перегляду серії → діалог з'явився
- [ ] Тапнути "Продовжити" → плеєр відкрився на збереженій позиції
- [ ] Тапнути "Не зараз" → діалог зникає, при повторному відкритті вкладки не повертається (лише при рестарті)
- [ ] Settings → toggle вимкнути → більше не з'являється
- [ ] Серія переглянута до кінця → діалог не пропонується

---

### Ризики

| Ризик | Вплив | Пом'якшення |
|---|---|---|
| Діалог з'являється поверх аналітики / оновлення одночасно | Med | Показувати `ContinueWatchingDialog` тільки якщо `!state.showAnalyticsConsent && !state.showUpdateDialog`; або пріоритизувати аналітику |
| `durationMs = 0` для всіх записів (збережено до відомої тривалості) | Low | Умова `durationMs = 0` в SQL → вважати незавершеним і все одно пропонувати |
| ViewModel recreated при rotate → `pendingContinueWatching` лишається в state → діалог знову з'являється | None | ViewModel переживає rotate в Android; `pendingContinueWatching` не перечитується повторно, бо `init` виконується лише раз |
| Manga chapter URL `file://` недійсний після переміщення файлів | Low | Обробляти ExoPlayer error в PageReaderScreen — вже є `RetryClicked` flow |

---

## Completed

- ~~Task: Remove all inline comments~~ — done (крім Log у PlayerViewModel)
- ~~Task: Delete empty contract files~~ — done
- ~~Task: Fix wildcard imports~~ — done
- ~~Task: Create `core/util/Constants.kt`~~ — done
- ~~Task: Split `MoodScreen.kt`~~ — done
- ~~Task: Split `SettingsScreen.kt`~~ — done
- ~~Task: Split `RandomScreen.kt`~~ — done
- ~~Task: Split `PlayerScreen.kt`~~ — done
- ~~Task: Extract `ScoreBadge`~~ — done
- ~~Task: Favorites — full UDF~~ — done
- ~~Task: Mood — full UDF~~ — done
- ~~Task: Random — full UDF~~ — done
- ~~Task: Player — full UDF~~ — done
- ~~Task: EpisodeList — add EpisodeClicked event~~ — done
- ~~UI: Source chips icons (VideoSourceType)~~ — done
- ~~UI: Favorites empty state~~ — done
- ~~UI: Episode list card redesign~~ — done
- ~~UI: Player non-fullscreen layout~~ — done
- ~~UI: PlayerErrorContent redesign~~ — done
- ~~UI: FavoriteIcon redesign (card + TopBar)~~ — done
