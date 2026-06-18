package com.photobooth.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.photobooth.data.model.PhotoFrame
import com.photobooth.data.model.PrintResult
import com.photobooth.data.model.ShareResult
import com.photobooth.data.repository.PhotoRepository
import com.photobooth.domain.service.PrinterService
import com.photobooth.domain.service.SharingService
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────
// Each UseCase encapsulates a SINGLE business operation.
// They are thin orchestrators between repositories and services.
// ──────────────────────────────────────────────────────────────

/**
 * Saves 3 captured Bitmaps and builds the composite vertical strip.
 * Returns the MediaStore URI of the saved strip image.
 */
class BuildPhotoStripUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
) {
    suspend operator fun invoke(
        photos    : List<Bitmap>,
        frame     : PhotoFrame,
        sessionId : String,
    ): Uri? = photoRepository.buildAndSaveStrip(photos, frame, sessionId)
}

/**
 * Sends the strip image to the connected printer.
 */
class PrintStripUseCase @Inject constructor(
    private val printerService  : PrinterService,
    private val photoRepository : PhotoRepository,
) {
    suspend operator fun invoke(
        stripUri   : Uri,
        printCount : Int,
        maxPrints  : Int,
    ): PrintResult {
        if (printCount >= maxPrints)          return PrintResult.MaxPrintsReached
        if (!printerService.isPrinterReady()) return PrintResult.PrinterNotAvailable
        return printerService.printPhoto(stripUri)
    }
}

/**
 * Shares the strip image via the chosen channel.
 */
class SharePhotosUseCase @Inject constructor(
    private val sharingService: SharingService,
) {
    fun shareSystem(uri: Uri, eventName: String): ShareResult =
        sharingService.shareViaSystem(uri, eventName)

    fun shareWhatsApp(uri: Uri): ShareResult =
        sharingService.shareViaWhatsApp(uri)

    fun shareBluetooth(uri: Uri): ShareResult =
        sharingService.shareViaBluetooth(uri)
}

/**
 * Validates the settings PIN entered by the user.
 */
class ValidatePinUseCase @Inject constructor() {
    operator fun invoke(entered: String, correctPin: String): Boolean =
        entered == correctPin
}