package com.greenrou.kanata.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.greenrou.kanata.domain.model.DownloadStatus
import com.greenrou.kanata.domain.repository.DownloadRepository
import com.greenrou.kanata.domain.usecase.GetVideoStreamUseCase
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream

class EpisodeDownloadWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val getVideoStream: GetVideoStreamUseCase by inject()
    private val downloadRepo: DownloadRepository by inject()

    private val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_EPISODE_URL = "episode_url"
        const val KEY_ANIME_TITLE = "anime_title"
        const val KEY_SOURCE_NAME = "source_name"
        const val KEY_EPISODE_TITLE = "episode_title"
        const val NOTIFICATION_CHANNEL_ID = "kanata_downloads"
        private const val FOREGROUND_NOTIFICATION_ID = 1001

        fun buildTag(downloadId: Long) = "download_$downloadId"

        fun sanitizeName(name: String): String =
            name.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        val episodeUrl = inputData.getString(KEY_EPISODE_URL) ?: return Result.failure()
        val animeTitle = inputData.getString(KEY_ANIME_TITLE) ?: ""
        val sourceName = inputData.getString(KEY_SOURCE_NAME) ?: ""
        val episodeTitle = inputData.getString(KEY_EPISODE_TITLE) ?: ""

        createNotificationChannel()
        setForeground(createForegroundInfo(episodeTitle, 0))

        downloadRepo.updateStatus(downloadId, DownloadStatus.DOWNLOADING)

        val streamResult = getVideoStream(episodeUrl)
        if (streamResult.isFailure) {
            downloadRepo.updateStatus(
                downloadId,
                DownloadStatus.FAILED,
                streamResult.exceptionOrNull()?.message ?: "Stream resolution failed"
            )
            return Result.failure()
        }
        val streamUrl = streamResult.getOrThrow()

        val destFolder = downloadRepo.getDownloadFolder()
        val isHls = streamUrl.contains(".m3u8", ignoreCase = true)
        val ext = if (isHls) "ts" else streamUrl.substringBefore("?").substringAfterLast(".").let {
            if (it.length in 2..4) it else "mp4"
        }
        val destDir = File("$destFolder/${sanitizeName(animeTitle)}/${sanitizeName(sourceName)}")
        destDir.mkdirs()

        val destFile = File(destDir, "${sanitizeName(episodeTitle)}.$ext")
        val tempFile = File(destDir, "${sanitizeName(episodeTitle)}.$ext.tmp")

        try {
            if (isHls) {
                downloadHls(streamUrl, tempFile, downloadId, episodeTitle)
            } else {
                downloadDirect(streamUrl, tempFile, downloadId, episodeTitle)
            }

            if (isStopped) {
                tempFile.delete()
                downloadRepo.updateStatus(downloadId, DownloadStatus.CANCELLED)
                return Result.success()
            }

            tempFile.renameTo(destFile)
            downloadRepo.setLocalFilePath(downloadId, destFile.absolutePath)
            downloadRepo.updateStatus(downloadId, DownloadStatus.COMPLETED)
            setForeground(createForegroundInfo(episodeTitle, 100))
            return Result.success()
        } catch (e: Exception) {
            tempFile.delete()
            downloadRepo.updateStatus(downloadId, DownloadStatus.FAILED, e.message)
            return Result.failure(workDataOf("error" to e.message))
        }
    }

    private suspend fun downloadDirect(
        url: String,
        destFile: File,
        downloadId: Long,
        title: String,
    ) {
        val request = Request.Builder().url(url).build()
        val response = downloadClient.newCall(request).execute()

        if (!response.isSuccessful) error("HTTP ${response.code} for $url")

        val body = response.body ?: error("Empty response body")
        val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
        var downloadedBytes = 0L
        var lastProgressUpdate = 0L

        FileOutputStream(destFile).use { out ->
            val buffer = ByteArray(8 * 1024)
            body.byteStream().use { input ->
                while (!isStopped) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    downloadedBytes += read

                    if (downloadedBytes - lastProgressUpdate > 256 * 1024) {
                        lastProgressUpdate = downloadedBytes
                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else 0
                        downloadRepo.updateProgress(downloadId, progress, downloadedBytes)
                        setForeground(createForegroundInfo(title, progress))
                    }
                }
            }
        }
        if (isStopped) {
            destFile.delete()
        }
    }

    private suspend fun downloadHls(
        m3u8Url: String,
        destFile: File,
        downloadId: Long,
        title: String,
    ) {
        val baseUrl = m3u8Url.substringBeforeLast("/")
        val playlistText = fetchText(m3u8Url)

        val segmentPlaylistUrl = if (playlistText.contains("#EXT-X-STREAM-INF")) {
            val streamLine = playlistText.lines()
                .dropWhile { !it.startsWith("#EXT-X-STREAM-INF") }
                .drop(1)
                .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                ?: error("Cannot parse HLS master playlist")
            resolveUrl(baseUrl, streamLine.trim())
        } else {
            m3u8Url
        }

        val segmentBaseUrl = segmentPlaylistUrl.substringBeforeLast("/")
        val segmentPlaylistText = if (segmentPlaylistUrl == m3u8Url) playlistText else fetchText(segmentPlaylistUrl)

        val segmentUrls = segmentPlaylistText.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { resolveUrl(segmentBaseUrl, it.trim()) }

        if (segmentUrls.isEmpty()) error("No segments found in HLS playlist")

        var downloaded = 0
        FileOutputStream(destFile).use { out ->
            for (segUrl in segmentUrls) {
                if (isStopped) break
                val request = Request.Builder().url(segUrl).build()
                val response = downloadClient.newCall(request).execute()
                if (!response.isSuccessful) continue
                response.body?.bytes()?.let { out.write(it) }

                downloaded++
                val progress = (downloaded * 100) / segmentUrls.size
                downloadRepo.updateProgress(downloadId, progress, downloaded.toLong() * 512 * 1024)
                setForeground(createForegroundInfo(title, progress))
            }
        }
        if (isStopped) destFile.delete()
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder().url(url).build()
        return downloadClient.newCall(request).execute().body?.string() ?: error("Empty playlist at $url")
    }

    private fun resolveUrl(base: String, url: String): String = when {
        url.startsWith("http") -> url
        url.startsWith("//") -> "https:$url"
        else -> "$base/$url"
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading: $title")
            .setProgress(100, progress, progress == 0)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

}
