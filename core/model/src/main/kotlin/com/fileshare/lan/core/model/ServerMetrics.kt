package com.fileshare.lan.core.model

/** 服务运行指标，每秒刷新一次。 */
data class ServerMetrics(
    val uptimeMillis: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val downloadBytesPerSec: Long = 0L,
    val totalTransferredBytes: Long = 0L,
) {
    val totalRateBytesPerSec: Long
        get() = uploadBytesPerSec + downloadBytesPerSec
}
