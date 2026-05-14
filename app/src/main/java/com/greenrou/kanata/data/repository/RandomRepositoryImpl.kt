package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.remote.NekosiaApi
import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.AniListRepository
import com.greenrou.kanata.domain.repository.RandomRepository
import kotlin.random.Random

class RandomRepositoryImpl(
    private val aniListRepository: AniListRepository,
    private val nekosiaApi: NekosiaApi
) : RandomRepository {

    override suspend fun getRandomAnime(): Result<Anime> = runCatching {
        val randomPage = Random.nextInt(1, 51) // Random page from first 50
        val result = aniListRepository.getAnimeList(page = randomPage, perPage = 20)
        val list = result.getOrThrow().items
        if (list.isEmpty()) error("No anime found")
        list.random()
    }

    override suspend fun getRandomImage(): Result<String> = runCatching {
        val response = nekosiaApi.getRandomImage()
        if (response.success) {
            response.image.original.url
        } else {
            error("Failed to fetch image from Nekosia")
        }
    }
}
