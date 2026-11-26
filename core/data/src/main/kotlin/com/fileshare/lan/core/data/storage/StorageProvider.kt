package com.fileshare.lan.core.data.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.fileshare.lan.core.model.AppSettings
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 根据设置（默认目录 / SAF 目录）提供对应的 [SharedStorage]，并按 URI 缓存实例。
 */
class StorageProvider(
    private val context: Context,
) {
    private val cache = ConcurrentHashMap<String, SharedStorage>()

    fun current(settings: AppSettings): SharedStorage = forUri(settings.saveDirUri)

    fun forUri(saveDirUri: String?): SharedStorage {
        val key = saveDirUri?.takeIf { it.isNotBlank() } ?: DEFAULT_KEY
        return cache.getOrPut(key) {
            if (key == DEFAULT_KEY) {
                val root = defaultRoot()
                FileSystemStorage(root, defaultDisplayPath(root))
            } else {
                SafStorage(context, Uri.parse(key))
            }
        }
    }

    fun displayPath(saveDirUri: String?): String = forUri(saveDirUri).displayPath

    fun defaultRoot(): File {
        val root = if (hasPublicStorageAccess()) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER_NAME)
        } else {
            File(context.getExternalFilesDir(null) ?: context.filesDir, FOLDER_NAME)
        }
        if (!root.exists()) root.mkdirs()
        return root
    }

    private fun defaultDisplayPath(root: File): String =
        if (hasPublicStorageAccess()) "Download/$FOLDER_NAME" else "应用专属目录/$FOLDER_NAME"

    private fun hasPublicStorageAccess(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val DEFAULT_KEY = "__default__"
        const val FOLDER_NAME = "FileShare"
    }
}
