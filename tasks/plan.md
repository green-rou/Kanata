# Implementation Plan: Kanata App Cleanup & Architecture Refactor

## Overview

Systematic cleanup of the Kanata Android app across four concerns:
1. Remove all comments and debug logs — code should be self-documenting
2. Extract hardcoded constants (colors, strings) to a dedicated file
3. Split multi-composable screen files into individual files under `features/xxx/content/`
4. Enforce consistent UDF pattern: every user action goes through `handleEvent()`, every nav/one-time UI effect goes through a `Channel<XxxEvent>` and is collected in `LaunchedEffect`

## Architecture Decisions

- **Event model is a single sealed interface per feature**: user actions + nav/UI effects share one file. User actions are handled in `handleEvent()`; nav/UI effects are emitted via `_events` Channel and collected by the screen.
- **No new abstraction layers**: only apply the existing MainEvent/AnimeDetailsEvent pattern (which already works) to the features that are missing it.
- **Composable extraction rule**: any `private fun` composable in a screen file moves to its own file in `features/xxx/content/`. `internal` visibility where needed for cross-file access within the feature.
- **Constants file**: `core/util/Constants.kt` — only values repeated or non-obvious. Layout magic numbers (dp values) stay inline.

## Dependency Graph

```
Constants.kt (no deps)
    │
    ├── UI composables (AnimeCard, AnimeDetailContent, …)
    │
    └── Feature event models (no deps on composables)
            │
            └── ViewModels
                    │
                    └── Screens (compose all pieces together)
```

Implementation order: Constants → Composable splits → Event models + ViewModels → Screen updates.

---

## Phase 1: Global Cleanup

### Task 1: Remove all inline comments and debug logs

**Description:** Strip every `// …` inline comment and every `Log.d`/`Log.e`/`Log.w` call from all `.kt` files. Code should speak for itself; debug logs don't belong in committed code.

**Files touched:**
- `features/details/AnimeDetailsViewModel.kt` — 2× Log calls
- `features/episodes/EpisodeListViewModel.kt` — 2× Log calls
- `features/player/PlayerViewModel.kt` — 2× Log calls
- `features/main/content/AnimeCard.kt` — 2 inline comments
- `features/player/PlayerScreen.kt` — 5 inline comments
- `features/mood/MoodScreen.kt` — 1 inline comment
- `navigation/NavGraph.kt` — 1 inline comment

**Acceptance criteria:**
- [ ] Zero `//` comment lines in any `.kt` source file
- [ ] Zero `Log.d`, `Log.e`, `Log.w` calls in any `.kt` file
- [ ] Project still compiles

**Verification:** `grep -rn "//\|Log\." app/src/main/java` returns no results in source files.

**Estimated scope:** S (automated)

---

### Task 2: Delete empty contract files

**Description:** `MoodContract.kt` and `RandomImageContract.kt` contain only a package declaration. Delete them.

**Files touched:**
- `features/mood/MoodContract.kt` — delete
- `features/random/RandomImageContract.kt` — delete

**Acceptance criteria:**
- [ ] Both files removed from the filesystem

**Estimated scope:** XS

---

### Task 3: Fix wildcard imports

**Description:** Replace star imports with explicit imports for clarity.

**Files touched:**
- `features/details/content/AnimeDetailContent.kt` — `import androidx.compose.foundation.layout.*` and `import androidx.compose.material3.*`
- `features/favorites/FavoritesViewModel.kt` — `import kotlinx.coroutines.flow.*`

**Acceptance criteria:**
- [ ] No `*` import in any `.kt` source file
- [ ] Project compiles

**Estimated scope:** S

---

### Checkpoint 1
- [ ] Project builds cleanly with no errors

---

## Phase 2: Constants Extraction

### Task 4: Create `core/util/Constants.kt`

**Description:** Extract repeated or opaque hardcoded values that appear in multiple files or are non-obvious without context.

Candidates found:
- `Color(0xFFFFD700)` — gold/star rating color (used in `AnimeCard`, `AnimeDetailContent`)
- `Color(0xFF2D1B69)`, `Color(0xFF6A1B9A)` — placeholder gradient in `AnimeDetailContent`
- `Color.Black.copy(alpha = 0.55f)` top scrim, `Color.Black.copy(alpha = 0.82f)` bottom scrim in `AnimeCard` — leave inline (one-off UI detail)

**New file:** `core/util/Constants.kt`

```kotlin
package com.greenrou.kanata.core.util

import androidx.compose.ui.graphics.Color

object UiConstants {
    val StarColor = Color(0xFFFFD700)
    val PlaceholderGradientStart = Color(0xFF2D1B69)
    val PlaceholderGradientEnd = Color(0xFF6A1B9A)
}
```

**Files updated:** `AnimeCard.kt`, `AnimeDetailContent.kt`

**Acceptance criteria:**
- [ ] `UiConstants` file exists at `core/util/Constants.kt`
- [ ] No inline `Color(0xFFFFD700)`, `Color(0xFF2D1B69)`, `Color(0xFF6A1B9A)` literals remain in feature files
- [ ] Project compiles

**Estimated scope:** S

---

### Checkpoint 2
- [ ] Project builds, no regressions in layout

---

## Phase 3: Composable File Splitting

### Task 5: Split `MoodScreen.kt`

**Description:** Extract private composables from `MoodScreen.kt` into separate files under `features/mood/content/`.

**New files:**
- `features/mood/content/MoodSelectionContent.kt` — `MoodSelection` composable
- `features/mood/content/MoodCard.kt` — `MoodCard` composable
- `features/mood/content/MoodResultContent.kt` — `MoodResult` composable

**Modified:** `features/mood/MoodScreen.kt` — becomes a thin coordinator (only `MoodScreen()` entry point)

**Acceptance criteria:**
- [ ] `MoodScreen.kt` contains only the public `MoodScreen()` function
- [ ] Three new files in `features/mood/content/`
- [ ] Mood UI is visually identical

**Estimated scope:** M

---

### Task 6: Split `SettingsScreen.kt`

**Description:** Extract private composables to `features/settings/content/`.

**New files:**
- `features/settings/content/SettingsSection.kt` — `SettingsSection`
- `features/settings/content/SettingsItem.kt` — `SettingsItem`

**Modified:** `features/settings/SettingsScreen.kt` — only public `SettingsScreen()` remains

**Acceptance criteria:**
- [ ] `SettingsScreen.kt` contains only `SettingsScreen()`
- [ ] Two new files in `features/settings/content/`
- [ ] Settings UI unchanged

**Estimated scope:** S

---

### Task 7: Split `RandomScreen.kt`

**Description:** Extract private composables and helper functions from `RandomScreen.kt`.

**New files:**
- `features/random/content/PillTabRow.kt` — `PillTabRow`
- `features/random/content/RandomAnimePage.kt` — `RandomAnimePage`
- `features/random/content/RandomImagePage.kt` — `RandomImagePage` (keeps `downloadImage`, `setAsWallpaper` as private top-level functions in the same file since they are exclusively used there)
- `features/random/content/PageError.kt` — `PageError`

**Modified:** `features/random/RandomScreen.kt` — thin coordinator only

**Acceptance criteria:**
- [ ] `RandomScreen.kt` contains only `RandomScreen()`
- [ ] Four new files in `features/random/content/`
- [ ] Random screen UI unchanged

**Estimated scope:** M

---

### Task 8: Split `PlayerScreen.kt`

**Description:** Extract private composables to `features/player/content/`.

**New files:**
- `features/player/content/EpisodeSideButtons.kt` — `EpisodeSideButtons`
- `features/player/content/PlayerStatusOverlay.kt` — `PlayerStatusOverlay`

**Modified:** `features/player/PlayerScreen.kt` — only `PlayerScreen()` remains

**Acceptance criteria:**
- [ ] `PlayerScreen.kt` contains only the public `PlayerScreen()` function
- [ ] Two new files in `features/player/content/`
- [ ] Player UI unchanged

**Estimated scope:** S

---

### Task 9: Extract `ScoreBadge` from `AnimeCard.kt`

**Description:** `ScoreBadge` is a private composable inside `AnimeCard.kt`. Move it to its own file.

**New file:** `features/main/content/ScoreBadge.kt`

**Modified:** `features/main/content/AnimeCard.kt`

**Acceptance criteria:**
- [ ] `ScoreBadge.kt` exists in `features/main/content/`
- [ ] `AnimeCard.kt` no longer defines `ScoreBadge`
- [ ] Card UI unchanged

**Estimated scope:** XS

---

### Checkpoint 3
- [ ] All screens render correctly
- [ ] No duplicate composable definitions

---

## Phase 4: Consistent UDF Event Pattern

Pattern to apply uniformly:
- All user actions → `viewModel.handleEvent(XxxEvent.Action)`
- VM processes action → may emit nav/UI event via `_events.send(XxxEvent.NavOrEffect)`
- Screen collects: `LaunchedEffect(Unit) { viewModel.events.collect { … } }`

### Task 10: Favorites — full UDF

**Current problems:**
- `FavoritesScreen` calls `viewModel.toggleFavorite(it)` and `viewModel.loadMore()` directly
- `FavoritesScreen` passes `onNavigateToDetails` directly to `AnimeGrid.onAnimeClick`
- `FavoritesViewModel` has no `handleEvent()` and no `events` Channel

**Changes:**

`features/favorites/model/FavoritesEvent.kt` (new):
```kotlin
sealed interface FavoritesEvent {
    data class AnimeClicked(val animeId: Int) : FavoritesEvent
    data class ToggleFavorite(val animeId: Int) : FavoritesEvent
    data object LoadMore : FavoritesEvent
    data class NavigateToDetails(val animeId: Int) : FavoritesEvent
}
```

`FavoritesViewModel.kt`:
- Add `private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)`
- Add `val events = _events.receiveAsFlow()`
- Add `fun handleEvent(event: FavoritesEvent)`
- Replace direct methods `toggleFavorite()` and `loadMore()` with cases in `handleEvent()`

`FavoritesScreen.kt`:
- Add `LaunchedEffect(Unit)` collecting `viewModel.events`
- Change `onAnimeClick = onNavigateToDetails` → `onAnimeClick = { viewModel.handleEvent(FavoritesEvent.AnimeClicked(it)) }`
- Change `onFavoriteClick = { viewModel.toggleFavorite(it) }` → `onFavoriteClick = { viewModel.handleEvent(FavoritesEvent.ToggleFavorite(it)) }`
- Change `onLoadMore = { viewModel.loadMore() }` → `onLoadMore = { viewModel.handleEvent(FavoritesEvent.LoadMore) }`

**Acceptance criteria:**
- [ ] `FavoritesEvent.kt` exists in `features/favorites/model/`
- [ ] `FavoritesViewModel` exposes `events` flow and `handleEvent()`
- [ ] No direct method calls from `FavoritesScreen` on the ViewModel
- [ ] Favorites navigation and toggle still work

**Estimated scope:** M

---

### Task 11: Mood — full UDF

**Current problems:**
- `MoodScreen` calls `viewModel.clearMood()` and `viewModel.selectMood(it)` directly
- `MoodScreen` passes `onNavigateToDetails` directly to composables

**Changes:**

`features/mood/model/MoodEvent.kt` (new):
```kotlin
sealed interface MoodEvent {
    data class SelectMood(val mood: Mood) : MoodEvent
    data object ClearMood : MoodEvent
    data class AnimeClicked(val animeId: Int) : MoodEvent
    data class NavigateToDetails(val animeId: Int) : MoodEvent
}
```

`MoodViewModel.kt`:
- Add `_events` Channel and `events` flow
- Add `handleEvent(event: MoodEvent)`
- Route `selectMood` and `clearMood` through `handleEvent()`
- `AnimeClicked` emits `NavigateToDetails` via Channel

`MoodScreen.kt`:
- Add `LaunchedEffect` collecting events
- All VM calls go through `handleEvent()`

**Acceptance criteria:**
- [ ] `MoodEvent.kt` exists in `features/mood/model/`
- [ ] No direct method calls on MoodViewModel from the screen
- [ ] Mood selection and anime navigation work correctly

**Estimated scope:** M

---

### Task 12: Random — full UDF

**Current problems:**
- `RandomScreen` calls `viewModel.loadRandomAnime()`, `viewModel.loadRandomImage()`, `viewModel.toggleFavorite()` directly
- Navigation to details via `onNavigateToDetails` callback passed directly

**Changes:**

`features/random/model/RandomEvent.kt` (new):
```kotlin
sealed interface RandomEvent {
    data object RefreshAnime : RandomEvent
    data object RefreshImage : RandomEvent
    data object ToggleFavorite : RandomEvent
    data class AnimeClicked(val animeId: Int) : RandomEvent
    data class NavigateToDetails(val animeId: Int) : RandomEvent
}
```

`RandomImageViewModel.kt`:
- Add `_events` Channel and `events` flow
- Add `handleEvent(event: RandomEvent)`
- Route all public methods through `handleEvent()`

`RandomScreen.kt` (and its content composables):
- All VM calls go through `handleEvent()`
- Collect events for navigation

**Acceptance criteria:**
- [ ] `RandomEvent.kt` exists in `features/random/model/`
- [ ] No direct method calls on `RandomImageViewModel` from composables
- [ ] Random anime and wallpaper screens work correctly

**Estimated scope:** M

---

### Task 13: Player — full UDF

**Current problems:**
- `PlayerScreen` calls `viewModel.previousEpisode()` and `viewModel.nextEpisode()` directly
- Back navigation uses `onNavigateBack` callback called directly in the composable
- `PlayerViewModel` has no `handleEvent()` and no `events` Channel

**Changes:**

`features/player/model/PlayerEvent.kt` (new):
```kotlin
sealed interface PlayerEvent {
    data object BackClicked : PlayerEvent
    data object PreviousEpisode : PlayerEvent
    data object NextEpisode : PlayerEvent
    data object NavigateBack : PlayerEvent
}
```

`PlayerViewModel.kt`:
- Add `_events` Channel and `events` flow
- Add `handleEvent(event: PlayerEvent)`
- `BackClicked` emits `NavigateBack` via Channel
- `PreviousEpisode`/`NextEpisode` route to existing private functions

`PlayerScreen.kt`:
- Collect events → call `onNavigateBack()` on `NavigateBack`
- All VM calls go through `handleEvent()`

**Acceptance criteria:**
- [ ] `PlayerEvent.kt` exists in `features/player/model/`
- [ ] No direct `viewModel.previousEpisode()` or `viewModel.nextEpisode()` calls in the screen
- [ ] Playback, episode switching, and back navigation work

**Estimated scope:** M

---

### Task 14: EpisodeList — add EpisodeClicked event

**Current problem:**
- Episode click in `EpisodeListScreen` calls `onEpisodeClick(urls, titles, index)` directly from a `clickable` modifier, bypassing the ViewModel entirely.

**Changes:**

`features/episodes/model/EpisodeListEvent.kt` — add:
```kotlin
data class EpisodeClicked(val urls: List<String>, val titles: List<String>, val index: Int) : EpisodeListEvent
data class NavigateToPlayer(val urls: List<String>, val titles: List<String>, val index: Int) : EpisodeListEvent
```

`EpisodeListViewModel.handleEvent()` — add `EpisodeClicked` case that sends `NavigateToPlayer` via Channel.

`EpisodeListScreen.kt`:
- Add `NavigateToPlayer` case in `LaunchedEffect` event collector → call `onEpisodeClick()`
- Replace direct `onEpisodeClick(urls, titles, index)` in `clickable` with `viewModel.handleEvent(EpisodeListEvent.EpisodeClicked(urls, titles, index))`

**Acceptance criteria:**
- [ ] No direct `onEpisodeClick` call from inside a Composable body
- [ ] Episode navigation to player works

**Estimated scope:** S

---

### Checkpoint 4 — Final
- [ ] Full app flow works: List → Details → Episodes → Player
- [ ] Favorites tab: view, add/remove, navigate to detail
- [ ] Mood tab: select mood, browse results, navigate to detail
- [ ] Random tab: random anime card, wallpaper download, set wallpaper
- [ ] Settings: theme toggle, adult content toggle
- [ ] No direct ViewModel method calls from any Composable (only `handleEvent()`)
- [ ] No inline comments or Log calls remain

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Visibility issues after composable split | Medium | Use `internal` for composables shared within a feature package |
| `downloadImage`/`setAsWallpaper` lose context after move | Low | Keep them as private top-level functions in `RandomImagePage.kt` |
| FavoritesViewModel `loadMore` uses internal `_limit` MutableStateFlow | Low | Keep logic in VM, just expose it via `handleEvent(FavoritesEvent.LoadMore)` |
| PlayerViewModel `onNavigateBack` was direct callback — screen owns Activity orientation reset on dispose | Low | `DisposableEffect` stays in screen; only the explicit back button action goes through VM |

## Open Questions

- None — all decisions resolved by reading the existing code.
