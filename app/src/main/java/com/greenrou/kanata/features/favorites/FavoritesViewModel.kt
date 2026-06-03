package com.greenrou.kanata.features.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.DeleteSavedPageUseCase
import com.greenrou.kanata.domain.usecase.GetFavoritesUseCase
import com.greenrou.kanata.domain.usecase.GetSavedPagesUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.features.favorites.model.FavoritesEvent
import com.greenrou.kanata.features.favorites.model.FavoritesState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModel(
    private val getFavorites: GetFavoritesUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val getSavedPages: GetSavedPagesUseCase,
    private val deleteSavedPage: DeleteSavedPageUseCase,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val _limit = MutableStateFlow(20)
    private val pageSize = 20

    private val _state = MutableStateFlow(FavoritesState())
    val state = _state.asStateFlow()

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeFavorites()
        observeSavedPages()
        observeMangaMode()
    }

    fun handleEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.AnimeClicked -> viewModelScope.launch {
                _events.send(FavoritesEvent.NavigateToDetails(event.animeId))
            }
            is FavoritesEvent.ToggleFavorite -> removeFavoriteItem(event.animeId)
            FavoritesEvent.LoadMore -> loadMore()
            is FavoritesEvent.SavedPageClicked -> viewModelScope.launch {
                _events.send(FavoritesEvent.NavigateToWebPlayer(event.url))
            }
            is FavoritesEvent.DeleteSavedPage -> viewModelScope.launch {
                deleteSavedPage(event.id)
            }
            is FavoritesEvent.NavigateToDetails -> Unit
            is FavoritesEvent.NavigateToWebPlayer -> Unit
        }
    }

    private fun observeFavorites() {
        combine(_limit, settingsManager.isMangaMode) { limit, isManga -> limit to isManga }
            .flatMapLatest { (limit, isManga) ->
                getFavorites.observePaged(limit, isManga).map { it to limit }
            }
            .onEach { (result, limit) ->
                result.onSuccess { list ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            favorites = list,
                            hasNextPage = list.size >= limit,
                        )
                    }
                }.onFailure {
                    _state.update { it.copy(isLoading = false, isLoadingMore = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeMangaMode() {
        settingsManager.isMangaMode
            .drop(1)
            .onEach { _limit.value = pageSize }
            .launchIn(viewModelScope)
    }

    private fun observeSavedPages() {
        getSavedPages()
            .onEach { pages -> _state.update { it.copy(savedPages = pages) } }
            .launchIn(viewModelScope)
    }

    private fun loadMore() {
        val currentLimit = _limit.value
        if (_state.value.isLoadingMore || !_state.value.hasNextPage) return
        _state.update { it.copy(isLoadingMore = true) }
        _limit.value = currentLimit + pageSize
    }

    private fun removeFavoriteItem(animeId: Int) {
        viewModelScope.launch {
            removeFavorite(animeId)
        }
    }
}
