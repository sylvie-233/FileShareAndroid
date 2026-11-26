package com.fileshare.lan.core.server.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fileshare.lan.core.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

/**
 * 开机自启：读取设置 autostart 后启动前台服务（功能设计文档 2.3 节）。
 * 注意：用户从最近任务划掉 App 属于 force-stop，系统不会再派发开机广播。
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settingsRepository = getKoin().get<SettingsRepository>()
                if (settingsRepository.current().autostart) {
                    ShareService.start(context)
                }
            } catch (_: Throwable) {
                // 开机广播失败不阻塞系统，用户仍可手动启动
            } finally {
                pendingResult.finish()
            }
        }
    }
}
