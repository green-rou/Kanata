package com.greenrou.kanata.core.di

import com.greenrou.kanata.BuildConfig
import com.greenrou.kanata.data.mod.InfoProviderRegistry
import com.greenrou.kanata.data.mod.ModLoader
import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.data.repository.AnimeRepositoryImpl
import com.greenrou.kanata.data.repository.DownloadRepositoryImpl
import com.greenrou.kanata.data.repository.EpisodeListRepositoryImpl
import com.greenrou.kanata.data.repository.ModRepositoryImpl
import com.greenrou.kanata.data.repository.RandomRepositoryImpl
import com.greenrou.kanata.data.repository.SearchRepositoryImpl
import com.greenrou.kanata.data.repository.SettingsManagerImpl
import com.greenrou.kanata.data.repository.UpdateRepositoryImpl
import com.greenrou.kanata.data.repository.VideoRepositoryImpl
import com.greenrou.kanata.domain.repository.AnimeRepository
import com.greenrou.kanata.domain.repository.DownloadRepository
import com.greenrou.kanata.domain.repository.EpisodeListRepository
import com.greenrou.kanata.domain.repository.ModRepository
import com.greenrou.kanata.domain.repository.RandomRepository
import com.greenrou.kanata.domain.repository.SearchRepository
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.repository.UpdateRepository
import com.greenrou.kanata.domain.repository.VideoRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { ModLoader(get()) }

    single {
        ParserRegistry(emptyList(), get(), get())
    }
    single { InfoProviderRegistry(get(), get()) }
    single<ModRepository> {
        ModRepositoryImpl(
            dao = get(),
            api = get(),
            okHttpClient = get(),
            modsDir = get<ModLoader>().modsDir,
            modIndexUrl = BuildConfig.MOD_INDEX_URL,
        )
    }
    single<AnimeRepository> { AnimeRepositoryImpl(get()) }
    single<SettingsManager> { SettingsManagerImpl(get()) }
    single<RandomRepository> { RandomRepositoryImpl(get(), get()) }
    single<VideoRepository> { VideoRepositoryImpl(get()) }
    single<SearchRepository> { SearchRepositoryImpl(get(), get()) }
    single<EpisodeListRepository> { EpisodeListRepositoryImpl(get()) }
    single<DownloadRepository> { DownloadRepositoryImpl(get(), get(), get()) }
    single<UpdateRepository> { UpdateRepositoryImpl(get()) }
}
