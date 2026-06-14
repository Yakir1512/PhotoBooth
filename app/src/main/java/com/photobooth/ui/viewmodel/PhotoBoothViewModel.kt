package com.photobooth.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.data.model.*
import com.photobooth.data.repository.PhotoRepository
import com.photobooth.data.repository.SettingsRepository
import com.photobooth.domain.service.CameraService
import com.photobooth.domain.usecase.BuildPhotoStripUseCase
import com.photobooth.domain.usecase.PrintStripUseCase
import com.photobooth.domain.usecase.SharePhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoBoothViewModel @Inject constructor(
    private val cameraService: CameraService,
    private val settingsRepository: SettingsRepository,
    private val buildPhotoStripUseCase: BuildPhotoStripUseCase,
    private val printStripUseCase: PrintStripUseCase,
    private val sharePhotosUseCase: SharePhotosUseCase,
) : ViewModel() {

    private val _session = MutableStateFlow(PhotoSession())
    val session: StateFlow<PhotoSession> = _session.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val capturedBitmaps = mutableListOf<Bitmap>()
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { _settings.value = it }
        }
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraService.bindPreview(
            lifecycleOwner = lifecycleOwner,
            previewView    = previewView,
            useFrontCamera = _settings.value.useFrontCamera
        )
    }

    fun startSession() {
        capturedBitmaps.clear()
        _session.value = PhotoSession(sessionState = SessionState.COUNTING_DOWN)
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        val totalSeconds = _settings.value.countdownSeconds
        countdownJob = viewModelScope.launch {
            for (i in totalSeconds downTo 1) {
                _session.update { it.copy(countdownValue = i) }
                delay(1000L)
            }
            onCountdownFinished()
        }
    }

    private fun onCountdownFinished() {
        _session.update { it.copy(sessionState = SessionState.CAPTURING) }
        captureNextPhoto()
    }

    private fun captureNextPhoto() {
        viewModelScope.launch {
            val bitmap = cameraService.capturePhoto()
            if (bitmap != null) {
                capturedBitmaps.add(bitmap)
                val count = capturedBitmaps.size
                
                if (count < 3) {
                    _session.update { it.copy(
                        currentPhotoIndex = count,
                        sessionState      = SessionState.WAITING_NEXT
                    )}
                    delay(1500L)
                    _session.update { it.copy(sessionState = SessionState.COUNTING_DOWN) }
                    startCountdown()
                } else {
                    _session.update { it.copy(
                        currentPhotoIndex = count,
                        sessionState      = SessionState.PROCESSING 
                    )}
                    buildStrip()
                }
            } else {
                _session.update { it.copy(sessionState = SessionState.IDLE) }
            }
        }
    }

    private fun buildStrip() {
        viewModelScope.launch {
            val stripUri = buildPhotoStripUseCase(
                photos    = capturedBitmaps.toList(),
                frame     = _settings.value.selectedFrame,
                sessionId = _session.value.id,
            )
            _session.update { it.copy(
                compositeStripUri = stripUri,
                sessionState      = SessionState.REVIEW,
            )}
            if (_settings.value.autoPrint && stripUri != null) {
                triggerPrint(stripUri)
            }
        }
    }

    fun triggerPrint(uri: Uri) {
        viewModelScope.launch {
            val currentSession = _session.value
            _session.update { it.copy(sessionState = SessionState.PRINTING) }
            val result = printStripUseCase(
                stripUri   = uri,
                printCount = currentSession.printCount,
                maxPrints  = _settings.value.maxPrintsPerSession,
            )
            _session.update { it.copy(
                printCount   = if (result is PrintResult.Success) it.printCount + 1 else it.printCount,
                sessionState = SessionState.REVIEW
            )}
        }
    }

    fun shareViaSystem(uri: Uri) = sharePhotosUseCase.shareSystem(uri, _settings.value.eventName)
    fun shareViaWhatsApp(uri: Uri) = sharePhotosUseCase.shareWhatsApp(uri)
    fun shareViaBluetooth(uri: Uri) = sharePhotosUseCase.shareBluetooth(uri)

    fun resetSession() {
        countdownJob?.cancel()
        capturedBitmaps.forEach { it.recycle() }
        capturedBitmaps.clear()
        _session.value = PhotoSession(sessionState = SessionState.IDLE)
    }

    override fun onCleared() {
        super.onCleared()
        cameraService.release()
        capturedBitmaps.forEach { it.recycle() }
    }
}
