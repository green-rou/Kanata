package com.greenrou.kanata.core.di

import com.greenrou.kanata.features.details.AnimeDetailsViewModel
import com.greenrou.kanata.features.downloads.DownloadManagerViewModel
import com.greenrou.kanata.features.episodes.EpisodeListViewModel
import com.greenrou.kanata.features.favorites.FavoritesViewModel
import com.greenrou.kanata.features.main.MainViewModel
import com.greenrou.kanata.features.mood.MoodViewModel
import com.greenrou.kanata.features.player.PlayerViewModel
import com.greenrou.kanata.features.random.RandomImageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AnimeDetailsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { FavoritesViewModel(get(), get()) }
    viewModel { MoodViewModel(get(), get()) }
    viewModel { RandomImageViewModel(get(), get(), get(), get(), get()) }
    viewModel { params ->
        EpisodeListViewModel(get(), get(), get(), get(), params.get(), params.get(), params.get(), params.get())
    }
    viewModel { params ->
        PlayerViewModel(get(), get(), get(), params.get(), params.get(), params.get(), params.get(), params.get())
    }
    viewModel { DownloadManagerViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}
