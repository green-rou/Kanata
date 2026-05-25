# Kanata

<p align="center">
  <img src="AppIcon.png" width="600" alt="Kanata icon" />
</p>

An Android app for browsing anime information and streaming or downloading episodes from third-party sources.

![Min SDK](https://img.shields.io/badge/Min%20SDK-28%20(Android%209)-brightgreen)
![Language](https://img.shields.io/badge/Language-Kotlin-purple)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean%20Arch-orange)
![License](https://img.shields.io/badge/License-MIT-yellow)
[![Contributing](https://img.shields.io/badge/PRs-welcome-brightgreen)](CONTRIBUTING.md)
[![Screenshots](https://img.shields.io/badge/Screenshots-view-lightgrey)](screenshots/SCREENSHOTS.md)
[![Website](https://img.shields.io/badge/Website-green--rou.github.io%2FKanata-blueviolet)](https://green-rou.github.io/Kanata/)
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/C0C31ZLH6K)

---

## Features

- **Anime catalogue** — paginated grid powered by the [AniList](https://anilist.co) GraphQL API
- **Catalogue filters** — filter by genre and anime format via a bottom sheet
- **Catalogue search** — search anime directly within the catalogue
- **Detail screen** — title, score, genres, synopsis, episode count, cover image
- **Favourites** — persist liked anime locally with Room
- **External search** — automatically finds the anime on multiple sources after you open it
- **Available streams** — clickable source chips (YummyAnime / Mikai / AniStar / AnimeGO / KissKH / YouTube / Archive.org) appear when found
- **Episode list** — browse all episodes from the selected source; download any episode from the list
- **Video player** — built-in HLS player via Media3 / ExoPlayer, auto-locks to landscape
- **Web Player** — opens embedded player pages in a built-in browser; detects stream URLs automatically and offers to play them in ExoPlayer
- **Ad Blocker** — built-in WebView ad blocker for the Web Player; blocks ads and trackers on embedded player pages
- **Saved Pages** — bookmark Web Player pages for quick access without re-searching
- **Episode downloading** — background download (HLS + direct video) via WorkManager with progress notifications
- **Offline playback** — play downloaded episodes without an internet connection
- **Download Manager** — dedicated screen with two tabs: active queue (drag-to-reorder, cancel) and completed downloads (open / delete)
- **Discover** — mood-based anime recommendations, random anime picker, and anime wallpaper browser in one section
- **Settings** — theme (light/dark/system), extended accent color palette, language, adult content toggle, active sources selector, download folder picker

---

## Architecture

```
app/
├── core/
│   ├── composable/     # Shared UI components (FavoriteFab, FavoriteIcon, …)
│   ├── di/             # Koin modules (Repository, UseCase, ViewModel, WorkManager)
│   └── network/        # OkHttp + Retrofit + Apollo setup
├── data/
│   ├── local/          # Room DB (favourites + downloads)
│   ├── parsers/        # Per-site parsers (search + episode extraction)
│   ├── remote/         # Retrofit API interfaces + DTO; AniList GraphQL queries
│   ├── repository/     # Repository implementations
│   ├── worker/         # EpisodeDownloadWorker (WorkManager)
│   └── youtube/        # NewPipe extractor downloader
├── domain/
│   ├── model/          # Pure Kotlin models
│   ├── parser/         # SiteParser interface
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Single-responsibility use cases
├── features/
│   ├── details/        # Anime detail screen
│   ├── discover/       # Discover section (Mood + Random + Wallpaper tabs)
│   ├── downloads/      # Download Manager screen
│   ├── episodes/       # Episode list screen
│   ├── favorites/      # Favourites screen
│   ├── main/           # Home screen (anime grid + filters)
│   ├── mood/           # Mood-based recommendation screen
│   ├── player/         # ExoPlayer screen
│   ├── random/         # Random anime screen
│   ├── settings/       # Settings screen
│   └── webplayer/      # Web Player screen (WebView + stream detection)
└── navigation/         # Navigation3 back-stack + routes
```

**Pattern:** MVVM + Clean Architecture + UDF (Unidirectional Data Flow)  
Each feature has its own `State` / `Event` model pair. ViewModels expose `StateFlow<State>` and `Channel<Event>` for one-shot navigation/UI effects.

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Navigation | [Navigation3](https://developer.android.com/jetpack/androidx/releases/navigation3) 1.0.0-alpha04 |
| DI | [Koin](https://insert-koin.io/) 4.0 |
| Networking | Retrofit 2 + OkHttp 4 |
| Web scraping | [Jsoup](https://jsoup.org/) |
| Image loading | [Coil](https://coil-kt.github.io/coil/) 2 |
| Video player | [Media3 / ExoPlayer](https://developer.android.com/media/media3) 1.5.1 |
| Local storage | Room 2.6, DataStore Preferences |
| Background work | WorkManager |
| GraphQL | Apollo 4 (AniList) |
| Serialization | Kotlin Serialization |

---

## Data Sources

| Source | Language | Method | Status |
|---|---|---|---|
| [AniList](https://anilist.co) | — | GraphQL API — anime list, metadata, mood search | ✅ Active |
| [Mikai](https://mikai.me) | Ukrainian dub | REST API | ✅ Active |
| [YummyAnime](https://yummyanime.tv) | Russian dub | HTML scraping | ✅ Active |
| [AniStar](https://astar.bz) | Ukrainian dub | HTML scraping | ✅ Active |
| [AnimeGO](https://animego.ngo) | Russian dub | HTML scraping | ✅ Active |
| [KissKH](https://kisskh.co) | English sub | HTML scraping | ✅ Active |
| [YouTube](https://youtube.com) | Various | NewPipe extractor — playlist search | ✅ Active |
| [Archive.org](https://archive.org) | Various | Public metadata API — direct MP4/MKV | ✅ Active |
| Kodik | — | Embedded player format resolved by VideoRepository | ✅ Active |
| ~~[Aniwave](https://aniwave.dk)~~ | ~~English sub~~ | ~~HTML scraping~~ | ❌ Disabled |
| ~~[AniTube](https://anitube.in.ua)~~ | ~~Ukrainian dub/sub~~ | ~~HTML scraping~~ | ❌ Disabled (DLE player detection) |
| ~~[Hanime.tv](https://hanime.tv)~~ | ~~Japanese~~ | ~~WebView + API~~ | ❌ Disabled (WebView detection blocks stream) |

> **Note:** All active streaming sources are publicly accessible. No credentials or private APIs are used.

---

## Requirements

- Android **9.0 (API 28)** or higher
- Android Studio **Hedgehog** or newer (AGP 8.x)
- JDK 11

---

## Getting Started

1. Clone the repo:
   ```bash
   git clone https://github.com/green-rou/Kanata.git
   ```

2. Open in Android Studio and let Gradle sync.

3. Run on a device or emulator (API 28+).

> No API keys required — all data sources are publicly accessible.

---

## Project Status

Active development. All core features are functional.

Planned:
- More streaming sources

---

## Contributing

Pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.

---

## License

[MIT](LICENSE) © 2026 Kanata Contributors
