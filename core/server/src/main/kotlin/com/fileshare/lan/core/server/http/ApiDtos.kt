package com.fileshare.lan.core.server.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ServerVersion {
    const val VERSION = "1.0.0"
}

@Serializable
data class ServerInfoDto(
    @SerialName("server_url") val serverUrl: String,
    val ip: String,
    val port: Int,
    val version: String = ServerVersion.VERSION,
)

@Serializable
data class FileDto(
    val name: String,
    val size: Long,
    /** Unix 秒，与 Web 页面 `new Date(modified * 1000)` 的约定保持一致。 */
    val modified: Long,
)

@Serializable
data class FilesResponse(
    val files: List<FileDto>,
    val total: Int,
    val page: Int,
    val size: Int,
)

@Serializable
data class UploadResponse(val name: String, val size: Long)

@Serializable
data class DeleteResponse(val name: String, val deleted: Boolean = true)

@Serializable
data class ClearResponse(val count: Int)

@Serializable
data class ErrorDto(val detail: String)

@Serializable
data class DeviceDto(
    val ip: String,
    val type: String,
    @SerialName("user_agent") val userAgent: String,
    val online: Boolean,
    @SerialName("last_seen") val lastSeen: Long,
)

@Serializable
data class DevicesResponse(val devices: List<DeviceDto>)

/** WebSocket 推送消息。 */
@Serializable
data class WsEventDto(
    val event: String,
    @SerialName("file_name") val fileName: String? = null,
    val ip: String? = null,
    val type: String? = null,
)
