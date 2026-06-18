package com.photobooth.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.data.model.*
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
 * Central ViewModel for the photo-booth session.
 *
 * STATE MACHINE:
 *   IDLE → COUNTING_DOWN → CAPTURING → WAITING_NEXT
 *        ↑_______(repeat × 3)_________|
 *   → PROCESSING → REVIEW → (PRINTING / SHARING optional)
 *
 * This ViewModel is scoped to the Activity (via NavGraph) so
 * WelcomeScreen and CaptureScreen always share the SAME instance.
 */
@HiltViewModel
class PhotoBoothViewModel @Inject constructor(
    private val cameraService: CameraService,
    private val settingsRepository: SettingsRepository,
    private val buildPhotoStripUseCase: BuildPhotoStripUseCase,
    private val printStripUseCase: PrintStripUseCase,
    private val sharePhotosUseCase: SharePhotosUseCase,
) : ViewModel() {

    // ── Exposed state ──────────────────────────────────────────────────────
    private val _session  = MutableStateFlow(PhotoSession())
    val session: StateFlow<PhotoSession> = _session.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // In-memory bitmap list (NOT part of StateFlow – bitmaps are heavy)
    private val capturedBitmaps = mutableListOf<Bitmap>()
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { _settings.value = it }
        }
    }

    // ── Camera binding ─────────────────────────────────────────────────────

    /**
     * Binds the camera preview to the supplied [previewView].
     * Safe to call from multiple screens: CameraXService calls unbindAll()
     * before each new bind, so the previous screen's preview is released.
     */
    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraService.bindPreview(
            lifecycleOwner = lifecycleOwner,
            previewView    = previewView,
            useFrontCamera = _settings.value.useFrontCamera,
        )
    }

    // ── Session control ────────────────────────────────────────────────────

    /**
     * FIX: Guard prevents the sequence from starting if it is already running.
     * Only IDLE or REVIEW are valid entry states for a new session.
     *
     * Call order (from CaptureScreen):
     *   1. bindCamera(...)    – rebinds camera to CaptureScreen's PreviewView
     *   2. startSession()     – kicks off the countdown → capture loop
     */
    fun startSession() {
        val current = _session.value.sessionState
        // Idempotency guard – do not restart an in-progress session
        if (current != SessionState.IDLE && current != SessionState.REVIEW) return

        capturedBitmaps.forEach { it.recycle() }
        capturedBitmaps.clear()

        _session.value = PhotoSession(sessionState = SessionState.COUNTING_DOWN)
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        val seconds = _settings.value.countdownSeconds

        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _session.update { it.copy(countdownValue = i) }
                delay(1_000L)
            }
            // Countdown reached zero
            _session.update { it.copy(countdownValue = 0, sessionState = SessionState.CAPTURING) }
            captureNextPhoto()
        }
    }

    private fun captureNextPhoto() {
        viewModelScope.launch {
            val bitmap = cameraService.capturePhoto()

            if (bitmap != null) {
                capturedBitmaps.add(bitmap)
                val count = capturedBitmaps.size   // 1, 2, or 3 after adding

                if (count < 3) {
                    // More photos to take → short pause → next countdown
                    _session.update { it.copy(
                        currentPhotoIndex = count,
                        sessionState      = SessionState.WAITING_NEXT,
                    )}
                    delay(1_500L)
                    _session.update { it.copy(sessionState = SessionState.COUNTING_DOWN) }
                    startCountdown()
                } else {
                    // All 3 photos captured → build the composite strip
                    _session.update { it.copy(
                        currentPhotoIndex = count,
                        sessionState      = SessionState.PROCESSING,
                    )}
                    buildStrip()
                }

            } else {
                // Camera returned null (not ready yet or device error) → reset
                _session.update { it.copy(sessionState = SessionState.IDLE) }
            }
        }
    }

    /**
     * Composites the 3 bitmaps into a single vertical strip via the UseCase,
     * saves it to MediaStore and transitions to REVIEW.
     */
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
            // Auto-print if the operator enabled it
            if (_settings.value.autoPrint && stripUri != null) {
                triggerPrint(stripUri)
            }
        }
    }

    // ── Print ──────────────────────────────────────────────────────────────

    fun triggerPrint(uri: Uri) {
        viewModelScope.launch {
            val snapshot = _session.value
            _session.update { it.copy(sessionState = SessionState.PRINTING) }

            val result = printStripUseCase(
                stripUri   = uri,
                printCount = snapshot.printCount,
                maxPrints  = _settings.value.maxPrintsPerSession,
            )
            _session.update { it.copy(
                printCount   = if (result is PrintResult.Success) it.printCount + 1 else it.printCount,
                sessionState = SessionState.REVIEW,
            )}
        }
    }

    // ── Share ──────────────────────────────────────────────────────────────

    fun shareViaSystem(uri: Uri)    = sharePhotosUseCase.shareSystem(uri, _settings.value.eventName)
    fun shareViaWhatsApp(uri: Uri)  = sharePhotosUseCase.shareWhatsApp(uri)
    fun shareViaBluetooth(uri: Uri) = sharePhotosUseCase.shareBluetooth(uri)

    // ── Reset ──────────────────────────────────────────────────────────────

    fun resetSession() {
        countdownJob?.cancel()
        capturedBitmaps.forEach { it.recycle() }
        capturedBitmaps.clear()
        _session.value = PhotoSession(sessionState = SessionState.IDLE)
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        cameraService.release()
        capturedBitmaps.forEach { it.recycle() }
    }
}