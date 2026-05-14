package com.greenrou.kanata.navigation

import kotlinx.serialization.Serializable

@Serializable
data object MainRoute

@Serializable
data class AnimeDetailsRoute(val animeId: Int)

@Serializable
data class EpisodeListRoute(val animePageUrl: String, val label: String)

@Serializable
data class PlayerRoute(
    val episodeUrls: List<String>,
    val episodeTitles: List<String>,
    val startIndex: Int,
)
