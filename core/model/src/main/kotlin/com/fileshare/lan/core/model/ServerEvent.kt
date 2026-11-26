package com.fileshare.lan.core.model

/** 服务端向 App UI / WebSocket 客户端推送的事件。 */
sealed interface ServerEvent {
    data object FilesChanged : ServerEvent
    data object DevicesChanged : ServerEvent
    data class UploadUpdated(val task: UploadTask) : ServerEvent
    data class UploadsChanged(val tasks: List<UploadTask>) : ServerEvent
}
