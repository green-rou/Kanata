package com.greenrou.kanata.data.remote

import com.greenrou.kanata.data.remote.dto.NekosiaResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NekosiaApi {
    @GET("images/{category}")
    suspend fun getRandomImage(
        @Path("category") category: String = "random",
        @Query("count") count: Int = 1,
        @Query("rating") rating: String = "safe"
    ): NekosiaResponse

    companion object {
        const val BASE_URL = "https://api.nekosia.cat/api/v1/"
    }
}
