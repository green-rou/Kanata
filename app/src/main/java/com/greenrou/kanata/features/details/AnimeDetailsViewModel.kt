package com.greenrou.kanata.features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeByIdUseCase
import com.greenrou.kanata.domain.usecase.GetVideoStreamUseCase
import com.greenrou.kanata.domain.usecase.IsFavoriteUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.domain.usecase.SearchExternalAnimeUseCase
import com.greenrou.kanata.features.details.model.AnimeDetailsEvent
import com.greenrou.kanata.features.details.model.AnimeDetailsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnimeDetailsViewModel(
    private val getAnimeById: GetAnimeByIdUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val searchExternalAnime: SearchExternalAnimeUseCase,
    private val getVideoStreamUseCase: GetVideoStreamUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AnimeDetailsState())
    val state = _state.asStateFlow()

    private var loadedAnimeId = -1

    private val _events = Channel<AnimeDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
                _events.send(AnimeDetailsEvent.NavigateToEpisodeList(event.source))
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
                    searchOnExternal(anime.title)
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                    _events.send(AnimeDetailsEvent.ShowError(e.message ?: "Unknown error"))
                }
        }
    }

    private fun searchOnExternal(title: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val sources = searchExternalAnime(title)
            _state.update { it.copy(isSearching = false, videoSources = sources) }
        }
    }
}
