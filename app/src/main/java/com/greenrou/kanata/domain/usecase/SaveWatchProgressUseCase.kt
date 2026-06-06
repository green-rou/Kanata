package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.WatchProgress
import com.greenrou.kanata.domain.repository.WatchProgressRepository

class SaveWatchProgressUseCase(private val repo: WatchProgressRepository) {
    suspend operator fun invoke(
        episodeUrl: String,
        playbackUrl: String,
        episodeTitle: String,
        animeTitle: String,
        isManga: Boolean,
        positionMs: Long,
        durationMs: Long,
    ) {
        if (positionMs <= 0) return
        repo.save(
            WatchProgress(
                episodeUrl = episodeUrl,
                playbackUrl = playbackUrl,
                episodeTitle = episodeTitle,
                animeTitle = animeTitle,
                isManga = isManga,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }
}
