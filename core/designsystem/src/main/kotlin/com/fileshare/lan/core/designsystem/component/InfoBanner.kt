package com.fileshare.lan.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fileshare.lan.core.designsystem.theme.AppColors

enum class BannerTone { WARN, INFO, SUCCESS }

@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.WARN,
    icon: ImageVector? = null,
) {
    val background: Color
    val foreground: Color
    when (tone) {
        BannerTone.WARN -> {
            background = AppColors.AmberSoft
            foreground = AppColors.Amber
        }
        BannerTone.INFO -> {
            background = AppColors.AccentSoft
            foreground = AppColors.Accent
        }
        BannerTone.SUCCESS -> {
            background = AppColors.GreenSoft
            foreground = AppColors.Green
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(17.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = foreground,
            lineHeight = 19.sp,
        )
    }
}
