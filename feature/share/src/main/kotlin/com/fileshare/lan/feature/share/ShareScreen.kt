package com.fileshare.lan.feature.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fileshare.lan.core.common.formatBytes
import com.fileshare.lan.core.common.formatRelativeTime
import com.fileshare.lan.core.common.formatSpeed
import com.fileshare.lan.core.common.formatUptime
import com.fileshare.lan.core.designsystem.component.AppCard
import com.fileshare.lan.core.designsystem.component.ConfirmDialog
import com.fileshare.lan.core.designsystem.component.InfoBanner
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.model.DeviceInfo
import com.fileshare.lan.core.model.DeviceType
import com.fileshare.lan.core.model.ServerInfo
import com.fileshare.lan.core.model.ServerMetrics
import com.fileshare.lan.core.model.ServerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                        copyToClipboard(context, "局域网访问地址", current.info.url)
                        Toast.makeText(context, "访问地址已复制", Toast.LENGTH_SHORT).show()
                    },
                )
                DeviceCard(devices)
                if (!settings.keepAwake) {
                    InfoBanner(
                        text = "提示：共享服务运行中，建议在设置中开启「保持屏幕常亮」，避免息屏后传输中断。",
                        tone = com.fileshare.lan.core.designsystem.component.BannerTone.INFO,
                        icon = Icons.Outlined.Info,
                    )
                }
            }

            is ServerState.Starting -> InfoBanner(
                text = "正在获取局域网地址并绑定端口，请稍候…",
                tone = com.fileshare.lan.core.designsystem.component.BannerTone.INFO,
                icon = Icons.Outlined.Info,
            )

            is ServerState.Failed -> InfoBanner(
                text = current.message,
                tone = com.fileshare.lan.core.designsystem.component.BannerTone.WARN,
                icon = Icons.Outlined.Info,
            )

            else -> InfoBanner(
                text = "服务未运行时，其他设备无法访问共享文件。",
                tone = com.fileshare.lan.core.designsystem.component.BannerTone.WARN,
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

@Composable
private fun PowerCard(
    state: ServerState,
    metrics: ServerMetrics,
    onPowerClick: () -> Unit,
) {
    val running = state is ServerState.Running
    val starting = state is ServerState.Starting || state is ServerState.Stopping

    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(6.dp))
            PowerButton(running = running, starting = starting, onClick = onPowerClick)
            Spacer(Modifier.height(14.dp))
            Text(
                text = when (state) {
                    is ServerState.Running -> "服务运行中"
                    is ServerState.Starting -> "正在启动…"
                    is ServerState.Stopping -> "正在停止…"
                    is ServerState.Failed -> "启动失败"
                    ServerState.Stopped -> "服务未运行"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (running) AppColors.Green else AppColors.Ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (val current = state) {
                    is ServerState.Running -> "局域网设备可通过下方地址访问共享文件"
                    is ServerState.Failed -> current.message
                    else -> "点击启动，共享本机文件给局域网设备"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state is ServerState.Failed) AppColors.Danger else AppColors.Faint,
                textAlign = TextAlign.Center,
            )

            if (running) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = AppColors.Border)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(formatUptime(metrics.uptimeMillis), "运行时长")
                    StatItem(formatSpeed(metrics.totalRateBytesPerSec), "实时速率")
                    StatItem(formatBytes(metrics.totalTransferredBytes), "累计传输")
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Ink,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = AppColors.Faint)
    }
}

@Composable
private fun PowerButton(running: Boolean, starting: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "powerBreath")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (running) 1.16f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = if (running) 0.10f else 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Box(Modifier.size(152.dp), contentAlignment = Alignment.Center) {
        if (running) {
            Box(
                Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = glowAlpha
                    }
                    .clip(CircleShape)
                    .background(AppColors.Green),
            )
        }
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(if (running) AppColors.Green else AppColors.SurfaceMuted)
                .border(
                    width = 1.dp,
                    color = if (running) AppColors.Green else AppColors.BorderStrong,
                    shape = CircleShape,
                )
                .clickable(enabled = !starting, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (starting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = AppColors.Accent,
                    strokeWidth = 3.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = if (running) "关闭服务" else "启动服务",
                    tint = if (running) Color.White else AppColors.Muted,
                    modifier = Modifier.size(46.dp),
                )
            }
        }
    }
}

@Composable
private fun AddressCard(
    url: String,
    onQrClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("局域网访问地址", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(AppColors.SurfaceMuted)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ActionPill(text = "二维码", icon = Icons.Outlined.QrCode2, onClick = onQrClick)
            ActionPill(text = "复制", icon = Icons.Outlined.ContentCopy, onClick = onCopyClick)
        }
    }
}

@Composable
private fun ActionPill(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.Muted, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = AppColors.Ink)
    }
}

@Composable
private fun DeviceCard(devices: List<DeviceInfo>) {
    val onlineCount = devices.count { it.online }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Devices,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("已连接设备", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
            }
            Text(
                text = "$onlineCount 台在线",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Faint,
            )
        }
        Spacer(Modifier.height(6.dp))
        if (devices.isEmpty()) {
            Text(
                text = "暂无设备连接。用其他设备的浏览器打开上方地址即可访问。",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Faint,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        } else {
            devices.forEachIndexed { index, device ->
                if (index > 0) HorizontalDivider(color = AppColors.Border)
                DeviceRow(device)
            }
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(AppColors.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = deviceIcon(device.deviceType),
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(device.ip, style = MaterialTheme.typography.bodyMedium, color = AppColors.Ink)
            Text(
                text = device.deviceType.displayName + " · " + formatRelativeTime(device.lastSeenAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Faint,
            )
        }
        Text(
            text = if (device.online) "在线" else "离线",
            style = MaterialTheme.typography.labelSmall,
            color = if (device.online) AppColors.Green else AppColors.Faint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun deviceIcon(type: DeviceType): ImageVector = when (type) {
    DeviceType.ANDROID, DeviceType.IPHONE -> Icons.Outlined.PhoneAndroid
    DeviceType.IPAD -> Icons.Outlined.TabletAndroid
    DeviceType.WINDOWS, DeviceType.MAC -> Icons.Outlined.Computer
    DeviceType.OTHER -> Icons.Outlined.Language
}

@Composable
private fun QrCodeDialog(url: String, onDismiss: () -> Unit) {
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.Default) { generateQrBitmap(url) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("扫码访问", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val current = bitmap
                if (current != null) {
                    Image(
                        bitmap = current.asImageBitmap(),
                        contentDescription = "局域网访问地址二维码",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Accent)
                    }
                }
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Muted,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "请确保扫码设备与本机处于同一局域网",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Faint,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = AppColors.Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = AppColors.Surface,
    )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}
