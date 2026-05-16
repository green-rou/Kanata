package com.greenrou.kanata.features.player.model

data class PlayerState(
    val isLoading: Boolean = true,
    val streamUrl: String? = null,
    val title: String = "",
    val error: String? = null,
    val currentIndex: Int = 0,
    val episodeCount: Int = 1,
    val nextEpisodeTitle: String? = null,
)
