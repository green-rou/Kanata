package com.greenrou.kanata.data.remote

import com.greenrou.kanata.data.remote.dto.GitHubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Headers

interface GitHubApi {
    @Headers(
        "Accept: application/vnd.github.v3+json",
        "User-Agent: Kanata-Android-App",
    )
    @GET("repos/green-rou/Kanata/releases/latest")
    suspend fun getLatestRelease(): GitHubReleaseDto

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}
