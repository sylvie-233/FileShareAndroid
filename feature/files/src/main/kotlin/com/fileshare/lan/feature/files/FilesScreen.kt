package com.fileshare.lan.feature.files

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    var showClearConfirm by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importDocument) }

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
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("搜索文件名…", style = MaterialTheme.typography.bodyMedium, color = AppColors.Faint)
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = AppColors.Faint, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Outlined.Close, contentDescription = "清除搜索", tint = AppColors.Faint, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Accent,
                    unfocusedBorderColor = AppColors.Border,
                    focusedContainerColor = AppColors.Surface,
                    unfocusedContainerColor = AppColors.Surface,
                    cursorColor = AppColors.Accent,
                ),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Accent)
                    .clickable { importLauncher.launch(arrayOf("*/*")) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                Text("导入", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }

        if (state.uploads.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            UploadPanel(state.uploads)
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "共 ${state.totalCount} 个文件",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Muted,
                )
                if (state.saveDirLabel.isNotBlank()) {
                    Text(
                        text = "保存至 ${state.saveDirLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Faint,
                    )
                }
            }
            Text(
                text = "清空全部",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.totalCount > 0) AppColors.Danger else AppColors.Faint,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = state.totalCount > 0) { showClearConfirm = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
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
                            hint = "其他设备可通过浏览器上传，也可以点击上方「导入」添加本机文件。",
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(state.files, key = { _, file -> file.name }) { index, file ->
                        FileRow(
                            file = file,
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
                            HorizontalDivider(color = AppColors.Border, modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
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
        Spacer(Modifier.height(10.dp))
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
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (task.totalBytes > 0) "${(task.progress * 100).toInt()}%" else "上传中…",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (task.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { task.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AppColors.Accent,
                        trackColor = AppColors.SurfaceMuted,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
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
    onCopyLink: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        val tone = fileTone(file.name)
        val (foreground, background) = toneColors(tone)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fileBadgeLabel(file.name),
                style = MaterialTheme.typography.labelSmall,
                color = foreground,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${formatBytes(file.sizeBytes)} · ${formatRelativeTime(file.modifiedAtMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Faint,
            )
        }
        IconButton(onClick = onCopyLink, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "复制下载链接",
                tint = AppColors.Muted,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "删除文件",
                tint = AppColors.Danger,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PagerBar(page: Int, pageCount: Int, onPageChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PagerButton(enabled = page > 1, onClick = { onPageChange(page - 1) }) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "上一页", modifier = Modifier.size(16.dp))
            Text("上一页", style = MaterialTheme.typography.labelMedium)
        }
        Text(
            text = "第 $page / $pageCount 页",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.Muted,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        PagerButton(enabled = page < pageCount, onClick = { onPageChange(page + 1) }) {
            Text("下一页", style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Outlined.ChevronRight, contentDescription = "下一页", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PagerButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (enabled) AppColors.Muted else AppColors.BorderStrong,
        ) {
            content()
        }
    }
}

private fun toneColors(tone: FileTone): Pair<Color, Color> = when (tone) {
    FileTone.DOC -> AppColors.Accent to AppColors.AccentSoft
    FileTone.IMAGE -> AppColors.Green to AppColors.GreenSoft
    FileTone.VIDEO -> AppColors.Amber to AppColors.AmberSoft
    FileTone.AUDIO -> Color(0xFF7C5CE0) to Color(0xFFF0ECFD)
    FileTone.ARCHIVE -> Color(0xFF5B6B7F) to AppColors.SurfaceMuted
    FileTone.CODE -> Color(0xFF0E8A8A) to Color(0xFFE4F4F4)
    FileTone.OTHER -> AppColors.Muted to AppColors.SurfaceMuted
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}
