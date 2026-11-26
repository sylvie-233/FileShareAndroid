package com.fileshare.lan.core.data.storage

import com.fileshare.lan.core.model.SharedFile
import java.io.InputStream

/**
 * 共享目录的统一访问抽象，屏蔽普通目录（File）与 SAF（DocumentFile）的差异。
 */
interface SharedStorage {

    /** 展示给用户的位置描述，如 Download/FileShare。 */
    val displayPath: String

    suspend fun list(): List<SharedFile>

    /** 打开只读流；文件不存在返回 null。 */
    suspend fun openRead(fileName: String): InputStream?

    /**
     * 保存一个文件流，自动处理同名冲突（追加 (1)、(2)）。
     *
     * @param expectedSize 预知的大小（取自 Content-Length），未知传 -1。
     * @param maxBytes 大小上限，0 表示不限。
     * @param onProgress 已写入字节数回调。
     */
    suspend fun save(
        fileName: String,
        input: InputStream,
        expectedSize: Long,
        maxBytes: Long,
        onProgress: (Long) -> Unit = {},
    ): SharedFile

    suspend fun delete(fileName: String): Boolean

    /** 清空全部文件，返回删除数量。 */
    suspend fun clearAll(): Int

    /** 可用空间（字节），无法查询时返回 null。 */
    suspend fun freeSpaceBytes(): Long?
}
