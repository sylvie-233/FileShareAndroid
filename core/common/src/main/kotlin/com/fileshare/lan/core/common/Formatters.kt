package com.fileshare.lan.core.common

import java.util.Locale
import java.util.concurrent.TimeUnit

/** 字节数格式化：1.2 MB / 340 KB。 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= kb && index < units.lastIndex) {
        value /= kb
        index++
    }
    return if (index == 0) {
        "${value.toLong()} ${units[index]}"
    } else {
        val pattern = if (value >= 10) "%.1f" else "%.2f"
        String.format(Locale.US, pattern, value) + " " + units[index]
    }
}

/** 传输速率格式化：1.2 MB/s；为 0 时展示 —。 */
fun formatSpeed(bytesPerSec: Long): String =
    if (bytesPerSec <= 0L) "—" else "${formatBytes(bytesPerSec)}/s"

/** 运行时长：01:23:45，不足 1 小时展示 12:34。 */
fun formatUptime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/** 相对时间：刚刚 / 3 分钟前 / 2 小时前 / 3 天前 / 2026-01-02 14:30。 */
fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (timestampMillis <= 0L) return "—"
    val diff = nowMillis - timestampMillis
    if (diff < 0) return formatAbsoluteTime(timestampMillis)
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "刚刚"
        minutes < 60 -> "${minutes} 分钟前"
        hours < 24 -> "${hours} 小时前"
        days < 7 -> "${days} 天前"
        else -> formatAbsoluteTime(timestampMillis)
    }
}

fun formatAbsoluteTime(timestampMillis: Long): String {
    val date = java.util.Date(timestampMillis)
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
}
