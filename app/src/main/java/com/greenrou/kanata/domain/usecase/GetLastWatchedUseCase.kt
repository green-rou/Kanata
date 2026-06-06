package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.WatchProgress
import com.greenrou.kanata.domain.repository.WatchProgressRepository

class GetLastWatchedUseCase(private val repo: WatchProgressRepository) {
    suspend operator fun invoke(): WatchProgress? = repo.getLastWatched()
}
