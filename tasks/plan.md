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
