package com.greenrou.kanata.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.usecase.GetEpisodeDownloadStatusUseCase
import com.greenrou.kanata.domain.usecase.GetVideoStreamUseCase
import com.greenrou.kanata.domain.usecase.StartEpisodeDownloadUseCase
import com.greenrou.kanata.features.player.model.PlayerEvent
import com.greenrou.kanata.features.player.model.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val getVideoStream: GetVideoStreamUseCase,
    private val startEpisodeDownload: StartEpisodeDownloadUseCase,
    private val getEpisodeDownloadStatus: GetEpisodeDownloadStatusUseCase,
    private val episodeUrls: List<String>,
    private val episodeTitles: List<String>,
    startIndex: Int,
    private val animeTitle: String = "",
    private val sourceName: String = "",
) : ViewModel() {

    private val _state = MutableStateFlow(
        PlayerState(
            isLoading = true,
            title = episodeTitles.getOrElse(startIndex) { "" },
            currentIndex = startIndex,
            episodeCount = episodeUrls.size,
            nextEpisodeTitle = episodeTitles.getOrNull(startIndex + 1),
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<PlayerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentIndex = startIndex
    private var loadJob: Job? = null
    private var statusJob: Job? = null

    init {
        loadStream()
        observeDownloadStatus()
    }

    fun handleEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.BackClicked -> viewModelScope.launch {
                _events.send(PlayerEvent.NavigateBack)
            }
            PlayerEvent.PreviousEpisode -> previousEpisode()
            PlayerEvent.NextEpisode -> nextEpisode()
            PlayerEvent.Retry -> loadStream()
            is PlayerEvent.PlaybackError -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "${event.message}\n\nURL: ${it.streamUrl ?: "unknown"}",
                    )
                }
            }
            is PlayerEvent.DownloadCurrentEpisode -> viewModelScope.launch {
                startEpisodeDownload(
                    animeTitle = event.animeTitle,
                    sourceName = event.sourceName,
                    episodeTitle = event.episodeTitle,
                    episodePageUrl = event.episodePageUrl,
                )
            }
            PlayerEvent.NavigateBack -> Unit
        }
    }

    private fun previousEpisode() {
        if (currentIndex > 0) {
            currentIndex--
            updateIndexState()
            loadStream()
            observeDownloadStatus()
        }
    }

    private fun nextEpisode() {
        if (currentIndex < episodeUrls.size - 1) {
            currentIndex++
            updateIndexState()
            loadStream()
            observeDownloadStatus()
        }
    }

    private fun updateIndexState() {
        _state.update {
            it.copy(
                currentIndex = currentIndex,
                title = episodeTitles.getOrElse(currentIndex) { "" },
                nextEpisodeTitle = episodeTitles.getOrNull(currentIndex + 1),
                streamUrl = null,
                currentEpisodeDownloadStatus = null,
            )
        }
    }

    private fun loadStream() {
        val url = episodeUrls.getOrNull(currentIndex) ?: return
        if (url.startsWith("file://")) {
            _state.update { it.copy(isLoading = false, streamUrl = url) }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getVideoStream(url)
                .onSuccess { streamUrl ->
                    _state.update { it.copy(isLoading = false, streamUrl = streamUrl) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun observeDownloadStatus() {
        val url = episodeUrls.getOrNull(currentIndex) ?: return
        if (url.startsWith("file://")) return
        statusJob?.cancel()
        statusJob = getEpisodeDownloadStatus(url)
            .onEach { item -> _state.update { it.copy(currentEpisodeDownloadStatus = item?.status) } }
            .launchIn(viewModelScope)
    }

    fun currentEpisodePageUrl() = episodeUrls.getOrNull(currentIndex).orEmpty()
    fun currentEpisodeTitle() = episodeTitles.getOrElse(currentIndex) { "" }
    fun animeTitle() = animeTitle
    fun sourceName() = sourceName
}
