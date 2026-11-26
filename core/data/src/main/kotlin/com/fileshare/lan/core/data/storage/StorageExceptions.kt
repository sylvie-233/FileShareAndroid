package com.fileshare.lan.core.data.storage

/** 共享目录读写相关的业务异常。 */
open class StorageException(message: String) : Exception(message)

/** 单文件超过大小上限。 */
class FileTooLargeException(val maxBytes: Long) :
    StorageException("文件超过大小限制（上限 ${maxBytes / 1024 / 1024} MB）")

/** 磁盘空间不足。 */
class InsufficientStorageException : StorageException("存储空间不足")
