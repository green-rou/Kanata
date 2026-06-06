package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.repository.DownloadRepository

class GetSiblingDownloadsUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(animeTitle: String): List<DownloadItem> =
        repo.getCompletedVideosByAnimeTitle(animeTitle)
}
