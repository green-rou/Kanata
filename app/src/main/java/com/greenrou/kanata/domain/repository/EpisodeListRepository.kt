package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.Episode

interface EpisodeListRepository {
    suspend fun getEpisodes(animePageUrl: String, expectedEpisodes: Int = 0): Result<List<Episode>>
}
