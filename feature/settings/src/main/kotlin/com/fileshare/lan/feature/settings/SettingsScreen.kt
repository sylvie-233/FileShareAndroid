package com.fileshare.lan.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fileshare.lan.core.designsystem.component.AppCard
import com.fileshare.lan.core.designsystem.component.SectionLabel
import com.fileshare.lan.core.designsystem.component.SegmentedOptions
import com.fileshare.lan.core.designsystem.component.SettingSwitchRow
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.model.AppSettings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saveDirLabel by viewModel.saveDirLabel.collectAsStateWithLifecycle()
    val running by viewModel.serviceRunning.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var permissionTick by remember { mutableIntStateOf(0) }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { permissionTick++ }

    val dirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.setSaveDir(uri.toString())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val batteryOptimizationIgnored = remember(permissionTick) {
        (context.getSystemService(PowerManager::class.java))
            .isIgnoringBatteryOptimizations(context.packageName)
    }
    val allFilesAccess = remember(permissionTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        SectionLabel("服务")
        AppCard {
            SettingRowWithControl(
                title = "服务端口",
                description = if (running) "运行中修改将自动重启服务" else "重启服务后生效",
            ) {
                SegmentedOptions(
                    options = AppSettings.PORT_OPTIONS,
                    selected = settings.port,
                    label = { it.toString() },
                    onSelect = viewModel::setPort,
                )
            }
            RowDivider()
            SettingSwitchRow(
                title = "开机自动启动服务",
                description = "设备开机后自动开启共享",
                checked = settings.autostart,
                onCheckedChange = viewModel::setAutostart,
            )
            RowDivider()
            SettingSwitchRow(
                title = "共享时保持屏幕常亮",
                description = "避免传输中息屏断开",
                checked = settings.keepAwake,
                onCheckedChange = viewModel::setKeepAwake,
            )
        }

        SectionLabel("传输", modifier = Modifier.padding(top = 8.dp))
        AppCard {
            SettingRowWithControl(
                title = "单文件大小上限",
                description = "超出将拒绝上传",
            ) {
                SegmentedOptions(
                    options = AppSettings.MAX_FILE_SIZE_OPTIONS,
                    selected = settings.maxFileSizeBytes,
                    label = { size ->
                        when (size) {
                            AppSettings.UNLIMITED_FILE_SIZE -> "不限"
                            1024L * 1024 * 1024 -> "1 GB"
                            else -> "${size / 1024 / 1024} MB"
                        }
                    },
                    onSelect = viewModel::setMaxFileSize,
                )
            }
            RowDivider()
            SettingSwitchRow(
                title = "允许其他设备上传",
                description = "关闭后仅允许下载",
                checked = settings.allowUpload,
                onCheckedChange = viewModel::setAllowUpload,
            )
            RowDivider()
            ValueRow(
                title = "保存目录",
                description = saveDirLabel.ifBlank { "Download/FileShare" },
                hint = "点击修改，需系统文件夹授权",
                onClick = { dirLauncher.launch(null) },
            )
            if (settings.saveDirUri != null) {
                RowDivider()
                Text(
                    text = "恢复默认保存目录",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .clickable { viewModel.setSaveDir(null) }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                )
            }
        }

        SectionLabel("系统权限", modifier = Modifier.padding(top = 8.dp))
        AppCard {
            PermissionRow(
                title = "所有文件访问",
                description = if (allFilesAccess) {
                    "已授予，可直接写入 Download/FileShare"
                } else {
                    "未授予，文件将保存到应用专属目录"
                },
                granted = allFilesAccess,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        )
                        runCatching { settingsLauncher.launch(intent) }
                            .onFailure {
                                settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            }
                    } else {
                        viewModel.notify("当前系统版本无需该权限")
                    }
                },
            )
            RowDivider()
            PermissionRow(
                title = "忽略电池优化",
                description = if (batteryOptimizationIgnored) {
                    "已加入白名单，后台更稳定"
                } else {
                    "建议加入白名单，防止服务被系统杀死"
                },
                granted = batteryOptimizationIgnored,
                onClick = {
                    val request = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    )
                    runCatching { settingsLauncher.launch(request) }
                        .onFailure {
                            settingsLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                },
            )
        }

        Text(
            text = "数据仅存于本机，不经任何第三方服务器\nFileShare v1.0.0 · MIT License",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Faint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, bottom = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = AppColors.Border,
        modifier = Modifier.padding(vertical = 13.dp),
    )
}

@Composable
private fun SettingRowWithControl(
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
private fun ValueRow(
    title: String,
    description: String,
    hint: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.labelSmall, color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
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
