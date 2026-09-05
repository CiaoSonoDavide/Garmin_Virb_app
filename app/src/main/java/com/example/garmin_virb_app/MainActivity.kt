package com.example.garmin_virb_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.garmin_virb_app.network.CameraDiscovery
import com.example.garmin_virb_app.ui.theme.GarminVirbTheme
import com.example.garmin_virb_app.ui.theme.camera.CameraRepositoryImpl
import com.example.garmin_virb_app.ui.theme.camera.CameraScreen
import com.example.garmin_virb_app.ui.theme.camera.CameraViewModel
import com.example.garmin_virb_app.ui.theme.camera.CameraViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    // fallback se la discovery non trova nulla
    private val defaultCameraBase = "http://192.168.0.1"
    private val baseUrlState = MutableStateFlow<String?>(null)

    private val repo by lazy {
        val client = OkHttpClient.Builder().build()
        CameraRepositoryImpl(
            baseUrlProvider = { baseUrlState.value ?: defaultCameraBase },
            client = client
        )
    }

    private val factory by lazy { CameraViewModelFactory(repo) }
    private val viewModel: CameraViewModel by viewModels { factory }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            lifecycleScope.launch { performDiscoveryAndConnect() }
        } else {
            Toast.makeText(this, "Permesso posizione richiesto per trovare la camera Wi‑Fi", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GarminVirbTheme {
                CameraScreen(
                    viewModel = viewModel,
                    onConnect = { checkPermissionsAndStartDiscovery() },
                    onOpenGallery = { /* open gallery screen */ },
                    onOpenSettings = { /* open settings screen */ }
                )
            }
        }
    }

    private fun checkPermissionsAndStartDiscovery() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestLocationPermission.launch(permission)
        } else {
            lifecycleScope.launch { performDiscoveryAndConnect() }
        }
    }

    private suspend fun performDiscoveryAndConnect() {
        runOnUiThread { Toast.makeText(this, "Ricerca camera in corso...", Toast.LENGTH_SHORT).show() }
        val discovered = CameraDiscovery.discoverCameraBaseUrl(this@MainActivity, virbSsidHint = "VIRB")
        if (!discovered.isNullOrEmpty()) {
            baseUrlState.value = discovered
            val rtsp = buildRtspStreamFromBase(discovered)
            viewModel.setStreamUrl(rtsp)
            viewModel.connect()
            runOnUiThread { Toast.makeText(this, "Camera trovata: $discovered\nRTSP: $rtsp", Toast.LENGTH_SHORT).show() }
        } else {
            baseUrlState.value = null
            val rtsp = buildRtspStreamFromBase(defaultCameraBase)
            viewModel.setStreamUrl(rtsp)
            viewModel.connect()
            runOnUiThread {
                Toast.makeText(this, "Camera non trovata automaticamente, tentato fallback su $defaultCameraBase", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildRtspStreamFromBase(baseHttp: String): String {
        val host = android.net.Uri.parse(baseHttp).host ?: baseHttp.removePrefix("http://").removePrefix("https://")
        return "rtsp://$host/livePreviewStream"
    }
}