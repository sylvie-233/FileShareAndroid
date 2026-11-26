package com.fileshare.lan.core.server

import com.fileshare.lan.core.model.DeviceInfo
import com.fileshare.lan.core.model.DeviceType
import com.fileshare.lan.core.common.deviceTypeFromUserAgent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 访问设备登记表（功能设计文档 4.1 节）。
 *
 * 每个 HTTP 请求都会刷新 lastSeen；WebSocket 连接另外维护在线计数。
 * 超过 [OFFLINE_AFTER_MS] 无活动即视为离线，无连接且长时间未活动的记录会被清理。
 */
class DeviceRegistry {

    private class Entry(
        val ip: String,
        var userAgent: String = "",
        var lastSeenAt: Long = 0L,
        var sockets: Int = 0,
        var uploadBytes: Long = 0L,
        var downloadBytes: Long = 0L,
    )

    private val lock = Any()
    private val entries = mutableMapOf<String, Entry>()
    private var totalUploadBytes = 0L
    private var totalDownloadBytes = 0L

    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices.asStateFlow()

    fun touch(ip: String, userAgent: String?) {
        if (ip.isBlank()) return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val entry = entryOf(ip)
            entry.lastSeenAt = now
            if (!userAgent.isNullOrBlank()) entry.userAgent = userAgent
        }
        publish()
    }

    fun recordUpload(ip: String, bytes: Long) {
        if (bytes <= 0L) return
        synchronized(lock) {
            entryOf(ip).uploadBytes += bytes
            totalUploadBytes += bytes
        }
    }

    fun recordDownload(ip: String, bytes: Long) {
        if (bytes <= 0L) return
        synchronized(lock) {
            entryOf(ip).downloadBytes += bytes
            totalDownloadBytes += bytes
        }
    }

    fun socketOpened(ip: String, userAgent: String?) {
        if (ip.isBlank()) return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val entry = entryOf(ip)
            entry.sockets++
            entry.lastSeenAt = now
            if (!userAgent.isNullOrBlank()) entry.userAgent = userAgent
        }
        publish()
    }

    fun socketClosed(ip: String) {
        if (ip.isBlank()) return
        synchronized(lock) {
            val entry = entries[ip] ?: return@synchronized
            entry.sockets = (entry.sockets - 1).coerceAtLeast(0)
            entry.lastSeenAt = System.currentTimeMillis()
        }
        publish()
    }

    /** 周期刷新在线状态并清理过期记录。 */
    fun refresh(now: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            entries.entries.removeAll { (_, entry) ->
                entry.sockets == 0 && now - entry.lastSeenAt > PRUNE_AFTER_MS
            }
        }
        publish()
    }

    fun totalUploadBytes(): Long = synchronized(lock) { totalUploadBytes }

    fun totalDownloadBytes(): Long = synchronized(lock) { totalDownloadBytes }

    fun reset() {
        synchronized(lock) {
            entries.clear()
            totalUploadBytes = 0L
            totalDownloadBytes = 0L
        }
        _devices.value = emptyList()
    }

    private fun entryOf(ip: String): Entry = entries.getOrPut(normalizeIp(ip)) { Entry(ip = normalizeIp(ip)) }

    /** adb forward / 本机访问时 origin 可能是 localhost 或 IPv6 回环，统一展示为 127.0.0.1。 */
    private fun normalizeIp(ip: String): String = when (ip) {
        "localhost", "::1", "0:0:0:0:0:0:0:1" -> "127.0.0.1"
        else -> ip
    }

    private fun publish() {
        val snapshot = synchronized(lock) {
            val now = System.currentTimeMillis()
            entries.values.map { entry ->
                DeviceInfo(
                    ip = entry.ip,
                    userAgent = entry.userAgent,
                    deviceType = deviceTypeFromUserAgent(entry.userAgent),
                    lastSeenAtMillis = entry.lastSeenAt,
                    online = entry.sockets > 0 || now - entry.lastSeenAt <= OFFLINE_AFTER_MS,
                    uploadBytes = entry.uploadBytes,
                    downloadBytes = entry.downloadBytes,
                )
            }.sortedWith(compareByDescending<DeviceInfo> { it.online }.thenByDescending { it.lastSeenAtMillis })
        }
        if (_devices.value != snapshot) _devices.value = snapshot
    }

    private companion object {
        const val OFFLINE_AFTER_MS = 30_000L
        const val PRUNE_AFTER_MS = 5 * 60_000L
    }
}
