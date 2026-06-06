package com.greenrou.kanata.features.episodes.model

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.Translation
import com.greenrou.kanata.domain.model.WatchProgress

data class EpisodeListState(
    val isLoading: Boolean = false,
    val episodes: List<Episode> = emptyList(),
    val error: String? = null,
    val downloadStatuses: Map<String, DownloadItem> = emptyMap(),
    val isTranslationSheetVisible: Boolean = false,
    val isTranslationsLoading: Boolean = false,
    val translations: List<Translation> = emptyList(),
    val pendingEpisodeUrl: String = "",
    val pendingEpisodeTitle: String = "",
    val watchProgress: Map<String, WatchProgress> = emptyMap(),
)
