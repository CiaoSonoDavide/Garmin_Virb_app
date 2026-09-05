package com.example.garmin_virb_app.ui.theme.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CameraViewModelFactory(
    private val repository: CameraRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}