package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.DownloadRepository
import java.io.File

class DeleteCompletedDownloadUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(id: Long, localFilePath: String?) {
        localFilePath?.let { File(it).delete() }
        repo.deleteDownload(id)
    }
}
