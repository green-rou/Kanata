package com.kanata.mod.example

import com.greenrou.kanata.modapi.ModEpisode
import com.greenrou.kanata.modapi.ModSiteParser

/**
 * Entry point for this mod. The class must always be named `ModEntry`.
 *
 * Distribute the compiled APK as:
 *   source-example__com.kanata.mod.example.ModEntry.apk
 *
 * Format: <mod-id>__<fully.qualified.ClassName>.apk
 * The host app uses the file name to find and instantiate this class via DexClassLoader.
 */
class ModEntry : ModSiteParser {

    override val id = "source-example"
    override val label = "Example Source"
    override val language = "uk"

    override fun supports(host: String) = host.contains("example.com")

    override suspend fun search(query: String): Result<String> {
        // TODO: search the site and return the URL of the anime page.
        return Result.failure(NotImplementedError("search not implemented"))
    }

    override suspend fun getEpisodes(pageUrl: String): List<ModEpisode> {
        // TODO: parse the page and return episode list.
        return emptyList()
    }
}
