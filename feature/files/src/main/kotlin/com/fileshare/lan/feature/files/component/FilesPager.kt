package com.fileshare.lan.feature.files.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fileshare.lan.core.designsystem.theme.AppColors

@Composable
internal fun FilesPager(page: Int, pageCount: Int, onPageChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PagerArrow(
            icon = Icons.Outlined.ChevronLeft,
            contentDescription = "上一页",
            enabled = page > 1,
            onClick = { onPageChange(page - 1) },
        )
        pageEntries(page, pageCount).forEach { number ->
            if (number == null) {
                Text(
                    text = "…",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Faint,
                )
            } else {
                PagerNumber(
                    number = number,
                    current = number == page,
                    onClick = { onPageChange(number) },
                )
            }
        }
        PagerArrow(
            icon = Icons.Outlined.ChevronRight,
            contentDescription = "下一页",
            enabled = page < pageCount,
            onClick = { onPageChange(page + 1) },
        )
    }
}

@Composable
private fun PagerArrow(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) AppColors.Muted else AppColors.BorderStrong,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun PagerNumber(
    number: Int,
    current: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .background(if (current) AppColors.Accent else AppColors.Surface)
            .border(1.dp, if (current) AppColors.Accent else AppColors.Border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
            color = if (current) Color.White else AppColors.Ink,
            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** 页码序列：页数较多时保留首尾与当前页附近的窗口，中间用 null 表示省略号。 */
private fun pageEntries(page: Int, pageCount: Int): List<Int?> {
    if (pageCount <= 7) return (1..pageCount).toList()
    return buildList {
        add(1)
        if (page > 4) add(null)
        val start = (page - 1).coerceAtLeast(2)
        val end = (page + 1).coerceAtMost(pageCount - 1)
        for (p in start..end) add(p)
        if (end < pageCount - 1) add(null)
        add(pageCount)
    }
}
