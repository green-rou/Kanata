package com.greenrou.kanata.features.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.greenrou.kanata.features.main.content.FilterBottomSheet
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
                    Column(
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
                                    if (state.isSearchActive) {
                                        TextField(
                                            value = state.searchQuery,
                                            onValueChange = { viewModel.handleEvent(MainEvent.SearchQueryChanged(it)) },
                                            placeholder = { Text("Search anime...") },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    } else {
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
                                    }
                                } else {
                                    Text(selectedTab.label)
                                }
                            },
                            actions = {
                                if (selectedTab == BottomNavItem.AnimeList) {
                                    if (state.isSearchActive) {
                                        IconButton(onClick = { viewModel.handleEvent(MainEvent.ToggleSearch) }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Close search")
                                        }
                                    } else {
                                        IconButton(onClick = { viewModel.handleEvent(MainEvent.ToggleSearch) }) {
                                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                                        }
                                        BadgedBox(badge = { if (state.hasActiveFilters) Badge() }) {
                                            IconButton(onClick = { viewModel.handleEvent(MainEvent.ToggleFilterSheet) }) {
                                                Icon(Icons.Rounded.FilterList, contentDescription = "Filter")
                                            }
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                            ),
                        )
                        if (state.hasActiveFilters && selectedTab == BottomNavItem.AnimeList) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                            ) {
                                items(state.selectedFormats.toList()) { format ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { viewModel.handleEvent(MainEvent.FormatToggled(format)) },
                                        label = { Text(format.displayName) },
                                    )
                                }
                                items(state.selectedGenres.toList()) { genre ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { viewModel.handleEvent(MainEvent.GenreToggled(genre)) },
                                        label = { Text(genre) },
                                    )
                                }
                            }
                        }
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

    if (state.isFilterSheetVisible) {
        FilterBottomSheet(
            selectedGenres = state.selectedGenres,
            selectedFormats = state.selectedFormats,
            onGenreToggled = { viewModel.handleEvent(MainEvent.GenreToggled(it)) },
            onFormatToggled = { viewModel.handleEvent(MainEvent.FormatToggled(it)) },
            onClearFilters = { viewModel.handleEvent(MainEvent.ClearFilters) },
            onDismiss = { viewModel.handleEvent(MainEvent.ToggleFilterSheet) },
        )
    }
}
