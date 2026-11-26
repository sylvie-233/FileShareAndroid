package com.fileshare.lan.core.server

import android.content.Context
import com.fileshare.lan.core.common.NetworkUtils
import com.fileshare.lan.core.data.settings.SettingsRepository
import com.fileshare.lan.core.data.storage.StorageProvider
import com.fileshare.lan.core.model.ServerEvent
import com.fileshare.lan.core.model.ServerInfo
import com.fileshare.lan.core.model.ServerMetrics
import com.fileshare.lan.core.model.ServerState
import com.fileshare.lan.core.server.http.ShareServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.BindException
import java.net.SocketException

/**
 * 共享服务的总控：承载状态机、指标、设备列表与上传任务，
 * 由 UI（[com.fileshare.lan.core.server.service.ShareService]）调用启停。
 */
class ShareController(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val storageProvider: StorageProvider,
    private val registry: DeviceRegistry,
    private val eventBus: ServerEventBus,
    private val uploadMonitor: UploadMonitor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<ServerState>(ServerState.Stopped)
    val state: StateFlow<ServerState> = _state.asStateFlow()

    private val _metrics = MutableStateFlow(ServerMetrics())
    val metrics: StateFlow<ServerMetrics> = _metrics.asStateFlow()

    val devices: StateFlow<List<com.fileshare.lan.core.model.DeviceInfo>> = registry.devices
    val uploads: StateFlow<List<com.fileshare.lan.core.model.UploadTask>> = uploadMonitor.tasks
    val events: SharedFlow<ServerEvent> = eventBus.events

    private var server: ShareServer? = null
    private var ticker: Job? = null
    private var startedAtMillis = 0L

    /** 启动服务，返回成功时的 [ServerInfo]。 */
    suspend fun startServer(): Result<ServerInfo> = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current is ServerState.Running || current is ServerState.Starting) {
            return@withContext when (current) {
                is ServerState.Running -> Result.success<ServerInfo>(current.info)
                else -> Result.failure<ServerInfo>(IllegalStateException("服务正在启动"))
            }
        }

        _state.value = ServerState.Starting
        val settings = settingsRepository.current()
        val ip = NetworkUtils.lanIpv4Address() ?: run {
            val message = "请先连接 Wi-Fi 或开启热点"
            _state.value = ServerState.Failed(message)
            return@withContext Result.failure<ServerInfo>(IllegalStateException(message))
        }

        val lanIpProvider = { NetworkUtils.lanIpv4Address() ?: ip }
        val newServer = ShareServer(
            context = context,
            settingsRepository = settingsRepository,
            storageProvider = storageProvider,
            registry = registry,
            eventBus = eventBus,
            uploadMonitor = uploadMonitor,
            lanIpProvider = lanIpProvider,
        )

        try {
            withTimeout(START_TIMEOUT_MS) { newServer.start(settings.port) }
        } catch (t: Throwable) {
            runCatching { newServer.stop() }
            val message = startErrorMessage(t, settings.port)
            _state.value = ServerState.Failed(message)
            return@withContext Result.failure<ServerInfo>(t)
        }

        server = newServer
        startedAtMillis = System.currentTimeMillis()
        registry.reset()
        val info = ServerInfo(url = "http://$ip:${settings.port}", ip = ip, port = settings.port)
        _state.value = ServerState.Running(info)
        startMetricsTicker()
        eventBus.emit(ServerEvent.FilesChanged)
        Result.success(info)
    }

    /** 异步停止，供 Service 的 onDestroy 等非挂起上下文调用。 */
    fun stopServer() {
        scope.launch { stopServerSuspend() }
    }

    suspend fun stopServerSuspend() {
        val current = _state.value
        if (current is ServerState.Stopped || current is ServerState.Stopping) return
        _state.value = ServerState.Stopping
        ticker?.cancel()
        ticker = null
        server?.stop()
        server = null
        registry.reset()
        uploadMonitor.clearAll()
        _metrics.value = ServerMetrics()
        _state.value = ServerState.Stopped
    }

    /** 设置页修改端口后调用：运行中则重启服务。 */
    fun restartIfRunning() {
        if (_state.value !is ServerState.Running) return
        scope.launch {
            stopServerSuspend()
            startServer()
        }
    }

    private fun startMetricsTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            var lastUpload = registry.totalUploadBytes()
            var lastDownload = registry.totalDownloadBytes()
            while (isActive) {
                delay(METRICS_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val upload = registry.totalUploadBytes()
                val download = registry.totalDownloadBytes()
                _metrics.value = ServerMetrics(
                    uptimeMillis = now - startedAtMillis,
                    uploadBytesPerSec = (upload - lastUpload).coerceAtLeast(0),
                    downloadBytesPerSec = (download - lastDownload).coerceAtLeast(0),
                    totalTransferredBytes = upload + download,
                )
                lastUpload = upload
                lastDownload = download
                registry.refresh(now)
            }
        }
    }

    private fun startErrorMessage(error: Throwable, port: Int): String = when {
        error is TimeoutCancellationException -> "启动超时，请检查端口 $port 是否被占用"
        error is BindException || error.cause is BindException -> "端口 $port 被占用，请更换端口"
        error is SocketException -> "端口 $port 绑定失败：${error.message ?: "未知错误"}"
        else -> error.message ?: "服务启动失败"
    }

    private companion object {
        const val START_TIMEOUT_MS = 5_000L
        const val METRICS_INTERVAL_MS = 1_000L
    }
}
