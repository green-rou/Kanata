package com.greenrou.kanata.modapi

interface ModSiteParser {
    val id: String
    val label: String
    val language: String
    val isAdultOnly: Boolean get() = false
    fun supports(host: String): Boolean
    suspend fun search(query: String): Result<String>
    suspend fun searchWithResults(query: String): Result<List<ModSearchResult>> =
        search(query).mapCatching { url -> listOf(ModSearchResult(query, url)) }
    suspend fun getEpisodes(pageUrl: String): List<ModEpisode>
    suspend fun getTranslations(episodePageUrl: String): List<ModTranslation> = emptyList()
}
