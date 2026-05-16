package com.greenrou.kanata.domain.parser

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType

interface SiteParser {
    val label: String
    val sourceType: VideoSourceType get() = VideoSourceType.UNKNOWN
    val isAdultOnly: Boolean get() = false
    fun supports(host: String): Boolean
    suspend fun search(query: String): Result<String>
    suspend fun getEpisodes(pageUrl: String): List<Episode>
}
