package com.greenrou.kanata.core.di

import com.greenrou.kanata.domain.usecase.AddFavoriteUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeByIdUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeByMoodUseCase
import com.greenrou.kanata.domain.usecase.GetAnimeListUseCase
import com.greenrou.kanata.domain.usecase.GetEpisodeListUseCase
import com.greenrou.kanata.domain.usecase.GetFavoritesUseCase
import com.greenrou.kanata.domain.usecase.GetRandomAnimeUseCase
import com.greenrou.kanata.domain.usecase.GetRandomImageUseCase
import com.greenrou.kanata.domain.usecase.GetVideoStreamUseCase
import com.greenrou.kanata.domain.usecase.IsFavoriteUseCase
import com.greenrou.kanata.domain.usecase.RemoveFavoriteUseCase
import com.greenrou.kanata.domain.usecase.SearchExternalAnimeUseCase
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
}
