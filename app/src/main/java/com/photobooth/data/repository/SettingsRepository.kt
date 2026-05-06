package com.photobooth.data.repository

import com.photobooth.data.local.SettingsDataStore
import com.photobooth.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for app settings.
 * Abstracts the data source from the domain/UI layers.
 * If we ever migrate from DataStore to a server-backed config,
 * only this file changes.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: SettingsDataStore
) {
    val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.saveSettings(settings)
    }

    suspend fun updatePin(newPin: String, currentSettings: AppSettings) {
        dataStore.saveSettings(currentSettings.copy(settingsPin = newPin))
    }
}
