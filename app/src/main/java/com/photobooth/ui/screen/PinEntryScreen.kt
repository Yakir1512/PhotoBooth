package com.photobooth.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photobooth.ui.viewmodel.SettingsViewModel

@Composable
fun PinEntryScreen(
    onCorrectPin: () -> Unit,
    onCancel: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError  by remember { mutableStateOf(false) }

    // Reset error when user starts typing again
    LaunchedEffect(enteredPin) { showError = false }

    fun onDigit(d: String) {
        if (enteredPin.length < 4) {
            enteredPin += d
            if (enteredPin.length == 4) {
                if (settingsViewModel.validatePin(enteredPin)) {
                    onCorrectPin()
                } else {
                    showError = true
                    enteredPin = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Back button
        IconButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,  // Use lock icon if available
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "הגדרות",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = "הזן קוד גישה",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )

            Spacer(Modifier.height(32.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    val filled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Error message
            AnimatedVisibility(visible = showError) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "קוד שגוי, נסה שנית",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── Numpad ────────────────────────────────────────────────────
            val digits = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
            val grid = digits.chunked(3)

            grid.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { label ->
                        if (label.isEmpty()) {
                            Spacer(Modifier.size(80.dp))
                        } else {
                            Button(
                                onClick = {
                                    if (label == "⌫") {
                                        enteredPin = enteredPin.dropLast(1)
                                    } else {
                                        onDigit(label)
                                    }
                                },
                                modifier = Modifier.size(80.dp),
                                shape    = RoundedCornerShape(16.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = if (label == "⌫")
                                        MaterialTheme.colorScheme.errorContainer
                                    else
                                        MaterialTheme.colorScheme.surface,
                                    contentColor   = if (label == "⌫")
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                ),
                                elevation = ButtonDefaults.buttonElevation(2.dp),
                            ) {
                                Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
