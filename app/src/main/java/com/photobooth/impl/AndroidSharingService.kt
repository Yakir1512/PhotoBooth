package com.photobooth.impl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.photobooth.data.model.ShareResult
import com.photobooth.domain.service.SharingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Intent-based sharing implementation.
 * Supports: system share sheet, WhatsApp direct share, Bluetooth.
 */
@Singleton
class AndroidSharingService @Inject constructor(
    @ApplicationContext private val context: Context,
) : SharingService {

    companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val BLUETOOTH_PACKAGE = "com.android.bluetooth"
    }

    override fun shareViaSystem(uri: Uri, eventName: String): ShareResult {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "📸 $eventName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "שתף תמונה").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            ShareResult.Success
        } catch (e: Exception) {
            ShareResult.Error(e.message ?: "Share failed")
        }
    }

    override fun shareViaWhatsApp(uri: Uri): ShareResult {
        if (!isAppInstalled(WHATSAPP_PACKAGE)) {
            return ShareResult.Error("WhatsApp is not installed")
        }
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                `package` = WHATSAPP_PACKAGE
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ShareResult.Success
        } catch (e: Exception) {
            ShareResult.Error(e.message ?: "WhatsApp share failed")
        }
    }

    override fun shareViaBluetooth(uri: Uri): ShareResult {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                // Filter chooser to Bluetooth only
                `package` = "com.android.bluetooth"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Fallback: open generic chooser if Bluetooth app not directly launchable
            val chooser = Intent.createChooser(intent, "שלח בבלוטוט'").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            ShareResult.Success
        } catch (e: Exception) {
            // Fallback to system share
            shareViaSystem(uri, "")
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isWhatsAppInstalled(): Boolean = isAppInstalled(WHATSAPP_PACKAGE)
}
