package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.DownloadRepository

class ReorderDownloadQueueUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(orderedIds: List<Long>) = repo.reorderQueue(orderedIds)
}
