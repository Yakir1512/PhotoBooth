package com.photobooth.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
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

/**
 * Central ViewModel for the photo capture session.
 *
 * Implements a clean STATE MACHINE:
 *   IDLE → COUNTING_DOWN → CAPTURING → WAITING_NEXT → (repeat 3x) → PROCESSING → REVIEW
 *
 * All state mutations happen through well-named functions (no raw setState soup).
 */
@HiltViewModel
class PhotoBoothViewModel @Inject constructor(
    private val cameraService: CameraService,
    private val settingsRepository: SettingsRepository,
    private val buildPhotoStripUseCase: BuildPhotoStripUseCase,
    private val printStripUseCase: PrintStripUseCase,
    private val sharePhotosUseCase: SharePhotosUseCase,
) : ViewModel() {

    // ── Exposed State ──────────────────────────────────────────────────────
    private val _session = MutableStateFlow(PhotoSession())
    val session: StateFlow<PhotoSession> = _session.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Captured bitmaps (held in memory for compositing; not serialised to StateFlow)
    private val capturedBitmaps = mutableListOf<Bitmap>()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { _settings.value = it }
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /** Called when user taps SHOOT on the welcome screen */
    fun startSession() {
        capturedBitmaps.clear()
        _session.value = PhotoSession(sessionState = SessionState.COUNTING_DOWN)
        startCountdown()
    }

    /** Called when countdown finishes – trigger capture */
    private fun onCountdownFinished() {
        _session.update { it.copy(sessionState = SessionState.CAPTURING) }
        captureNextPhoto()
    }

    private fun captureNextPhoto() {
        viewModelScope.launch {
            val bitmap = cameraService.capturePhoto()
            if (bitmap != null) {
                capturedBitmaps.add(bitmap)
                val newIndex = _session.value.capturedPhotos.size  // before adding
                _session.update { session ->
                    session.copy(
                        currentPhotoIndex = newIndex + 1,
                        sessionState = if (capturedBitmaps.size < 3)
                            SessionState.WAITING_NEXT
                        else
                            SessionState.PROCESSING,
                    )
                }

                if (capturedBitmaps.size < 3) {
                    // Short pause, then next countdown
                    delay(_settings.value.delayBetweenPhotos * 1000L)
                    _session.update { it.copy(sessionState = SessionState.COUNTING_DOWN) }
                    startCountdown()
                } else {
                    // All 3 taken → build strip
                    buildStrip()
                }
            } else {
                // Camera error – reset gracefully
                _session.update { it.copy(sessionState = SessionState.IDLE) }
            }
        }
    }

    private fun buildStrip() {
        viewModelScope.launch {
            val sessionId = _session.value.id
            val stripUri = buildPhotoStripUseCase(
                photos    = capturedBitmaps.toList(),
                frame     = _settings.value.selectedFrame,
                sessionId = sessionId,
            )
            _session.update { it.copy(
                compositeStripUri = stripUri,
                sessionState      = SessionState.REVIEW,
            )}

            // Auto-print if enabled
            if (_settings.value.autoPrint && stripUri != null) {
                triggerPrint(stripUri)
            }
        }
    }

    fun triggerPrint(uri: Uri) {
        val currentSession = _session.value
        viewModelScope.launch {
            _session.update { it.copy(sessionState = SessionState.PRINTING) }
            val result = printStripUseCase(
                stripUri   = uri,
                printCount = currentSession.printCount,
                maxPrints  = _settings.value.maxPrintsPerSession,
            )
            when (result) {
                is PrintResult.Success -> _session.update {
                    it.copy(printCount = it.printCount + 1, sessionState = SessionState.REVIEW)
                }
                else -> _session.update { it.copy(sessionState = SessionState.REVIEW) }
            }
        }
    }

    fun shareViaSystem(uri: Uri) {
        sharePhotosUseCase.shareSystem(uri, _settings.value.eventName)
    }

    fun shareViaWhatsApp(uri: Uri) {
        sharePhotosUseCase.shareWhatsApp(uri)
    }

    fun shareViaBluetooth(uri: Uri) {
        sharePhotosUseCase.shareBluetooth(uri)
    }

    fun resetSession() {
        countdownJob?.cancel()
        capturedBitmaps.forEach { it.recycle() }
        capturedBitmaps.clear()
        _session.value = PhotoSession(sessionState = SessionState.IDLE)
    }

    // ── Private Helpers ────────────────────────────────────────────────────

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

    override fun onCleared() {
        super.onCleared()
        cameraService.release()
        capturedBitmaps.forEach { it.recycle() }
    }
}
