package com.greenrou.kanata.domain.repository

interface VideoRepository {
    /**
     * Extracts a playable video stream URL from a given website URL.
     * Uses scraping and optionally FFmpeg to resolve the direct link.
     */
    suspend fun getVideoStream(siteUrl: String): Result<String>
}
