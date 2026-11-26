package com.fileshare.lan.core.model

/** 服务端信息，服务运行期间有效。 */
data class ServerInfo(
    val url: String,
    val ip: String,
    val port: Int,
)

/** 共享服务状态机（功能设计文档 2.1 节）。 */
sealed interface ServerState {
    data object Stopped : ServerState
    data object Starting : ServerState
    data object Stopping : ServerState
    data class Running(val info: ServerInfo) : ServerState
    data class Failed(val message: String) : ServerState
}

val ServerState.isRunning: Boolean
    get() = this is ServerState.Running
