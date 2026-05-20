package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.VideoStream

interface VideoRepository {
    suspend fun getVideoStream(siteUrl: String): Result<VideoStream>
}
