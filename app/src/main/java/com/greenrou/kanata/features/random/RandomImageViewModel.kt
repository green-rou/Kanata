package com.greenrou.kanata.features.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.core.analytics.reportToCrashlytics
import com.greenrou.kanata.data.mod.MangaModRegistry
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.GetRandomAnimeUseCase
import com.greenrou.kanata.domain.usecase.GetRandomImageUseCase
import com.greenrou.kanata.domain.usecase.IsFavoriteUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.features.random.model.RandomEvent
import com.greenrou.kanata.features.random.model.RandomImageState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RandomImageViewModel(
    private val getRandomAnime: GetRandomAnimeUseCase,
    private val getRandomImage: GetRandomImageUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val isFavorite: IsFavoriteUseCase,
    private val settingsManager: SettingsManager,
    private val mangaModRegistry: MangaModRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(RandomImageState())
    val state = _state.asStateFlow()

    private val _events = Channel<RandomEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var favoriteObserverJob: Job? = null

    init {
        loadRandomAnime()
        loadRandomImage()
    }

    fun handleEvent(event: RandomEvent) {
        when (event) {
            RandomEvent.RefreshAnime -> loadRandomAnime()
            RandomEvent.RefreshImage -> loadRandomImage()
            RandomEvent.ToggleFavorite -> toggleFavorite()
            is RandomEvent.AnimeClicked -> viewModelScope.launch {
                _events.send(RandomEvent.NavigateToDetails(event.animeId))
            }
            is RandomEvent.NavigateToDetails -> Unit
        }
    }

    private fun loadRandomAnime() {
        viewModelScope.launch {
            favoriteObserverJob?.cancel()
            _state.update { it.copy(isAnimeLoading = true, animeError = null, isAnimeFavorite = false) }
            val mediaType = if (settingsManager.isMangaMode.first()) {
                mangaModRegistry.activeProvider.value?.mediaType ?: "ANIME"
            } else {
                "ANIME"
            }
            getRandomAnime(mediaType)
                .onSuccess { anime ->
                    _state.update { it.copy(isAnimeLoading = false, randomAnime = anime) }
                    favoriteObserverJob = launch { observeFavoriteStatus(anime.id) }
                }
                .onFailure { e ->
                    e.reportToCrashlytics("random_load_anime")
                    _state.update { it.copy(isAnimeLoading = false, animeError = e.message) }
                }
        }
    }

    private fun toggleFavorite() {
        val anime = _state.value.randomAnime ?: return
        viewModelScope.launch {
            if (_state.value.isAnimeFavorite) {
                removeFavorite(anime.id)
            } else {
                addFavorite(anime)
            }
        }
    }

    private fun loadRandomImage() {
        viewModelScope.launch {
            _state.update { it.copy(isImageLoading = true, imageError = null) }
            getRandomImage()
                .onSuccess { url ->
                    _state.update { it.copy(isImageLoading = false, imageUrl = url) }
                }
                .onFailure { e ->
                    e.reportToCrashlytics("random_load_image")
                    _state.update { it.copy(isImageLoading = false, imageError = e.message) }
                }
        }
    }

    private suspend fun observeFavoriteStatus(animeId: Int) {
        isFavorite.observe(animeId).collect { result ->
            result.onSuccess { fav ->
                _state.update { it.copy(isAnimeFavorite = fav) }
            }
        }
    }
}
