package com.greenrou.kanata.domain.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.greenrou.kanata.data.worker.ChapterDownloadWorker

class StartChapterDownloadUseCase(
    private val enqueue: EnqueueDownloadUseCase,
    private val workManager: WorkManager,
) {
    suspend operator fun invoke(
        mangaTitle: String,
        sourceName: String,
        chapterTitle: String,
        chapterUrl: String,
        mangaPageUrl: String = "",
    ) {
        val downloadId = enqueue(
            animeTitle = mangaTitle,
            sourceName = sourceName,
            episodeTitle = chapterTitle,
            episodePageUrl = chapterUrl,
            animePageUrl = mangaPageUrl,
            isManga = true,
        ) ?: return

        val request = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setInputData(
                workDataOf(
                    ChapterDownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                    ChapterDownloadWorker.KEY_CHAPTER_URL to chapterUrl,
                    ChapterDownloadWorker.KEY_MANGA_TITLE to mangaTitle,
                    ChapterDownloadWorker.KEY_SOURCE_NAME to sourceName,
                    ChapterDownloadWorker.KEY_CHAPTER_TITLE to chapterTitle,
                )
            )
            .addTag(ChapterDownloadWorker.buildTag(downloadId))
            .build()

        workManager.enqueueUniqueWork(
            ChapterDownloadWorker.buildTag(downloadId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
