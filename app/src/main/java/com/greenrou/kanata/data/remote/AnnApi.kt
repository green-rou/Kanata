package com.greenrou.kanata.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface AnnApi {

    @GET("reports.xml")
    suspend fun getAnimeList(
        @Query("id") reportId: Int = 155,
        @Query("type") type: String = "anime",
        @Query("nlist") nlist: Int = 50,
    ): String

    @GET("nodelay.api.xml")
    suspend fun getAnimeDetail(
        @Query("anime") animeId: Int,
    ): String

    @GET("nodelay.api.xml")
    suspend fun getAnimeDetails(
        @Query("anime") animeIds: String,
    ): String
}
