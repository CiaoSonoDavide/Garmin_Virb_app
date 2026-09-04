package com.example.garmin_virb_app.ui.theme.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UIState{
    data object Idle: UIState
    data object Loading: UIState
    data class Error(val message: String): UIState
}

class CameraViewModel(
    private val repository: CameraRepository
): ViewModel() {

    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _mode = MutableStateFlow(CameraMode.PHOTO)
    val mode: StateFlow<CameraMode> = _mode.asStateFlow()

    private val _uiState = MutableStateFlow<UIState>(UIState.Idle)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    private val _gallery = MutableStateFlow<List<String>>(emptyList())
    val gallery: StateFlow<List<String>> = _gallery.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()
}