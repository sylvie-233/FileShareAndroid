package com.fileshare.lan.core.common

/** 文件类型色系，用于列表徽章配色。 */
enum class FileTone { DOC, IMAGE, VIDEO, AUDIO, ARCHIVE, APK, CODE, OTHER }

private val TONES = mapOf(
    FileTone.DOC to setOf("doc", "docx", "pdf", "txt", "md", "rtf", "xls", "xlsx", "ppt", "pptx", "csv"),
    FileTone.IMAGE to setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "heic", "ico"),
    FileTone.VIDEO to setOf("mp4", "mkv", "mov", "avi", "webm", "flv", "wmv", "3gp"),
    FileTone.AUDIO to setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus"),
    FileTone.ARCHIVE to setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz"),
    FileTone.APK to setOf("apk"),
    FileTone.CODE to setOf("kt", "java", "js", "ts", "html", "css", "json", "xml", "py", "go", "rs", "sh"),
)

/** 扩展名，不含点，统一小写。 */
fun fileExtension(fileName: String): String {
    val base = fileName.substringAfterLast('/')
    val dot = base.lastIndexOf('.')
    return if (dot <= 0 || dot == base.length - 1) "" else base.substring(dot + 1).lowercase()
}

/** 文件徽章文字，最多 4 个字符。 */
fun fileBadgeLabel(fileName: String): String {
    val ext = fileExtension(fileName)
    return if (ext.isEmpty()) "FILE" else ext.take(4).uppercase()
}

fun fileTone(fileName: String): FileTone {
    val ext = fileExtension(fileName)
    return TONES.entries.firstOrNull { (_, extensions) -> ext in extensions }?.key ?: FileTone.OTHER
}
