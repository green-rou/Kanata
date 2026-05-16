package com.greenrou.kanata.domain.usecase

import androidx.work.WorkManager
import com.greenrou.kanata.data.worker.EpisodeDownloadWorker
import com.greenrou.kanata.domain.repository.DownloadRepository

class CancelDownloadUseCase(
    private val repo: DownloadRepository,
    private val workManager: WorkManager,
) {
    suspend operator fun invoke(id: Long) {
        workManager.cancelUniqueWork(EpisodeDownloadWorker.buildTag(id))
        repo.cancelDownload(id)
    }
}
