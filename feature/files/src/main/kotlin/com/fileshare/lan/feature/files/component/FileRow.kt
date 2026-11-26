package com.fileshare.lan.feature.files.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fileshare.lan.core.common.FileTone
import com.fileshare.lan.core.common.fileBadgeLabel
import com.fileshare.lan.core.common.fileTone
import com.fileshare.lan.core.common.formatBytes
import com.fileshare.lan.core.common.formatRelativeTime
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.model.SharedFile

@Composable
internal fun FileRow(
    file: SharedFile,
    onDownload: () -> Unit,
    onCopyLink: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val (foreground, background) = toneColors(fileTone(file.name))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fileBadgeLabel(file.name),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.4.sp,
                ),
                color = foreground,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = AppColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = "${formatBytes(file.sizeBytes)} · ${formatRelativeTime(file.modifiedAtMillis)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = AppColors.Faint,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            RowAction(
                icon = Icons.Outlined.Download,
                contentDescription = "下载文件",
                onClick = onDownload,
            )
            RowAction(
                icon = Icons.Outlined.ContentCopy,
                contentDescription = "复制下载链接",
                onClick = onCopyLink,
            )
            RowAction(
                icon = Icons.Outlined.Delete,
                contentDescription = "删除文件",
                onClick = onDelete,
                tint = AppColors.Danger,
            )
        }
    }
}

@Composable
private fun RowAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = AppColors.Muted,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
    }
}

private fun toneColors(tone: FileTone): Pair<Color, Color> = when (tone) {
    FileTone.DOC -> Color(0xFFA3702A) to Color(0xFFFDF3E3)
    FileTone.IMAGE -> Color(0xFF3869B8) to Color(0xFFE9F2FD)
    FileTone.VIDEO, FileTone.AUDIO -> Color(0xFF6F4CC0) to Color(0xFFF1ECFA)
    FileTone.ARCHIVE -> Color(0xFF7A6449) to Color(0xFFEEE9E2)
    FileTone.APK -> Color(0xFF128A70) to Color(0xFFE8F7F3)
    FileTone.CODE -> Color(0xFF2C8A63) to Color(0xFFE6F5EE)
    FileTone.OTHER -> AppColors.Muted to AppColors.SurfaceMuted
}
