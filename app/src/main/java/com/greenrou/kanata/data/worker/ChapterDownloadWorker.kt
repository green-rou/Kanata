package com.greenrou.kanata.data.worker

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.greenrou.kanata.R
import com.greenrou.kanata.domain.model.DownloadStatus
import com.greenrou.kanata.domain.repository.DownloadRepository
import com.greenrou.kanata.domain.usecase.GetContentPagesUseCase
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ChapterDownloadWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val getContentPages: GetContentPagesUseCase by inject()
    private val downloadRepo: DownloadRepository by inject()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_CHAPTER_URL = "chapter_url"
        const val KEY_MANGA_TITLE = "manga_title"
        const val KEY_SOURCE_NAME = "source_name"
        const val KEY_CHAPTER_TITLE = "chapter_title"

        fun buildTag(downloadId: Long) = "chapter_download_$downloadId"

        private fun sanitize(name: String) =
            name.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return Result.failure()
        val mangaTitle = inputData.getString(KEY_MANGA_TITLE) ?: ""
        val sourceName = inputData.getString(KEY_SOURCE_NAME) ?: ""
        val chapterTitle = inputData.getString(KEY_CHAPTER_TITLE) ?: ""

        ensureNotificationChannel()
        setForeground(buildForeground(chapterTitle, 0))
        downloadRepo.updateStatus(downloadId, DownloadStatus.DOWNLOADING)

        val pages = getContentPages(chapterUrl).getOrElse { e ->
            downloadRepo.updateStatus(downloadId, DownloadStatus.FAILED, e.message)
            return Result.failure(workDataOf("error" to e.message))
        }

        if (pages.isEmpty()) {
            downloadRepo.updateStatus(downloadId, DownloadStatus.FAILED, "No pages found")
            return Result.failure()
        }

        val destFolder = downloadRepo.getDownloadFolder()
        val chapterDir = File(
            "$destFolder/Manga/${sanitize(mangaTitle)}/${sanitize(sourceName)}/${sanitize(chapterTitle)}"
        )
        chapterDir.mkdirs()

        try {
            pages.forEachIndexed { index, page ->
                if (isStopped) {
                    chapterDir.deleteRecursively()
                    downloadRepo.updateStatus(downloadId, DownloadStatus.CANCELLED)
                    return Result.success()
                }

                val ext = page.url.substringBefore("?").substringAfterLast(".").lowercase()
                    .let { if (it in listOf("jpg", "jpeg", "png", "webp")) it else "jpg" }
                val fileName = "%03d.$ext".format(index + 1)
                val destFile = File(chapterDir, fileName)

                val request = Request.Builder().url(page.url)
                    .apply { page.headers.forEach { (k, v) -> header(k, v) } }
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@forEachIndexed
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(destFile).use { out -> input.copyTo(out) }
                }

                val progress = ((index + 1) * 100) / pages.size
                downloadRepo.updateProgress(downloadId, progress, chapterDir.totalBytes())
                setForeground(buildForeground(chapterTitle, progress))
            }

            if (isStopped) {
                chapterDir.deleteRecursively()
                downloadRepo.updateStatus(downloadId, DownloadStatus.CANCELLED)
                return Result.success()
            }

            downloadRepo.setLocalFilePath(downloadId, chapterDir.absolutePath)
            downloadRepo.updateStatus(downloadId, DownloadStatus.COMPLETED)
            return Result.success()
        } catch (e: Exception) {
            chapterDir.deleteRecursively()
            downloadRepo.updateStatus(downloadId, DownloadStatus.FAILED, e.message)
            return Result.failure(workDataOf("error" to e.message))
        }
    }

    private fun File.totalBytes(): Long =
        walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun ensureNotificationChannel() {
        val channel = android.app.NotificationChannel(
            EpisodeDownloadWorker.NOTIFICATION_CHANNEL_ID,
            appContext.getString(R.string.notif_channel_downloads),
            NotificationManager.IMPORTANCE_LOW,
        )
        appContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildForeground(title: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(
            appContext,
            EpisodeDownloadWorker.NOTIFICATION_CHANNEL_ID,
        )
            .setContentTitle(appContext.getString(R.string.notif_downloading, title))
            .setProgress(100, progress, progress == 0)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return ForegroundInfo(
            1002,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}
