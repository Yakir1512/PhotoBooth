package com.photobooth.ui.screen

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.photobooth.data.model.AppLanguage
import com.photobooth.ui.viewmodel.PhotoBoothViewModel
import com.photobooth.ui.viewmodel.SettingsViewModel

/**
 * Opening screen.
 *
 * FIXES applied here
 * ──────────────────
 * FIX A – Camera background:
 *   PreviewView is created with `remember` so the same View instance
 *   survives recompositions.  The camera is bound exactly ONCE via
 *   LaunchedEffect(lifecycleOwner) instead of inside the `update` lambda,
 *   which was triggering unbindAll() on every recomposition.
 *
 * FIX B – Camera permission:
 *   Permission is requested here so that the live preview is visible
 *   immediately when the screen opens, before the user taps SHOOT.
 *
 * FIX C – No session logic on WelcomeScreen:
 *   WelcomeScreen only navigates; CaptureScreen owns startSession().
 *
 * FIX D – Shared ViewModel:
 *   `viewModel` is passed in from NavGraph (not created with hiltViewModel()
 *   here), so WelcomeScreen and CaptureScreen share the exact same instance.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WelcomeScreen(
    onStartSession: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: PhotoBoothViewModel,                          // shared instance from NavGraph
    settingsViewModel: SettingsViewModel = hiltViewModel(),  // screen-local settings
) {
    val settings       by settingsViewModel.settings.collectAsState()
    val lifecycleOwner  = LocalLifecycleOwner.current
    val context         = LocalContext.current

    // ── Camera permission ──────────────────────────────────────────────────
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Request permission as soon as the screen appears
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // ── PreviewView – created once, never recreated on recomposition ───────
    // FIX A: Using remember{} means the same PreviewView instance is reused.
    // Previously AndroidView's `update` lambda was calling bindCamera() on
    // every recomposition, which called cameraProvider.unbindAll() repeatedly.
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // FIX A: Bind camera exactly once (or when lifecycleOwner changes).
    // LaunchedEffect with lifecycleOwner as key guarantees a single bind call.
    LaunchedEffect(lifecycleOwner, cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted) {
            viewModel.bindCamera(lifecycleOwner, previewView)
        }
    }

    // ── Pulsing SHOOT button animation ─────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    // ── Layout ─────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // Live camera preview as full-screen background
        AndroidView(
            factory  = { previewView },   // factory returns the remembered View
            modifier = Modifier.fillMaxSize(),
            // update lambda intentionally left empty:
            // binding is handled by LaunchedEffect above
        )

        // Dark scrim so text is legible over the camera feed
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // ── Settings gear (top-right) ──────────────────────────────────────
        IconButton(
            onClick  = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.Settings,
                contentDescription = "Settings",
                tint               = Color.White.copy(alpha = 0.75f),
                modifier           = Modifier.size(26.dp),
            )
        }

        // ── Central content ────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
        ) {

            // Event name
            Text(
                text      = settings.eventName,
                style     = MaterialTheme.typography.headlineLarge,
                color     = Color.White.copy(alpha = 0.9f),
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
                style         = MaterialTheme.typography.displayMedium,
                color         = MaterialTheme.colorScheme.primary,
                fontWeight    = FontWeight.Black,
                textAlign     = TextAlign.Center,
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
                style         = MaterialTheme.typography.bodyMedium,
                color         = Color.White.copy(alpha = 0.7f),
                textAlign     = TextAlign.Center,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(64.dp))

            // ── SHOOT button ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            ),
                        )
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    // FIX C: only navigate – session starts in CaptureScreen
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
                        style      = MaterialTheme.typography.titleLarge,
                        color      = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black,
                        textAlign  = TextAlign.Center,
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
                style     = MaterialTheme.typography.bodyMedium,
                color     = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }

        // "No camera permission" notice (shown only if denied)
        if (!cameraPermission.status.isGranted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text  = "נדרשת הרשאת מצלמה | Camera permission required",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}