package com.greenrou.kanata.features.main.model

sealed interface MainEvent {
    data object LoadAnime : MainEvent
    data object Refresh : MainEvent
    data object LoadMore : MainEvent
    data class AnimeClicked(val animeId: Int) : MainEvent
    data object SearchClicked : MainEvent
    data object ToggleAdultContent : MainEvent
    data object ToggleTheme : MainEvent
    data object ToggleCoverLayout : MainEvent
    data class ToggleFavorite(val animeId: Int) : MainEvent

    data class NavigateToDetail(val animeId: Int) : MainEvent
    data class ShowError(val message: String) : MainEvent
}
