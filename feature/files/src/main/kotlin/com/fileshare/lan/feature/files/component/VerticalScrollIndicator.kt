package com.fileshare.lan.feature.files.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.designsystem.theme.AppColors

/** 列表右侧的竖向滚动条，仅在内容超出视口时显示。 */
@Composable
internal fun VerticalScrollIndicator(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems == 0 || visibleItems.isEmpty() || visibleItems.size >= totalItems) return

    val alpha by animateFloatAsState(
        targetValue = if (listState.isScrollInProgress) 1f else 0.85f,
        label = "scrollbarAlpha",
    )

    Canvas(modifier) {
        val fraction = (visibleItems.size.toFloat() / totalItems).coerceIn(0.08f, 1f)
        val thumbHeight = (size.height * fraction).coerceAtLeast(24.dp.toPx())
        val first = visibleItems.first()
        val itemSize = first.size.coerceAtLeast(1)
        val scrolled = listState.firstVisibleItemIndex +
            listState.firstVisibleItemScrollOffset.toFloat() / itemSize
        val scrollableItems = (totalItems - visibleItems.size).coerceAtLeast(1)
        val scrollFraction = (scrolled / scrollableItems).coerceIn(0f, 1f)
        val travel = (size.height - thumbHeight).coerceAtLeast(0f)
        val top = travel * scrollFraction

        val thumbWidth = 3.dp.toPx()
        val inset = 3.dp.toPx()
        drawRoundRect(
            color = AppColors.BorderStrong.copy(alpha = alpha),
            topLeft = Offset(size.width - thumbWidth - inset, top),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbWidth / 2, thumbWidth / 2),
        )
    }
}
