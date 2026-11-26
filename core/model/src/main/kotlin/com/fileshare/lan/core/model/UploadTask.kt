package com.fileshare.lan.core.model

enum class UploadStatus { RUNNING, COMPLETED, FAILED }

/** 正在进行的上传任务（浏览器上传 / App 内导入）。 */
data class UploadTask(
    val id: String,
    val fileName: String,
    val totalBytes: Long,
    val receivedBytes: Long,
    val status: UploadStatus,
    val startedAtMillis: Long,
    val message: String? = null,
) {
    val progress: Float
        get() = if (totalBytes <= 0L) 0f else (receivedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}
