package com.greenrou.kanata.domain.usecase

import android.content.Context
import com.greenrou.kanata.domain.model.AppRelease
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.first

class CheckUpdateUseCase(
    private val repo: UpdateRepository,
    private val settings: SettingsManager,
    private val context: Context,
) {
    suspend operator fun invoke(): Result<AppRelease?> =
        repo.fetchLatestRelease().map { release ->
            val current = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: return@map null
            val skipped = settings.skippedVersion.first()
            if (isNewer(release.version, current) && release.version != skipped) release
            else null
        }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }
}
