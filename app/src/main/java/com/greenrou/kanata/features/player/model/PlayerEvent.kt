package com.greenrou.kanata.features.player.model

sealed interface PlayerEvent {
    data object BackClicked : PlayerEvent
    data object PreviousEpisode : PlayerEvent
    data object NextEpisode : PlayerEvent

    data object NavigateBack : PlayerEvent
}
