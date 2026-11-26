package com.fileshare.lan.core.data.di

import com.fileshare.lan.core.data.settings.DataStoreSettingsRepository
import com.fileshare.lan.core.data.settings.SettingsRepository
import com.fileshare.lan.core.data.storage.StorageProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    single { StorageProvider(androidContext()) }
}
