package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.data.local.InstalledModEntity
import com.greenrou.kanata.domain.repository.ModRepository
import kotlinx.coroutines.flow.Flow

class GetInstalledModsUseCase(private val repo: ModRepository) {
    operator fun invoke(): Flow<List<InstalledModEntity>> = repo.observeInstalled()
}
