package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.model.AnimeFilter
import com.greenrou.kanata.domain.model.AnimeListPage

interface AniListRepository {
    suspend fun getAnimeList(
        page: Int = 1,
        perPage: Int = 20,
        showAdultContent: Boolean = false,
        filter: AnimeFilter = AnimeFilter(),
    ): Result<AnimeListPage>
    suspend fun getAnimeById(id: Int): Result<Anime>
    suspend fun getAnimeByMood(
        page: Int = 1,
        perPage: Int = 20,
        genres: List<String>? = null,
        tags: List<String>? = null,
        showAdultContent: Boolean = false
    ): Result<AnimeListPage>
}
