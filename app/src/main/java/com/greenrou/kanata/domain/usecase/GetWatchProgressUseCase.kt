package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.WatchProgress
import com.greenrou.kanata.domain.repository.WatchProgressRepository

class GetWatchProgressUseCase(private val repo: WatchProgressRepository) {
    suspend operator fun invoke(url: String): WatchProgress? = repo.getByUrl(url)
}
