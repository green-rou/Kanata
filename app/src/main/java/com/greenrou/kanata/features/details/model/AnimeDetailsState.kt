package com.greenrou.kanata.features.details.model

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.model.VideoSource

data class AnimeDetailsState(
    val isLoading: Boolean = false,
    val anime: Anime? = null,
    val isFavorite: Boolean = false,
    val videoSources: List<VideoSource> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
)
