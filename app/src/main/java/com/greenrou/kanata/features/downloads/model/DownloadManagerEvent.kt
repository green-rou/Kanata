package com.greenrou.kanata.features.downloads.model

import com.greenrou.kanata.domain.model.DownloadItem

sealed interface DownloadManagerEvent {
    data class SearchQueryChanged(val query: String) : DownloadManagerEvent
    data class CancelDownload(val id: Long) : DownloadManagerEvent
    data class DeleteDownload(val id: Long, val localFilePath: String?) : DownloadManagerEvent
    data class ReorderQueue(val orderedIds: List<Long>) : DownloadManagerEvent
    data class PlayDownloaded(val item: DownloadItem) : DownloadManagerEvent
    data class RetryDownload(val item: DownloadItem) : DownloadManagerEvent
    data class FolderChosen(val uri: String) : DownloadManagerEvent

    data class ShowSnackbar(val message: String) : DownloadManagerEvent
    data class NavigateToPlayer(val localFilePaths: List<String>, val titles: List<String>, val startIndex: Int, val episodePageUrls: List<String> = emptyList()) : DownloadManagerEvent
    data class NavigateToReader(val chapterFolderPath: String, val title: String, val chapterPageUrl: String = "", val animeTitle: String = "") : DownloadManagerEvent
}
