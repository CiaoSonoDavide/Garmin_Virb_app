package com.example.garmin_virb_app.ui.theme.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModeSelector(
    selected: CameraMode,
    onSelect: (CameraMode) -> Unit
){
    var expanded by remember {
        mutableStateOf(false)
    }
    Box(modifier = Modifier.padding(4.dp)){
        Text(
            text = selected.name,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CameraMode.values().forEach{ mode ->
                DropdownMenuItem(
                    text = { Text(mode.name)},
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    })
            }
        }
    }
}