package com.fileshare.lan.core.common

import com.fileshare.lan.core.model.DeviceType

/** 从浏览器 User-Agent 解析设备类型（功能设计文档 4.1 节）。 */
fun deviceTypeFromUserAgent(userAgent: String?): DeviceType {
    val ua = userAgent.orEmpty()
    if (ua.isBlank()) return DeviceType.OTHER
    return when {
        ua.contains("iPad", ignoreCase = true) -> DeviceType.IPAD
        ua.contains("iPhone", ignoreCase = true) -> DeviceType.IPHONE
        ua.contains("Android", ignoreCase = true) -> DeviceType.ANDROID
        ua.contains("Windows", ignoreCase = true) -> DeviceType.WINDOWS
        ua.contains("Macintosh", ignoreCase = true) || ua.contains("Mac OS X", ignoreCase = true) -> DeviceType.MAC
        else -> DeviceType.OTHER
    }
}
