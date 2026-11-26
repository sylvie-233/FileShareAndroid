package com.fileshare.lan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.fileshare.lan.core.data.settings.SettingsRepository
import com.fileshare.lan.core.designsystem.theme.FileShareTheme
import com.fileshare.lan.core.model.ServerState
import com.fileshare.lan.core.server.ShareController
import com.fileshare.lan.ui.FileShareApp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val controller: ShareController by inject()
    private val settingsRepository: SettingsRepository by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 拒绝时前台服务仍可运行，仅通知不可见 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        setContent {
            FileShareTheme {
                FileShareApp()
            }
        }

        observeKeepScreenOn()
        requestNotificationPermissionIfNeeded()
    }

    /** 服务运行且开启「保持屏幕常亮」时，禁止息屏导致传输中断。 */
    private fun observeKeepScreenOn() {
        lifecycleScope.launch {
            combine(controller.state, settingsRepository.settings) { state, settings ->
                state is ServerState.Running && settings.keepAwake
            }
                .distinctUntilChanged()
                .collect { keepOn ->
                    if (keepOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
