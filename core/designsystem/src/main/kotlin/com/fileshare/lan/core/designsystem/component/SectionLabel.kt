package com.fileshare.lan.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fileshare.lan.core.designsystem.theme.AppColors

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 2.dp, top = 4.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.Faint,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
    )
}
