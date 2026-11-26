package com.fileshare.lan.feature.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileshare.lan.core.data.settings.SettingsRepository
import com.fileshare.lan.core.model.AppSettings
import com.fileshare.lan.core.model.DeviceInfo
import com.fileshare.lan.core.model.ServerMetrics
import com.fileshare.lan.core.model.ServerState
import com.fileshare.lan.core.server.ShareController
import com.fileshare.lan.core.server.service.ShareService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn

class ShareViewModel(
    private val controller: ShareController,
    settingsRepository: SettingsRepository,
    private val appContext: Context,
) : ViewModel() {

    val state: StateFlow<ServerState> = controller.state
    val metrics: StateFlow<ServerMetrics> = controller.metrics
    val devices: StateFlow<List<DeviceInfo>> = controller.devices
    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun startService() {
        ShareService.start(appContext)
    }

    fun stopService() {
        ShareService.stop(appContext)
        _messages.tryEmit("正在停止共享服务…")
    }

    fun notify(message: String) {
        _messages.tryEmit(message)
    }
}
