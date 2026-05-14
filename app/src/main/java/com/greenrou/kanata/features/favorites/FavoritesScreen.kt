package com.greenrou.kanata.features.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.favorites.model.FavoritesEvent
import com.greenrou.kanata.features.main.content.AnimeGrid
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesScreen(
    onNavigateToDetails: (Int) -> Unit,
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
                else -> Unit
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(
                Modifier.align(Alignment.Center).padding(contentPadding)
            )
            state.favorites.isEmpty() -> Text(
                text = "No favorites yet",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(contentPadding),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            )
        }
    }
}
