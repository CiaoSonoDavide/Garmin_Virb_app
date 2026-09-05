package com.example.garmin_virb_app.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

object CameraDiscovery {

    // Lista di IP probabili da provare se non trovi gateway
    private val commonIps = listOf(
        "http://192.168.1.1",
        "http://192.168.0.1",
        "http://10.0.0.1",
        "http://192.168.0.134",
        "http://192.168.1.254"
    )

    // Timeout HTTP
    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(700, TimeUnit.MILLISECONDS)
        .callTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1000, TimeUnit.MILLISECONDS)
        .build()

    // Read SSID
    fun getCurrentSsid(context: Context): String? {
        try{
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val rawSsid = info?.ssid ?: return null
            return rawSsid.trim('"')
        }
        catch (t: Throwable){
            return null
        }
    }

    fun getGatewayIpFromDhcp(context: Context): String? {
        return try{
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo ?: return null
            val gw = dhcp.gateway
            intToIp(gw)
        }
        catch (t: Throwable){
            return null
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
        try{
            val en = NetworkInterface.getNetworkInterfaces()
            while(en.hasMoreElements()){
                val intf = en.nextElement()
                val addrs = intf.inetAddresses
                while(addrs.hasMoreElements()){
                    val addr = addrs.nextElement()
                    if(!addr.isLoopbackAddress && addr is InetAddress){
                        val host = addr.hostAddress ?: continue
                        if(host.indexOf(':') < 0) results.add(host)
                    }
                }
            }
        }
        catch (_: Throwable){ }
        return results
    }

    suspend fun probeForCameraBaseUrl(): String? = withContext(Dispatchers.IO){
        val candidateUrls = mutableListOf<String>()
        candidateUrls.addAll(commonIps)
        for(base in candidateUrls){
            try{
                val statusUrl = base.trimEnd('/')+ "/status"
                val req = Request.Builder().url(statusUrl).get().build()
                val resp = probeClient.newCall(req).execute()
                resp.use{ r ->
                    if(r.isSuccessful) return@withContext base
                }
            }
            catch (_: Exception){

            }
        }
        null
    }

    suspend fun discoverCamerBaseUrl(context: Context, virbSsidHint: String? = null): String? = withContext(Dispatchers.IO){
        //verifica SSID se fornito come hint
        val ssid = getCurrentSsid(context)
        if(virbSsidHint != null && ssid != null &&!ssid.contains(virbSsidHint, ignoreCase = true)){
           return@withContext null
        }

        //prova gateway DHCP
        val gw = getGatewayIpFromDhcp(context)
        if(!gw.isNullOrEmpty()){
            val candidate = "http://$gw"
            try{
                val url = candidate + "/status"
                val req = Request.Builder().url(url).get().build()
                val resp = probeClient.newCall(req).execute()
                resp.use { r ->
                    if(r.isSuccessful) return@withContext candidate
                }
            }
            catch (_: Exception){ }
        }

        //prova commonIps
        val found = probeForCameraBaseUrl()
        if(!found.isNullOrEmpty()) return@withContext found

        //fallback_ prova gli indirizzi locali generati dall'indirizzo della macchina
        val localAddrs = getLocalIpAddress()
        for(local in localAddrs){
            val prefix = local.substringBeforeLast('.', "")
            if(prefix.isNotEmpty()){
                val candidate = "http://$prefix.1"
                try{
                    val req = Request.Builder().url(candidate  + "/status").get().build()
                    val resp = probeClient.newCall(req).execute()
                    resp.use { r ->
                        if(r.isSuccessful) return@withContext candidate
                    }
                }
                catch (_: Exception){ }
            }
        }
        null
    }
}