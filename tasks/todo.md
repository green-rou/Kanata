# Task List: Kanata Refactor

## Phase 1 — Global Cleanup

- [x] **Task 1** — Remove all inline comments (`// …`) and `Log.*` calls from all `.kt` files
- [x] **Task 2** — Delete `features/mood/MoodContract.kt` and `features/random/RandomImageContract.kt` (empty files)
- [x] **Task 3** — Fix wildcard star imports (all 5 files fixed)

### ✅ Checkpoint 1: Project compiles

---

## Phase 2 — Constants

- [x] **Task 4** — Create `core/util/Constants.kt` with `UiConstants` (StarColor, PlaceholderGradient*)

### ✅ Checkpoint 2: Project compiles

---

## Phase 3 — Composable File Splitting

- [x] **Task 5** — Split `MoodScreen` → `mood/content/` (MoodSelectionContent, MoodCard, MoodResultContent)
- [x] **Task 6** — Split `SettingsScreen` → `settings/content/` (SettingsSection, SettingsItem)
- [x] **Task 7** — Split `RandomScreen` → `random/content/` (PillTabRow, RandomAnimePage, RandomImagePage, PageError)
- [x] **Task 8** — Split `PlayerScreen` → `player/content/` (EpisodeSideButtons, PlayerStatusOverlay)
- [x] **Task 9** — Extract `ScoreBadge` → `main/content/ScoreBadge.kt`

### ✅ Checkpoint 3: All screens render correctly, no duplicate composables

---

## Phase 4 — UDF Event Pattern

- [x] **Task 10** — Favorites: FavoritesEvent + handleEvent() + events Channel
- [x] **Task 11** — Mood: MoodEvent + handleEvent() + events Channel
- [x] **Task 12** — Random: RandomEvent + handleEvent() + events Channel
- [x] **Task 13** — Player: PlayerEvent + handleEvent() + events Channel
- [x] **Task 14** — EpisodeList: EpisodeClicked → NavigateToPlayer via VM

### ✅ Checkpoint 4: Full app flow works end-to-end, no direct VM method calls from composables

---

## Feature: Перемикання серій у плеєрі з екрану завантажень

- [x] **Task A** — `DownloadDao`: додати `getCompletedVideosByAnimeTitle`; `DownloadRepository`: новий метод; `DownloadRepositoryImpl`: реалізація
- [x] **Task B** — Створити `GetSiblingDownloadsUseCase`; зареєструвати в `UseCaseModule`; додати до `ViewModelModule` і конструктору `DownloadManagerViewModel`
- [x] **Task C** — `DownloadManagerEvent.NavigateToPlayer` → нова сигнатура з `List<String>, List<String>, Int`; оновити `PlayDownloaded` у `DownloadManagerViewModel`
- [x] **Task D** — Оновити `NavGraph.onNavigateToPlayer` та `MainScreen` на нову сигнатуру; передавати повний список у `PlayerRoute`

### Checkpoint: З завантажень відкривається плеєр з перемиканням серій в межах одного аніме

---

## Feature: Відстеження прогресу перегляду (Watch Progress)

### Phase 1 — Data layer

- [ ] **Task 1** — `WatchProgressEntity` + `WatchProgressDao` (upsert, getByUrl, observeByUrls); `StorageDatabase` v10 + MIGRATION_9_10
- [ ] **Task 2** — `WatchProgress` domain model; `WatchProgressRepository` interface + impl; `SaveWatchProgressUseCase`, `GetWatchProgressUseCase`, `ObserveWatchProgressUseCase`; DI wiring (DatabaseModule, RepositoryModule, UseCaseModule)

### Checkpoint 1: компіляція, app стартує без Koin errors

### Phase 2 — Player (збереження + resume)

- [ ] **Task 3** — `PlayerRoute.episodePageUrls`; `PlayerEvent.SaveProgress`; `PlayerState.resumePositionMs`; `PlayerViewModel` — save + load resume (+ DI)
- [ ] **Task 4** — `PlayerScreen` — periodic save (5s LaunchedEffect), pause save (Player.Listener), seekTo на resume
- [ ] **Task 5** — `DownloadManagerEvent.NavigateToPlayer` + `DownloadManagerViewModel` + `NavGraph` — передавати `episodePageUrls` у `PlayerRoute`

### Checkpoint 2: подивитись серію → вийти → зайти → продовжує з того ж місця

### Phase 3 — EpisodeListScreen (відображення)

- [ ] **Task 6** — `EpisodeListState.watchProgress`; `EpisodeListViewModel` — observe progress по URLs серій (+ DI)
- [ ] **Task 7** — `EpisodeCard` — border + "Дивились цю" + LinearProgressIndicator; `EpisodeListScreen` — обчислити lastWatchedUrl

### Checkpoint 3: у списку серій видно прогрес-бар і рамку на останній переглянутій

### Phase 4 — DownloadsScreen (відображення)

- [ ] **Task 8** — `DownloadManagerState.watchProgress`; `DownloadManagerViewModel` observe; `CompletedDownloadCard` — border + label + progress bar; `DownloadManagerScreen` — lastWatchedUrl per group (+ DI)

### Checkpoint 4: в закачках видно прогрес + "Дивились цю"

### Phase 5 — Manga

- [ ] **Task 9** — `PageReaderRoute.chapterPageUrls`; `PageReaderEvent.SaveProgress`; `PageReaderState.resumePageIndex`; `PageReaderViewModel` save+resume; `PageReaderScreen` LaunchedEffect save + scroll resume; навігація з Downloads передає `chapterPageUrls`

### Checkpoint 5: читати мангу → вийти → зайти → відкривається на тій самій сторінці

---

## Feature: Діалог "Продовжити перегляд" при запуску

> **Залежить від**: Watch Progress Phase 1 (Task 1–2) + Phase 2 (Task 3–4)

### Phase 1 — Налаштування

- [ ] **Task A** — `SettingsManager` + impl: `showContinueWatchingDialog` Flow + setter; `MainState` + `MainEvent.ToggleContinueWatchingDialog`; `MainViewModel.observeSettings()` + toggle handler; `SettingsScreen` — новий SettingsItem

### Phase 2 — Розширення WatchProgressEntity

- [ ] **Task B** — `WatchProgressEntity` + 4 нових колонки (`playbackUrl`, `episodeTitle`, `animeTitle`, `isManga`); MIGRATION_9_10 або CREATE TABLE; `WatchProgressDao.getLastWatched()`; domain model + repository оновлення
- [ ] **Task C** — `PlayerEvent.SaveProgress` + `PageReaderEvent.SaveProgress` несуть назви; VM передають їх у `SaveWatchProgressUseCase`

### Phase 3 — Діалог і навігація

- [ ] **Task D** — `GetLastWatchedUseCase` + DI; `MainState.pendingContinueWatching`; `MainEvent.DismissContinueWatching`; `MainViewModel.init` завантажує last watched; `ContinueWatchingDialog` composable; `MainActivity` показує діалог і навігує при підтвердженні

### Checkpoint: запуск після перегляду → діалог → "Продовжити" → плеєр з правильної позиції
