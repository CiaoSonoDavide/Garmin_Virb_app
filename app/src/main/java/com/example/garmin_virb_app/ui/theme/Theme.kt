package com.example.garmin_virb_app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color.White,
    secondary = Color(0xFF03A9F4),
    background = Color(0xFFF6F7FB),
    surface = Color.White,
    error = Color(0xFFB00020),
    onBackground = Color(0xFF0B1B2B),
    onSurface = Color(0xFF0B1B2B)
)

@Composable
fun GarminVirbTheme(
    content: @Composable () -> Unit
){
    MaterialTheme(
        colorScheme = LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
