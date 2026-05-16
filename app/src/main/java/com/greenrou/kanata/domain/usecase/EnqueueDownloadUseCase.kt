package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.DownloadStatus
import com.greenrou.kanata.domain.repository.DownloadRepository

class EnqueueDownloadUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(
        animeTitle: String,
        sourceName: String,
        episodeTitle: String,
        episodePageUrl: String,
        animePageUrl: String = "",
        animeId: Int = 0,
    ): Long? {
        val existing = repo.getDownloadByEpisodeUrl(episodePageUrl)
        if (existing != null && existing.status in listOf(
                DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.COMPLETED
            )
        ) return null

        return repo.enqueueDownload(
            DownloadItem(
                id = 0,
                animeTitle = animeTitle,
                sourceName = sourceName,
                episodeTitle = episodeTitle,
                episodePageUrl = episodePageUrl,
                animePageUrl = animePageUrl,
                animeId = animeId,
                localFilePath = null,
                status = DownloadStatus.QUEUED,
                progressPercent = 0,
                queuePosition = 0,
                createdAt = System.currentTimeMillis(),
                fileSizeBytes = 0,
                errorMessage = null,
            )
        )
    }
}
