package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.data.local.InstalledModEntity
import com.greenrou.kanata.data.remote.dto.ModIndexDto
import kotlinx.coroutines.flow.Flow

interface ModRepository {
    fun observeInstalled(): Flow<List<InstalledModEntity>>
    suspend fun fetchRemoteIndex(): Result<List<ModIndexDto>>
    suspend fun install(mod: ModIndexDto, onProgress: (Int) -> Unit): Result<Unit>
    suspend fun uninstall(modId: String): Result<Unit>
    suspend fun setEnabled(modId: String, enabled: Boolean)
}
