package com.greenrou.kanata.features.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.core.analytics.AnalyticsManager
import com.greenrou.kanata.data.mod.MangaModRegistry
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeByMoodUseCase
import com.greenrou.kanata.domain.usecase.GetFavoritesUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.features.mood.model.Mood
import com.greenrou.kanata.features.mood.model.MoodEvent
import com.greenrou.kanata.features.mood.model.MoodState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoodViewModel(
    private val getAnimeByMood: GetAnimeByMoodUseCase,
    private val settingsManager: SettingsManager,
    private val analytics: AnalyticsManager,
    private val getFavorites: GetFavoritesUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val mangaModRegistry: MangaModRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(MoodState())
    val state = _state.asStateFlow()

    private val _events = Channel<MoodEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val favoriteIds: StateFlow<List<Int>> = getFavorites.observeIds()
        .mapNotNull { it.getOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        analytics.setScreen("discover")
        observeMangaMode()
    }

    fun handleEvent(event: MoodEvent) {
        when (event) {
            is MoodEvent.SelectMood -> selectMood(event.mood)
            MoodEvent.ClearMood -> clearMood()
            is MoodEvent.AnimeClicked -> viewModelScope.launch {
                _events.send(MoodEvent.NavigateToDetails(event.animeId))
            }
            is MoodEvent.ToggleFavorite -> toggleFavorite(event.animeId)
            is MoodEvent.NavigateToDetails -> Unit
        }
    }

    private fun selectMood(mood: Mood) {
        _state.update { it.copy(selectedMood = mood, isLoading = true, error = null) }
        loadAnimeByMood(mood)
    }

    private fun clearMood() {
        _state.update { it.copy(selectedMood = null, animeList = emptyList(), error = null) }
    }

    private fun observeMangaMode() {
        settingsManager.isMangaMode
            .drop(1)
            .onEach {
                val currentMood = _state.value.selectedMood
                if (currentMood != null) {
                    _state.update { it.copy(animeList = emptyList(), isLoading = true, error = null) }
                    loadAnimeByMood(currentMood)
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

    private fun loadAnimeByMood(mood: Mood) {
        viewModelScope.launch {
            val showAdult = settingsManager.showAdultContent.first()
            val mediaType = if (settingsManager.isMangaMode.first()) {
                mangaModRegistry.activeProvider.value?.mediaType ?: "ANIME"
            } else {
                "ANIME"
            }
            getAnimeByMood(
                genres = mood.genres,
                tags = mood.tags,
                showAdultContent = showAdult,
                mediaType = mediaType,
            ).onSuccess { page ->
                _state.update { it.copy(isLoading = false, animeList = page.items) }
            }.onFailure { e ->
                analytics.recordError(e, "mood_load_anime", mapOf("mood" to mood.name))
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
