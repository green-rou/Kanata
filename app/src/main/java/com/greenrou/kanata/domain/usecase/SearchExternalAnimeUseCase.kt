package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.VideoSource
import com.greenrou.kanata.domain.repository.SearchRepository

class SearchExternalAnimeUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(query: String): List<VideoSource> {
        val sources = mutableListOf<VideoSource>()
        repository.searchOnYummy(query).onSuccess { sources.add(VideoSource("YummyAnime", it)) }
        repository.searchOnAniwave(query).onSuccess { sources.add(VideoSource("Aniwave", it)) }
        return sources
    }
}
