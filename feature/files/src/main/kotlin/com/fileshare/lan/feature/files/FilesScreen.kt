package com.fileshare.lan.feature.files

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fileshare.lan.core.common.FileTone
import com.fileshare.lan.core.common.fileBadgeLabel
import com.fileshare.lan.core.common.fileTone
import com.fileshare.lan.core.common.formatBytes
import com.fileshare.lan.core.common.formatRelativeTime
import com.fileshare.lan.core.designsystem.component.BannerTone
import com.fileshare.lan.core.designsystem.component.ConfirmDialog
import com.fileshare.lan.core.designsystem.component.EmptyState
import com.fileshare.lan.core.designsystem.component.InfoBanner
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.model.SharedFile
import com.fileshare.lan.core.model.UploadTask
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FilesScreen(
    viewModel: FilesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingDelete by remember { mutableStateOf<SharedFile?>(null) }
    var pendingExport by remember { mutableStateOf<SharedFile?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.page, state.query) {
        listState.scrollToItem(0)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importDocument) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val file = pendingExport
        pendingExport = null
        if (uri != null && file != null) viewModel.exportFile(file, uri)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                onClear = viewModel::clearQuery,
                modifier = Modifier.weight(1f),
            )
            ToolbarButton(
                text = "上传",
                icon = Icons.Outlined.UploadFile,
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )
        }

        if (state.uploads.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            UploadPanel(state.uploads)
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "共 ${state.totalCount} 个文件",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Muted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "清空全部",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.totalCount > 0) AppColors.Danger else AppColors.Faint,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = state.totalCount > 0) { showClearConfirm = true }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        if (state.storageError != null) {
            Spacer(Modifier.height(8.dp))
            InfoBanner(text = state.storageError.orEmpty(), tone = BannerTone.WARN)
        }

        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = AppColors.Surface,
            border = BorderStroke(1.dp, AppColors.Border),
        ) {
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Accent, strokeWidth = 2.5.dp)
                }
            } else if (state.files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.query.isBlank()) {
                        EmptyState(
                            title = "还没有文件",
                            hint = "其他设备可通过浏览器上传，也可以点击上方「上传」添加本机文件。",
                            icon = Icons.Outlined.FolderOpen,
                        )
                    } else {
                        EmptyState(
                            title = "没有找到匹配的文件",
                            hint = "换个关键词试试。",
                            icon = Icons.Outlined.Search,
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(state.files, key = { _, file -> file.name }) { index, file ->
                            FileRow(
                                file = file,
                                onDownload = {
                                    pendingExport = file
                                    exportLauncher.launch(file.name)
                                },
                                onCopyLink = {
                                    val url = viewModel.downloadUrlFor(file)
                                    if (url == null) {
                                        Toast.makeText(context, "请先启动共享服务", Toast.LENGTH_SHORT).show()
                                    } else {
                                        copyToClipboard(context, "文件下载链接", url)
                                        Toast.makeText(context, "文件链接已复制", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = { pendingDelete = file },
                            )
                            if (index < state.files.lastIndex) {
                                HorizontalDivider(color = AppColors.Border)
                            }
                        }
                    }
                    VerticalScrollIndicator(
                        listState = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(8.dp),
                    )
                }
            }
        }

        if (state.pageCount > 1) {
            PagerBar(
                page = state.page,
                pageCount = state.pageCount,
                onPageChange = viewModel::onPageChange,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    val deleting = pendingDelete
    if (deleting != null) {
        ConfirmDialog(
            title = "删除文件？",
            message = "确定要删除「${deleting.name}」吗？此操作不可恢复。",
            confirmText = "删除",
            onConfirm = {
                viewModel.deleteFile(deleting)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "清空全部文件？",
            message = "将删除共享目录中的 ${state.totalCount} 个文件，此操作不可恢复。",
            confirmText = "全部清空",
            onConfirm = {
                showClearConfirm = false
                viewModel.clearAllFiles()
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, if (focused) AppColors.Accent else AppColors.Border, shape)
            .padding(start = 12.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = AppColors.Faint,
            modifier = Modifier.size(16.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = "搜索文件名…",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Faint,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(AppColors.Accent),
                textStyle = TextStyle(fontSize = 13.5.sp, color = AppColors.Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "清除搜索",
                    tint = AppColors.Faint,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Ink,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.Ink,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 列表右侧的竖向滚动条，仅在内容超出视口时显示。 */
@Composable
private fun VerticalScrollIndicator(
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

@Composable
private fun UploadPanel(uploads: List<UploadTask>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Border),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            uploads.forEachIndexed { index, task ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = AppColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (task.totalBytes > 0) "${(task.progress * 100).toInt()}%" else "上传中…",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.Accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(7.dp))
                if (task.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { task.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = AppColors.Accent,
                        trackColor = AppColors.SurfaceMuted,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = AppColors.Accent,
                        trackColor = AppColors.SurfaceMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRow(
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

@Composable
private fun PagerBar(page: Int, pageCount: Int, onPageChange: (Int) -> Unit) {
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

private fun toneColors(tone: FileTone): Pair<Color, Color> = when (tone) {
    FileTone.DOC -> Color(0xFFA3702A) to Color(0xFFFDF3E3)
    FileTone.IMAGE -> Color(0xFF3869B8) to Color(0xFFE9F2FD)
    FileTone.VIDEO, FileTone.AUDIO -> Color(0xFF6F4CC0) to Color(0xFFF1ECFA)
    FileTone.ARCHIVE -> Color(0xFF7A6449) to Color(0xFFEEE9E2)
    FileTone.APK -> Color(0xFF128A70) to Color(0xFFE8F7F3)
    FileTone.CODE -> Color(0xFF2C8A63) to Color(0xFFE6F5EE)
    FileTone.OTHER -> AppColors.Muted to AppColors.SurfaceMuted
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}
