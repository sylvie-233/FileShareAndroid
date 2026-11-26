package com.fileshare.lan

import android.app.Application
import com.fileshare.lan.core.data.di.dataModule
import com.fileshare.lan.core.server.di.serverModule
import com.fileshare.lan.feature.files.di.filesModule
import com.fileshare.lan.feature.settings.di.settingsModule
import com.fileshare.lan.feature.share.di.shareModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class FileShareApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            logger(AndroidLogger(Level.ERROR))
            androidContext(this@FileShareApplication)
            modules(dataModule, serverModule, shareModule, filesModule, settingsModule)
        }
    }
}
