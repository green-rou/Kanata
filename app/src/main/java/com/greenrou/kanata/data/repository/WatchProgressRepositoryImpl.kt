package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.local.WatchProgressDao
import com.greenrou.kanata.data.local.WatchProgressEntity
import com.greenrou.kanata.data.local.toDomain
import com.greenrou.kanata.domain.model.WatchProgress
import com.greenrou.kanata.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchProgressRepositoryImpl(private val dao: WatchProgressDao) : WatchProgressRepository {

    override suspend fun save(progress: WatchProgress) {
        dao.upsert(
            WatchProgressEntity(
                episodeUrl = progress.episodeUrl,
                playbackUrl = progress.playbackUrl,
                episodeTitle = progress.episodeTitle,
                animeTitle = progress.animeTitle,
                isManga = progress.isManga,
                positionMs = progress.positionMs,
                durationMs = progress.durationMs,
                updatedAt = progress.updatedAt,
            )
        )
    }

    override suspend fun getByUrl(url: String): WatchProgress? =
        dao.getByUrl(url)?.toDomain()

    override fun observeByUrls(urls: List<String>): Flow<List<WatchProgress>> {
        if (urls.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return dao.observeByUrls(urls).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getLastWatched(): WatchProgress? =
        dao.getLastWatched()?.toDomain()
}
