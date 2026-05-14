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
