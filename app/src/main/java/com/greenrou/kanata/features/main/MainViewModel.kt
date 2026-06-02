package com.greenrou.kanata.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.core.analytics.AnalyticsManager
import com.greenrou.kanata.core.analytics.reportToCrashlytics
import com.greenrou.kanata.core.network.NetworkMonitor
import com.greenrou.kanata.data.mod.ChapterParserRegistry
import com.greenrou.kanata.data.mod.DownloadFeatureRegistry
import com.greenrou.kanata.data.mod.InfoProviderRegistry
import com.greenrou.kanata.data.mod.MangaModRegistry
import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.domain.model.AnimeFilter
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeListUseCase
import com.greenrou.kanata.domain.usecase.GetFavoritesUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.domain.usecase.SetDownloadFolderUseCase
import com.greenrou.kanata.features.main.model.MainEvent
import com.greenrou.kanata.features.main.model.MainState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val getAnimeList: GetAnimeListUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    getFavorites: GetFavoritesUseCase,
    private val settingsManager: SettingsManager,
    private val setDownloadFolder: SetDownloadFolderUseCase,
    private val networkMonitor: NetworkMonitor,
    private val analytics: AnalyticsManager,
    parserRegistry: ParserRegistry,
    infoProviderRegistry: InfoProviderRegistry,
    downloadFeatureRegistry: DownloadFeatureRegistry,
    private val mangaModRegistry: MangaModRegistry,
    chapterParserRegistry: ChapterParserRegistry,
) : ViewModel() {

    val isDownloadFeatureEnabled: StateFlow<Boolean> = downloadFeatureRegistry.isEnabled
    val mangaModResources = mangaModRegistry.modResources
    val contentProviderHasStreams: StateFlow<Boolean> = mangaModRegistry.contentProviderHasStreams

    val regularSources: StateFlow<List<String>> = parserRegistry.parsers
        .map { list -> list.filter { !it.isAdultOnly }.map { it.label } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val adultSources: StateFlow<List<String>> = parserRegistry.parsers
        .map { list -> list.filter { it.isAdultOnly }.map { it.label } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mangaSources: StateFlow<List<String>> = chapterParserRegistry.parsers
        .map { list -> list.map { it.label } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val infoProviders: StateFlow<List<Pair<String, String>>> = infoProviderRegistry.providers
        .map { list -> list.map { it.id to it.label } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    private var searchDebounceJob: Job? = null

    val favoriteIds: StateFlow<List<Int>> = getFavorites.observeIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.success(emptyList())
        ).let { flow ->
            val resultFlow = MutableStateFlow<List<Int>>(emptyList())
            viewModelScope.launch {
                flow.collect { result ->
                    result.onSuccess { ids -> resultFlow.value = ids }
                }
            }
            resultFlow.asStateFlow()
        }

    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeSettings()
        observeNetwork()
        observeMangaMod()
        loadAnime()
    }

    private fun currentMediaType() =
        if (_state.value.isMangaMode) mangaModRegistry.activeProvider.value?.mediaType ?: "ANIME"
        else "ANIME"

    private fun observeMangaMod() {
        mangaModRegistry.isInstalled
            .onEach { installed ->
                _state.update { it.copy(isMangaModInstalled = installed) }
                if (!installed && _state.value.isMangaMode) {
                    settingsManager.setMangaMode(false)
                }
            }
            .launchIn(viewModelScope)
        mangaModRegistry.activeProvider
            .onEach { provider ->
                if (provider != null && _state.value.isMangaMode) loadAnime()
            }
            .launchIn(viewModelScope)
    }

    private fun observeSettings() {
        combine(
            settingsManager.showAdultContent,
            settingsManager.isDarkTheme,
            settingsManager.coverFillsTopBar,
        ) { showAdult, isDark, coverFills ->
            if (_state.value.showAdultContent != showAdult) {
                _state.update { it.copy(showAdultContent = showAdult, animeList = emptyList()) }
                loadAnime(showAdultContent = showAdult)
            }
            _state.update { it.copy(isDarkTheme = isDark, coverFillsTopBar = coverFills) }
        }.launchIn(viewModelScope)

        settingsManager.downloadFolder
            .onEach { folder -> _state.update { it.copy(downloadFolder = folder) } }
            .launchIn(viewModelScope)

        settingsManager.accentColor
            .onEach { color -> _state.update { it.copy(accentColor = color) } }
            .launchIn(viewModelScope)

        settingsManager.disabledSources
            .onEach { sources -> _state.update { it.copy(disabledSources = sources) } }
            .launchIn(viewModelScope)

        settingsManager.adBlockerEnabled
            .onEach { enabled -> _state.update { it.copy(adBlockerEnabled = enabled) } }
            .launchIn(viewModelScope)
        settingsManager.webBackNavTopBar
            .onEach { enabled -> _state.update { it.copy(webBackNavTopBar = enabled) } }
            .launchIn(viewModelScope)
        settingsManager.analyticsEnabled
            .onEach { enabled ->
                _state.update { it.copy(analyticsEnabled = enabled) }
                analytics.setCollectionEnabled(enabled)
            }
            .launchIn(viewModelScope)
        settingsManager.analyticsConsentShown
            .onEach { shown -> _state.update { it.copy(analyticsConsentShown = shown) } }
            .launchIn(viewModelScope)
        settingsManager.activeInfoProviderId
            .onEach { id -> _state.update { it.copy(activeInfoProviderId = id) } }
            .launchIn(viewModelScope)

        settingsManager.isMangaMode
            .onEach { isManga ->
                val changed = _state.value.isMangaMode != isManga
                _state.update { it.copy(isMangaMode = isManga, animeList = if (changed) emptyList() else it.animeList, selectedFormats = if (changed) emptySet() else it.selectedFormats) }
                if (changed && (!isManga || mangaModRegistry.activeProvider.value != null)) loadAnime()
            }
            .launchIn(viewModelScope)
    }

    private fun observeNetwork() {
        networkMonitor.isConnected
            .onEach { isConnected ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !isConnected) }
                if (!isConnected && _state.value.isRefreshing) {
                    _state.update { it.copy(isRefreshing = false) }
                }
                if (isConnected && wasOffline && _state.value.animeList.isEmpty()) {
                    loadAnime()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun toggleFavorite(animeId: Int) {
        viewModelScope.launch {
            val isCurrentFavorite = favoriteIds.value.contains(animeId)
            val anime = _state.value.animeList.find { it.id == animeId }
            if (isCurrentFavorite) {
                removeFavorite(animeId)
                anime?.let { analytics.logFavoriteToggled(animeId, it.title, added = false) }
            } else {
                anime ?: return@launch
                addFavorite(anime)
                analytics.logFavoriteToggled(animeId, anime.title, added = true)
            }
        }
    }

    fun handleEvent(event: MainEvent) {
        when (event) {
            MainEvent.LoadAnime -> loadAnime()
            MainEvent.Refresh -> refreshAnime()
            MainEvent.LoadMore -> loadMore()
            MainEvent.ToggleAdultContent -> {
                viewModelScope.launch {
                    settingsManager.setShowAdultContent(!_state.value.showAdultContent)
                }
            }
            MainEvent.ToggleTheme -> {
                viewModelScope.launch {
                    settingsManager.setDarkTheme(!_state.value.isDarkTheme)
                }
            }
            MainEvent.ToggleCoverLayout -> {
                viewModelScope.launch {
                    settingsManager.setCoverFillsTopBar(!_state.value.coverFillsTopBar)
                }
            }
            is MainEvent.ToggleFavorite -> toggleFavorite(event.animeId)
            is MainEvent.AnimeClicked -> viewModelScope.launch {
                _state.value.animeList.find { it.id == event.animeId }?.let {
                    analytics.logAnimeOpened(event.animeId, it.title)
                }
                _events.send(MainEvent.NavigateToDetail(event.animeId))
            }
            MainEvent.ToggleSearch -> {
                val wasActive = _state.value.isSearchActive
                _state.update { it.copy(isSearchActive = !wasActive, searchQuery = "") }
                if (wasActive) reloadWithCurrentFilters()
            }
            is MainEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                searchDebounceJob?.cancel()
                searchDebounceJob = viewModelScope.launch {
                    delay(400)
                    if (event.query.isNotBlank()) analytics.logSearch(event.query)
                    reloadWithCurrentFilters()
                }
            }
            MainEvent.ToggleFilterSheet -> _state.update { it.copy(isFilterSheetVisible = !it.isFilterSheetVisible) }
            is MainEvent.GenreToggled -> {
                val updated = _state.value.selectedGenres.toMutableSet()
                if (!updated.add(event.genre)) updated.remove(event.genre)
                _state.update { it.copy(selectedGenres = updated) }
                reloadWithCurrentFilters()
            }
            is MainEvent.FormatToggled -> {
                val updated = _state.value.selectedFormats.toMutableSet()
                if (!updated.add(event.format)) updated.remove(event.format)
                _state.update { it.copy(selectedFormats = updated) }
                reloadWithCurrentFilters()
            }
            MainEvent.ClearFilters -> {
                _state.update { it.copy(selectedGenres = emptySet(), selectedFormats = emptySet()) }
                reloadWithCurrentFilters()
            }
            is MainEvent.SetDownloadFolder -> viewModelScope.launch {
                setDownloadFolder(event.uri)
            }
            is MainEvent.SetAccentColor -> viewModelScope.launch {
                settingsManager.setAccentColor(event.name)
            }
            is MainEvent.ToggleSource -> viewModelScope.launch {
                val updated = _state.value.disabledSources.toMutableSet()
                if (!updated.add(event.label)) updated.remove(event.label)
                settingsManager.setDisabledSources(updated)
            }
            MainEvent.ToggleAdBlocker -> viewModelScope.launch {
                settingsManager.setAdBlockerEnabled(!_state.value.adBlockerEnabled)
            }
            MainEvent.ToggleMangaMode -> viewModelScope.launch {
                settingsManager.setMangaMode(!_state.value.isMangaMode)
            }
            is MainEvent.SetActiveInfoProvider -> viewModelScope.launch {
                settingsManager.setActiveInfoProviderId(event.id)
            }
            MainEvent.ToggleWebBackNavTopBar -> viewModelScope.launch {
                settingsManager.setWebBackNavTopBar(!_state.value.webBackNavTopBar)
            }
            MainEvent.ToggleAnalytics -> viewModelScope.launch {
                settingsManager.setAnalyticsEnabled(!_state.value.analyticsEnabled)
            }
            MainEvent.AcceptAnalytics -> viewModelScope.launch {
                settingsManager.setAnalyticsEnabled(true)
                settingsManager.setAnalyticsConsentShown(true)
            }
            MainEvent.DenyAnalytics -> viewModelScope.launch {
                settingsManager.setAnalyticsEnabled(false)
                settingsManager.setAnalyticsConsentShown(true)
            }
            else -> Unit
        }
    }

    private fun reloadWithCurrentFilters() {
        val s = _state.value
        loadAnime(
            showAdultContent = s.showAdultContent,
            filter = AnimeFilter(
                search = s.searchQuery,
                genres = s.selectedGenres.toList(),
                formats = s.selectedFormats.toList(),
            ),
            mediaType = currentMediaType(),
        )
    }

    private fun currentFilter() = with(_state.value) {
        AnimeFilter(
            search = searchQuery,
            genres = selectedGenres.toList(),
            formats = selectedFormats.toList(),
        )
    }

    private fun loadAnime(
        showAdultContent: Boolean = _state.value.showAdultContent,
        filter: AnimeFilter = currentFilter(),
        mediaType: String = currentMediaType(),
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getAnimeList(page = 1, showAdultContent = showAdultContent, filter = filter, mediaType = mediaType)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            animeList = page.items,
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNextPage,
                        )
                    }
                }
                .onFailure { e ->
                    e.reportToCrashlytics("main_load_anime")
                    _state.update { it.copy(isLoading = false, error = e.message) }
                    if (!_state.value.isOffline) {
                        _events.send(MainEvent.ShowError(e.message ?: "Unknown error"))
                    }
                }
        }
    }

    private fun refreshAnime() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            getAnimeList(page = 1, showAdultContent = _state.value.showAdultContent, filter = currentFilter(), mediaType = currentMediaType())
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            animeList = page.items,
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNextPage,
                        )
                    }
                }
                .onFailure { e ->
                    e.reportToCrashlytics("main_refresh_anime")
                    _state.update { it.copy(isRefreshing = false) }
                    if (!_state.value.isOffline) {
                        _events.send(MainEvent.ShowError(e.message ?: "Unknown error"))
                    }
                }
        }
    }

    private fun loadMore() {
        val current = _state.value
        if (!current.hasNextPage || current.isLoadingMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            getAnimeList(
                page = current.currentPage + 1,
                showAdultContent = current.showAdultContent,
                filter = currentFilter(),
                mediaType = currentMediaType(),
            )
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            animeList = it.animeList + page.items,
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNextPage,
                        )
                    }
                }
                .onFailure { e ->
                    e.reportToCrashlytics("main_load_more")
                    _state.update { it.copy(isLoadingMore = false) }
                    _events.send(MainEvent.ShowError(e.message ?: "Unknown error"))
                }
        }
    }
}
