package com.fileshare.lan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fileshare.lan.core.designsystem.component.StatusChip
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.model.ServerState
import com.fileshare.lan.core.server.ShareController
import com.fileshare.lan.feature.files.FilesScreen
import com.fileshare.lan.feature.settings.SettingsScreen
import com.fileshare.lan.feature.share.ShareScreen
import org.koin.compose.koinInject

private enum class AppTab(val label: String, val icon: ImageVector) {
    SERVICE("服务", Icons.Outlined.PowerSettingsNew),
    FILES("文件", Icons.Outlined.FolderOpen),
    SETTINGS("设置", Icons.Outlined.Settings),
}

@Composable
fun FileShareApp() {
    val controller: ShareController = koinInject()
    val state by controller.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppColors.Background,
        topBar = { AppTopBar(running = state is ServerState.Running) },
        bottomBar = {
            AppBottomBar(selectedIndex = selectedTab, onSelect = { selectedTab = it })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                0 -> ShareScreen()
                1 -> FilesScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun AppTopBar(running: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text("FileShare", style = MaterialTheme.typography.titleLarge, color = AppColors.Ink)
            Text("局域网文件共享", style = MaterialTheme.typography.labelSmall, color = AppColors.Faint)
        }
        StatusChip(running = running)
    }
}

@Composable
private fun AppBottomBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column {
        HorizontalDivider(color = AppColors.Border)
        NavigationBar(
            containerColor = AppColors.Surface,
            tonalElevation = 0.dp,
        ) {
            AppTab.entries.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(22.dp)) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppColors.Accent,
                        selectedTextColor = AppColors.Accent,
                        indicatorColor = AppColors.AccentSoft,
                        unselectedIconColor = AppColors.Muted,
                        unselectedTextColor = AppColors.Muted,
                    ),
                )
            }
        }
    }
}
