package com.fileshare.lan.feature.share.di

import com.fileshare.lan.feature.share.ShareViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val shareModule = module {
    viewModel { ShareViewModel(get(), get(), androidContext()) }
}
