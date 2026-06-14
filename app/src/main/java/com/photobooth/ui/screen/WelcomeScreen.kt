package com.photobooth.ui.screen

import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.photobooth.data.model.AppLanguage
import com.photobooth.ui.viewmodel.PhotoBoothViewModel
import com.photobooth.ui.viewmodel.SettingsViewModel

@Composable
fun WelcomeScreen(
    onStartSession: () -> Unit,
    onOpenSettings: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    photoViewModel: PhotoBoothViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview Background
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                photoViewModel.bindCamera(lifecycleOwner, previewView)
            }
        )

        // Dark overlay for readability
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

        // Settings button
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
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp).align(Alignment.Center),
        ) {
            Text(
                text = settings.eventName,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "פוטו בוט"
                    AppLanguage.ENGLISH   -> "PHOTO BOOTH"
                    AppLanguage.BILINGUAL -> "PHOTO BOOTH · פוטו בוט"
                },
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "3 תמונות · 1 רגע מושלם"
                    AppLanguage.ENGLISH   -> "3 SHOTS · 1 PERFECT MOMENT"
                    AppLanguage.BILINGUAL -> "3 תמונות · 3 SHOTS"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(64.dp))

            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        )
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable { onStartSession() },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📸", fontSize = 52.sp)
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

            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "יצלמו 3 תמונות אחת אחרי השנייה"
                    AppLanguage.ENGLISH   -> "3 photos will be taken in sequence"
                    AppLanguage.BILINGUAL -> "יצלמו 3 תמונות   •   3 photos in sequence"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
