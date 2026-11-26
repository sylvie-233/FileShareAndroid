package com.fileshare.lan.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileshare.lan.core.data.settings.SettingsRepository
import com.fileshare.lan.core.data.storage.StorageProvider
import com.fileshare.lan.core.model.AppSettings
import com.fileshare.lan.core.model.ServerState
import com.fileshare.lan.core.server.ShareController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val storageProvider: StorageProvider,
    private val controller: ShareController,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    val serviceRunning: StateFlow<Boolean> = controller.state
        .map { it is ServerState.Running }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val saveDirLabel: StateFlow<String> = settingsRepository.settings
        .map { it.saveDirUri }
        .distinctUntilChanged()
        .map { uri ->
            withContext(Dispatchers.IO) {
                runCatching { storageProvider.forUri(uri).displayPath }.getOrDefault("")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setPort(port: Int) {
        viewModelScope.launch {
            settingsRepository.setPort(port)
            if (controller.state.value is ServerState.Running) {
                _messages.tryEmit("端口已修改，服务正在重启…")
                controller.restartIfRunning()
            }
        }
    }

    fun setAutostart(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutostart(enabled) }
    }

    fun setKeepAwake(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepAwake(enabled) }
    }

    fun setMaxFileSize(bytes: Long) {
        viewModelScope.launch { settingsRepository.setMaxFileSize(bytes) }
    }

    fun setAllowUpload(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAllowUpload(enabled)
            _messages.tryEmit(if (enabled) "已允许其他设备上传" else "已关闭其他设备上传")
        }
    }

    fun setSaveDir(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setSaveDirUri(uri)
            _messages.tryEmit(if (uri == null) "已恢复默认保存目录" else "保存目录已更新")
        }
    }

    fun notify(message: String) {
        _messages.tryEmit(message)
    }
}
