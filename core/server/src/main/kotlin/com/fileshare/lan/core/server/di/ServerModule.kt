package com.fileshare.lan.core.server.di

import com.fileshare.lan.core.server.DeviceRegistry
import com.fileshare.lan.core.server.ServerEventBus
import com.fileshare.lan.core.server.ShareController
import com.fileshare.lan.core.server.UploadMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val serverModule = module {
    single { ServerEventBus() }
    single { DeviceRegistry() }
    single { UploadMonitor(get()) }
    single {
        ShareController(
            context = androidContext(),
            settingsRepository = get(),
            storageProvider = get(),
            registry = get(),
            eventBus = get(),
            uploadMonitor = get(),
        )
    }
}
