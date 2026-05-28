package com.greenrou.kanata.features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.core.analytics.AnalyticsManager
import com.greenrou.kanata.core.network.NetworkMonitor
import com.greenrou.kanata.data.mod.MangaModRegistry
import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeByIdUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeEnrichmentUseCase
import com.greenrou.kanata.domain.usecase.GetCompletedDownloadsUseCase
import com.greenrou.kanata.domain.usecase.IsFavoriteUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.domain.usecase.SearchContentSourcesUseCase
import com.greenrou.kanata.domain.usecase.SearchExternalAnimeUseCase
import com.greenrou.kanata.features.details.model.AnimeDetailsEvent
import com.greenrou.kanata.features.details.model.AnimeDetailsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnimeDetailsViewModel(
    private val getAnimeById: GetAnimeByIdUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val searchExternalAnime: SearchExternalAnimeUseCase,
    private val settingsManager: SettingsManager,
    private val getCompletedDownloads: GetCompletedDownloadsUseCase,
    private val networkMonitor: NetworkMonitor,
    private val analytics: AnalyticsManager,
    private val getAnimeEnrichment: GetAnimeEnrichmentUseCase,
    private val parserRegistry: ParserRegistry,
    private val mangaModRegistry: MangaModRegistry,
    private val searchContentSources: SearchContentSourcesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AnimeDetailsState())
    val state = _state.asStateFlow()

    private var loadedAnimeId = -1

    private val _events = Channel<AnimeDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        analytics.setScreen("anime_details")
        settingsManager.coverFillsTopBar
            .onEach { enabled -> _state.update { it.copy(coverFillsTopBar = enabled) } }
            .launchIn(viewModelScope)
        settingsManager.activeInfoProviderId
            .distinctUntilChanged()
            .onEach { _ ->
                val anime = _state.value.anime ?: return@onEach
                fetchEnrichment(anime)
            }
            .launchIn(viewModelScope)
        parserRegistry.parsers
            .onEach { parsers -> _state.update { it.copy(hasStreamSources = parsers.isNotEmpty()) } }
            .launchIn(viewModelScope)
    }

    fun handleEvent(event: AnimeDetailsEvent) {
        when (event) {
            is AnimeDetailsEvent.LoadAnime -> {
                loadAnime(event.animeId)
                observeFavorite(event.animeId)
            }
            AnimeDetailsEvent.ToggleFavorite -> toggleFavorite()
            AnimeDetailsEvent.BackClicked -> viewModelScope.launch {
                _events.send(AnimeDetailsEvent.NavigateBack)
            }
            is AnimeDetailsEvent.OpenEpisodeList -> viewModelScope.launch {
                val animeTitle = _state.value.anime?.title.orEmpty()
                val episodeCount = _state.value.anime?.episodes ?: 0
                _events.send(AnimeDetailsEvent.NavigateToEpisodeList(event.source, animeTitle, episodeCount))
            }
            is AnimeDetailsEvent.OpenChapterList -> viewModelScope.launch {
                _events.send(AnimeDetailsEvent.NavigateToChapterList(event.source, _state.value.anime?.title.orEmpty()))
            }
            AnimeDetailsEvent.WatchOffline -> viewModelScope.launch {
                val animeTitle = _state.value.anime?.title.orEmpty()
                val matching = getCompletedDownloads().first().filter { it.animeTitle == animeTitle }
                if (matching.isNotEmpty()) {
                    _state.update { it.copy(offlineEpisodesForPicker = matching) }
                }
            }
            AnimeDetailsEvent.DismissOfflinePicker -> {
                _state.update { it.copy(offlineEpisodesForPicker = emptyList()) }
            }
            is AnimeDetailsEvent.SelectOfflineEpisode -> viewModelScope.launch {
                val items = _state.value.offlineEpisodesForPicker
                _state.update { it.copy(offlineEpisodesForPicker = emptyList()) }
                _events.send(AnimeDetailsEvent.NavigateToOfflinePlayer(items, event.index))
            }
            else -> Unit
        }
    }

    private fun toggleFavorite() {
        val anime = _state.value.anime ?: return
        viewModelScope.launch {
            if (_state.value.isFavorite) removeFavorite(anime.id) else addFavorite(anime)
        }
    }

    private fun observeFavorite(animeId: Int) {
        viewModelScope.launch {
            isFavoriteUseCase.observe(animeId).collect { result ->
                result.onSuccess { fav -> _state.update { s -> s.copy(isFavorite = fav) } }
            }
        }
    }

    private fun loadAnime(animeId: Int) {
        if (animeId == loadedAnimeId) return
        loadedAnimeId = animeId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, anime = null, videoSources = emptyList(), isSearching = false) }
            val isMangaMode = settingsManager.isMangaMode.first()
            val mediaType = if (isMangaMode) mangaModRegistry.activeProvider.value?.mediaType ?: "ANIME" else "ANIME"
            getAnimeById(animeId, mediaType = mediaType)
                .onSuccess { anime ->
                    _state.update { it.copy(isLoading = false, anime = anime) }
                    if (_state.value.hasStreamSources) searchOnExternal(anime)
                    searchContentSourcesForAnime(anime)
                    observeDownloadedCount(anime.title)
                    fetchEnrichment(anime)
                }
                .onFailure { e ->

                    analytics.recordError(e, "details_load_anime")
                    loadedAnimeId = -1
                    val isOffline = !networkMonitor.isConnectedNow()
                    _state.update { it.copy(isLoading = false, error = e.message, isOffline = isOffline) }
                    if (!isOffline) {
                        _events.send(AnimeDetailsEvent.ShowError(e.message ?: "Unknown error"))
                    }
                }
        }
    }

    private fun observeDownloadedCount(animeTitle: String) {
        getCompletedDownloads()
            .onEach { downloads ->
                val count = downloads.count { it.animeTitle == animeTitle }
                _state.update { it.copy(downloadedEpisodeCount = count) }
            }
            .launchIn(viewModelScope)
    }

    private fun searchContentSourcesForAnime(anime: Anime) {
        viewModelScope.launch {
            val titles = listOfNotNull(
                anime.titleEnglish.takeIf { it.isNotBlank() },
                anime.titleRomaji.takeIf { it.isNotBlank() },
                anime.title.takeIf { it.isNotBlank() },
            )
            val sources = searchContentSources(titles)
            if (sources.isNotEmpty()) _state.update { it.copy(contentSources = sources) }
        }
    }

    private fun searchOnExternal(anime: Anime) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val titles = listOfNotNull(
                anime.titleRomaji.takeIf { it.isNotBlank() },
                anime.titleEnglish.takeIf { it.isNotBlank() },
                anime.title.takeIf { it.isNotBlank() },
            )
            val sources = searchExternalAnime(titles)
            _state.update { it.copy(isSearching = false, videoSources = sources) }
        }
    }

    private fun fetchEnrichment(anime: Anime) {
        viewModelScope.launch {
            val titles = listOfNotNull(
                anime.titleRomaji.takeIf { it.isNotBlank() },
                anime.titleEnglish.takeIf { it.isNotBlank() },
                anime.title.takeIf { it.isNotBlank() },
            )
            val enrichment = getAnimeEnrichment(titles)
            _state.update { it.copy(enrichment = enrichment) }
        }
    }

}
