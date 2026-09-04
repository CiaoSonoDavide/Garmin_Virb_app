package com.example.garmin_virb_app.ui.theme.camera

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit
){
    val context = LocalContext.current
    val streamUrl by viewModel.streamUrl.collectAsState()
    val connected by viewModel.connected.collectAsState()

    Scaffold(
        topBar = {
           Row(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(12.dp),
               verticalAlignment = Alignment.CenterVertically
           ) {
               Text(text = if(connected) "Connesso" else "Disconnesso", modifier = Modifier.weight(1f))
               IconButton(onClick = onOpenGallery){Icon(/*icona  gallery*/ imageVector="" /*...*/, contentDescription = "Gallery")}
               IconButton(onClick = onOpenSettings){Icon(/*settings icon*/ imageVector="" /*..*/, contentDescription = "Settings")}
           }
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ){
        innerPadding ->
        Column (modifier = Modifer
            .fillMaxSize()
            .padding(innerPadding)){
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
            )
            {
                //Preview via ExoPlayer PlayerView embedded
                if(!streamUrl.isNullOrEmpty()){
                    AndroidView(factory = { ctx ->
                        val player = ExoPlayer.Builder(ctx).build()
                        val playerView = PlayerView(ctx).apply {
                            useController = false
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            player = player
                        }
                        val mediaItem = MediaItem.fromUri(streamUrl!!)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.playWhenReady = true
                        playerView
                    }, modifier = Modifier.fillMaxSize())
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

            //Controls footer
            Row (modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround)
            {
                ///Mode Selector
                ModeSelector(selected = viewmodel.mode.collecAsState().value){newMode -> viewModel.setModel(newMode)}

                //Capture - Record button
                CaptureButton(
                    isRecording = viewModel.isRecording.collectAsState().value,
                    onClick = {
                        if(viewModel.isRecording.value) viewModel.stopRecording()
                        else if(viewModel.mode.value == CameraMode.PHOTO) viewModel.takePhoto()
                        else viewModel.startRecording()
                    }
                )
            }
        }
    }
}