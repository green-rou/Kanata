package com.greenrou.kanata.data.mod

import com.greenrou.kanata.domain.model.AnimeEnrichment
import com.greenrou.kanata.domain.parser.InfoProvider
import com.greenrou.kanata.modapi.ModInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModInfoProviderAdapter(private val mod: ModInfoProvider) : InfoProvider {
    override val id: String = mod.id
    override val label: String = mod.label

    override suspend fun getInfo(titles: List<String>): Result<AnimeEnrichment> =
        withContext(Dispatchers.IO) {
            runCatching {
                val info = mod.getInfo(titles).getOrThrow()
                AnimeEnrichment(
                    synopsis = info.synopsis,
                    score = info.score,
                    scoreLabel = info.scoreLabel,
                    genres = info.genres,
                    studios = info.studios,
                )
            }
        }
}
