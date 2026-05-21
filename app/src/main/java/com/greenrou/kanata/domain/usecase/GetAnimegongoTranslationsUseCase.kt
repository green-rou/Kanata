package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.data.parsers.AnimegongoSiteParser
import com.greenrou.kanata.domain.model.Translation
import com.greenrou.kanata.domain.parser.SiteParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAnimegongoTranslationsUseCase(private val parsers: List<SiteParser>) {

    private val parser: AnimegongoSiteParser?
        get() = parsers.filterIsInstance<AnimegongoSiteParser>().firstOrNull()

    suspend operator fun invoke(episodePageUrl: String): Result<List<Translation>> =
        withContext(Dispatchers.IO) {
            parser?.getTranslations(episodePageUrl)
                ?: Result.failure(IllegalStateException("AnimeGO parser not available"))
        }
}
