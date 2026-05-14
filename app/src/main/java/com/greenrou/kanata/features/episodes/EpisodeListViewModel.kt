package com.greenrou.kanata.features.episodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.usecase.GetEpisodeListUseCase
import com.greenrou.kanata.features.episodes.model.EpisodeListEvent
import com.greenrou.kanata.features.episodes.model.EpisodeListState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EpisodeListViewModel(
    private val getEpisodeList: GetEpisodeListUseCase,
    val animePageUrl: String,
    val label: String,
) : ViewModel() {

    private val _state = MutableStateFlow(EpisodeListState(isLoading = true))
    val state = _state.asStateFlow()

    private val _events = Channel<EpisodeListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadEpisodes()
    }

    fun handleEvent(event: EpisodeListEvent) {
        when (event) {
            EpisodeListEvent.BackClicked -> viewModelScope.launch {
                _events.send(EpisodeListEvent.NavigateBack)
            }
            is EpisodeListEvent.EpisodeClicked -> viewModelScope.launch {
                _events.send(EpisodeListEvent.NavigateToPlayer(event.urls, event.titles, event.index))
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
}
