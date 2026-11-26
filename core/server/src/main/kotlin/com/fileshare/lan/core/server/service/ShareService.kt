package com.fileshare.lan.core.server.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fileshare.lan.core.model.ServerState
import com.fileshare.lan.core.server.R
import com.fileshare.lan.core.server.ShareController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 前台服务：承载共享服务的生命周期，并展示常驻通知（功能设计文档 2.2 节）。
 */
class ShareService : Service(), KoinComponent {

    private val controller: ShareController by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var everStarted = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observerJob = scope.launch {
            controller.state.collect { state ->
                when (state) {
                    is ServerState.Starting -> {
                        everStarted = true
                        updateNotification("正在启动共享服务…")
                    }

                    is ServerState.Running -> {
                        everStarted = true
                        updateNotification("共享服务运行中 · ${state.info.url}")
                    }

                    is ServerState.Stopping -> updateNotification("正在停止共享服务…")

                    is ServerState.Failed -> {
                        everStarted = true
                        updateNotification("启动失败：${state.message}")
                        scope.launch {
                            delay(2_000)
                            // 用户可能已重新点击启动，失败状态已变化时不要误停服务
                            if (controller.state.value is ServerState.Failed) stopSelf()
                        }
                    }

                    ServerState.Stopped -> if (everStarted) stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                everStarted = true
                enterForeground("正在停止共享服务…")
                scope.launch {
                    controller.stopServerSuspend()
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            else -> {
                if (!enterForeground("正在启动共享服务…")) {
                    // Android 12+ 在后台受限场景可能拒绝 startForeground，避免直接崩溃
                    stopSelf()
                    return START_NOT_STICKY
                }
                everStarted = true
                scope.launch { controller.startServer() }
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        observerJob?.cancel()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        runCatching { controller.stopServer() }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "共享服务",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "FileShare 局域网共享服务的运行状态"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun enterForeground(text: String): Boolean = runCatching {
        startForeground(NOTIFICATION_ID, buildNotification(text))
    }.isSuccess

    private fun updateNotification(text: String) {
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(text))
        }
    }

    private fun buildNotification(text: String): android.app.Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ShareService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_share)
            .setContentTitle("FileShare · 局域网文件共享")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "停止服务", stopIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "share_service"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.fileshare.lan.action.START_SHARE"
        const val ACTION_STOP = "com.fileshare.lan.action.STOP_SHARE"

        fun start(context: Context) {
            val intent = Intent(context, ShareService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ShareService::class.java).setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
        }
    }
}
