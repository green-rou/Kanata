package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.VideoSource
import com.greenrou.kanata.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow

class SearchExternalAnimeUseCase(private val repository: SearchRepository) {
    operator fun invoke(titles: List<String>): Flow<VideoSource> = repository.searchAll(titles)
}
