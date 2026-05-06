package com.photobooth.domain.service

import android.net.Uri
import com.photobooth.data.model.PrintResult
import com.photobooth.data.model.ShareResult

/**
 * CameraService interface.
 * Domain layer depends on this abstraction, not on CameraX directly.
 * This allows mocking in tests and swapping implementations (CameraX, USB camera, etc.)
 */
interface CameraService {
    /** Bind the camera preview to a lifecycle owner */
    fun bindPreview(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: androidx.camera.view.PreviewView,
        useFrontCamera: Boolean,
    )

    /** Capture a single frame and return its Bitmap asynchronously */
    suspend fun capturePhoto(): android.graphics.Bitmap?

    /** Release camera resources */
    fun release()
}

/**
 * PrinterService interface.
 * Abstracts over Android Print Framework / Bluetooth thermal printers.
 */
interface PrinterService {
    /** Returns true if a printer is connected and ready */
    suspend fun isPrinterReady(): Boolean

    /** Sends the image at [uri] to the printer */
    suspend fun printPhoto(uri: Uri): PrintResult

    /** Connect to a printer by address (Bluetooth MAC or Wi-Fi IP) */
    suspend fun connectPrinter(address: String, name: String): Boolean

    /** Disconnect from current printer */
    fun disconnect()
}

/**
 * SharingService interface.
 * Abstracts over Android Intent sharing / WhatsApp deep links.
 */
interface SharingService {
    /** Open the system share sheet for the given image URI */
    fun shareViaSystem(uri: Uri, eventName: String): ShareResult

    /** Share directly to WhatsApp if installed */
    fun shareViaWhatsApp(uri: Uri): ShareResult

    /** Share via Bluetooth */
    fun shareViaBluetooth(uri: Uri): ShareResult
}
