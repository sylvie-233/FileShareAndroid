package com.fileshare.lan.feature.share.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.common.formatRelativeTime
import com.fileshare.lan.core.designsystem.component.AppCard
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.model.DeviceInfo
import com.fileshare.lan.core.model.DeviceType

@Composable
internal fun DeviceCard(devices: List<DeviceInfo>) {
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
