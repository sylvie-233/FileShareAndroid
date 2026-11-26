package com.fileshare.lan.core.data.storage

import com.fileshare.lan.core.common.isSafeFileName
import com.fileshare.lan.core.common.numberedFileName
import com.fileshare.lan.core.model.SharedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * 直接使用 File API 的共享目录实现，适用于默认目录 Download/FileShare
 * 或已授予“所有文件访问权限”的场景。
 */
class FileSystemStorage(
    private val rootDir: File,
    override val displayPath: String,
) : SharedStorage {

    private val nameLock = Any()

    override suspend fun list(): List<SharedFile> = withContext(Dispatchers.IO) {
        ensureRoot().listFiles()
            ?.asSequence()
            ?.filter { it.isFile && !it.name.startsWith(".") }
            ?.map { SharedFile(it.name, it.length(), it.lastModified()) }
            ?.sortedByDescending { it.modifiedAtMillis }
            ?.toList()
            .orEmpty()
    }

    override suspend fun openRead(fileName: String): InputStream? = withContext(Dispatchers.IO) {
        if (!isSafeFileName(fileName)) return@withContext null
        val file = File(ensureRoot(), fileName)
        if (file.isFile) BufferedInputStream(FileInputStream(file)) else null
    }

    override suspend fun save(
        fileName: String,
        input: InputStream,
        expectedSize: Long,
        maxBytes: Long,
        onProgress: (Long) -> Unit,
    ): SharedFile = withContext(Dispatchers.IO) {
        if (maxBytes > 0 && expectedSize > maxBytes) throw FileTooLargeException(maxBytes)

        val dir = ensureRoot()
        if (expectedSize > 0 && expectedSize > dir.usableSpace) throw InsufficientStorageException()

        val temp = File.createTempFile(".upload-", ".part", dir)
        var total = 0L
        try {
            BufferedOutputStream(FileOutputStream(temp)).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (maxBytes > 0 && total > maxBytes) throw FileTooLargeException(maxBytes)
                    output.write(buffer, 0, read)
                    onProgress(total)
                }
            }
            val target = reserveUniqueTarget(dir, fileName)
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            SharedFile(target.name, target.length(), target.lastModified())
        } catch (t: Throwable) {
            temp.delete()
            throw t
        } finally {
            runCatching { input.close() }
        }
    }

    override suspend fun delete(fileName: String): Boolean = withContext(Dispatchers.IO) {
        if (!isSafeFileName(fileName)) return@withContext false
        val file = File(ensureRoot(), fileName)
        file.isFile && file.delete()
    }

    override suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        var removed = 0
        ensureRoot().listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".") && file.delete()) removed++
        }
        removed
    }

    override suspend fun freeSpaceBytes(): Long? = withContext(Dispatchers.IO) {
        runCatching { ensureRoot().usableSpace }.getOrNull()
    }

    /** 原子地占位一个不冲突的文件名，避免并发上传互相覆盖。 */
    private fun reserveUniqueTarget(dir: File, fileName: String): File = synchronized(nameLock) {
        var index = 0
        while (index < MAX_NAME_ATTEMPTS) {
            val candidate = File(dir, numberedFileName(fileName, index))
            if (candidate.createNewFile()) return candidate
            index++
        }
        throw IOException("无法生成唯一文件名")
    }

    private fun ensureRoot(): File {
        if (!rootDir.exists()) rootDir.mkdirs()
        if (!rootDir.isDirectory) throw IOException("共享目录不可用：$displayPath")
        return rootDir
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_NAME_ATTEMPTS = 1000
    }
}
