package com.greenrou.kanata.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.greenrou.kanata.data.local.InstalledModDao
import com.greenrou.kanata.data.local.InstalledModEntity
import com.greenrou.kanata.data.mod.ModLoader
import com.greenrou.kanata.modapi.ModInfoProvider
import com.greenrou.kanata.modapi.ModSiteParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class InstallModFromFileUseCase(
    private val context: Context,
    private val modLoader: ModLoader,
    private val dao: InstalledModDao,
) {
    suspend operator fun invoke(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = resolveFileName(uri)
                ?: error("Cannot determine file name")
            check(fileName.endsWith(".apk")) { "Selected file is not an APK" }
            check("__" in fileName) { "APK name must follow the format: id__com.package.ClassName.apk" }

            val dest = File(modLoader.modsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: error("Cannot open selected file")

            val instance = modLoader.tryInstantiate(dest)
                ?: run { dest.delete(); error("Failed to load mod from $fileName") }

            val entity = when (instance) {
                is ModSiteParser -> InstalledModEntity(
                    id = instance.id,
                    label = instance.label,
                    language = instance.language,
                    version = 0,
                    apkFileName = fileName,
                )
                is ModInfoProvider -> InstalledModEntity(
                    id = instance.id,
                    label = instance.label,
                    language = "*",
                    version = 0,
                    apkFileName = fileName,
                )
                else -> { dest.delete(); error("File is not a recognized mod type") }
            }

            val existing = dao.findById(entity.id)
            dao.insert(entity.copy(isEnabled = existing?.isEnabled ?: true))
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        if (uri.scheme == "file") return File(uri.path ?: return null).name
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
        }
    }
}
