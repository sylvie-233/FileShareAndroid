package com.fileshare.lan.feature.settings.di

import com.fileshare.lan.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get(), get(), get()) }
}
