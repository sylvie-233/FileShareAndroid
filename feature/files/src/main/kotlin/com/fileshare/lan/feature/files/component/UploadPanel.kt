package com.fileshare.lan.feature.files.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.core.model.UploadTask

@Composable
internal fun UploadPanel(uploads: List<UploadTask>) {
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
