package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.DownloadRepository

class GetDownloadFolderUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(): String = repo.getDownloadFolder()
}
