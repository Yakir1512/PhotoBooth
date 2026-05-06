package com.photobooth.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.photobooth.data.model.PhotoFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles all disk I/O for photos.
 * Single Responsibility: save bitmaps, build composite strip, apply frames.
 */
@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val ALBUM_NAME = "PhotoBooth"
        const val STRIP_WIDTH  = 1080   // px  (4:3 portrait strip)
        const val STRIP_HEIGHT = 1920
        const val PHOTO_HEIGHT = 600    // each of 3 photos
        const val PHOTO_MARGIN = 20
        const val FRAME_PADDING = 40
    }

    /**
     * Saves a single capture bitmap and returns its URI.
     */
    suspend fun saveCapture(bitmap: Bitmap, sessionId: String, index: Int): Uri? =
        withContext(Dispatchers.IO) {
            saveBitmapToMediaStore(bitmap, "capture_${sessionId}_$index")
        }

    /**
     * Composites 3 photos into a vertical strip, optionally applies a frame,
     * saves to MediaStore and returns the URI.
     */
    suspend fun buildAndSaveStrip(
        photos: List<Bitmap>,
        frame: PhotoFrame,
        sessionId: String,
    ): Uri? = withContext(Dispatchers.IO) {
        if (photos.size < 3) return@withContext null

        val strip = Bitmap.createBitmap(STRIP_WIDTH, STRIP_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(strip)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Draw the 3 photos vertically, centered with margins
        val photoW = STRIP_WIDTH - (FRAME_PADDING * 2)
        val photoH = PHOTO_HEIGHT
        val totalPhotosH = (photoH * 3) + (PHOTO_MARGIN * 2)
        val topOffset = (STRIP_HEIGHT - totalPhotosH) / 2

        photos.forEachIndexed { i, bitmap ->
            val scaled = Bitmap.createScaledBitmap(bitmap, photoW, photoH, true)
            val left = FRAME_PADDING.toFloat()
            val top  = (topOffset + i * (photoH + PHOTO_MARGIN)).toFloat()
            canvas.drawBitmap(scaled, left, top, paint)
            scaled.recycle()
        }

        // Apply frame overlay if selected
        if (frame != PhotoFrame.NONE) {
            applyFrame(canvas, strip.width, strip.height, frame, paint)
        }

        saveBitmapToMediaStore(strip, "strip_$sessionId").also {
            strip.recycle()
        }
    }

    /**
     * Applies a decorative frame drawn programmatically.
     * Each frame style is handled here – extend with new PhotoFrame variants easily.
     */
    private fun applyFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        frame: PhotoFrame,
        paint: Paint
    ) {
        when (frame) {
            PhotoFrame.CLASSIC_WHITE -> drawSimpleBorder(canvas, width, height, android.graphics.Color.WHITE, 24f, paint)
            PhotoFrame.GOLD_ORNATE   -> drawSimpleBorder(canvas, width, height, android.graphics.Color.parseColor("#D4AF37"), 18f, paint)
            PhotoFrame.NEON_GLOW     -> drawNeonFrame(canvas, width, height, paint)
            else -> { /* Asset-based frames are loaded in UI layer via Coil */ }
        }
    }

    private fun drawSimpleBorder(canvas: Canvas, w: Int, h: Int, color: Int, strokeWidth: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = strokeWidth
        val half = strokeWidth / 2f
        canvas.drawRect(half, half, w - half, h - half, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawNeonFrame(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        val neonColor = android.graphics.Color.parseColor("#00FFFF")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        paint.color = neonColor
        paint.maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.OUTER)
        canvas.drawRect(6f, 6f, w - 6f, h - 6f, paint)
        paint.maskFilter = null
        paint.style = Paint.Style.FILL
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap, filename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /**
     * Returns a File handle for the given URI (needed for printing).
     */
    suspend fun getFileForUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "print_temp.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
