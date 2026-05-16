package com.greenrou.kanata.features.player.model

sealed interface PlayerEvent {
    data object BackClicked : PlayerEvent
    data object PreviousEpisode : PlayerEvent
    data object NextEpisode : PlayerEvent
    data object Retry : PlayerEvent
    data class PlaybackError(val message: String) : PlayerEvent
    data class DownloadCurrentEpisode(
        val episodePageUrl: String,
        val episodeTitle: String,
        val animeTitle: String,
        val sourceName: String,
    ) : PlayerEvent

    data object NavigateBack : PlayerEvent
}
