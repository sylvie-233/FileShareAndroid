package com.fileshare.lan.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `format bytes uses binary units`() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
        assertEquals("1.00 KB", formatBytes(1024))
        assertEquals("1.50 MB", formatBytes(1536 * 1024L))
    }

    @Test
    fun `format uptime shows minutes or hours`() {
        assertEquals("00:05", formatUptime(5_000))
        assertEquals("01:05", formatUptime(65_000))
        assertEquals("01:00:05", formatUptime(3_605_000))
    }

    @Test
    fun `format speed returns dash when idle`() {
        assertEquals("—", formatSpeed(0))
        assertEquals("1.00 KB/s", formatSpeed(1024))
    }

    @Test
    fun `relative time buckets`() {
        val now = 1_700_000_000_000L
        assertEquals("刚刚", formatRelativeTime(now - 10_000, now))
        assertEquals("3 分钟前", formatRelativeTime(now - 3 * 60_000, now))
        assertEquals("2 小时前", formatRelativeTime(now - 2 * 3_600_000, now))
        assertEquals("3 天前", formatRelativeTime(now - 3 * 86_400_000, now))
    }
}
