package com.photobooth.data.model

import android.net.Uri

/**
 * Represents the state of a single photo-booth session.
 * Immutable – state transitions produce new instances (functional style).
 */
data class PhotoSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val capturedPhotos: List<Uri> = emptyList(),
    val compositeStripUri: Uri? = null,
    val sessionState: SessionState = SessionState.IDLE,
    val countdownValue: Int = 0,
    val currentPhotoIndex: Int = 0,    // 0-based, which photo we're capturing next
    val printCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val totalPhotos: Int get() = 3  // Always a 3-photo vertical strip
    val isComplete: Boolean get() = capturedPhotos.size >= totalPhotos
    val isBusy: Boolean get() = sessionState in listOf(
        SessionState.COUNTING_DOWN,
        SessionState.CAPTURING,
        SessionState.PROCESSING,
    )
}

enum class SessionState {
    IDLE,             // Waiting on welcome screen
    COUNTING_DOWN,    // 3-2-1 countdown running
    CAPTURING,        // Shutter triggered, flash showing
    WAITING_NEXT,     // Brief pause between shots
    PROCESSING,       // Compositing the strip image
    REVIEW,           // Showing result, awaiting user action
    PRINTING,         // Sending to printer
    SHARING,          // Share sheet open
}

/**
 * Sealed class representing all possible outcomes of a print operation.
 * Follows the Result pattern for explicit error handling.
 */
sealed class PrintResult {
    data object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
    data object PrinterNotAvailable : PrintResult()
    data object MaxPrintsReached : PrintResult()
}

/**
 * Sealed class representing all possible outcomes of a share operation.
 */
sealed class ShareResult {
    data object Success : ShareResult()
    data class Error(val message: String) : ShareResult()
    data object Cancelled : ShareResult()
}
