package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.remote.GitHubApi
import com.greenrou.kanata.domain.model.AppRelease
import com.greenrou.kanata.domain.repository.UpdateRepository

class UpdateRepositoryImpl(private val api: GitHubApi) : UpdateRepository {

    override suspend fun fetchLatestRelease(): Result<AppRelease> = runCatching {
        val dto = api.getLatestRelease()
        val apkAsset = dto.assets.firstOrNull { it.name.endsWith(".apk") }
            ?: error("No APK asset found in release ${dto.tagName}")
        AppRelease(
            version = dto.tagName.removePrefix("v"),
            title = dto.name,
            body = dto.body,
            apkUrl = apkAsset.browserDownloadUrl,
            pageUrl = dto.htmlUrl,
        )
    }
}
