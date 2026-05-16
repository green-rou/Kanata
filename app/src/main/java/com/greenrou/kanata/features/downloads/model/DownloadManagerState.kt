package com.greenrou.kanata.features.downloads.model

import com.greenrou.kanata.domain.model.DownloadItem

data class DownloadManagerState(
    val queuedDownloads: List<DownloadItem> = emptyList(),
    val completedDownloads: List<DownloadItem> = emptyList(),
    val downloadFolder: String = "",
    val searchQuery: String = "",
)
