package com.greenrou.kanata.features.downloads.model

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.WatchProgress

data class DownloadManagerState(
    val queuedDownloads: List<DownloadItem> = emptyList(),
    val completedDownloads: List<DownloadItem> = emptyList(),
    val downloadFolder: String = "",
    val searchQuery: String = "",
    val watchProgress: Map<String, WatchProgress> = emptyMap(),
)
