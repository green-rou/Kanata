package com.greenrou.kanata.domain.parser

import com.greenrou.kanata.domain.model.AnimeEnrichment

interface InfoProvider {
    val id: String
    val label: String
    suspend fun getInfo(titles: List<String>): Result<AnimeEnrichment>
}
