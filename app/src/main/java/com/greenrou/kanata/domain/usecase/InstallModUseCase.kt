package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.data.remote.dto.ModIndexDto
import com.greenrou.kanata.domain.repository.ModRepository

class InstallModUseCase(private val repo: ModRepository) {
    suspend operator fun invoke(mod: ModIndexDto, onProgress: (Int) -> Unit): Result<Unit> =
        repo.install(mod, onProgress)
}
