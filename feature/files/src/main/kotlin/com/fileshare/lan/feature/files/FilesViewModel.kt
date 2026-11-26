package com.fileshare.lan.feature.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileshare.lan.core.common.encodePathSegment
import com.fileshare.lan.core.data.settings.SettingsRepository
import com.fileshare.lan.core.data.storage.StorageProvider
import com.fileshare.lan.core.model.ServerEvent
import com.fileshare.lan.core.model.ServerState
import com.fileshare.lan.core.model.SharedFile
import com.fileshare.lan.core.model.UploadStatus
import com.fileshare.lan.core.model.UploadTask
import com.fileshare.lan.core.server.ShareController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

data class FilesUiState(
    val files: List<SharedFile> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageCount: Int = 1,
    val query: String = "",
    val loading: Boolean = true,
    val saveDirLabel: String = "",
    val uploads: List<UploadTask> = emptyList(),
    val storageError: String? = null,
)

class FilesViewModel(
    private val settingsRepository: SettingsRepository,
    private val storageProvider: StorageProvider,
    private val controller: ShareController,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val refreshTrigger = MutableStateFlow(0)

    private var allFiles: List<SharedFile> = emptyList()
    private var queryText = ""
    private var currentPage = 1
    private var importTask: UploadTask? = null
    private var serverUploads: List<UploadTask> = emptyList()
    private var saveDirLabel = ""
    private var queryDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings.map { it.saveDirUri }.distinctUntilChanged(),
                refreshTrigger,
            ) { uri, _ -> uri }
                .collectLatest { uri -> reload(uri) }
        }
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.saveDirUri }
                .distinctUntilChanged()
                .collectLatest { uri ->
                    saveDirLabel = withContext(Dispatchers.IO) {
                        runCatching { storageProvider.forUri(uri).displayPath }.getOrDefault("")
                    }
                    render()
                }
        }
        viewModelScope.launch {
            controller.uploads.collect { uploads ->
                serverUploads = uploads.filter { it.status == UploadStatus.RUNNING }
                render()
            }
        }
        viewModelScope.launch {
            controller.events.collect { event ->
                when (event) {
                    is ServerEvent.FilesChanged -> refresh()
                    is ServerEvent.UploadUpdated -> when (event.task.status) {
                        UploadStatus.COMPLETED -> _messages.tryEmit("已接收 ${event.task.fileName}")
                        UploadStatus.FAILED -> _messages.tryEmit("上传失败：${event.task.message ?: event.task.fileName}")
                        UploadStatus.RUNNING -> Unit
                    }

                    else -> Unit
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        queryText = value
        _uiState.update { it.copy(query = value) }
        queryDebounceJob?.cancel()
        queryDebounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            currentPage = 1
            render()
        }
    }

    fun clearQuery() {
        onQueryChange("")
    }

    fun onPageChange(page: Int) {
        currentPage = page.coerceAtLeast(1)
        render()
    }

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    fun deleteFile(file: SharedFile) {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val deleted = runCatching {
                withContext(Dispatchers.IO) { storageProvider.current(settings).delete(file.name) }
            }.getOrDefault(false)
            _messages.tryEmit(if (deleted) "已删除 ${file.name}" else "删除失败")
            refresh()
        }
    }

    fun clearAllFiles() {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val count = runCatching {
                withContext(Dispatchers.IO) { storageProvider.current(settings).clearAll() }
            }.getOrDefault(0)
            _messages.tryEmit("已清空 $count 个文件")
            refresh()
        }
    }

    /** 复制文件直链；服务未运行时返回 null。 */
    fun downloadUrlFor(file: SharedFile): String? {
        val info = (controller.state.value as? ServerState.Running)?.info ?: return null
        return "${info.url}/api/v1/download/${encodePathSegment(file.name)}"
    }

    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val (displayName, size) = queryFileMeta(uri)
            val fileName = displayName ?: "导入文件-${System.currentTimeMillis()}"
            val task = UploadTask(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                totalBytes = if (size > 0) size else 0L,
                receivedBytes = 0L,
                status = UploadStatus.RUNNING,
                startedAtMillis = System.currentTimeMillis(),
            )
            importTask = task
            render()
            try {
                val input = appContext.contentResolver.openInputStream(uri)
                    ?: throw IOException("无法读取所选文件")
                var lastUpdateAt = 0L
                val saved = withContext(Dispatchers.IO) {
                    storageProvider.current(settings).save(
                        fileName = fileName,
                        input = input,
                        expectedSize = size,
                        maxBytes = settings.maxFileSizeBytes,
                    ) { received ->
                        val now = System.currentTimeMillis()
                        if (now - lastUpdateAt >= PROGRESS_UPDATE_MS) {
                            lastUpdateAt = now
                            viewModelScope.launch {
                                importTask = importTask?.copy(receivedBytes = received)
                                render()
                            }
                        }
                    }
                }
                importTask = null
                _messages.tryEmit("已导入 ${saved.name}")
                refresh()
            } catch (t: Throwable) {
                importTask = null
                _messages.tryEmit(t.message ?: "导入失败")
            }
            render()
        }
    }

    private suspend fun reload(uri: String?) {
        _uiState.update { it.copy(loading = true) }
        val result = runCatching {
            withContext(Dispatchers.IO) { storageProvider.forUri(uri).list() }
        }
        allFiles = result.getOrElse { emptyList() }
        _uiState.update {
            it.copy(
                loading = false,
                storageError = result.exceptionOrNull()?.message,
            )
        }
        render()
    }

    private fun render() {
        val filtered = if (queryText.isBlank()) {
            allFiles
        } else {
            allFiles.filter { it.name.contains(queryText, ignoreCase = true) }
        }
        val pageCount = ((filtered.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        if (currentPage > pageCount) currentPage = pageCount
        val slice = filtered.drop((currentPage - 1) * PAGE_SIZE).take(PAGE_SIZE)

        val activeUploads = buildList {
            importTask?.let(::add)
            addAll(serverUploads)
        }
        _uiState.update {
            it.copy(
                files = slice,
                totalCount = filtered.size,
                page = currentPage,
                pageCount = pageCount,
                saveDirLabel = saveDirLabel,
                uploads = activeUploads,
            )
        }
    }

    private fun queryFileMeta(uri: Uri): Pair<String?, Long> {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        runCatching {
            appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
                    return name to size
                }
            }
        }
        return null to -1L
    }

    private companion object {
        const val PAGE_SIZE = 8
        const val SEARCH_DEBOUNCE_MS = 150L
        const val PROGRESS_UPDATE_MS = 150L
    }
}
