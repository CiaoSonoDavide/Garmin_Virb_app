package com.example.garmin_virb_app

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.garmin_virb_app.network.CameraDiscovery
import com.example.garmin_virb_app.ui.theme.camera.CameraRepositoryImpl
import com.example.garmin_virb_app.ui.theme.camera.CameraViewModel
import com.example.garmin_virb_app.ui.theme.camera.CameraViewModelFactory
import com.example.garmin_virb_app.ui.theme.camera.CameraScreen
import com.example.garmin_virb_app.ui.theme.GarminVirbTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {

    // fallback se la discovery non trova nulla
    private val defaultCameraBase = "http://192.168.0.134"

    // stato dinamico che può essere aggiornato dalla discovery
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

    // ActivityResultLauncher per richiesta permesso runtime
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

    // chiamare quando l'utente preme Connect
    private fun checkPermissionsAndStartDiscovery() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            // richiedi permesso
            requestLocationPermission.launch(permission)
        } else {
            lifecycleScope.launch { performDiscoveryAndConnect() }
        }
    }

    // esegue la discovery (usa CameraDiscovery) e poi avvia la connessione tramite ViewModel
    private suspend fun performDiscoveryAndConnect() {
        // mostra feedback all'utente (opzionale)
        runOnUiThread { Toast.makeText(this, "Ricerca camera in corso...", Toast.LENGTH_SHORT).show() }

        // virbSsidHint opzionale: metti il frammento comune dell'SSID della Virb (es. "VIRB") se vuoi limitare la discovery
        val discovered = CameraDiscovery.discoverCameraBaseUrl(this@MainActivity, virbSsidHint = "VIRB")
        if (!discovered.isNullOrEmpty()) {
            baseUrlState.value = discovered
            // ora repository leggerà il baseUrl aggiornato dalla lambda e connect imposterà lo stato UI
            viewModel.connect()
            runOnUiThread { Toast.makeText(this, "Camera trovata: $discovered", Toast.LENGTH_SHORT).show() }
        } else {
            // fallback: prova comunque la default o segnala errore
            baseUrlState.value = null
            // puoi scegliere di chiamare viewModel.connect() per tentare il default
            viewModel.connect()
            runOnUiThread {
                Toast.makeText(this, "Camera non trovata automaticamente, tentato fallback su $defaultCameraBase", Toast.LENGTH_LONG).show()
            }
        }
    }
}