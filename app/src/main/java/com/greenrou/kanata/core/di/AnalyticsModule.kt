package com.greenrou.kanata.core.di

import com.greenrou.kanata.core.analytics.AnalyticsManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val analyticsModule = module {
    single { AnalyticsManager(androidContext()) }
}
