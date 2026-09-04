package com.example.garmin_virb_app.ui.theme.camera

interface CameraRepository {
    suspend fun connectToCamera(): Result<String>
    suspend fun takePhoto(): Result<String>
    suspend fun startRecording(): Result<Unit>
    suspend fun stopRecording(): Result<Unit>
    suspend fun fetchGallery(): Result<List<String>>
}