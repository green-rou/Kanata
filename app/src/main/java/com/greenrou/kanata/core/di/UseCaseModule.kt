package com.greenrou.kanata.core.di

import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.CancelDownloadUseCase
import com.greenrou.kanata.domain.usecase.DeleteCompletedDownloadUseCase
import com.greenrou.kanata.domain.usecase.EnqueueDownloadUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeByIdUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeByMoodUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeListUseCase
import com.greenrou.kanata.domain.usecase.GetCompletedDownloadsUseCase
import com.greenrou.kanata.domain.usecase.GetDownloadFolderUseCase
import com.greenrou.kanata.domain.usecase.GetDownloadQueueUseCase
import com.greenrou.kanata.domain.usecase.GetEpisodeDownloadStatusUseCase
import com.greenrou.kanata.domain.usecase.GetEpisodeListUseCase
import com.greenrou.kanata.domain.usecase.GetFavoritesUseCase
import com.greenrou.kanata.domain.usecase.GetRandomAnimeUseCase
import com.greenrou.kanata.domain.usecase.GetRandomImageUseCase
import com.greenrou.kanata.domain.usecase.GetVideoStreamUseCase
import com.greenrou.kanata.domain.usecase.IsFavoriteUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.domain.usecase.ReorderDownloadQueueUseCase
import com.greenrou.kanata.domain.usecase.RetryDownloadUseCase
import com.greenrou.kanata.domain.usecase.SearchExternalAnimeUseCase
import com.greenrou.kanata.domain.usecase.SetDownloadFolderUseCase
import com.greenrou.kanata.domain.usecase.StartEpisodeDownloadUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { GetAnimeListUseCase(get()) }
    factory { GetAnimeByIdUseCase(get()) }
    factory { AddFavoriteUseCase(get()) }
    factory { RemoveFavoriteUseCase(get()) }
    factory { IsFavoriteUseCase(get()) }
    factory { GetFavoritesUseCase(get()) }
    factory { GetAnimeByMoodUseCase(get()) }
    factory { GetRandomAnimeUseCase(get()) }
    factory { GetRandomImageUseCase(get()) }
    factory { GetVideoStreamUseCase(get()) }
    factory { SearchExternalAnimeUseCase(get()) }
    factory { GetEpisodeListUseCase(get()) }
    factory { EnqueueDownloadUseCase(get()) }
    factory { CancelDownloadUseCase(get(), get()) }
    factory { GetDownloadQueueUseCase(get()) }
    factory { GetCompletedDownloadsUseCase(get()) }
    factory { GetEpisodeDownloadStatusUseCase(get()) }
    factory { DeleteCompletedDownloadUseCase(get()) }
    factory { ReorderDownloadQueueUseCase(get()) }
    factory { GetDownloadFolderUseCase(get()) }
    factory { SetDownloadFolderUseCase(get()) }
    factory { StartEpisodeDownloadUseCase(get(), get()) }
    factory { RetryDownloadUseCase(get(), get()) }
}
