package com.fileshare.lan.feature.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.designsystem.theme.AppColors

@Composable
internal fun RowDivider() {
    HorizontalDivider(
        color = AppColors.Border,
        modifier = Modifier.padding(vertical = 13.dp),
    )
}

@Composable
internal fun SettingRowWithControl(
    title: String,
    description: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.labelSmall, color = AppColors.Faint)
        }
        control()
    }
}

@Composable
internal fun ValueRow(
    title: String,
    description: String,
    hint: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(hint, style = MaterialTheme.typography.labelSmall, color = AppColors.Faint)
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AppColors.Faint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.labelSmall, color = AppColors.Faint)
        }
        Text(
            text = if (granted) "已授予" else "去设置",
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) AppColors.Green else AppColors.Accent,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AppColors.Faint,
            modifier = Modifier.size(18.dp),
        )
    }
}
