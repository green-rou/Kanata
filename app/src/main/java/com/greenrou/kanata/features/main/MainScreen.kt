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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.favorites.FavoritesScreen
import com.greenrou.kanata.features.main.content.AnimeGrid
import com.greenrou.kanata.features.main.content.ErrorState
import com.greenrou.kanata.features.main.content.FilterBottomSheet
import com.greenrou.kanata.features.main.model.MainEvent
import com.greenrou.kanata.features.downloads.DownloadManagerScreen
import com.greenrou.kanata.features.downloads.DownloadManagerViewModel
import com.greenrou.kanata.features.downloads.model.DownloadManagerEvent
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
    onNavigateToPlayer: (localFilePath: String, title: String) -> Unit = { _, _ -> },
    onOpenEpisodeList: (animePageUrl: String, sourceName: String, animeTitle: String) -> Unit = { _, _, _ -> },
    onNavigateToAnimeDetails: (animeId: Int) -> Unit = {},
    viewModel: MainViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val downloadsViewModel: DownloadManagerViewModel = koinViewModel()

    val selectedTab = remember(selectedTabName) { BottomNavItem.valueOf(selectedTabName) }
    var isDownloadSearchActive by rememberSaveable { mutableStateOf(false) }
    var downloadSearchQuery by rememberSaveable { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val animeSearchFocus = remember { FocusRequester() }
    val downloadSearchFocus = remember { FocusRequester() }

    LaunchedEffect(state.isSearchActive) {
        if (state.isSearchActive) {
            animeSearchFocus.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(isDownloadSearchActive) {
        if (isDownloadSearchActive) {
            downloadSearchFocus.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != BottomNavItem.Downloads && (isDownloadSearchActive || downloadSearchQuery.isNotEmpty())) {
            isDownloadSearchActive = false
            downloadSearchQuery = ""
            downloadsViewModel.handleEvent(DownloadManagerEvent.SearchQueryChanged(""))
        }
    }

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
                if (selectedTab != BottomNavItem.Discover) {
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
                                            placeholder = { Text(stringResource(R.string.main_search_anime_hint)) },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(animeSearchFocus),
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
                                                text = stringResource(R.string.main_title),
                                                style = MaterialTheme.typography.titleLarge.merge(
                                                    TextStyle(brush = gradientBrush)
                                                ),
                                            )
                                            Text(
                                                text = stringResource(R.string.main_subtitle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                } else if (selectedTab == BottomNavItem.Downloads && isDownloadSearchActive) {
                                    TextField(
                                        value = downloadSearchQuery,
                                        onValueChange = { q ->
                                            downloadSearchQuery = q
                                            downloadsViewModel.handleEvent(DownloadManagerEvent.SearchQueryChanged(q))
                                        },
                                        placeholder = { Text(stringResource(R.string.main_search_downloads_hint)) },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(downloadSearchFocus),
                                    )
                                } else {
                                    Text(when (selectedTab) {
                                        BottomNavItem.AnimeList -> stringResource(R.string.tab_anime)
                                        BottomNavItem.Favorites -> stringResource(R.string.tab_favorites)
                                        BottomNavItem.Discover -> stringResource(R.string.tab_discover)
                                        BottomNavItem.Downloads -> stringResource(R.string.tab_downloads)
                                        BottomNavItem.Settings -> stringResource(R.string.tab_settings)
                                    })
                                }
                            },
                            actions = {
                                if (selectedTab == BottomNavItem.AnimeList) {
                                    if (state.isSearchActive) {
                                        IconButton(onClick = { viewModel.handleEvent(MainEvent.ToggleSearch) }) {
                                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.main_cd_close_search))
                                        }
                                    } else {
                                        IconButton(onClick = { viewModel.handleEvent(MainEvent.ToggleSearch) }) {
                                            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.main_cd_search))
                                        }
                                        BadgedBox(badge = { if (state.hasActiveFilters) Badge() }) {
                                            IconButton(onClick = { viewModel.handleEvent(MainEvent.ToggleFilterSheet) }) {
                                                Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.main_cd_filter))
                                            }
                                        }
                                    }
                                }
                                if (selectedTab == BottomNavItem.Downloads) {
                                    if (isDownloadSearchActive) {
                                        IconButton(onClick = {
                                            isDownloadSearchActive = false
                                            downloadSearchQuery = ""
                                            downloadsViewModel.handleEvent(DownloadManagerEvent.SearchQueryChanged(""))
                                        }) {
                                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.main_cd_close_search))
                                        }
                                    } else {
                                        IconButton(onClick = { isDownloadSearchActive = true }) {
                                            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.main_cd_search_downloads))
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
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = floatingNavBottom),
                )
            },
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
                                        stringResource(R.string.main_no_anime_found),
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
                        onExploreClick = { onTabSelected(BottomNavItem.AnimeList.name) },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    )

                    BottomNavItem.Discover -> RandomScreen(
                        onNavigateToDetails = onNavigateToDetails,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    )

                    BottomNavItem.Downloads -> DownloadManagerScreen(
                        onPlayDownloaded = onNavigateToPlayer,
                        onOpenEpisodeList = onOpenEpisodeList,
                        onNavigateToAnimeDetails = onNavigateToAnimeDetails,
                        onShowSnackbar = { msg -> snackbarHostState.showSnackbar(msg) },
                        bottomPadding = contentPadding.calculateBottomPadding(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = contentPadding.calculateTopPadding()),
                    )

                    BottomNavItem.Settings -> SettingsScreen(
                        showAdultContent = state.showAdultContent,
                        onToggleAdultContent = { viewModel.handleEvent(MainEvent.ToggleAdultContent) },
                        isDarkTheme = state.isDarkTheme,
                        onToggleTheme = { viewModel.handleEvent(MainEvent.ToggleTheme) },
                        coverFillsTopBar = state.coverFillsTopBar,
                        onToggleCoverLayout = { viewModel.handleEvent(MainEvent.ToggleCoverLayout) },
                        downloadFolder = state.downloadFolder,
                        onSetDownloadFolder = { viewModel.handleEvent(MainEvent.SetDownloadFolder(it)) },
                        accentColor = state.accentColor,
                        onSetAccentColor = { viewModel.handleEvent(MainEvent.SetAccentColor(it)) },
                        bottomPadding = contentPadding.calculateBottomPadding(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = contentPadding.calculateTopPadding()),
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
