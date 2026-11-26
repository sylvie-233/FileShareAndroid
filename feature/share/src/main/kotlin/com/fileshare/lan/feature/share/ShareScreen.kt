package com.fileshare.lan.feature.share

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fileshare.lan.core.designsystem.component.BannerTone
import com.fileshare.lan.core.designsystem.component.ConfirmDialog
import com.fileshare.lan.core.designsystem.component.InfoBanner
import com.fileshare.lan.core.designsystem.util.copyToClipboard
import com.fileshare.lan.core.model.ServerState
import com.fileshare.lan.feature.share.component.AddressCard
import com.fileshare.lan.feature.share.component.DeviceCard
import com.fileshare.lan.feature.share.component.PowerCard
import com.fileshare.lan.feature.share.component.QrCodeDialog
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShareScreen(
    viewModel: ShareViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showStopConfirm by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val running = state is ServerState.Running
    val starting = state is ServerState.Starting || state is ServerState.Stopping

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        PowerCard(
            state = state,
            metrics = metrics,
            onPowerClick = {
                when {
                    starting -> Unit
                    running -> showStopConfirm = true
                    else -> viewModel.startService()
                }
            },
        )

        when (val current = state) {
            is ServerState.Running -> {
                AddressCard(
                    url = current.info.url,
                    onQrClick = { showQrDialog = true },
                    onCopyClick = {
                        context.copyToClipboard("局域网访问地址", current.info.url)
                        Toast.makeText(context, "访问地址已复制", Toast.LENGTH_SHORT).show()
                    },
                )
                DeviceCard(devices)
                if (!settings.keepAwake) {
                    InfoBanner(
                        text = "提示：共享服务运行中，建议在设置中开启「保持屏幕常亮」，避免息屏后传输中断。",
                        tone = BannerTone.INFO,
                        icon = Icons.Outlined.Info,
                    )
                }
            }

            is ServerState.Starting -> InfoBanner(
                text = "正在获取局域网地址并绑定端口，请稍候…",
                tone = BannerTone.INFO,
                icon = Icons.Outlined.Info,
            )

            is ServerState.Failed -> InfoBanner(
                text = current.message,
                tone = BannerTone.WARN,
                icon = Icons.Outlined.Info,
            )

            else -> InfoBanner(
                text = "服务未运行时，其他设备无法访问共享文件。",
                tone = BannerTone.WARN,
                icon = Icons.Outlined.Info,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showStopConfirm) {
        ConfirmDialog(
            title = "关闭共享服务？",
            message = "关闭后，已连接设备将无法继续访问共享文件。",
            confirmText = "关闭服务",
            onConfirm = {
                showStopConfirm = false
                viewModel.stopService()
            },
            onDismiss = { showStopConfirm = false },
        )
    }

    if (showQrDialog && state is ServerState.Running) {
        QrCodeDialog(
            url = (state as ServerState.Running).info.url,
            onDismiss = { showQrDialog = false },
        )
    }
}
