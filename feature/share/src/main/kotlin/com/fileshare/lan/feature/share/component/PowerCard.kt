package com.fileshare.lan.feature.share.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.common.formatBytes
import com.fileshare.lan.core.common.formatSpeed
import com.fileshare.lan.core.common.formatUptime
import com.fileshare.lan.core.designsystem.component.AppCard
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.model.ServerMetrics
import com.fileshare.lan.core.model.ServerState

@Composable
internal fun PowerCard(
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
