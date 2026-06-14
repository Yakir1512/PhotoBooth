package com.photobooth.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.photobooth.data.model.AppLanguage
import com.photobooth.data.model.SessionState
import com.photobooth.ui.viewmodel.PhotoBoothViewModel
import com.photobooth.ui.viewmodel.SettingsViewModel

@Composable
fun ReviewScreen(
    sessionId: String,
    onDone: () -> Unit,
    viewModel: PhotoBoothViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val session  by viewModel.session.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val stripUri = session.compositeStripUri

    val isPrinting = session.sessionState == SessionState.PRINTING
    val canPrint   = settings.printerEnabled &&
                     session.printCount < settings.maxPrintsPerSession

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Header ──────────────────────────────────────────────────
            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "🎉 יצא מושלם!"
                    AppLanguage.ENGLISH   -> "🎉 Looking Great!"
                    AppLanguage.BILINGUAL -> "🎉 יצא מושלם! Looking Great!"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = settings.eventName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // ── Photo Strip preview ──────────────────────────────────────
            if (stripUri != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                ) {
                    AsyncImage(
                        model = stripUri,
                        contentDescription = "Photo Strip",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
            } else {
                // Loading placeholder
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .height(360.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Action buttons ───────────────────────────────────────────

            // PRINT button
            if (settings.printerEnabled) {
                Button(
                    onClick = { stripUri?.let { viewModel.triggerPrint(it) } },
                    enabled = canPrint && !isPrinting && stripUri != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = when {
                            isPrinting -> localizedString(settings.appLanguage, "מדפיס...", "Printing...")
                            !canPrint  -> localizedString(settings.appLanguage, "מקסימום הדפסות", "Max prints reached")
                            else       -> localizedString(settings.appLanguage,
                                "הדפס (${settings.maxPrintsPerSession - session.printCount} נותרו)",
                                "Print (${settings.maxPrintsPerSession - session.printCount} left)")
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            // SHARE button - System share sheet
            OutlinedButton(
                onClick  = { stripUri?.let { viewModel.shareViaSystem(it) } },
                enabled  = stripUri != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    localizedString(settings.appLanguage, "שתף", "Share"),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            // WhatsApp button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick  = { stripUri?.let { viewModel.shareViaWhatsApp(it) } },
                    enabled  = stripUri != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF25D366),  // WhatsApp green
                    ),
                ) {
                    Text("💬 WhatsApp", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick  = { stripUri?.let { viewModel.shareViaBluetooth(it) } },
                    enabled  = stripUri != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1976D2),  // Bluetooth blue
                    ),
                ) {
                    Text(
                        localizedString(settings.appLanguage, "📶 בלוטוט'", "📶 Bluetooth"),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // DONE / New session
            TextButton(
                onClick  = { viewModel.resetSession(); onDone() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    localizedString(settings.appLanguage, "סיים / צילום נוסף", "Done / New Session"),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun localizedString(lang: AppLanguage, he: String, en: String) =
    when (lang) {
        AppLanguage.HEBREW    -> he
        AppLanguage.ENGLISH   -> en
        AppLanguage.BILINGUAL -> "$he / $en"
    }
