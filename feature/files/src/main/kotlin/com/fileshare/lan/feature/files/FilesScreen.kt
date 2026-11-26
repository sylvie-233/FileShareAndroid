package com.fileshare.lan.feature.files

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fileshare.lan.core.designsystem.component.BannerTone
import com.fileshare.lan.core.designsystem.component.ConfirmDialog
import com.fileshare.lan.core.designsystem.component.EmptyState
import com.fileshare.lan.core.designsystem.component.InfoBanner
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.designsystem.util.copyToClipboard
import com.fileshare.lan.core.model.SharedFile
import com.fileshare.lan.feature.files.component.FileRow
import com.fileshare.lan.feature.files.component.FilesPager
import com.fileshare.lan.feature.files.component.SearchField
import com.fileshare.lan.feature.files.component.ToolbarButton
import com.fileshare.lan.feature.files.component.UploadPanel
import com.fileshare.lan.feature.files.component.VerticalScrollIndicator
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
                                        context.copyToClipboard("文件下载链接", url)
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
            FilesPager(
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
