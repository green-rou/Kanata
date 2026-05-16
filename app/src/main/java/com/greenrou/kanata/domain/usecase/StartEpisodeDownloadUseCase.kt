package com.greenrou.kanata.domain.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.greenrou.kanata.data.worker.EpisodeDownloadWorker

class StartEpisodeDownloadUseCase(
    private val enqueue: EnqueueDownloadUseCase,
    private val workManager: WorkManager,
) {
    suspend operator fun invoke(
        animeTitle: String,
        sourceName: String,
        episodeTitle: String,
        episodePageUrl: String,
        animePageUrl: String = "",
        animeId: Int = 0,
    ) {
        val downloadId = enqueue(animeTitle, sourceName, episodeTitle, episodePageUrl, animePageUrl, animeId) ?: return

        val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .setInputData(
                workDataOf(
                    EpisodeDownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                    EpisodeDownloadWorker.KEY_EPISODE_URL to episodePageUrl,
                    EpisodeDownloadWorker.KEY_ANIME_TITLE to animeTitle,
                    EpisodeDownloadWorker.KEY_SOURCE_NAME to sourceName,
                    EpisodeDownloadWorker.KEY_EPISODE_TITLE to episodeTitle,
                )
            )
            .addTag(EpisodeDownloadWorker.buildTag(downloadId))
            .build()

        workManager.enqueueUniqueWork(
            EpisodeDownloadWorker.buildTag(downloadId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
