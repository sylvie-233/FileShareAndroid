package com.fileshare.lan.core.data.settings

import com.fileshare.lan.core.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** 应用设置的读写入口，默认实现基于 DataStore。 */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    /** 同步读取一次当前设置（服务启动、开机广播等场景）。 */
    suspend fun current(): AppSettings

    suspend fun setPort(port: Int)
    suspend fun setAutostart(enabled: Boolean)
    suspend fun setKeepAwake(enabled: Boolean)
    suspend fun setMaxFileSize(bytes: Long)
    suspend fun setAllowUpload(enabled: Boolean)
    suspend fun setSaveDirUri(uri: String?)
}
