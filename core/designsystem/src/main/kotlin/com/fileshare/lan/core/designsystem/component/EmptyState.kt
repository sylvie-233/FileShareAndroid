package com.fileshare.lan.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.designsystem.theme.AppColors

@Composable
fun EmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = AppColors.BorderStrong, modifier = Modifier.size(40.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Muted)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = AppColors.Faint, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            action()
        }
    }
}
