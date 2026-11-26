package com.fileshare.lan.core.model

/** 共享目录中的一个文件。 */
data class SharedFile(
    val name: String,
    val sizeBytes: Long,
    val modifiedAtMillis: Long,
)
