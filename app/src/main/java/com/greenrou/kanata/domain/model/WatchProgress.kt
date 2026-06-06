package com.greenrou.kanata.domain.model

data class WatchProgress(
    val episodeUrl: String,
    val playbackUrl: String,
    val episodeTitle: String,
    val animeTitle: String,
    val isManga: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
) {
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = durationMs > 0 && fraction >= 0.9f
}
