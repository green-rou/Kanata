package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.Anime

interface AnimeRepository {
    suspend fun getAnimeList(): Result<List<Anime>>
    suspend fun getAnimeById(id: Int): Result<Anime>
}
