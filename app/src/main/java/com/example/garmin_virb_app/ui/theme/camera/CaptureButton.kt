package com.example.garmin_virb_app.ui.theme.camera

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CaptureButton(
    isRecording: Boolean,
    onClick: () -> Unit
){
    Surface(
        shape = CircleShape,
        color = if(isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    ){
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp)
        ) {
            val icon = when {
                isRecording -> Icons.Filled.Stop
                else -> Icons.Filled.Camera
            }
            Icon(
                imageVector = icon,
                contentDescription = if (isRecording) "Stop recording" else "Capture photo",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}