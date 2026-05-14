package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.AnimeListPage
import com.greenrou.kanata.domain.repository.AniListRepository

class GetAnimeListUseCase(
    private val repository: AniListRepository,
) {
    suspend operator fun invoke(page: Int = 1, perPage: Int = 20, showAdultContent: Boolean = false): Result<AnimeListPage> =
        repository.getAnimeList(page, perPage, showAdultContent)
}
