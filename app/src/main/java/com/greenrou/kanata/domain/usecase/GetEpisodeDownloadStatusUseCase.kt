package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetEpisodeDownloadStatusUseCase(private val repo: DownloadRepository) {
    operator fun invoke(episodePageUrl: String): Flow<DownloadItem?> =
        repo.getAllDownloads().map { list -> list.find { it.episodePageUrl == episodePageUrl } }
}
