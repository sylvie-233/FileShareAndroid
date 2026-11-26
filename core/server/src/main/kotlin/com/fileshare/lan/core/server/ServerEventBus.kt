package com.fileshare.lan.core.server

import com.fileshare.lan.core.model.ServerEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** App UI 与 WebSocket 订阅的服务端事件总线。 */
class ServerEventBus {

    private val _events = MutableSharedFlow<ServerEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<ServerEvent> = _events.asSharedFlow()

    fun emit(event: ServerEvent) {
        _events.tryEmit(event)
    }
}
