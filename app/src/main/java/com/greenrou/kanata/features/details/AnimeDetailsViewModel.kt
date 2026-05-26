package com.greenrou.kanata.features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.core.analytics.AnalyticsManager
import com.greenrou.kanata.core.network.NetworkMonitor
import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeByIdUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeEnrichmentUseCase
import com.greenrou.kanata.domain.usecase.GetCompletedDownloadsUseCase
import com.greenrou.kanata.domain.usecase.IsFavoriteUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.domain.usecase.SearchExternalAnimeUseCase
import com.greenrou.kanata.features.details.model.AnimeDetailsEvent
import com.greenrou.kanata.features.details.model.AnimeDetailsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    settingsManager: SettingsManager,
    private val getCompletedDownloads: GetCompletedDownloadsUseCase,
    private val networkMonitor: NetworkMonitor,
    private val analytics: AnalyticsManager,
    private val getAnimeEnrichment: GetAnimeEnrichmentUseCase,
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
            getAnimeById(animeId)
                .onSuccess { anime ->
                    _state.update { it.copy(isLoading = false, anime = anime) }
                    searchOnExternal(anime)
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
            if (enrichment != null) {
                _state.update { it.copy(enrichment = enrichment) }
            }
        }
    }
}
