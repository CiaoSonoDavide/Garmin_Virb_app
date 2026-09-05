package com.example.garmin_virb_app.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

object CameraDiscovery {

    private val commonIps = listOf(
        "http://192.168.0.1",       // VIRB canonical gateway
        "http://192.168.1.1",
        "http://10.0.0.1",
        "http://192.168.0.134",
        "http://192.168.1.254"
    )

    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(700, TimeUnit.MILLISECONDS)
        .callTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1000, TimeUnit.MILLISECONDS)
        .build()

    fun getCurrentSsid(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val rawSsid = info?.ssid ?: return null
            val ssid = rawSsid.trim('"')
            Log.d("CameraDiscovery", "Current SSID: $ssid")
            ssid
        } catch (t: Throwable) {
            Log.d("CameraDiscovery", "getCurrentSsid failed: ${t.message}")
            null
        }
    }

    fun getGatewayIpFromDhcp(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo ?: return null
            val gw = dhcp.gateway
            val ip = intToIp(gw)
            Log.d("CameraDiscovery", "DHCP gateway: $ip")
            ip
        } catch (t: Throwable) {
            Log.d("CameraDiscovery", "getGatewayIpFromDhcp failed: ${t.message}")
            null
        }
    }

    private fun intToIp(i: Int): String {
        return ((i and 0xFF).toString() + "." +
                ((i shr 8) and 0xFF) + "." +
                ((i shr 16) and 0xFF) + "." +
                ((i shr 24) and 0xFF))
    }

    fun getLocalIpAddress(): List<String> {
        val results = mutableListOf<String>()
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val host = addr.hostAddress ?: continue
                        if (host.indexOf(':') < 0) results.add(host)
                    }
                }
            }
        } catch (_: Throwable) { }
        return results
    }

    suspend fun probeForCameraBaseUrl(): String? = withContext(Dispatchers.IO) {
        val candidateUrls = mutableListOf<String>().apply { addAll(commonIps) }
        val endpoints = listOf("/virb", "/", "/status", "/info")
        Log.d("CameraDiscovery", "probeForCameraBaseUrl: candidates=${candidateUrls.joinToString()}")
        for (base in candidateUrls) {
            for (ep in endpoints) {
                val url = base.trimEnd('/') + ep
                try {
                    Log.d("CameraDiscovery", "Probing $url")
                    val req = Request.Builder().url(url).get().build()
                    val resp = probeClient.newCall(req).execute()
                    resp.use { r ->
                        Log.d("CameraDiscovery", "Response for $url -> ${r.code}")
                        if (r.isSuccessful) {
                            Log.d("CameraDiscovery", "Found camera at base=$base via $url")
                            return@withContext base
                        }
                    }
                } catch (ex: Exception) {
                    Log.d("CameraDiscovery", "Probe failed for $url : ${ex.message}")
                }
            }
        }
        null
    }

    suspend fun discoverCameraBaseUrl(context: Context, virbSsidHint: String? = null): String? = withContext(Dispatchers.IO) {
        val ssid = getCurrentSsid(context)
        if (virbSsidHint != null && ssid != null && !ssid.contains(virbSsidHint, ignoreCase = true)) {
            Log.d("CameraDiscovery", "SSID '$ssid' non corrisponde al hint '$virbSsidHint'")
        }

        val gw = getGatewayIpFromDhcp(context)
        if (!gw.isNullOrEmpty()) {
            val candidate = "http://$gw"
            try {
                val req = Request.Builder().url(candidate.trimEnd('/') + "/virb").get().build()
                val resp = probeClient.newCall(req).execute()
                resp.use { r ->
                    if (r.isSuccessful) {
                        Log.d("CameraDiscovery", "Found camera at gateway $candidate")
                        return@withContext candidate
                    }
                }
            } catch (ex: Exception) {
                Log.d("CameraDiscovery", "Gateway probe failed for $candidate : ${ex.message}")
            }
        }

        val found = probeForCameraBaseUrl()
        if (!found.isNullOrEmpty()) return@withContext found

        val localAddrs = getLocalIpAddress()
        for (local in localAddrs) {
            val prefix = local.substringBeforeLast('.', "")
            if (prefix.isNotEmpty()) {
                val candidate = "http://$prefix.1"
                try {
                    val req = Request.Builder().url(candidate + "/virb").get().build()
                    val resp = probeClient.newCall(req).execute()
                    resp.use { r ->
                        if (r.isSuccessful) {
                            Log.d("CameraDiscovery", "Found camera at fallback $candidate")
                            return@withContext candidate
                        }
                    }
                } catch (ex: Exception) {
                    Log.d("CameraDiscovery", "Fallback probe failed for $candidate : ${ex.message}")
                }
            }
        }

        Log.d("CameraDiscovery", "discoverCameraBaseUrl: nothing found")
        null
    }
}