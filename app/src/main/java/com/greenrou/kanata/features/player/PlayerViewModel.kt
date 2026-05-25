package com.greenrou.kanata.features.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.core.analytics.AnalyticsManager
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

private const val TAG = "PlayerVM"

class PlayerViewModel(
    private val getVideoStream: GetVideoStreamUseCase,
    private val startEpisodeDownload: StartEpisodeDownloadUseCase,
    private val getEpisodeDownloadStatus: GetEpisodeDownloadStatusUseCase,
    private val analytics: AnalyticsManager,
    private val episodeUrls: List<String>,
    private val episodeTitles: List<String>,
    startIndex: Int,
    private val animeTitle: String = "",
    private val sourceName: String = "",
    private val initialHeaderKeys: List<String> = emptyList(),
    private val initialHeaderValues: List<String> = emptyList(),
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
        analytics.setScreen("player")
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
                val url = _state.value.streamUrl ?: "unknown"
                Log.e(TAG, "PlaybackError: ${event.message}  streamUrl=$url")
                analytics.recordError(
                    RuntimeException("PlaybackError: ${event.message}"),
                    "player_playback_error",
                    mapOf("stream_url" to url, "anime" to animeTitle, "source" to sourceName),
                )
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
                streamHeaders = emptyMap(),
                currentEpisodeDownloadStatus = null,
                isChangingEpisode = true,
            )
        }
    }

    private fun loadStream() {
        val url = episodeUrls.getOrNull(currentIndex) ?: return
        Log.i(TAG, "loadStream: episodeUrl=$url")
        if (url.startsWith("file://") || isDirectStreamUrl(url)) {
            val headers = initialHeaderKeys.zip(initialHeaderValues).toMap()
            Log.i(TAG, "Direct stream, skipping extraction → streamUrl=$url  headers=$headers")
            _state.update { it.copy(isLoading = false, streamUrl = url, streamHeaders = headers, isChangingEpisode = false) }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getVideoStream(url)
                .onSuccess { stream ->
                    Log.i(TAG, "Stream loaded → url=${stream.url}  headers=${stream.headers}")
                    _state.update { it.copy(isLoading = false, streamUrl = stream.url, streamHeaders = stream.headers, isChangingEpisode = false) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Stream load failed for episodeUrl=$url", e)
                    analytics.recordError(e, "player_load_stream", mapOf("episode_url" to url, "anime" to animeTitle, "source" to sourceName))
                    _state.update { it.copy(isLoading = false, error = e.message, isChangingEpisode = false) }
                }
        }
    }

    private fun isDirectStreamUrl(url: String) =
        Regex("""\.(m3u8|mp4|mkv|webm|ts)(\?|${'$'})""", RegexOption.IGNORE_CASE).containsMatchIn(url)

    private fun observeDownloadStatus() {
        val url = episodeUrls.getOrNull(currentIndex) ?: return
        if (url.startsWith("file://") || isDirectStreamUrl(url)) return
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
