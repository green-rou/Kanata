package com.greenrou.kanata.data.repository

import android.util.Log
import com.greenrou.kanata.data.local.InstalledModDao
import com.greenrou.kanata.data.local.InstalledModEntity
import com.greenrou.kanata.data.remote.ModIndexApi
import com.greenrou.kanata.data.remote.dto.ModIndexDto
import com.greenrou.kanata.domain.repository.ModRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class ModRepositoryImpl(
    private val dao: InstalledModDao,
    private val api: ModIndexApi,
    private val okHttpClient: OkHttpClient,
    private val modsDir: File,
    private val modIndexUrl: String,
) : ModRepository {

    override fun observeInstalled(): Flow<List<InstalledModEntity>> = dao.observeAll()

    override suspend fun fetchRemoteIndex(): Result<List<ModIndexDto>> =
        runCatching {
            Log.d(TAG, "Fetching mod index from: $modIndexUrl")
            val result = api.getModIndex(modIndexUrl)
            Log.d(TAG, "Index loaded: ${result.size} mods — URLs: ${result.map { it.apkUrl }}")
            result
        }.onFailure { Log.e(TAG, "fetchRemoteIndex failed", it) }

    override suspend fun install(mod: ModIndexDto, onProgress: (Int) -> Unit): Result<Unit> =
        runCatching {
            Log.d(TAG, "install: starting ${mod.id} from ${mod.apkUrl}")
            withContext(Dispatchers.IO) {
                val fileName = "${mod.id}__${mod.parserClass}.apk"
                val dest = File(modsDir, fileName)
                downloadFile(mod.apkUrl, dest, onProgress)
                Log.d(TAG, "install: saved to DB as $fileName")
                dao.insert(
                    InstalledModEntity(
                        id = mod.id,
                        label = mod.label,
                        language = mod.language,
                        version = mod.version,
                        apkFileName = fileName,
                    )
                )
            }
        }.onFailure { Log.e(TAG, "install failed for ${mod.id}", it) }

    override suspend fun uninstall(modId: String): Result<Unit> =
        runCatching {
            val entity = dao.findById(modId) ?: return@runCatching
            File(modsDir, entity.apkFileName).delete()
            dao.deleteById(modId)
        }

    override suspend fun setEnabled(modId: String, enabled: Boolean) =
        dao.setEnabled(modId, enabled)

    private fun downloadFile(url: String, dest: File, onProgress: (Int) -> Unit) {
        Log.d(TAG, "Downloading mod APK from: $url")
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            Log.d(TAG, "Response: ${response.code} ${response.message} — url=${response.request.url}")
            check(response.isSuccessful) { "HTTP ${response.code}: ${response.message}" }
            val body = response.body ?: error("Empty response body")
            val total = body.contentLength()
            body.byteStream().use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress((downloaded * 100 / total).toInt())
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ModRepository"
    }
}
