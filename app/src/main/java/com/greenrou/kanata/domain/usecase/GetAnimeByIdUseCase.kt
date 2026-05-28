package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.AniListRepository

class GetAnimeByIdUseCase(
    private val repository: AniListRepository,
) {
    suspend operator fun invoke(id: Int, mediaType: String = "ANIME"): Result<Anime> = repository.getAnimeById(id, mediaType)
}
