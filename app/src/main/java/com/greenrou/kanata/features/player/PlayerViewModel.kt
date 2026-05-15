package com.greenrou.kanata.features.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.usecase.GetVideoStreamUseCase
import com.greenrou.kanata.features.player.model.PlayerEvent
import com.greenrou.kanata.features.player.model.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val getVideoStream: GetVideoStreamUseCase,
    private val episodeUrls: List<String>,
    private val episodeTitles: List<String>,
    startIndex: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(
        PlayerState(
            isLoading = true,
            title = episodeTitles.getOrElse(startIndex) { "" },
            currentIndex = startIndex,
            episodeCount = episodeUrls.size,
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<PlayerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentIndex = startIndex
    private var loadJob: Job? = null

    init {
        loadStream()
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
                Log.e("Kanata:Player", "ExoPlayer error: ${event.message} | streamUrl=${_state.value.streamUrl}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "${event.message}\n\nURL: ${it.streamUrl ?: "unknown"}",
                    )
                }
            }
            PlayerEvent.NavigateBack -> Unit
        }
    }

    private fun previousEpisode() {
        if (currentIndex > 0) {
            currentIndex--
            updateIndexState()
            loadStream()
        }
    }

    private fun nextEpisode() {
        if (currentIndex < episodeUrls.size - 1) {
            currentIndex++
            updateIndexState()
            loadStream()
        }
    }

    private fun updateIndexState() {
        _state.update {
            it.copy(
                currentIndex = currentIndex,
                title = episodeTitles.getOrElse(currentIndex) { "" },
                streamUrl = null,
            )
        }
    }

    private fun loadStream() {
        val url = episodeUrls.getOrNull(currentIndex) ?: return
        Log.d("Kanata:Player", "Loading episode [$currentIndex]: $url")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getVideoStream(url)
                .onSuccess { streamUrl ->
                    Log.d("Kanata:Player", "Stream URL: $streamUrl")
                    _state.update { it.copy(isLoading = false, streamUrl = streamUrl) }
                }
                .onFailure { e ->
                    Log.e("Kanata:Player", "Failed to get stream for $url", e)
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
