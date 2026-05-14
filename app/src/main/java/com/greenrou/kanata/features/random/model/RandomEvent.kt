package com.greenrou.kanata.features.random.model

sealed interface RandomEvent {
    data object RefreshAnime : RandomEvent
    data object RefreshImage : RandomEvent
    data object ToggleFavorite : RandomEvent
    data class AnimeClicked(val animeId: Int) : RandomEvent

    data class NavigateToDetails(val animeId: Int) : RandomEvent
}
