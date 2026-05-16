package com.greenrou.kanata.domain.model

data class DownloadItem(
    val id: Long,
    val animeTitle: String,
    val sourceName: String,
    val episodeTitle: String,
    val episodePageUrl: String,
    val animePageUrl: String,
    val animeId: Int,
    val localFilePath: String?,
    val status: DownloadStatus,
    val progressPercent: Int,
    val queuePosition: Int,
    val createdAt: Long,
    val fileSizeBytes: Long,
    val errorMessage: String?,
)
