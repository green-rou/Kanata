package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class GetDownloadQueueUseCase(private val repo: DownloadRepository) {
    operator fun invoke(): Flow<List<DownloadItem>> = repo.getQueuedDownloads()
}
