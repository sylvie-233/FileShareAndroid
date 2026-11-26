package com.fileshare.lan.core.model

/** 根据 User-Agent 推断的访问设备类型（浏览器不会提供真实设备名）。 */
enum class DeviceType(val displayName: String) {
    ANDROID("Android"),
    IPHONE("iPhone"),
    IPAD("iPad"),
    WINDOWS("Windows"),
    MAC("Mac"),
    OTHER("未知设备"),
}

/** 局域网内的一台访问设备。 */
data class DeviceInfo(
    val ip: String,
    val userAgent: String,
    val deviceType: DeviceType,
    val lastSeenAtMillis: Long,
    val online: Boolean,
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
)
