package com.fileshare.lan.core.server.http

import android.content.Context
import com.fileshare.lan.core.common.contentDisposition
import com.fileshare.lan.core.common.isSafeFileName
import com.fileshare.lan.core.common.sanitizeFileName
import com.fileshare.lan.core.data.settings.SettingsRepository
import com.fileshare.lan.core.data.storage.FileTooLargeException
import com.fileshare.lan.core.data.storage.InsufficientStorageException
import com.fileshare.lan.core.data.storage.StorageProvider
import com.fileshare.lan.core.model.AppSettings
import com.fileshare.lan.core.model.ServerEvent
import com.fileshare.lan.core.model.SharedFile
import com.fileshare.lan.core.server.DeviceRegistry
import com.fileshare.lan.core.server.ServerEventBus
import com.fileshare.lan.core.server.UploadMonitor
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.userAgent
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.OutputStream

/**
 * 内嵌 Ktor HTTP / WebSocket 服务，接口契约沿用 FileShareTs（功能设计文档第 3 节）。
 */
class ShareServer(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val storageProvider: StorageProvider,
    private val registry: DeviceRegistry,
    private val eventBus: ServerEventBus,
    private val uploadMonitor: UploadMonitor,
    private val lanIpProvider: () -> String?,
) {
    private val json = Json { encodeDefaults = true }

    private var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var runningPort: Int = 0

    suspend fun start(port: Int) {
        require(engine == null) { "服务已在运行" }
        runningPort = port
        val server = embeddedServer(CIO, host = HOST, port = port) {
            appModule()
        }
        server.start(wait = false)
        engine = server
    }

    suspend fun stop() {
        val server = engine ?: return
        engine = null
        runCatching { server.stop(gracePeriodMillis = 300, timeoutMillis = 2_000) }
    }

    private fun Application.appModule() {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets) {
            pingPeriodMillis = 20_000
            timeoutMillis = 40_000
        }
        install(StatusPages) {
            exception<FileTooLargeException> { call, cause ->
                call.respond(HttpStatusCode.PayloadTooLarge, ErrorDto(cause.message ?: "文件超过大小限制"))
            }
            exception<InsufficientStorageException> { call, cause ->
                call.respond(HttpStatusCode(507, "Insufficient Storage"), ErrorDto(cause.message ?: "存储空间不足"))
            }
            exception<IllegalArgumentException> { call, cause ->
                call.respond(HttpStatusCode.BadRequest, ErrorDto(cause.message ?: "请求不合法"))
            }
            exception<Throwable> { call, cause ->
                runCatching {
                    call.respond(HttpStatusCode.InternalServerError, ErrorDto(cause.message ?: "服务器内部错误"))
                }
            }
        }

        install(createApplicationPlugin(name = "DeviceTracking") {
            onCall { call ->
                registry.touch(call.request.origin.remoteHost, call.request.userAgent())
            }
        })

        routing {
            get("/") {
                call.respondRedirect("/static/index.html", permanent = false)
            }

            route("/static/{path...}") {
                get {
                    val segments = call.parameters.getAll("path").orEmpty()
                    serveAsset(call, segments.joinToString("/"))
                }
            }

            route("/api/v1") {
                get("/server-info") {
                    val ip = lanIpProvider() ?: ""
                    val port = runningPort
                    call.respond(
                        ServerInfoDto(
                            serverUrl = "http://$ip:$port",
                            ip = ip,
                            port = port,
                        ),
                    )
                }

                get("/files") {
                    val settings = settingsRepository.current()
                    val storage = storageProvider.current(settings)
                    val query = call.request.queryParameters["q"].orEmpty()
                    val page = (call.request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val pageSize = (call.request.queryParameters["size"]?.toIntOrNull() ?: 0).coerceIn(0, 500)

                    val matched = storage.list().filter { file ->
                        query.isBlank() || file.name.contains(query, ignoreCase = true)
                    }
                    val slice = if (pageSize > 0) {
                        matched.drop((page - 1) * pageSize).take(pageSize)
                    } else {
                        matched
                    }
                    call.respond(
                        FilesResponse(
                            files = slice.map { FileDto(it.name, it.sizeBytes, it.modifiedAtMillis / 1000) },
                            total = matched.size,
                            page = page,
                            size = pageSize,
                        ),
                    )
                }

                post("/upload") {
                    handleUpload(call)
                }

                get("/download/{filename}") {
                    handleDownload(call)
                }

                delete("/files/{filename}") {
                    val name = call.parameters["filename"].orEmpty()
                    if (!isSafeFileName(name)) {
                        call.respond(HttpStatusCode.BadRequest, ErrorDto("非法文件名"))
                        return@delete
                    }
                    val settings = settingsRepository.current()
                    val storage = storageProvider.current(settings)
                    if (storage.delete(name)) {
                        eventBus.emit(ServerEvent.FilesChanged)
                        call.respond(DeleteResponse(name))
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorDto("文件不存在"))
                    }
                }

                delete("/files") {
                    val settings = settingsRepository.current()
                    val storage = storageProvider.current(settings)
                    val count = storage.clearAll()
                    eventBus.emit(ServerEvent.FilesChanged)
                    call.respond(ClearResponse(count))
                }

                get("/devices") {
                    call.respond(DevicesResponse(registry.devices.value.map { it.toDto() }))
                }

                webSocket("/ws") {
                    val ip = call.request.origin.remoteHost
                    val userAgent = call.request.userAgent()
                    registry.socketOpened(ip, userAgent)
                    eventBus.emit(ServerEvent.DevicesChanged)

                    val sender = launch {
                        eventBus.events.collect { event ->
                            val message = when (event) {
                                is ServerEvent.FilesChanged -> WsEventDto(event = "files-changed")
                                is ServerEvent.DevicesChanged -> WsEventDto(event = "devices-changed")
                                is ServerEvent.UploadUpdated -> WsEventDto(
                                    event = "upload",
                                    fileName = event.task.fileName,
                                )
                                is ServerEvent.UploadsChanged -> WsEventDto(event = "uploads-changed")
                            }
                            send(Frame.Text(json.encodeToString(WsEventDto.serializer(), message)))
                        }
                    }
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text && frame.readText().trim() == "ping") {
                                registry.touch(ip, userAgent)
                                send(Frame.Text("""{"event":"pong"}"""))
                            }
                        }
                    } finally {
                        sender.cancel()
                        registry.socketClosed(ip)
                        eventBus.emit(ServerEvent.DevicesChanged)
                    }
                }
            }
        }
    }

    private suspend fun handleUpload(call: ApplicationCall) {
        val settings = settingsRepository.current()
        if (!settings.allowUpload) {
            call.respond(HttpStatusCode.Forbidden, ErrorDto("上传功能已关闭"))
            return
        }
        val storage = storageProvider.current(settings)
        val ip = call.request.origin.remoteHost
        var saved: SharedFile? = null

        call.receiveMultipart().forEachPart { part ->
            try {
                if (part is PartData.FileItem && part.name == "file" && saved == null) {
                    val fileName = sanitizeFileName(part.originalFileName)
                        ?: throw IllegalArgumentException("文件名不合法")
                    val declaredSize = part.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                    val taskId = uploadMonitor.start(fileName, declaredSize)
                    var reportedBytes = 0L
                    try {
                        val file = storage.save(
                            fileName = fileName,
                            input = part.provider().toInputStream(),
                            expectedSize = declaredSize,
                            maxBytes = settings.maxFileSizeBytes,
                        ) { received ->
                            uploadMonitor.progress(taskId, received)
                            val delta = received - reportedBytes
                            if (delta > 0) {
                                registry.recordUpload(ip, delta)
                                reportedBytes = received
                            }
                        }
                        saved = file
                        uploadMonitor.complete(taskId, file.sizeBytes)
                    } catch (t: Throwable) {
                        uploadMonitor.fail(taskId, t.message ?: "上传失败")
                        throw t
                    }
                }
            } finally {
                part.dispose()
            }
        }

        val file = saved
        if (file == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorDto("缺少上传文件"))
            return
        }
        eventBus.emit(ServerEvent.FilesChanged)
        call.respond(UploadResponse(file.name, file.sizeBytes))
    }

    private suspend fun handleDownload(call: ApplicationCall) {
        val name = call.parameters["filename"].orEmpty()
        if (!isSafeFileName(name)) {
            call.respond(HttpStatusCode.BadRequest, ErrorDto("非法文件名"))
            return
        }
        val settings = settingsRepository.current()
        val storage = storageProvider.current(settings)
        val input = storage.openRead(name)
        if (input == null) {
            call.respond(HttpStatusCode.NotFound, ErrorDto("文件不存在"))
            return
        }
        val ip = call.request.origin.remoteHost
        call.response.header(HttpHeaders.ContentDisposition, contentDisposition(name))
        call.response.header(HttpHeaders.CacheControl, "no-store")

        call.respondOutputStream(contentType = contentTypeFor(name), status = HttpStatusCode.OK) {
            streamToClient(input, ip)
        }
    }

    private fun OutputStream.streamToClient(input: java.io.InputStream, ip: String) {
        input.use { source ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                write(buffer, 0, read)
                registry.recordDownload(ip, read.toLong())
            }
        }
    }

    private suspend fun serveAsset(call: ApplicationCall, relativePath: String) {
        if (relativePath.isBlank() || relativePath.contains("..")) {
            call.respond(HttpStatusCode.NotFound, ErrorDto("Not Found"))
            return
        }
        val stream = runCatching { context.assets.open("web/$relativePath") }.getOrNull()
        if (stream == null) {
            call.respond(HttpStatusCode.NotFound, ErrorDto("Not Found"))
            return
        }
        call.respondOutputStream(contentType = contentTypeFor(relativePath), status = HttpStatusCode.OK) {
            stream.use { it.copyTo(this) }
        }
    }

    private fun contentTypeFor(path: String): ContentType = when (path.substringAfterLast('.', "").lowercase()) {
        "html" -> ContentType.Text.Html
        "js" -> ContentType.Text.JavaScript
        "css" -> ContentType.Text.CSS
        "json" -> ContentType.Application.Json
        "png" -> ContentType.Image.PNG
        "jpg", "jpeg" -> ContentType.Image.JPEG
        "svg" -> ContentType.Image.SVG
        "ico" -> ContentType.parse("image/x-icon")
        else -> ContentType.Application.OctetStream
    }.let { if (it.contentType.startsWith("text/") || it == ContentType.Text.JavaScript) it.withCharset(Charsets.UTF_8) else it }

    private fun com.fileshare.lan.core.model.DeviceInfo.toDto() = DeviceDto(
        ip = ip,
        type = deviceType.name,
        userAgent = userAgent,
        online = online,
        lastSeen = lastSeenAtMillis / 1000,
    )

    private companion object {
        const val HOST = "0.0.0.0"
        const val BUFFER_SIZE = 64 * 1024
    }
}
