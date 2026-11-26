package com.fileshare.lan.core.common

import java.net.URLEncoder

private const val MAX_FILE_NAME_LENGTH = 180

/** 是否为可安全落盘的文件名（拒绝路径穿越与隐藏文件）。 */
fun isSafeFileName(fileName: String): Boolean {
    if (fileName.isBlank() || fileName.length > MAX_FILE_NAME_LENGTH) return false
    if (fileName == "." || fileName == "..") return false
    if (fileName.startsWith(".")) return false
    if (fileName.any { it == '/' || it == '\\' || it.code < 0x20 }) return false
    return true
}

/**
 * 修复 multipart 头部中的 UTF-8 文件名被按 Latin-1 解码产生的乱码。
 * 正常中文落在 CJK 区间，仅当出现 0x80..0xFF 的字符时才尝试按 UTF-8 重新解码。
 */
fun repairUtf8Mojibake(name: String): String {
    if (name.none { it.code in 0x80..0xFF }) return name
    val decoded = String(name.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
    return if (decoded.contains('�')) name else decoded
}

/**
 * 规范化浏览器上传的原始文件名：
 * 去掉路径前缀、非法字符，必要时回退为 untitled。
 */
fun sanitizeFileName(raw: String?): String? {
    val trimmed = raw?.let(::repairUtf8Mojibake)?.trim()?.substringAfterLast('/')?.substringAfterLast('\\')
        ?: return null
    if (trimmed.isBlank()) return null
    val cleaned = trimmed
        .filter { it != '\u0000' && it.code >= 0x20 && it != '/' && it != '\\' }
        .trim()
        .take(MAX_FILE_NAME_LENGTH)
    if (cleaned.isBlank() || cleaned == "." || cleaned == "..") return null
    return if (cleaned.startsWith(".")) "_$cleaned" else cleaned
}

/** 为同名文件生成 "name (1).ext" 形式的候选名。 */
fun numberedFileName(fileName: String, index: Int): String {
    if (index <= 0) return fileName
    val dot = fileName.lastIndexOf('.')
    return if (dot > 0) {
        "${fileName.substring(0, dot)} ($index)${fileName.substring(dot)}"
    } else {
        "$fileName ($index)"
    }
}

/** URL 路径段编码（空格编码为 %20 而非 +）。 */
fun encodePathSegment(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

/** RFC 5987 / RFC 6266 的 Content-Disposition，兼容中文文件名。 */
fun contentDisposition(fileName: String): String {
    val fallback = fileName.map { if (it.code in 0x20..0x7E && it != '"' && it != '\\') it else '_' }
        .joinToString("")
    val encoded = encodePathSegment(fileName)
    return """attachment; filename="$fallback"; filename*=UTF-8''$encoded"""
}
