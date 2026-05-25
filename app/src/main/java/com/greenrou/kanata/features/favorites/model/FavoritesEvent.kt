package com.greenrou.kanata.features.favorites.model

sealed interface FavoritesEvent {
    data class AnimeClicked(val animeId: Int) : FavoritesEvent
    data class ToggleFavorite(val animeId: Int) : FavoritesEvent
    data object LoadMore : FavoritesEvent
    data class SavedPageClicked(val url: String) : FavoritesEvent
    data class DeleteSavedPage(val id: Long) : FavoritesEvent

    // navigation — sent via Channel
    data class NavigateToDetails(val animeId: Int) : FavoritesEvent
    data class NavigateToWebPlayer(val url: String) : FavoritesEvent
}
