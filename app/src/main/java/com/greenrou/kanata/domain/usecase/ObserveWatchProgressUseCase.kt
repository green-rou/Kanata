package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.WatchProgress
import com.greenrou.kanata.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveWatchProgressUseCase(private val repo: WatchProgressRepository) {
    operator fun invoke(urls: List<String>): Flow<Map<String, WatchProgress>> =
        repo.observeByUrls(urls).map { list -> list.associateBy { it.episodeUrl } }
}
