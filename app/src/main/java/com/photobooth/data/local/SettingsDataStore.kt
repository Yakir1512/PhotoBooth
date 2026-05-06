package com.photobooth.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.photobooth.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "photobooth_settings")

/**
 * DataStore-backed persistence for AppSettings.
 * Single Responsibility: only reads/writes settings keys.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ── Keys ──────────────────────────────────────────────────────────────
    private object Keys {
        val THEME             = stringPreferencesKey("theme")
        val LANGUAGE          = stringPreferencesKey("language")
        val FRAME             = stringPreferencesKey("frame")
        val USE_FRONT_CAMERA  = booleanPreferencesKey("use_front_camera")
        val EXTERNAL_CAMERA   = booleanPreferencesKey("external_camera")
        val COUNTDOWN_SECS    = intPreferencesKey("countdown_seconds")
        val DELAY_BETWEEN     = intPreferencesKey("delay_between")
        val PRINTER_ENABLED   = booleanPreferencesKey("printer_enabled")
        val PRINTER_ADDRESS   = stringPreferencesKey("printer_address")
        val PRINTER_NAME      = stringPreferencesKey("printer_name")
        val AUTO_PRINT        = booleanPreferencesKey("auto_print")
        val MAX_PRINTS        = intPreferencesKey("max_prints")
        val EVENT_NAME        = stringPreferencesKey("event_name")
        val GALLERY_URL       = stringPreferencesKey("gallery_url")
        val SETTINGS_PIN      = stringPreferencesKey("settings_pin")
    }

    // ── Read ──────────────────────────────────────────────────────────────
    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                selectedTheme        = prefs[Keys.THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.DARK_GOLD,
                appLanguage          = prefs[Keys.LANGUAGE]?.let { AppLanguage.valueOf(it) } ?: AppLanguage.HEBREW,
                selectedFrame        = prefs[Keys.FRAME]?.let { PhotoFrame.valueOf(it) } ?: PhotoFrame.NONE,
                useFrontCamera       = prefs[Keys.USE_FRONT_CAMERA] ?: true,
                externalCameraEnabled = prefs[Keys.EXTERNAL_CAMERA] ?: false,
                countdownSeconds     = prefs[Keys.COUNTDOWN_SECS] ?: 3,
                delayBetweenPhotos   = prefs[Keys.DELAY_BETWEEN] ?: 4,
                printerEnabled       = prefs[Keys.PRINTER_ENABLED] ?: false,
                printerAddress       = prefs[Keys.PRINTER_ADDRESS] ?: "",
                printerName          = prefs[Keys.PRINTER_NAME] ?: "",
                autoPrint            = prefs[Keys.AUTO_PRINT] ?: false,
                maxPrintsPerSession  = prefs[Keys.MAX_PRINTS] ?: 2,
                eventName            = prefs[Keys.EVENT_NAME] ?: "האירוע שלנו",
                galleryUrl           = prefs[Keys.GALLERY_URL] ?: "",
                settingsPin          = prefs[Keys.SETTINGS_PIN] ?: "1234",
            )
        }

    // ── Write ─────────────────────────────────────────────────────────────
    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME]            = settings.selectedTheme.name
            prefs[Keys.LANGUAGE]         = settings.appLanguage.name
            prefs[Keys.FRAME]            = settings.selectedFrame.name
            prefs[Keys.USE_FRONT_CAMERA] = settings.useFrontCamera
            prefs[Keys.EXTERNAL_CAMERA]  = settings.externalCameraEnabled
            prefs[Keys.COUNTDOWN_SECS]   = settings.countdownSeconds
            prefs[Keys.DELAY_BETWEEN]    = settings.delayBetweenPhotos
            prefs[Keys.PRINTER_ENABLED]  = settings.printerEnabled
            prefs[Keys.PRINTER_ADDRESS]  = settings.printerAddress
            prefs[Keys.PRINTER_NAME]     = settings.printerName
            prefs[Keys.AUTO_PRINT]       = settings.autoPrint
            prefs[Keys.MAX_PRINTS]       = settings.maxPrintsPerSession
            prefs[Keys.EVENT_NAME]       = settings.eventName
            prefs[Keys.GALLERY_URL]      = settings.galleryUrl
            prefs[Keys.SETTINGS_PIN]     = settings.settingsPin
        }
    }
}
