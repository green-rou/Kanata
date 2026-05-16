package com.greenrou.kanata.features.details.model

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.VideoSource

sealed interface AnimeDetailsEvent {
    data class LoadAnime(val animeId: Int) : AnimeDetailsEvent
    data object BackClicked : AnimeDetailsEvent
    data object ToggleFavorite : AnimeDetailsEvent
    data class OpenEpisodeList(val source: VideoSource) : AnimeDetailsEvent
    data object WatchOffline : AnimeDetailsEvent

    data object NavigateBack : AnimeDetailsEvent
    data class ShowError(val message: String) : AnimeDetailsEvent
    data class NavigateToEpisodeList(val source: VideoSource, val animeTitle: String) : AnimeDetailsEvent
    data class NavigateToOfflinePlayer(val items: List<DownloadItem>) : AnimeDetailsEvent
}
