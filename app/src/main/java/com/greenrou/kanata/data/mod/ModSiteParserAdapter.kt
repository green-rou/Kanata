package com.greenrou.kanata.data.mod

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.OnlineSearchResult
import com.greenrou.kanata.domain.model.Translation
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.modapi.ModSiteParser

class ModSiteParserAdapter(private val mod: ModSiteParser) : SiteParser {
    override val label: String = mod.label
    override val isAdultOnly: Boolean = mod.isAdultOnly

    override fun supports(host: String) = mod.supports(host)
    override suspend fun search(query: String): Result<String> =
        runCatching { mod.search(query).getOrThrow() }

    override suspend fun searchWithResults(query: String): Result<List<OnlineSearchResult>> =
        runCatching {
            mod.searchWithResults(query).getOrThrow().map {
                OnlineSearchResult(label, it.title, it.pageUrl, it.coverUrl)
            }
        }
    override suspend fun getEpisodes(pageUrl: String) =
        mod.getEpisodes(pageUrl).map { Episode(it.title, it.url) }
    override suspend fun getTranslations(episodePageUrl: String) =
        mod.getTranslations(episodePageUrl)
            .map { Translation(it.id, it.title, it.type, it.mediaId, it.mediaHash, it.mediaType) }
}
