package com.greenrou.kanata.features.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.favorites.FavoritesScreen
import com.greenrou.kanata.features.main.content.AnimeGrid
import com.greenrou.kanata.features.main.content.ErrorState
import com.greenrou.kanata.features.main.model.MainEvent
import com.greenrou.kanata.features.mood.MoodScreen
import com.greenrou.kanata.features.random.RandomScreen
import com.greenrou.kanata.features.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

private val NavBarHeight = 82.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    gridState: LazyGridState,
    selectedTabName: String,
    onTabSelected: (String) -> Unit,
    onNavigateToDetails: (Int) -> Unit,
    viewModel: MainViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab = remember(selectedTabName) { BottomNavItem.valueOf(selectedTabName) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.NavigateToDetail -> onNavigateToDetails(event.animeId)
                is MainEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    val systemNavBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingNavBottom = NavBarHeight + systemNavBarBottom

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (selectedTab != BottomNavItem.Mood && selectedTab != BottomNavItem.Random) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to MaterialTheme.colorScheme.surface,
                                    0.6f to MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    1.0f to Color.Transparent,
                                )
                            )
                    ) {
                        TopAppBar(
                            title = {
                                if (selectedTab == BottomNavItem.AnimeList) {
                                    Column {
                                        val gradientBrush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary,
                                            ),
                                        )
                                        Text(
                                            text = "Kanata",
                                            style = MaterialTheme.typography.titleLarge.merge(
                                                TextStyle(brush = gradientBrush)
                                            ),
                                        )
                                        Text(
                                            text = "Discover anime",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    Text(selectedTab.label)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                            ),
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { scaffoldPadding ->
            val contentPadding = PaddingValues(
                top = scaffoldPadding.calculateTopPadding(),
                bottom = floatingNavBottom,
            )

            AnimatedContent(
                targetState = selectedTab,
                label = "tab_transition",
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                when (tab) {
                    BottomNavItem.AnimeList -> {
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.handleEvent(MainEvent.Refresh) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                when {
                                    state.isLoading -> CircularProgressIndicator(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(contentPadding),
                                    )

                                    state.error != null -> ErrorState(
                                        message = state.error!!,
                                        onRetry = { viewModel.handleEvent(MainEvent.LoadAnime) },
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(contentPadding),
                                    )

                                    state.animeList.isEmpty() -> Text(
                                        "No anime found",
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(contentPadding),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    else -> AnimeGrid(
                                        animeList = state.animeList,
                                        favoriteIds = favoriteIds,
                                        isLoadingMore = state.isLoadingMore,
                                        onAnimeClick = {
                                            viewModel.handleEvent(
                                                MainEvent.AnimeClicked(
                                                    it
                                                )
                                            )
                                        },
                                        onFavoriteClick = {
                                            viewModel.handleEvent(
                                                MainEvent.ToggleFavorite(
                                                    it
                                                )
                                            )
                                        },
                                        onLoadMore = { viewModel.handleEvent(MainEvent.LoadMore) },
                                        gridState = gridState,
                                        contentPadding = contentPadding,
                                    )
                                }
                            }
                        }
                    }

                    BottomNavItem.Favorites -> FavoritesScreen(
                        onNavigateToDetails = onNavigateToDetails,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    )

                    BottomNavItem.Mood -> MoodScreen(
                        onNavigateToDetails = onNavigateToDetails,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    )

                    BottomNavItem.Random -> RandomScreen(
                        onNavigateToDetails = onNavigateToDetails,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    )

                    BottomNavItem.Settings -> SettingsScreen(
                        showAdultContent = state.showAdultContent,
                        onToggleAdultContent = { viewModel.handleEvent(MainEvent.ToggleAdultContent) },
                        isDarkTheme = state.isDarkTheme,
                        onToggleTheme = { viewModel.handleEvent(MainEvent.ToggleTheme) },
                        coverFillsTopBar = state.coverFillsTopBar,
                        onToggleCoverLayout = { viewModel.handleEvent(MainEvent.ToggleCoverLayout) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            AnimatedBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { onTabSelected(it.name) },
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding(),
            )
        }
    }
}
