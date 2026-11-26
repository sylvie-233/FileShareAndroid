package com.fileshare.lan.core.designsystem.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** 把纯文本写入系统剪贴板。 */
fun Context.copyToClipboard(label: String, text: String) {
    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}
