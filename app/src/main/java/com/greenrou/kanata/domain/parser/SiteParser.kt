package com.greenrou.kanata.domain.parser

import com.greenrou.kanata.domain.model.Episode

interface SiteParser {
    val label: String
    val isAdultOnly: Boolean get() = false
    fun supports(host: String): Boolean
    suspend fun search(query: String): Result<String>
    suspend fun getEpisodes(pageUrl: String): List<Episode>
}
