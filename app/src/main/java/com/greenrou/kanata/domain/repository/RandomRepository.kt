package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.Anime

interface RandomRepository {
    suspend fun getRandomAnime(): Result<Anime>
    suspend fun getRandomImage(): Result<String>
}
