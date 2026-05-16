package com.greenrou.kanata.features.episodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.usecase.GetCompletedDownloadsUseCase
import com.greenrou.kanata.domain.usecase.GetDownloadQueueUseCase
import com.greenrou.kanata.domain.usecase.GetEpisodeListUseCase
import com.greenrou.kanata.domain.usecase.StartEpisodeDownloadUseCase
import com.greenrou.kanata.features.episodes.model.EpisodeListEvent
import com.greenrou.kanata.features.episodes.model.EpisodeListState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EpisodeListViewModel(
    private val getEpisodeList: GetEpisodeListUseCase,
    private val startEpisodeDownload: StartEpisodeDownloadUseCase,
    private val getDownloadQueue: GetDownloadQueueUseCase,
    private val getCompletedDownloads: GetCompletedDownloadsUseCase,
    val animePageUrl: String,
    val label: String,
    val animeTitle: String,
    val animeId: Int = 0,
) : ViewModel() {

    private val _state = MutableStateFlow(EpisodeListState(isLoading = true))
    val state = _state.asStateFlow()

    private val _events = Channel<EpisodeListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadEpisodes()
        observeDownloadStatuses()
    }

    fun handleEvent(event: EpisodeListEvent) {
        when (event) {
            EpisodeListEvent.BackClicked -> viewModelScope.launch {
                _events.send(EpisodeListEvent.NavigateBack)
            }
            is EpisodeListEvent.EpisodeClicked -> viewModelScope.launch {
                _events.send(EpisodeListEvent.NavigateToPlayer(event.urls, event.titles, event.index))
            }
            is EpisodeListEvent.DownloadEpisode -> viewModelScope.launch {
                startEpisodeDownload(
                    animeTitle = event.animeTitle,
                    sourceName = event.sourceName,
                    episodeTitle = event.episodeTitle,
                    episodePageUrl = event.episodePageUrl,
                    animePageUrl = event.animePageUrl,
                    animeId = event.animeId,
                )
            }
            else -> Unit
        }
    }

    private fun loadEpisodes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getEpisodeList(animePageUrl)
                .onSuccess { episodes ->
                    _state.update { it.copy(isLoading = false, episodes = episodes) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun observeDownloadStatuses() {
        combine(getDownloadQueue(), getCompletedDownloads()) { queued, completed ->
            (queued + completed).associateBy(DownloadItem::episodePageUrl)
        }
            .onEach { map -> _state.update { it.copy(downloadStatuses = map) } }
            .launchIn(viewModelScope)
    }
}
