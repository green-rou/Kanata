package com.greenrou.kanata.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.greenrou.kanata.features.details.AnimeDetailsScreen
import com.greenrou.kanata.features.episodes.EpisodeListScreen
import com.greenrou.kanata.features.main.BottomNavItem
import com.greenrou.kanata.features.main.MainScreen
import com.greenrou.kanata.features.player.PlayerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(backStack: SnapshotStateList<Any>) {
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.size - 1)
    }

    val gridState = rememberLazyGridState()

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
        )
        is AnimeDetailsRoute -> AnimeDetailsScreen(
            animeId = current.animeId,
            onNavigateBack = { backStack.removeAt(backStack.size - 1) },
            onNavigateToEpisodeList = { source, animeTitle ->
                backStack.add(EpisodeListRoute(source.animePageUrl, source.label, animeTitle, current.animeId))
            },
            onNavigateToOfflinePlayer = { localFilePaths, titles ->
                backStack.add(
                    PlayerRoute(
                        episodeUrls = localFilePaths,
                        episodeTitles = titles,
                        startIndex = 0,
                    )
                )
            },
        )
        is EpisodeListRoute -> EpisodeListScreen(
            animePageUrl = current.animePageUrl,
            label = current.label,
            animeTitle = current.animeTitle,
            animeId = current.animeId,
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
        is PlayerRoute -> PlayerScreen(
            episodeUrls = current.episodeUrls,
            episodeTitles = current.episodeTitles,
            startIndex = current.startIndex,
            animeTitle = current.animeTitle,
            sourceName = current.sourceName,
            onNavigateBack = { backStack.removeAt(backStack.size - 1) },
        )
    }
}
