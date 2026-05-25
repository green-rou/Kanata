package com.greenrou.kanata.data.remote

import com.greenrou.kanata.data.remote.dto.ModIndexDto
import retrofit2.http.GET
import retrofit2.http.Url

interface ModIndexApi {
    // Full URL is passed at runtime so debug/release can point to different branches.
    @GET
    suspend fun getModIndex(@Url url: String): List<ModIndexDto>

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/"
    }
}
