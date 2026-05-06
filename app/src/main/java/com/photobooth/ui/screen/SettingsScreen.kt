package com.photobooth.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photobooth.data.model.*
import com.photobooth.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("הגדרות / Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── APPEARANCE ───────────────────────────────────────────────
            item { SettingsSectionHeader("🎨 עיצוב / Appearance") }

            item {
                SettingsCard {
                    Text("ערכת נושא / Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppTheme.entries) { theme ->
                            ThemeChip(
                                label    = theme.displayNameHe,
                                selected = settings.selectedTheme == theme,
                                onClick  = { settingsViewModel.updateTheme(theme) },
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    Text("שפה / Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = settings.appLanguage == lang,
                                onClick  = { settingsViewModel.updateLanguage(lang) },
                                label    = { Text(lang.displayName, fontSize = 13.sp) },
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    Text("מסגרת תמונה / Frame", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PhotoFrame.entries) { frame ->
                            FilterChip(
                                selected = settings.selectedFrame == frame,
                                onClick  = { settingsViewModel.updateFrame(frame) },
                                label    = { Text(frame.displayNameHe, fontSize = 12.sp) },
                            )
                        }
                    }
                }
            }

            // ── SESSION ──────────────────────────────────────────────────
            item { SettingsSectionHeader("⏱️ צילום / Session") }

            item {
                SettingsCard {
                    Text("שם האירוע / Event Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    var eventText by remember(settings.eventName) { mutableStateOf(settings.eventName) }
                    OutlinedTextField(
                        value    = eventText,
                        onValueChange = { eventText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { settingsViewModel.updateEventName(eventText) }) {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    Text("ספירה לאחור / Countdown: ${settings.countdownSeconds}s",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value         = settings.countdownSeconds.toFloat(),
                        onValueChange = { settingsViewModel.updateCountdownSeconds(it.toInt()) },
                        valueRange    = 1f..10f,
                        steps         = 8,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── CAMERA ───────────────────────────────────────────────────
            item { SettingsSectionHeader("📷 מצלמה / Camera") }

            item {
                SettingsCard {
                    SwitchRow(
                        label   = "מצלמה קדמית / Front Camera",
                        checked = settings.useFrontCamera,
                        onCheckedChange = { settingsViewModel.updateUseFrontCamera(it) },
                    )
                }
            }

            // ── PRINTER ──────────────────────────────────────────────────
            item { SettingsSectionHeader("🖨️ מדפסת / Printer") }

            item {
                SettingsCard {
                    SwitchRow(
                        label   = "מדפסת מחוברת / Printer Enabled",
                        checked = settings.printerEnabled,
                        onCheckedChange = { settingsViewModel.updatePrinterEnabled(it) },
                    )

                    if (settings.printerEnabled) {
                        Spacer(Modifier.height(12.dp))
                        var printerName by remember(settings.printerName) { mutableStateOf(settings.printerName) }
                        var printerAddr by remember(settings.printerAddress) { mutableStateOf(settings.printerAddress) }

                        OutlinedTextField(
                            value = printerName,
                            onValueChange = { printerName = it },
                            label = { Text("שם מדפסת / Printer Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = printerAddr,
                            onValueChange = { printerAddr = it },
                            label = { Text("כתובת IP / Bluetooth MAC") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    settingsViewModel.updatePrinterName(printerName)
                                    settingsViewModel.updatePrinterAddress(printerAddr)
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save")
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        SwitchRow(
                            label   = "הדפסה אוטומטית / Auto Print",
                            checked = settings.autoPrint,
                            onCheckedChange = { settingsViewModel.updateAutoPrint(it) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("מקסימום הדפסות לסשן / Max prints: ${settings.maxPrintsPerSession}",
                            style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value         = settings.maxPrintsPerSession.toFloat(),
                            onValueChange = { settingsViewModel.updateMaxPrints(it.toInt()) },
                            valueRange    = 1f..5f,
                            steps         = 3,
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── SECURITY ─────────────────────────────────────────────────
            item { SettingsSectionHeader("🔒 אבטחה / Security") }

            item {
                SettingsCard {
                    var newPin    by remember { mutableStateOf("") }
                    var confirmPin by remember { mutableStateOf("") }
                    var pinError  by remember { mutableStateOf("") }

                    Text("שנה קוד / Change PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("קוד חדש / New PIN (4 digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("אשר קוד / Confirm PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (pinError.isNotEmpty()) {
                        Text(pinError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            when {
                                newPin.length != 4    -> pinError = "הקוד חייב להיות 4 ספרות"
                                newPin != confirmPin  -> pinError = "הקודים לא תואמים"
                                else -> {
                                    settingsViewModel.updatePin(newPin)
                                    newPin = ""; confirmPin = ""; pinError = "נשמר ✓"
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("שמור / Save")
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable settings components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content  = content,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
