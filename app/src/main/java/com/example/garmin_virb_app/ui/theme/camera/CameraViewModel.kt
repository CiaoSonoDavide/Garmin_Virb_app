package com.example.garmin_virb_app.ui.theme.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UIState{
    data object Idle: UIState
    data object Loading: UIState
    data class Error(val message: String): UIState
}

class CameraViewModel(
    private val repository: CameraRepository
): ViewModel() {

    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl

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

    fun setMode(newMode: CameraMode){
        _mode.value = newMode
    }

    fun connect(){
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val res = repository.connectToCamera()
            if(res.isSuccess){
                _streamUrl.value = res.getOrNull()
                _connected.value = true
                _uiState.value = UIState.Idle
            }
            else{
                _connected.value = false
                _uiState.value = UIState.Error(res.exceptionOrNull()?.message ?: "Connessione fallita")
                _events.emit("Connessione fallita: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun takePhoto(){
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val res = repository.takePhoto()
            if(res.isSuccess){
                val url = res.getOrNull().orEmpty()
                _gallery.value = listOf(url) + _gallery.value
                _events.emit("Foto scattata")
                _uiState.value = UIState.Idle
            }
            else{
                _uiState.value = UIState.Error(res.exceptionOrNull()?.message ?: "Errore scatto")
                _events.emit("Errore scatto: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun startRecording(){
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val res = repository.startRecording()
            if(res.isSuccess){
                _isRecording.value = true
                _events.emit("Registrazione avviata")
                _uiState.value = UIState.Idle
            }
            else{
                _uiState.value  = UIState.Error(res.exceptionOrNull()?.message ?: "Errore avvio registrazione")
                _events.emit("Errore avvio registrazione: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun stopRecording(){
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val res = repository.stopRecording()
            if(res.isSuccess){
                _isRecording.value = false
                _events.emit("Registrazione fermata")
                _uiState.value = UIState.Idle
            }
            else{
                _uiState.value  = UIState.Error(res.exceptionOrNull()?.message ?: "Errore stop registrazione")
                _events.emit("Errore stop registrazione: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun fetchGallery(){
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val res = repository.fetchGallery()
            if(res.isSuccess){
                _gallery.value = res.getOrNull().orEmpty()
                _uiState.value = UIState.Idle
            }
            else{
                _uiState.value = UIState.Error(res.exceptionOrNull()?.message ?: "Errore caricamento gallery")
                _events.emit("Errore caricamento gallery: ${res.exceptionOrNull()?.message}")
        }
        }
    }

    fun setStreamUrl(url: String) {
        _streamUrl.value = url
    }
}