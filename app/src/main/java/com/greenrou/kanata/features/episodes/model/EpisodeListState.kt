package com.greenrou.kanata.features.episodes.model

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.Episode

data class EpisodeListState(
    val isLoading: Boolean = false,
    val episodes: List<Episode> = emptyList(),
    val error: String? = null,
    val downloadStatuses: Map<String, DownloadItem> = emptyMap(),
)
