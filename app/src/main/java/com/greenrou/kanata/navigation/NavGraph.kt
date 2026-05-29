package com.greenrou.kanata.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.greenrou.kanata.features.chapters.ChapterListScreen
import com.greenrou.kanata.features.details.AnimeDetailsScreen
import com.greenrou.kanata.features.episodes.EpisodeListScreen
import com.greenrou.kanata.features.main.BottomNavItem
import com.greenrou.kanata.features.main.MainScreen
import com.greenrou.kanata.features.mods.ModsScreen
import com.greenrou.kanata.features.onlinesearch.OnlineSearchScreen
import com.greenrou.kanata.features.pagereader.PageReaderScreen
import com.greenrou.kanata.features.player.PlayerScreen
import com.greenrou.kanata.features.webplayer.WebPlayerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(backStack: SnapshotStateList<Any>) {
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.size - 1)
    }

    val gridState = rememberLazyGridState()

    @Suppress("UNUSED_VALUE")
    var selectedTabName by rememberSaveable { mutableStateOf(BottomNavItem.AnimeList.name) }

    when (val current = backStack.last()) {
        is MainRoute -> MainScreen(
            gridState = gridState,
            selectedTabName = selectedTabName,
            onTabSelected = { selectedTabName = it },
            onNavigateToDetails = { id -> backStack.add(AnimeDetailsRoute(id)) },
            onNavigateToPlayer = { path, title ->
                backStack.add(PlayerRoute(listOf(path), listOf(title), 0))
            },
            onOpenEpisodeList = { animePageUrl, sourceName, animeTitle ->
                backStack.add(EpisodeListRoute(animePageUrl, sourceName, animeTitle))
            },
            onNavigateToAnimeDetails = { animeId ->
                backStack.add(AnimeDetailsRoute(animeId))
            },
            onOpenWebPlayer = { backStack.add(WebPlayerRoute()) },
            onNavigateToWebPlayer = { url -> backStack.add(WebPlayerRoute(url)) },
            onNavigateToMods = { backStack.add(ModsRoute) },
            onNavigateToOnlineSearch = { query -> backStack.add(OnlineSearchRoute(query)) },
            onReadMangaChapter = { folderPath, title ->
                backStack.add(PageReaderRoute(listOf("file://$folderPath"), listOf(title), 0))
            },
        )
        is AnimeDetailsRoute -> AnimeDetailsScreen(
            animeId = current.animeId,
            onNavigateBack = { backStack.removeAt(backStack.size - 1) },
            onNavigateToEpisodeList = { source, animeTitle, episodeCount ->
                backStack.add(EpisodeListRoute(source.animePageUrl, source.label, animeTitle, current.animeId, episodeCount))
            },
            onNavigateToChapterList = { source, title ->
                backStack.add(ChapterListRoute(source.pageUrl, source.label, title))
            },
            onNavigateToOfflinePlayer = { localFilePaths, titles, startIndex ->
                backStack.add(
                    PlayerRoute(
                        episodeUrls = localFilePaths,
                        episodeTitles = titles,
                        startIndex = startIndex,
                    )
                )
            },
        )
        is EpisodeListRoute -> key(current) {
            EpisodeListScreen(
                animePageUrl = current.animePageUrl,
                label = current.label,
                animeTitle = current.animeTitle,
                animeId = current.animeId,
                episodeCount = current.episodeCount,
                onNavigateBack = { backStack.removeAt(backStack.size - 1) },
                onEpisodeClick = { urls, titles, index ->
                    backStack.add(
                        PlayerRoute(
                            episodeUrls = urls,
                            episodeTitles = titles,
                            startIndex = index,
                            animeTitle = current.animeTitle,
                            sourceName = current.label,
                        )
                    )
                },
            )
        }
        is PlayerRoute -> key(current) {
            PlayerScreen(
                episodeUrls = current.episodeUrls,
                episodeTitles = current.episodeTitles,
                startIndex = current.startIndex,
                animeTitle = current.animeTitle,
                sourceName = current.sourceName,
                headerKeys = current.headerKeys,
                headerValues = current.headerValues,
                onNavigateBack = { backStack.removeAt(backStack.size - 1) },
            )
        }
        is WebPlayerRoute -> WebPlayerScreen(
            initialUrl = current.initialUrl,
            onNavigateBack = { backStack.removeAt(backStack.size - 1) },
            onNavigateToPlayer = { streamUrl, referer ->
                backStack.add(
                    PlayerRoute(
                        episodeUrls = listOf(streamUrl),
                        episodeTitles = listOf(""),
                        startIndex = 0,
                        headerKeys = if (referer.isNotEmpty()) listOf("Referer") else emptyList(),
                        headerValues = if (referer.isNotEmpty()) listOf(referer) else emptyList(),
                    )
                )
            },
        )
        is ModsRoute -> ModsScreen(
            onNavigateBack = { backStack.removeAt(backStack.size - 1) },
        )
        is ChapterListRoute -> key(current) {
            ChapterListScreen(
                pageUrl = current.pageUrl,
                label = current.label,
                title = current.title,
                onNavigateBack = { backStack.removeAt(backStack.size - 1) },
                onChapterClick = { urls, titles, index ->
                    backStack.add(PageReaderRoute(urls, titles, index))
                },
            )
        }
        is PageReaderRoute -> key(current) {
            PageReaderScreen(
                chapterUrls = current.chapterUrls,
                chapterTitles = current.chapterTitles,
                startIndex = current.startIndex,
                onNavigateBack = { backStack.removeAt(backStack.size - 1) },
            )
        }
        is OnlineSearchRoute -> key(current) {
            OnlineSearchScreen(
                query = current.query,
                onNavigateBack = { backStack.removeAt(backStack.size - 1) },
                onNavigateToDetails = { animeId -> backStack.add(AnimeDetailsRoute(animeId)) },
                onNavigateToEpisodeList = { pageUrl, label, title ->
                    backStack.add(EpisodeListRoute(pageUrl, label, title))
                },
                onNavigateToChapterList = { pageUrl, label, title ->
                    backStack.add(ChapterListRoute(pageUrl, label, title))
                },
            )
        }
    }
}
