package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.ContentSource
import com.greenrou.kanata.domain.repository.ContentSearchRepository
import kotlinx.coroutines.flow.Flow

class SearchContentSourcesUseCase(private val repository: ContentSearchRepository) {
    operator fun invoke(titles: List<String>): Flow<ContentSource> = repository.searchAll(titles)
}
