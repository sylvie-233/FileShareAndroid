package com.fileshare.lan.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.designsystem.theme.AppColors

/** 分段选择器（端口、文件大小上限等）。 */
@Composable
fun <T> SegmentedOptions(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(AppColors.SurfaceMuted)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) AppColors.Surface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) AppColors.Ink else AppColors.Muted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}
