package com.fileshare.lan.feature.files.di

import com.fileshare.lan.feature.files.FilesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val filesModule = module {
    viewModel { FilesViewModel(get(), get(), get(), androidContext()) }
}
