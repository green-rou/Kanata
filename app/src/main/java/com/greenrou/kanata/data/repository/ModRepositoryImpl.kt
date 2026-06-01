package com.greenrou.kanata.data.repository

import android.system.Os
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
) : ModRepository {

    override fun observeInstalled(): Flow<List<InstalledModEntity>> = dao.observeAll()

    override suspend fun fetchRemoteIndex(url: String): Result<List<ModIndexDto>> =
        runCatching { api.getModIndex(url) }

    override suspend fun install(mod: ModIndexDto, onProgress: (Int) -> Unit): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                val fileName = "${mod.id}__${mod.parserClass}.apk"
                val dest = File(modsDir, fileName)
                downloadFile(mod.apkUrl, dest, onProgress)
                makeReadOnly(dest)
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
        }

    override suspend fun uninstall(modId: String): Result<Unit> =
        runCatching {
            val entity = dao.findById(modId) ?: return@runCatching
            File(modsDir, entity.apkFileName).delete()
            dao.deleteById(modId)
        }

    override suspend fun setEnabled(modId: String, enabled: Boolean) =
        dao.setEnabled(modId, enabled)

    private fun makeReadOnly(file: File) {
        try {
            Os.chmod(file.absolutePath, 0b100_100_100)
        } catch (_: Exception) {
            file.setReadOnly()
        }
    }

    private fun downloadFile(url: String, dest: File, onProgress: (Int) -> Unit) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
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

}
