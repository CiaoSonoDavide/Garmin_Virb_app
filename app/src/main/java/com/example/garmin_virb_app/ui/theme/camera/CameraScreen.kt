package com.example.garmin_virb_app.ui.theme.camera

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onConnect: () -> Unit = {},
    onOpenGallery: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
){
    val context = LocalContext.current
    val streamUrl by viewModel.streamUrl.collectAsState()
    val connected by viewModel.connected.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val exoPlayer = remember(context){ ExoPlayer.Builder(context).build()}
    DisposableEffect(exoPlayer){
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(streamUrl){
        if(!streamUrl.isNullOrEmpty()){
            val uri = Uri.parse(streamUrl)
            val mediaItem = androidx.media3.common.MediaItem.Builder()
                .setUri(uri)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
        else{
            exoPlayer.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(connected) "Connesso" else "Disconnesso")},
                actions = {
                    IconButton(onClick = onOpenGallery) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Gallery")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ){
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
            ){
               if(!streamUrl.isNullOrEmpty()){
                   AndroidView(factory = { ctx ->
                       PlayerView(ctx).apply {
                           useController = false
                           layoutParams = ViewGroup.LayoutParams(
                               ViewGroup.LayoutParams.MATCH_PARENT,
                               ViewGroup.LayoutParams.MATCH_PARENT
                           )
                           this.player = exoPlayer
                       }
                   },
                   update = { view ->
                       view.player = exoPlayer
                   },
                   modifier = Modifier.fillMaxSize())
               }
                else{
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                        Text(
                            text = "Preview non disponibile",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                   }
               }
            }
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ){
                ModeSelector(selected = mode){newMode -> viewModel.setMode(newMode)}

                CaptureButton(
                    isRecording = isRecording,
                    onClick = {
                        if(isRecording) viewModel.stopRecording()
                        else if (mode == CameraMode.PHOTO) viewModel.takePhoto()
                        else viewModel.startRecording()
                    }
                )

                Button(onClick = { onConnect() }){
                    Text("Connect")
                }

                if(uiState is UIState.Loading){
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

    }
}