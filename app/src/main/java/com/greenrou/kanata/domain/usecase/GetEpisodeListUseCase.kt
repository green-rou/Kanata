package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.EpisodeListRepository

class GetEpisodeListUseCase(private val repository: EpisodeListRepository) {
    suspend operator fun invoke(animePageUrl: String, expectedEpisodes: Int = 0) = repository.getEpisodes(animePageUrl, expectedEpisodes)
}
