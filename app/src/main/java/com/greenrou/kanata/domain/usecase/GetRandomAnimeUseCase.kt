package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.RandomRepository

class GetRandomAnimeUseCase(private val repository: RandomRepository) {
    suspend operator fun invoke(): Result<Anime> = repository.getRandomAnime()
}
