package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.domain.model.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAnimegongoTranslationsUseCase(private val registry: ParserRegistry) {

    suspend operator fun invoke(episodePageUrl: String): Result<List<Translation>> =
        withContext(Dispatchers.IO) {
            val parser = registry.parsers.value.firstOrNull { it.supports("animego.ngo") }
                ?: return@withContext Result.failure(IllegalStateException("AnimeGO parser not available"))
            val translations = parser.getTranslations(episodePageUrl)
            if (translations.isEmpty()) Result.failure(IllegalStateException("No translations found"))
            else Result.success(translations)
        }
}
