package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.ModRepository

class UninstallModUseCase(private val repo: ModRepository) {
    suspend operator fun invoke(modId: String): Result<Unit> = repo.uninstall(modId)
}
