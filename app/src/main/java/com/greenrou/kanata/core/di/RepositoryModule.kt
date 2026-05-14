package com.greenrou.kanata.core.di

import com.greenrou.kanata.data.repository.AnimeRepositoryImpl
import com.greenrou.kanata.data.repository.EpisodeListRepositoryImpl
import com.greenrou.kanata.data.repository.RandomRepositoryImpl
import com.greenrou.kanata.data.repository.SearchRepositoryImpl
import com.greenrou.kanata.data.repository.SettingsManagerImpl
import com.greenrou.kanata.data.repository.VideoRepositoryImpl
import com.greenrou.kanata.domain.repository.AnimeRepository
import com.greenrou.kanata.domain.repository.EpisodeListRepository
import com.greenrou.kanata.domain.repository.RandomRepository
import com.greenrou.kanata.domain.repository.SearchRepository
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.repository.VideoRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<AnimeRepository> { AnimeRepositoryImpl(get()) }
    single<SettingsManager> { SettingsManagerImpl(get()) }
    single<RandomRepository> { RandomRepositoryImpl(get(), get()) }
    single<VideoRepository> { VideoRepositoryImpl(get()) }
    single<SearchRepository> { SearchRepositoryImpl() }
    single<EpisodeListRepository> { EpisodeListRepositoryImpl() }
}
