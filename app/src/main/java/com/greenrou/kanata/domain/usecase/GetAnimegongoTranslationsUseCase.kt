package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.data.parsers.AnimegongoSiteParser
import com.greenrou.kanata.domain.model.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAnimegongoTranslationsUseCase(private val registry: ParserRegistry) {

    private val parser: AnimegongoSiteParser?
        get() = registry.parsers.value.filterIsInstance<AnimegongoSiteParser>().firstOrNull()

    suspend operator fun invoke(episodePageUrl: String): Result<List<Translation>> =
        withContext(Dispatchers.IO) {
            parser?.getTranslations(episodePageUrl)
                ?: Result.failure(IllegalStateException("AnimeGO parser not available"))
        }
}
