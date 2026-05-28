package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.ContentSearchRepository

class SearchContentSourcesUseCase(private val repository: ContentSearchRepository) {
    suspend operator fun invoke(titles: List<String>) = repository.searchAll(titles)
}
