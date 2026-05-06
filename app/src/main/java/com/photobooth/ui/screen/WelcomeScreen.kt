package com.photobooth.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photobooth.data.model.AppLanguage
import com.photobooth.data.model.AppTheme
import com.photobooth.ui.viewmodel.SettingsViewModel

@Composable
fun WelcomeScreen(
    onStartSession: () -> Unit,
    onOpenSettings: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsState()

    // Pulsing animation on SHOOT button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor      = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        // ── Settings button (top-right corner) ──────────────────────────
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(26.dp),
            )
        }

        // ── Main content ─────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            // Event name
            Text(
                text = settings.eventName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            // App title
            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "פוטו בוט"
                    AppLanguage.ENGLISH   -> "PHOTO BOOTH"
                    AppLanguage.BILINGUAL -> "PHOTO BOOTH · פוטו בוט"
                },
                style = MaterialTheme.typography.displayMedium,
                color = primaryColor,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp,
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "3 תמונות · 1 רגע מושלם"
                    AppLanguage.ENGLISH   -> "3 SHOTS · 1 PERFECT MOMENT"
                    AppLanguage.BILINGUAL -> "3 תמונות · 3 SHOTS"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(64.dp))

            // ── SHOOT button ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f)),
                        )
                    )
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
                    .clickable { onStartSession() },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📸",
                        fontSize = 52.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (settings.appLanguage) {
                            AppLanguage.HEBREW    -> "צלם!"
                            AppLanguage.ENGLISH   -> "SHOOT!"
                            AppLanguage.BILINGUAL -> "SHOOT!\nצלם!"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Photo count hint
            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "יצלמו 3 תמונות אחת אחרי השנייה"
                    AppLanguage.ENGLISH   -> "3 photos will be taken in sequence"
                    AppLanguage.BILINGUAL -> "יצלמו 3 תמונות   •   3 photos in sequence"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
