package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.ModRepository

class ToggleModUseCase(private val repo: ModRepository) {
    suspend operator fun invoke(modId: String, enabled: Boolean) =
        repo.setEnabled(modId, enabled)
}
