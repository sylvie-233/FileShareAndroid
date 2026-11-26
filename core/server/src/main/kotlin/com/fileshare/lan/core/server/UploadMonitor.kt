package com.fileshare.lan.core.server

import com.fileshare.lan.core.model.ServerEvent
import com.fileshare.lan.core.model.UploadStatus
import com.fileshare.lan.core.model.UploadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** 跟踪上传任务进度并广播给 UI。 */
class UploadMonitor(
    private val eventBus: ServerEventBus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()

    private val _tasks = MutableStateFlow<List<UploadTask>>(emptyList())
    val tasks: StateFlow<List<UploadTask>> = _tasks.asStateFlow()

    private val lastProgressAt = mutableMapOf<String, Long>()

    fun start(fileName: String, totalBytes: Long): String {
        val task = UploadTask(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            totalBytes = totalBytes.coerceAtLeast(0L),
            receivedBytes = 0L,
            status = UploadStatus.RUNNING,
            startedAtMillis = System.currentTimeMillis(),
        )
        synchronized(lock) { _tasks.value = _tasks.value + task }
        eventBus.emit(ServerEvent.UploadUpdated(task))
        return task.id
    }

    fun progress(id: String, receivedBytes: Long) {
        val now = System.currentTimeMillis()
        val shouldEmit = synchronized(lock) {
            if (now - (lastProgressAt[id] ?: 0L) < PROGRESS_INTERVAL_MS) return
            lastProgressAt[id] = now
            true
        }
        if (!shouldEmit) return
        update(id) { it.copy(receivedBytes = receivedBytes) }
    }

    fun complete(id: String, finalSize: Long) {
        update(id) {
            it.copy(
                receivedBytes = if (finalSize > 0) finalSize else it.receivedBytes,
                totalBytes = if (it.totalBytes > 0) it.totalBytes else finalSize,
                status = UploadStatus.COMPLETED,
            )
        }
        cleanupLater(id)
    }

    fun fail(id: String, message: String) {
        update(id) { it.copy(status = UploadStatus.FAILED, message = message) }
        cleanupLater(id)
    }

    fun clearAll() {
        synchronized(lock) {
            _tasks.value = emptyList()
            lastProgressAt.clear()
        }
    }

    private fun update(id: String, transform: (UploadTask) -> UploadTask) {
        synchronized(lock) {
            _tasks.value = _tasks.value.map { if (it.id == id) transform(it) else it }
        }
        eventBus.emit(ServerEvent.UploadsChanged(_tasks.value))
    }

    private fun cleanupLater(id: String) {
        scope.launch {
            delay(FINISHED_LINGER_MS)
            synchronized(lock) {
                _tasks.value = _tasks.value.filterNot { it.id == id }
                lastProgressAt.remove(id)
            }
            eventBus.emit(ServerEvent.UploadsChanged(_tasks.value))
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 150L
        const val FINISHED_LINGER_MS = 3_000L
    }
}
