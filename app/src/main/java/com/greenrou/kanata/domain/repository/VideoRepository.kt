package com.greenrou.kanata.domain.repository

interface VideoRepository {
    suspend fun getVideoStream(siteUrl: String): Result<String>
}
