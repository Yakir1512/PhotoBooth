package com.photobooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.photobooth.ui.navigation.PhotoBoothNavGraph
import com.photobooth.ui.theme.PhotoBoothTheme
import com.photobooth.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity - hosts the entire Compose navigation graph.
 * Keeps Android framework concerns isolated here.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsState()

            PhotoBoothTheme(appTheme = settings.selectedTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PhotoBoothNavGraph()
                }
            }
        }
    }
}
