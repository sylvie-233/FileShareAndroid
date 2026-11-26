package com.fileshare.lan.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.designsystem.theme.AppColors

@Composable
fun StatusChip(running: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (running) AppColors.GreenSoft else AppColors.SurfaceMuted)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (running) AppColors.Green else AppColors.Faint),
        )
        Text(
            text = if (running) "运行中" else "未运行",
            style = MaterialTheme.typography.labelMedium,
            color = if (running) AppColors.Green else AppColors.Muted,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
