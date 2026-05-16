package com.greenrou.kanata.domain.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.greenrou.kanata.data.worker.EpisodeDownloadWorker
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.DownloadStatus
import com.greenrou.kanata.domain.repository.DownloadRepository

class RetryDownloadUseCase(
    private val repo: DownloadRepository,
    private val workManager: WorkManager,
) {
    suspend operator fun invoke(item: DownloadItem) {
        repo.updateStatus(item.id, DownloadStatus.QUEUED, null)

        val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .setInputData(
                workDataOf(
                    EpisodeDownloadWorker.KEY_DOWNLOAD_ID to item.id,
                    EpisodeDownloadWorker.KEY_EPISODE_URL to item.episodePageUrl,
                    EpisodeDownloadWorker.KEY_ANIME_TITLE to item.animeTitle,
                    EpisodeDownloadWorker.KEY_SOURCE_NAME to item.sourceName,
                    EpisodeDownloadWorker.KEY_EPISODE_TITLE to item.episodeTitle,
                )
            )
            .addTag(EpisodeDownloadWorker.buildTag(item.id))
            .build()

        workManager.enqueueUniqueWork(
            EpisodeDownloadWorker.buildTag(item.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
