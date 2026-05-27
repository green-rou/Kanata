package com.greenrou.kanata.features.details.model

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.model.AnimeEnrichment
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.VideoSource

data class AnimeDetailsState(
    val isLoading: Boolean = false,
    val anime: Anime? = null,
    val isFavorite: Boolean = false,
    val videoSources: List<VideoSource> = emptyList(),
    val isSearching: Boolean = false,
    val hasStreamSources: Boolean = true,
    val error: String? = null,
    val coverFillsTopBar: Boolean = true,
    val downloadedEpisodeCount: Int = 0,
    val isOffline: Boolean = false,
    val offlineEpisodesForPicker: List<DownloadItem> = emptyList(),
    val enrichment: AnimeEnrichment? = null,
)
