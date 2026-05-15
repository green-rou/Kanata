package com.greenrou.kanata.data.repository

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.domain.repository.EpisodeListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class EpisodeListRepositoryImpl(
    private val parsers: List<SiteParser>
) : EpisodeListRepository {

    override suspend fun getEpisodes(animePageUrl: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        runCatching {
            val host = URL(animePageUrl).host
            val parser = parsers.find { it.supports(host) }
                ?: error("Unsupported site: $host")
            parser.getEpisodes(animePageUrl)
        }
    }
}
