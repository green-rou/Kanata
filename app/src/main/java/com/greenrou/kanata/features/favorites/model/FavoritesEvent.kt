package com.greenrou.kanata.features.favorites.model

sealed interface FavoritesEvent {
    data class AnimeClicked(val animeId: Int) : FavoritesEvent
    data class ToggleFavorite(val animeId: Int) : FavoritesEvent
    data object LoadMore : FavoritesEvent

    data class NavigateToDetails(val animeId: Int) : FavoritesEvent
}
