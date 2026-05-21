package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.AppRelease

interface UpdateRepository {
    suspend fun fetchLatestRelease(): Result<AppRelease>
}
