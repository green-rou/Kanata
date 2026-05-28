package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.data.remote.dto.ModIndexDto
import com.greenrou.kanata.domain.repository.ModRepository

class FetchRemoteModsUseCase(private val repo: ModRepository) {
    suspend operator fun invoke(): Result<List<ModIndexDto>> = repo.fetchRemoteIndex()
}
