package com.photobooth.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.*
import android.print.pdf.PrintedPdfDocument
import com.photobooth.data.model.PrintResult
import com.photobooth.data.repository.PhotoRepository
import com.photobooth.domain.service.PrinterService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implements PrinterService using Android's built-in Print Framework.
 * This approach works with:
 *   • Any Android-compatible Wi-Fi printer (Canon SELPHY, DNP, etc.)
 *   • Bluetooth printers (with PrintJobCallback)
 *   • Google Cloud Print (deprecated but still relevant in some setups)
 *
 * For Bluetooth thermal printers (e.g. Hiti, Citizen), extend this class
 * and inject a BluetoothPrinterStrategy (Strategy pattern).
 */
@Singleton
class AndroidPrinterService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoRepository: PhotoRepository,
) : PrinterService {

    private var printManager: PrintManager =
        context.getSystemService(Context.PRINT_SERVICE) as PrintManager

    private var isConnected = false
    private var connectedAddress = ""
    private var connectedName = ""

    override suspend fun isPrinterReady(): Boolean = isConnected

    override suspend fun connectPrinter(address: String, name: String): Boolean {
        // For network printers: validate the address is reachable
        // For Bluetooth printers: attempt pairing via BluetoothAdapter
        // Here we optimistically set connected = true; real BT logic goes in BluetoothPrinterStrategy
        return withContext(Dispatchers.IO) {
            try {
                connectedAddress = address
                connectedName = name
                isConnected = address.isNotBlank()
                isConnected
            } catch (e: Exception) {
                isConnected = false
                false
            }
        }
    }

    override fun disconnect() {
        isConnected = false
        connectedAddress = ""
        connectedName = ""
    }

    override suspend fun printPhoto(uri: Uri): PrintResult =
        withContext(Dispatchers.IO) {
            try {
                val file = photoRepository.getFileForUri(uri)
                    ?: return@withContext PrintResult.Error("Could not read image file")

                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: return@withContext PrintResult.Error("Could not decode image")

                // Trigger Android print dialog on the main thread
                withContext(Dispatchers.Main) {
                    val printAttrs = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A6)   // 105x148mm – photo size
                        .setResolution(PrintAttributes.Resolution("res1", "300dpi", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    val jobName = "PhotoBooth_${System.currentTimeMillis()}"
                    printManager.print(jobName, BitmapPrintDocumentAdapter(context, bitmap), printAttrs)
                }

                PrintResult.Success
            } catch (e: Exception) {
                PrintResult.Error(e.message ?: "Unknown print error")
            }
        }
}

// ──────────────────────────────────────────────────────────────
// PrintDocumentAdapter that renders a Bitmap into a PDF page
// ──────────────────────────────────────────────────────────────
private class BitmapPrintDocumentAdapter(
    private val context: Context,
    private val bitmap: Bitmap,
) : PrintDocumentAdapter() {

    private var pageWidth  = 0
    private var pageHeight = 0

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val mediaSize = newAttributes.mediaSize ?: PrintAttributes.MediaSize.ISO_A6
        pageWidth  = mediaSize.widthMils
        pageHeight = mediaSize.heightMils

        val info = PrintDocumentInfo.Builder("photo_strip.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
            .setPageCount(1)
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: android.os.ParcelFileDescriptor,
        cancellationSignal: android.os.CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val printAttrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
            .setResolution(PrintAttributes.Resolution("r1", "300dpi", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val pdfDoc = PrintedPdfDocument(context, printAttrs)
        val page  = pdfDoc.startPage(0)
        val canvas = page.canvas

        // Scale bitmap to fill the page
        val scale = minOf(
            canvas.width.toFloat() / bitmap.width,
            canvas.height.toFloat() / bitmap.height,
        )
        val scaledW = (bitmap.width * scale).toInt()
        val scaledH = (bitmap.height * scale).toInt()
        val left = (canvas.width - scaledW) / 2f
        val top  = (canvas.height - scaledH) / 2f
        val dst  = android.graphics.RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(bitmap, null, dst, null)

        pdfDoc.finishPage(page)

        try {
            pdfDoc.writeTo(java.io.FileOutputStream(destination.fileDescriptor))
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        } finally {
            pdfDoc.close()
        }
    }
}
