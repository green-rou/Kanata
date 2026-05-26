package com.kanata.mod.example

import com.greenrou.kanata.modapi.ModEpisode
import com.greenrou.kanata.modapi.ModSiteParser

class ModEntry : ModSiteParser {

    override val id = "source-example"
    override val label = "Example Source"
    override val language = "uk"

    override fun supports(host: String) = host.contains("example.com")

    override suspend fun search(query: String): Result<String> {
        return Result.failure(NotImplementedError("search not implemented"))
    }

    override suspend fun getEpisodes(pageUrl: String): List<ModEpisode> {
        return emptyList()
    }
}
