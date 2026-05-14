package com.greenrou.kanata

import android.app.Application
import com.greenrou.kanata.core.di.aniListModule
import com.greenrou.kanata.core.di.databaseModule
import com.greenrou.kanata.core.di.repositoryModule
import com.greenrou.kanata.core.di.useCaseModule
import com.greenrou.kanata.core.di.viewModelModule
import com.greenrou.kanata.core.network.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KanataApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@KanataApp)
            modules(networkModule, databaseModule, repositoryModule, useCaseModule, viewModelModule, aniListModule)
        }
    }
}
