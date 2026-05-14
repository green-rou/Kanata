package com.greenrou.kanata.features.random

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.random.content.PillTabRow
import com.greenrou.kanata.features.random.content.RandomAnimePage
import com.greenrou.kanata.features.random.content.RandomImagePage
import com.greenrou.kanata.features.random.model.RandomEvent
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun RandomScreen(
    onNavigateToDetails: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: RandomImageViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RandomEvent.NavigateToDetails -> onNavigateToDetails(event.animeId)
                else -> Unit
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> RandomAnimePage(
                    anime = state.randomAnime,
                    isFavorite = state.isAnimeFavorite,
                    isLoading = state.isAnimeLoading,
                    error = state.animeError,
                    onRefresh = { viewModel.handleEvent(RandomEvent.RefreshAnime) },
                    onFavoriteClick = { viewModel.handleEvent(RandomEvent.ToggleFavorite) },
                    onAnimeClick = { viewModel.handleEvent(RandomEvent.AnimeClicked(it)) },
                    bottomPadding = contentPadding.calculateBottomPadding(),
                )
                1 -> RandomImagePage(
                    imageUrl = state.imageUrl,
                    isLoading = state.isImageLoading,
                    error = state.imageError,
                    onRefresh = { viewModel.handleEvent(RandomEvent.RefreshImage) },
                    bottomPadding = contentPadding.calculateBottomPadding(),
                )
            }
        }

        PillTabRow(
            currentPage = pagerState.currentPage,
            pageOffset = pagerState.currentPageOffsetFraction,
            tabs = listOf(
                Icons.Rounded.AutoAwesome to "Random Anime",
                Icons.Rounded.Image to "Wallpaper",
            ),
            onTabClick = { scope.launch { pagerState.animateScrollToPage(it) } },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding(),
        )
    }
}
