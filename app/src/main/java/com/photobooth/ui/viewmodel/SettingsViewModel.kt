package com.photobooth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.data.model.AppSettings
import com.photobooth.data.model.AppTheme
import com.photobooth.data.model.AppLanguage
import com.photobooth.data.model.PhotoFrame
import com.photobooth.data.repository.SettingsRepository
import com.photobooth.domain.usecase.ValidatePinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val validatePinUseCase: ValidatePinUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun validatePin(entered: String): Boolean =
        validatePinUseCase(entered, settings.value.settingsPin)

    fun updateTheme(theme: AppTheme) = save { it.copy(selectedTheme = theme) }
    fun updateLanguage(lang: AppLanguage) = save { it.copy(appLanguage = lang) }
    fun updateFrame(frame: PhotoFrame) = save { it.copy(selectedFrame = frame) }
    fun updateEventName(name: String) = save { it.copy(eventName = name) }
    fun updateCountdownSeconds(secs: Int) = save { it.copy(countdownSeconds = secs) }
    fun updatePrinterEnabled(enabled: Boolean) = save { it.copy(printerEnabled = enabled) }
    fun updatePrinterAddress(address: String) = save { it.copy(printerAddress = address) }
    fun updatePrinterName(name: String) = save { it.copy(printerName = name) }
    fun updateAutoPrint(auto: Boolean) = save { it.copy(autoPrint = auto) }
    fun updateMaxPrints(max: Int) = save { it.copy(maxPrintsPerSession = max) }
    fun updatePin(newPin: String) = save { it.copy(settingsPin = newPin) }
    fun updateUseFrontCamera(front: Boolean) = save { it.copy(useFrontCamera = front) }
    fun updateGalleryUrl(url: String) = save { it.copy(galleryUrl = url) }

    private fun save(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform(settings.value))
        }
    }
}
