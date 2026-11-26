package com.fileshare.lan.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fileshare.lan.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(
    private val context: Context,
) : SettingsRepository {

    private object Keys {
        val PORT = intPreferencesKey("port")
        val AUTOSTART = booleanPreferencesKey("autostart")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val MAX_FILE_SIZE = longPreferencesKey("max_file_size")
        val ALLOW_UPLOAD = booleanPreferencesKey("allow_upload")
        val SAVE_DIR_URI = stringPreferencesKey("save_dir_uri")
    }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            val defaults = AppSettings()
            AppSettings(
                port = prefs[Keys.PORT] ?: defaults.port,
                autostart = prefs[Keys.AUTOSTART] ?: defaults.autostart,
                keepAwake = prefs[Keys.KEEP_AWAKE] ?: defaults.keepAwake,
                maxFileSizeBytes = prefs[Keys.MAX_FILE_SIZE] ?: defaults.maxFileSizeBytes,
                allowUpload = prefs[Keys.ALLOW_UPLOAD] ?: defaults.allowUpload,
                saveDirUri = prefs[Keys.SAVE_DIR_URI],
            )
        }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setPort(port: Int) {
        context.settingsDataStore.edit { it[Keys.PORT] = port }
    }

    override suspend fun setAutostart(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTOSTART] = enabled }
    }

    override suspend fun setKeepAwake(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_AWAKE] = enabled }
    }

    override suspend fun setMaxFileSize(bytes: Long) {
        context.settingsDataStore.edit { it[Keys.MAX_FILE_SIZE] = bytes }
    }

    override suspend fun setAllowUpload(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ALLOW_UPLOAD] = enabled }
    }

    override suspend fun setSaveDirUri(uri: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri.isNullOrBlank()) prefs.remove(Keys.SAVE_DIR_URI) else prefs[Keys.SAVE_DIR_URI] = uri
        }
    }
}
