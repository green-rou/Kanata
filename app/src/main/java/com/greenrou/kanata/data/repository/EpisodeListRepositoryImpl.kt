package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.repository.EpisodeListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class EpisodeListRepositoryImpl(
    private val parserRegistry: ParserRegistry,
) : EpisodeListRepository {

    override suspend fun getEpisodes(animePageUrl: String, expectedEpisodes: Int): Result<List<Episode>> = withContext(Dispatchers.IO) {
        runCatching {
            val host = URL(animePageUrl).host
            val parser = parserRegistry.parsers.value.find { it.supports(host) }
                ?: error("Unsupported site: $host")
            val episodes = parser.getEpisodes(animePageUrl, expectedEpisodes)
            episodes
        }.onFailure { e ->
        }
    }
}
