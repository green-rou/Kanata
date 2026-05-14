package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.AnimeListPage
import com.greenrou.kanata.domain.repository.AniListRepository

class GetAnimeByMoodUseCase(
    private val repository: AniListRepository,
) {
    suspend operator fun invoke(
        page: Int = 1,
        perPage: Int = 20,
        genres: List<String>? = null,
        tags: List<String>? = null,
        showAdultContent: Boolean = false
    ): Result<AnimeListPage> =
        repository.getAnimeByMood(page, perPage, genres, tags, showAdultContent)
}
