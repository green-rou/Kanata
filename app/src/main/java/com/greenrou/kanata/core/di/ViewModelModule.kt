package com.greenrou.kanata.core.di

import com.greenrou.kanata.data.mod.ChapterParserRegistry
import com.greenrou.kanata.data.mod.DownloadFeatureRegistry
import com.greenrou.kanata.data.mod.InfoProviderRegistry
import com.greenrou.kanata.data.mod.MangaModRegistry
import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.domain.usecase.CheckUpdateUseCase
import com.greenrou.kanata.features.chapters.ChapterListViewModel
import com.greenrou.kanata.features.details.AnimeDetailsViewModel
import com.greenrou.kanata.features.downloads.DownloadManagerViewModel
import com.greenrou.kanata.features.episodes.EpisodeListViewModel
import com.greenrou.kanata.features.favorites.FavoritesViewModel
import com.greenrou.kanata.features.main.MainViewModel
import com.greenrou.kanata.features.mods.ModsViewModel
import com.greenrou.kanata.features.mood.MoodViewModel
import com.greenrou.kanata.features.onlinesearch.OnlineSearchViewModel
import com.greenrou.kanata.features.pagereader.PageReaderViewModel
import com.greenrou.kanata.features.player.PlayerViewModel
import com.greenrou.kanata.features.random.RandomImageViewModel
import com.greenrou.kanata.features.update.UpdateViewModel
import com.greenrou.kanata.features.webplayer.WebPlayerViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get<ParserRegistry>(), get<InfoProviderRegistry>(), get<DownloadFeatureRegistry>(), get<MangaModRegistry>(), get<ChapterParserRegistry>()) }
    viewModel { AnimeDetailsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get<ParserRegistry>(), get<MangaModRegistry>(), get()) }
    viewModel { FavoritesViewModel(get(), get(), get(), get()) }
    viewModel { MoodViewModel(get(), get(), get(), get<MangaModRegistry>()) }
    viewModel { RandomImageViewModel(get(), get(), get(), get(), get(), get(), get<MangaModRegistry>()) }
    viewModel { params ->
        EpisodeListViewModel(get(), get(), get(), get(), get(), get(), get<DownloadFeatureRegistry>(), params.get(), params.get(), params.get(), params.get(), params.get())
    }
    viewModel { params ->
        ChapterListViewModel(get(), get(), get(), get(), get<DownloadFeatureRegistry>(), params.get(), params.get(), params.get())
    }
    viewModel { params ->
        PageReaderViewModel(get(), params.get(), params.get(), params.get())
    }
    viewModel { params ->
        PlayerViewModel(get(), get(), get(), get(), get<DownloadFeatureRegistry>(), params.get(), params.get(), params.get(), params.get(), params.get(), params.get(), params.get())
    }
    viewModel { DownloadManagerViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { WebPlayerViewModel(get(), get()) }
    viewModel { ModsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { params -> OnlineSearchViewModel(get(), get(), get(), params.get()) }
    single { CheckUpdateUseCase(get(), get(), androidContext()) }
    viewModel { UpdateViewModel(get(), get(), androidApplication(), get()) }
}
