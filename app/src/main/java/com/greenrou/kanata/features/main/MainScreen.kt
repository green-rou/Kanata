package com.greenrou.kanata.features.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.R
import com.greenrou.kanata.core.composable.KanataLoader
import com.greenrou.kanata.core.composable.KanataSnackbarHost
import com.greenrou.kanata.core.composable.OfflineBanner
import com.greenrou.kanata.core.composable.OfflineState
import com.greenrou.kanata.features.downloads.DownloadManagerScreen
import com.greenrou.kanata.features.downloads.DownloadManagerViewModel
import com.greenrou.kanata.features.downloads.model.DownloadManagerEvent
import com.greenrou.kanata.features.favorites.FavoritesScreen
import com.greenrou.kanata.features.main.content.AnimeGrid
import com.greenrou.kanata.features.main.content.ErrorState
import com.greenrou.kanata.features.main.content.FilterBottomSheet
import com.greenrou.kanata.features.main.model.MainEvent
import com.greenrou.kanata.features.random.RandomScreen
import com.greenrou.kanata.features.settings.SettingsScreen
import com.greenrou.kanata.features.update.UpdateViewModel
import com.greenrou.kanata.features.update.model.UpdateEvent
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
    onOpenWebPlayer: () -> Unit = {},
    onNavigateToWebPlayer: (url: String) -> Unit = {},
    onNavigateToMods: () -> Unit = {},
    onReadMangaChapter: (chapterFolderPath: String, title: String) -> Unit = { _, _ -> },
    onNavigateToOnlineSearch: (query: String) -> Unit = {},
    viewModel: MainViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDownloadFeatureEnabled by viewModel.isDownloadFeatureEnabled.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val regularSources by viewModel.regularSources.collectAsStateWithLifecycle()
    val adultSources by viewModel.adultSources.collectAsStateWithLifecycle()
    val infoProviders by viewModel.infoProviders.collectAsStateWithLifecycle()
    val mangaModResources by viewModel.mangaModResources.collectAsStateWithLifecycle()
    val mangaModeOnTitle = mangaModResources?.getString("mod_mode_on_title")
        ?: stringResource(R.string.settings_content_type_manga)
    val mangaModeOffTitle = mangaModResources?.getString("mod_mode_off_title")
        ?: stringResource(R.string.settings_content_type_anime)
    val mangaModeSubtitle = mangaModResources?.getString("mod_mode_subtitle")
        ?: stringResource(R.string.settings_content_type_subtitle)
    val snackbarHostState = remember { SnackbarHostState() }
    val downloadsViewModel: DownloadManagerViewModel = koinViewModel()
    val updateViewModel: UpdateViewModel = koinViewModel()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val noUpdatesMessage = stringResource(R.string.update_no_updates)

    LaunchedEffect(updateState.noUpdatesAvailable) {
        if (updateState.noUpdatesAvailable) {
            snackbarHostState.showSnackbar(noUpdatesMessage)
            updateViewModel.handleEvent(UpdateEvent.ConsumeNoUpdatesMessage)
        }
    }

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

    LaunchedEffect(isDownloadFeatureEnabled) {
        if (!isDownloadFeatureEnabled && selectedTab == BottomNavItem.Downloads) {
            onTabSelected(BottomNavItem.AnimeList.name)
        }
    }

    val fabAllowedOnTab = selectedTab == BottomNavItem.AnimeList || selectedTab == BottomNavItem.Favorites

    var fabScrollVisible by remember { mutableStateOf(true) }
    LaunchedEffect(gridState, selectedTab) {
        if (selectedTab != BottomNavItem.AnimeList) {
            fabScrollVisible = true
            return@LaunchedEffect
        }
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                when {
                    index == 0 -> fabScrollVisible = true
                    index != lastIndex -> fabScrollVisible = index < lastIndex
                    offset != lastOffset -> fabScrollVisible = offset < lastOffset
                }
                lastIndex = index
                lastOffset = offset
            }
    }

    val isFabVisible = fabAllowedOnTab && fabScrollVisible && isDownloadFeatureEnabled

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
    val isSnackbarVisible = snackbarHostState.currentSnackbarData != null
    val fabBottomPadding by animateDpAsState(
        targetValue = floatingNavBottom + 12.dp + if (isFabVisible && isSnackbarVisible) 60.dp else 0.dp,
        label = "fab_bottom",
    )

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
                                        BottomNavItem.Favorites -> stringResource(R.string.tab_favorites)
                                        BottomNavItem.Downloads -> stringResource(R.string.tab_downloads)
                                        BottomNavItem.Settings -> stringResource(R.string.tab_settings)
                                        else -> ""
                                    })
                                }
                            },
                            actions = {
                                if (selectedTab == BottomNavItem.AnimeList) {
                                    if (state.isSearchActive) {
                                        if (state.searchQuery.isNotBlank()) {
                                            IconButton(onClick = { onNavigateToOnlineSearch(state.searchQuery) }) {
                                                Icon(Icons.Rounded.TravelExplore, contentDescription = stringResource(R.string.online_search_button))
                                            }
                                        }
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
                KanataSnackbarHost(
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
                        val pullToRefreshState = rememberPullToRefreshState()
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.handleEvent(MainEvent.Refresh) },
                            state = pullToRefreshState,
                            modifier = Modifier.fillMaxSize(),
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullToRefreshState,
                                    isRefreshing = state.isRefreshing,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = contentPadding.calculateTopPadding()),
                                )
                            },
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                when {
                                    state.isLoading -> KanataLoader(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(contentPadding),
                                    )

                                    state.isOffline && state.animeList.isEmpty() -> OfflineState(
                                        onRetry = { viewModel.handleEvent(MainEvent.LoadAnime) },
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(contentPadding),
                                    )

                                    !state.isOffline && state.error != null -> ErrorState(
                                        onRetry = { viewModel.handleEvent(MainEvent.LoadAnime) },
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(contentPadding),
                                    )

                                    !state.isOffline && state.animeList.isEmpty() && (state.hasActiveFilters || state.searchQuery.isNotEmpty()) -> Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(contentPadding),
                                    ) {
                                        Text(
                                            stringResource(R.string.main_no_anime_found),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (state.searchQuery.isNotEmpty() && (regularSources.isNotEmpty() || adultSources.isNotEmpty())) {
                                            Button(onClick = { onNavigateToOnlineSearch(state.searchQuery) }) {
                                                Text(stringResource(R.string.main_search_in_mods))
                                            }
                                        }
                                    }

                                    !state.isOffline && state.animeList.isEmpty() -> ErrorState(
                                        onRetry = { viewModel.handleEvent(MainEvent.LoadAnime) },
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(contentPadding),
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

                                AnimatedVisibility(
                                    visible = state.isOffline && state.animeList.isNotEmpty(),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(
                                            top = contentPadding.calculateTopPadding() + 8.dp,
                                        ),
                                    enter = slideInVertically { -it } + fadeIn(),
                                    exit = slideOutVertically { -it } + fadeOut(),
                                ) {
                                    OfflineBanner()
                                }
                            }
                        }
                    }

                    BottomNavItem.Favorites -> FavoritesScreen(
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToWebPlayer = onNavigateToWebPlayer,
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
                        onReadMangaChapter = onReadMangaChapter,
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
                        disabledSources = state.disabledSources,
                        regularSources = regularSources,
                        adultSources = adultSources,
                        onToggleSource = { viewModel.handleEvent(MainEvent.ToggleSource(it)) },
                        adBlockerEnabled = state.adBlockerEnabled,
                        onToggleAdBlocker = { viewModel.handleEvent(MainEvent.ToggleAdBlocker) },
                        webBackNavTopBar = state.webBackNavTopBar,
                        onToggleWebBackNavTopBar = { viewModel.handleEvent(MainEvent.ToggleWebBackNavTopBar) },
                        analyticsEnabled = state.analyticsEnabled,
                        onToggleAnalytics = { viewModel.handleEvent(MainEvent.ToggleAnalytics) },
                        isMangaModInstalled = state.isMangaModInstalled,
                        isMangaMode = state.isMangaMode,
                        onToggleMangaMode = { viewModel.handleEvent(MainEvent.ToggleMangaMode) },
                        mangaModeOnTitle = mangaModeOnTitle,
                        mangaModeOffTitle = mangaModeOffTitle,
                        mangaModeSubtitle = mangaModeSubtitle,
                        isCheckingUpdate = updateState.isChecking,
                        onCheckUpdate = { updateViewModel.handleEvent(UpdateEvent.CheckUpdate) },
                        onNavigateToMods = onNavigateToMods,
                        infoProviders = infoProviders,
                        activeInfoProviderId = state.activeInfoProviderId,
                        onSetInfoProvider = { viewModel.handleEvent(MainEvent.SetActiveInfoProvider(it)) },
                        bottomPadding = contentPadding.calculateBottomPadding(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = contentPadding.calculateTopPadding()),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isFabVisible,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = fabBottomPadding),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            FloatingActionButton(onClick = onOpenWebPlayer) {
                Icon(
                    Icons.Rounded.Language,
                    contentDescription = stringResource(R.string.webplayer_cd_open_webplayer),
                )
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
                visibleItems = if (isDownloadFeatureEnabled) {
                    BottomNavItem.entries.toSet()
                } else {
                    BottomNavItem.entries.filter { it != BottomNavItem.Downloads }.toSet()
                },
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
