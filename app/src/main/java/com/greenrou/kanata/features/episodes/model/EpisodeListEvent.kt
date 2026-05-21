package com.greenrou.kanata.features.episodes.model

import com.greenrou.kanata.domain.model.Translation

sealed interface EpisodeListEvent {
    data object BackClicked : EpisodeListEvent
    data class EpisodeClicked(
        val urls: List<String>,
        val titles: List<String>,
        val index: Int,
    ) : EpisodeListEvent
    data class DownloadEpisode(
        val episodePageUrl: String,
        val animePageUrl: String,
        val episodeTitle: String,
        val animeTitle: String,
        val sourceName: String,
        val animeId: Int,
    ) : EpisodeListEvent

    data object DismissTranslationSheet : EpisodeListEvent
    data class TranslationSelected(val translation: Translation) : EpisodeListEvent

    data object NavigateBack : EpisodeListEvent
    data class NavigateToPlayer(
        val urls: List<String>,
        val titles: List<String>,
        val index: Int,
    ) : EpisodeListEvent
}
