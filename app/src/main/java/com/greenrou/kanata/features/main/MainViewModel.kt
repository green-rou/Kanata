package com.greenrou.kanata.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeListUseCase
import com.greenrou.kanata.domain.usecase.GetFavoritesUseCase
import com.greenrou.kanata.domain.usecase.IsFavoriteUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.features.main.model.MainEvent
import com.greenrou.kanata.features.main.model.MainState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val getAnimeList: GetAnimeListUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val isFavorite: IsFavoriteUseCase,
    private val getFavorites: GetFavoritesUseCase,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    val favoriteIds: StateFlow<List<Int>> = getFavorites.observeIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.success(emptyList())
        ).let { flow ->
            val resultFlow = MutableStateFlow<List<Int>>(emptyList())
            viewModelScope.launch {
                flow.collect { result ->
                    result.onSuccess { ids -> resultFlow.value = ids }
                }
            }
            resultFlow.asStateFlow()
        }

    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeSettings()
        loadAnime()
    }

    private fun observeSettings() {
        combine(
            settingsManager.showAdultContent,
            settingsManager.isDarkTheme
        ) { showAdult, isDark ->
            if (_state.value.showAdultContent != showAdult) {
                _state.update { it.copy(showAdultContent = showAdult, animeList = emptyList()) }
                loadAnime(showAdultContent = showAdult)
            }
            _state.update { it.copy(isDarkTheme = isDark) }
        }.launchIn(viewModelScope)
    }

    private fun toggleFavorite(animeId: Int) {
        viewModelScope.launch {
            val isCurrentFavorite = favoriteIds.value.contains(animeId)
            if (isCurrentFavorite) {
                removeFavorite(animeId)
            } else {
                val anime = _state.value.animeList.find { it.id == animeId } ?: return@launch
                addFavorite(anime)
            }
        }
    }

    fun handleEvent(event: MainEvent) {
        when (event) {
            MainEvent.LoadAnime -> loadAnime()
            MainEvent.Refresh -> refreshAnime()
            MainEvent.LoadMore -> loadMore()
            MainEvent.ToggleAdultContent -> {
                viewModelScope.launch {
                    settingsManager.setShowAdultContent(!_state.value.showAdultContent)
                }
            }
            MainEvent.ToggleTheme -> {
                viewModelScope.launch {
                    settingsManager.setDarkTheme(!_state.value.isDarkTheme)
                }
            }
            is MainEvent.ToggleFavorite -> toggleFavorite(event.animeId)
            is MainEvent.AnimeClicked -> viewModelScope.launch {
                _events.send(MainEvent.NavigateToDetail(event.animeId))
            }
            else -> Unit
        }
    }

    private fun loadAnime(showAdultContent: Boolean = _state.value.showAdultContent) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getAnimeList(page = 1, showAdultContent = showAdultContent)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            animeList = page.items,
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNextPage,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                    _events.send(MainEvent.ShowError(e.message ?: "Unknown error"))
                }
        }
    }

    private fun refreshAnime() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            getAnimeList(page = 1, showAdultContent = _state.value.showAdultContent)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            animeList = page.items,
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNextPage,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isRefreshing = false) }
                    _events.send(MainEvent.ShowError(e.message ?: "Unknown error"))
                }
        }
    }

    private fun loadMore() {
        val current = _state.value
        if (!current.hasNextPage || current.isLoadingMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            getAnimeList(
                page = current.currentPage + 1,
                showAdultContent = current.showAdultContent
            )
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            animeList = it.animeList + page.items,
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNextPage,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingMore = false) }
                    _events.send(MainEvent.ShowError(e.message ?: "Unknown error"))
                }
        }
    }
}
