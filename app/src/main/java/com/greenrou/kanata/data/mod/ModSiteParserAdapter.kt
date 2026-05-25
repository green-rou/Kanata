package com.greenrou.kanata.data.mod

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.modapi.ModSiteParser

class ModSiteParserAdapter(private val mod: ModSiteParser) : SiteParser {
    override val label: String = mod.label
    override val isAdultOnly: Boolean = mod.isAdultOnly

    override fun supports(host: String) = mod.supports(host)
    override suspend fun search(query: String) = mod.search(query)
    override suspend fun getEpisodes(pageUrl: String) =
        mod.getEpisodes(pageUrl).map { Episode(it.title, it.url) }
}
