package com.fileshare.lan.feature.share.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fileshare.lan.core.designsystem.theme.AppColors
import com.fileshare.lan.feature.share.generateQrBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun QrCodeDialog(url: String, onDismiss: () -> Unit) {
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.Default) { generateQrBitmap(url) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("扫码访问", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val current = bitmap
                if (current != null) {
                    Image(
                        bitmap = current.asImageBitmap(),
                        contentDescription = "局域网访问地址二维码",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Accent)
                    }
                }
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Muted,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "请确保扫码设备与本机处于同一局域网",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Faint,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = AppColors.Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = AppColors.Surface,
    )
}
