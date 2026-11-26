package com.fileshare.lan.core.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.fileshare.lan.core.common.isSafeFileName
import com.fileshare.lan.core.common.numberedFileName
import com.fileshare.lan.core.model.SharedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * 基于 SAF（Storage Access Framework）的共享目录实现。
 * 用户在设置页通过系统文件夹选择器授权后，以持久化 URI 访问。
 */
class SafStorage(
    private val context: Context,
    private val treeUri: Uri,
) : SharedStorage {

    override val displayPath: String by lazy {
        runCatching { tree()?.name }.getOrNull() ?: "已选择目录"
    }

    private fun tree(): DocumentFile? = DocumentFile.fromTreeUri(context, treeUri)

    private fun requireTree(): DocumentFile =
        tree()?.takeIf { it.exists() && it.isDirectory }
            ?: throw IOException("共享目录不可访问，请重新选择保存目录")

    override suspend fun list(): List<SharedFile> = withContext(Dispatchers.IO) {
        val root = requireTree()
        root.listFiles()
            .asSequence()
            .filter { it.isFile && !it.name.orEmpty().startsWith(".") }
            .map { doc -> SharedFile(doc.name.orEmpty(), doc.length(), doc.lastModified()) }
            .sortedByDescending { it.modifiedAtMillis }
            .toList()
    }

    override suspend fun openRead(fileName: String): InputStream? = withContext(Dispatchers.IO) {
        if (!isSafeFileName(fileName)) return@withContext null
        val doc = requireTree().findFile(fileName)?.takeIf { it.isFile } ?: return@withContext null
        context.contentResolver.openInputStream(doc.uri)
    }

    override suspend fun save(
        fileName: String,
        input: InputStream,
        expectedSize: Long,
        maxBytes: Long,
        onProgress: (Long) -> Unit,
    ): SharedFile = withContext(Dispatchers.IO) {
        if (maxBytes > 0 && expectedSize > maxBytes) throw FileTooLargeException(maxBytes)

        val root = requireTree()
        val tempName = ".upload-${UUID.randomUUID()}.part"
        val temp = root.createFile(MIME_OCTET_STREAM, tempName)
            ?: throw IOException("无法在共享目录中创建文件")
        var total = 0L
        try {
            val raw = context.contentResolver.openOutputStream(temp.uri)
                ?: throw IOException("无法写入共享目录")
            BufferedOutputStream(raw).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (maxBytes > 0 && total > maxBytes) throw FileTooLargeException(maxBytes)
                    output.write(buffer, 0, read)
                    onProgress(total)
                }
                output.flush()
            }
            val finalName = uniqueName(root, fileName)
            if (!temp.renameTo(finalName)) {
                temp.delete()
                throw IOException("保存文件失败：无法重命名")
            }
            SharedFile(finalName, total, System.currentTimeMillis())
        } catch (t: Throwable) {
            runCatching { temp.delete() }
            throw t
        } finally {
            runCatching { input.close() }
        }
    }

    override suspend fun delete(fileName: String): Boolean = withContext(Dispatchers.IO) {
        if (!isSafeFileName(fileName)) return@withContext false
        requireTree().findFile(fileName)?.takeIf { it.isFile }?.delete() ?: false
    }

    override suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        var removed = 0
        requireTree().listFiles().forEach { doc ->
            if (doc.isFile && doc.delete()) removed++
        }
        removed
    }

    override suspend fun freeSpaceBytes(): Long? = null

    private fun uniqueName(root: DocumentFile, fileName: String): String {
        var index = 0
        while (index < MAX_NAME_ATTEMPTS) {
            val candidate = numberedFileName(fileName, index)
            if (root.findFile(candidate) == null) return candidate
            index++
        }
        throw IOException("无法生成唯一文件名")
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_NAME_ATTEMPTS = 1000
        const val MIME_OCTET_STREAM = "application/octet-stream"
    }
}
