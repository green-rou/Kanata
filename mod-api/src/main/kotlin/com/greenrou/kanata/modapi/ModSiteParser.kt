package com.greenrou.kanata.modapi

interface ModSiteParser {
    val id: String
    val label: String
    val language: String
    val isAdultOnly: Boolean get() = false
    fun supports(host: String): Boolean
    suspend fun search(query: String): Result<String>
    suspend fun getEpisodes(pageUrl: String): List<ModEpisode>
}
