package com.fileshare.lan.core.model

/** 应用设置项，对应功能设计文档 5.2 节。 */
data class AppSettings(
    val port: Int = DEFAULT_PORT,
    val autostart: Boolean = true,
    val keepAwake: Boolean = true,
    /** 0 表示不限。 */
    val maxFileSizeBytes: Long = 100L * 1024 * 1024,
    val allowUpload: Boolean = true,
    /** SAF tree URI；为 null 时使用默认目录 Download/FileShare。 */
    val saveDirUri: String? = null,
) {
    companion object {
        const val DEFAULT_PORT = 8080
        const val UNLIMITED_FILE_SIZE = 0L

        val PORT_OPTIONS = listOf(8080, 8000, 9090)
        val MAX_FILE_SIZE_OPTIONS = listOf(100L * 1024 * 1024, 1024L * 1024 * 1024, UNLIMITED_FILE_SIZE)
    }
}
