package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.VideoSource

interface SearchRepository {
    suspend fun searchAll(titles: List<String>): List<VideoSource>
}
