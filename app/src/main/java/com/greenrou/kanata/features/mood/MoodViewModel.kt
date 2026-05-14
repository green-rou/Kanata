package com.greenrou.kanata.features.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.GetAnimeByMoodUseCase
import com.greenrou.kanata.features.mood.model.Mood
import com.greenrou.kanata.features.mood.model.MoodEvent
import com.greenrou.kanata.features.mood.model.MoodState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoodViewModel(
    private val getAnimeByMood: GetAnimeByMoodUseCase,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MoodState())
    val state = _state.asStateFlow()

    private val _events = Channel<MoodEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun handleEvent(event: MoodEvent) {
        when (event) {
            is MoodEvent.SelectMood -> selectMood(event.mood)
            MoodEvent.ClearMood -> clearMood()
            is MoodEvent.AnimeClicked -> viewModelScope.launch {
                _events.send(MoodEvent.NavigateToDetails(event.animeId))
            }
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

    private fun loadAnimeByMood(mood: Mood) {
        viewModelScope.launch {
            val showAdult = settingsManager.showAdultContent.first()
            getAnimeByMood(
                genres = mood.genres,
                tags = mood.tags,
                showAdultContent = showAdult,
            ).onSuccess { page ->
                _state.update { it.copy(isLoading = false, animeList = page.items) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
