package com.fileshare.lan.core.common

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 获取本机局域网 IPv4 地址。
 * 优先 wlan / ap（热点）/ eth 网段，排除回环、链路本地与虚拟网卡。
 */
object NetworkUtils {

    private val PREFERRED_PREFIXES = listOf("wlan", "ap", "eth", "en", "wl")
    private val IGNORED_PREFIXES = listOf("lo", "dummy", "tun", "rmnet", "p2p", "usb", "v4-")

    fun lanIpv4Address(): String? {
        val candidates = mutableListOf<Pair<Int, String>>()
        runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().forEach { nic ->
                if (!nic.isUp || nic.isLoopback || nic.isVirtual) return@forEach
                val name = nic.name.lowercase()
                if (IGNORED_PREFIXES.any { name.startsWith(it) }) return@forEach
                nic.inetAddresses.toList().forEach { address ->
                    if (address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress) {
                        val ip = address.hostAddress ?: return@forEach
                        val priority = PREFERRED_PREFIXES.indexOfFirst { name.startsWith(it) }
                        candidates += (if (priority >= 0) priority else PREFERRED_PREFIXES.size) to ip
                    }
                }
            }
        }
        return candidates.minByOrNull { it.first }?.second
    }
}
