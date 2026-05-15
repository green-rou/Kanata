package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.VideoSource
import com.greenrou.kanata.domain.repository.SearchRepository

class SearchExternalAnimeUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(query: String): List<VideoSource> = repository.searchAll(query)
}
