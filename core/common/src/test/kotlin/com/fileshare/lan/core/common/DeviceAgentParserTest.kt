package com.fileshare.lan.core.common

import com.fileshare.lan.core.model.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceAgentParserTest {

    @Test
    fun `parses common user agents`() {
        assertEquals(
            DeviceType.ANDROID,
            deviceTypeFromUserAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36"),
        )
        assertEquals(
            DeviceType.IPHONE,
            deviceTypeFromUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)"),
        )
        assertEquals(
            DeviceType.IPAD,
            deviceTypeFromUserAgent("Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X)"),
        )
        assertEquals(
            DeviceType.WINDOWS,
            deviceTypeFromUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"),
        )
        assertEquals(
            DeviceType.MAC,
            deviceTypeFromUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 13_5)"),
        )
        assertEquals(DeviceType.OTHER, deviceTypeFromUserAgent(null))
    }
}
