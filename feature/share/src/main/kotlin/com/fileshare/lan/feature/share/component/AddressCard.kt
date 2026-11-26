package com.fileshare.lan.feature.share.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.designsystem.component.AppCard
import com.fileshare.lan.core.designsystem.theme.AppColors

@Composable
internal fun AddressCard(
    url: String,
    onQrClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("局域网访问地址", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(AppColors.SurfaceMuted)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ActionPill(text = "二维码", icon = Icons.Outlined.QrCode2, onClick = onQrClick)
            ActionPill(text = "复制", icon = Icons.Outlined.ContentCopy, onClick = onCopyClick)
        }
    }
}

@Composable
private fun ActionPill(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.Muted, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = AppColors.Ink)
    }
}
