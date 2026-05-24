package com.greenrou.kanata.features.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.R
import com.greenrou.kanata.core.composable.KanataLoader
import com.greenrou.kanata.features.favorites.content.FavoritesEmptyState
import com.greenrou.kanata.features.favorites.content.SavedPageCard
import com.greenrou.kanata.features.favorites.model.FavoritesEvent
import com.greenrou.kanata.features.main.content.AnimeGrid
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesScreen(
    onNavigateToDetails: (Int) -> Unit,
    onNavigateToWebPlayer: (String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FavoritesEvent.NavigateToDetails -> onNavigateToDetails(event.animeId)
                is FavoritesEvent.NavigateToWebPlayer -> onNavigateToWebPlayer(event.url)
                else -> Unit
            }
        }
    }

    val isEmpty = state.favorites.isEmpty() && state.savedPages.isEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && isEmpty -> KanataLoader(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(contentPadding)
            )
            isEmpty -> FavoritesEmptyState(
                onExploreClick = onExploreClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
            else -> AnimeGrid(
                animeList = state.favorites,
                favoriteIds = state.favorites.map { it.id },
                gridState = gridState,
                isLoadingMore = state.isLoadingMore,
                onAnimeClick = { viewModel.handleEvent(FavoritesEvent.AnimeClicked(it)) },
                onFavoriteClick = { viewModel.handleEvent(FavoritesEvent.ToggleFavorite(it)) },
                onLoadMore = { viewModel.handleEvent(FavoritesEvent.LoadMore) },
                contentPadding = contentPadding,
                headerContent = if (state.savedPages.isNotEmpty()) {
                    {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.favorites_saved_pages_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    top = 8.dp,
                                    bottom = 4.dp,
                                ),
                            )
                        }
                        items(
                            count = state.savedPages.size,
                            key = { state.savedPages[it].id },
                            span = { GridItemSpan(maxLineSpan) },
                        ) { index ->
                            val page = state.savedPages[index]
                            SavedPageCard(
                                page = page,
                                onClick = { viewModel.handleEvent(FavoritesEvent.SavedPageClicked(page.url)) },
                                onDelete = { viewModel.handleEvent(FavoritesEvent.DeleteSavedPage(page.id)) },
                                modifier = Modifier
                                    .animateItem(fadeInSpec = null)
                                    .padding(bottom = 6.dp),
                            )
                        }
                        if (state.favorites.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = stringResource(R.string.tab_favorites),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        start = 4.dp,
                                        top = 8.dp,
                                        bottom = 4.dp,
                                    ),
                                )
                            }
                        }
                    }
                } else null,
            )
        }
    }
}
