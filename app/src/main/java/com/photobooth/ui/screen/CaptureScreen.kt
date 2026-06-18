package com.photobooth.ui.screen

import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.photobooth.data.model.SessionState
import com.photobooth.ui.viewmodel.PhotoBoothViewModel
import com.photobooth.ui.viewmodel.SettingsViewModel

/**
 * Capture screen – runs the full automated photo sequence.
 *
 * FIXES applied here
 * ──────────────────
 * FIX 1 – startSession() is now called here, NOT on WelcomeScreen.
 *   The previous code never called startSession() anywhere, so the countdown
 *   never started.  The call happens inside LaunchedEffect after the camera
 *   is bound, giving CameraX time to initialise before the first capture.
 *
 * FIX 2 – Stable PreviewView via remember{}.
 *   Same pattern as WelcomeScreen: the view is created once and the camera
 *   is re-bound via LaunchedEffect, not via the `update` lambda.  This
 *   prevents repeated unbindAll() calls on recomposition.
 *
 * FIX 3 – Shared ViewModel received as parameter.
 *   The viewModel parameter comes from NavGraph, ensuring this screen and
 *   WelcomeScreen operate on the exact same ViewModel instance / state.
 *
 * FIX 4 – Animated countdown numbers using AnimatedContent.
 *   Each number scales in/out so the transition is visually clear.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onSessionComplete: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: PhotoBoothViewModel,                          // shared instance from NavGraph
    settingsViewModel: SettingsViewModel = hiltViewModel(),  // screen-local settings
) {
    val session  by viewModel.session.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val lifecycleOwner  = LocalLifecycleOwner.current
    val context         = LocalContext.current
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    // ── Stable PreviewView ─────────────────────────────────────────────────
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // ── Navigate to Review when the session is fully processed ─────────────
    LaunchedEffect(session.sessionState) {
        if (session.sessionState == SessionState.REVIEW) {
            onSessionComplete(session.id)
        }
    }

    // ── FIX 1 + FIX 2: bind camera then start session ─────────────────────
    // This effect fires once when permission is (or becomes) granted.
    // Binding the camera here re-points CameraX at CaptureScreen's PreviewView
    // (WelcomeScreen's PreviewView is released via unbindAll() inside bindCamera).
    // startSession() is idempotent: its guard in the ViewModel ignores the call
    // if the session is already running.
    LaunchedEffect(cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted) {
            viewModel.bindCamera(lifecycleOwner, previewView)  // rebind to this screen's view
            viewModel.startSession()                            // kick off countdown → capture loop
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }

    // ── Root container ─────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {

        // Camera preview
        if (cameraPermission.status.isGranted) {
            AndroidView(
                factory  = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Flash overlay (white flash on capture) ─────────────────────────
        AnimatedVisibility(
            visible  = session.sessionState == SessionState.CAPTURING,
            enter    = fadeIn(tween(60)),
            exit     = fadeOut(tween(350)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }

        // ── Cancel button ──────────────────────────────────────────────────
        IconButton(
            onClick  = { viewModel.resetSession(); onCancel() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
        }

        // ── Photo counter + dot progress (top-center) ──────────────────────
        Column(
            modifier            = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // "Photo X of 3" label
            val displayIndex = (session.currentPhotoIndex + 1).coerceIn(1, 3)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = when (settings.appLanguage) {
                        AppLanguage.HEBREW    -> "תמונה $displayIndex מתוך 3"
                        AppLanguage.ENGLISH   -> "Photo $displayIndex of 3"
                        AppLanguage.BILINGUAL -> "תמונה $displayIndex / Photo $displayIndex of 3"
                    },
                    color      = Color.White,
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Three dots: filled = done, bright = active, dim = upcoming
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { idx ->
                    val isDone   = idx < session.currentPhotoIndex
                    val isActive = idx == session.currentPhotoIndex &&
                            session.sessionState != SessionState.PROCESSING

                    Box(
                        modifier = Modifier
                            .size(if (isActive) 14.dp else 10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone   -> MaterialTheme.colorScheme.primary
                                    isActive -> Color.White
                                    else     -> Color.White.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }
        }

        // ── Countdown circle (center) ──────────────────────────────────────
        AnimatedVisibility(
            visible  = session.sessionState == SessionState.COUNTING_DOWN,
            enter    = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit     = scaleOut(targetScale = 0.5f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CountdownCircle(number = session.countdownValue)
        }

        // ── "SMILE!" text during capture ───────────────────────────────────
        AnimatedVisibility(
            visible  = session.sessionState == SessionState.CAPTURING,
            enter    = scaleIn() + fadeIn(),
            exit     = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "😊\nחייכו!"
                    AppLanguage.ENGLISH   -> "😊\nSMILE!"
                    AppLanguage.BILINGUAL -> "😊\nSMILE! • חייכו!"
                },
                fontSize   = 64.sp,
                fontWeight = FontWeight.Black,
                color      = Color.White,
                textAlign  = TextAlign.Center,
            )
        }

        // ── Building-strip indicator ───────────────────────────────────────
        AnimatedVisibility(
            visible  = session.sessionState == SessionState.PROCESSING,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(
                    color       = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier    = Modifier.size(64.dp),
                )
                Text(
                    text = when (settings.appLanguage) {
                        AppLanguage.HEBREW    -> "בונה תמונה..."
                        AppLanguage.ENGLISH   -> "Building strip..."
                        AppLanguage.BILINGUAL -> "Building strip... • בונה תמונה..."
                    },
                    color      = Color.White,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ── Permission denied message ──────────────────────────────────────
        if (!cameraPermission.status.isGranted) {
            Column(
                modifier            = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text      = "נדרשת הרשאת מצלמה\nCamera permission required",
                    color     = Color.White,
                    style     = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text("הענק הרשאה / Grant Permission")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Countdown circle with animated number transitions
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CountdownCircle(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.72f))
            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
    ) {
        // AnimatedContent swaps the digit with a scale+fade transition
        // so it's obvious when the count changes (3 → 2 → 1).
        AnimatedContent(
            targetState   = number,
            transitionSpec = {
                (scaleIn(initialScale = 1.6f, animationSpec = tween(220)) +
                        fadeIn(tween(220))) togetherWith
                        (scaleOut(targetScale = 0.4f, animationSpec = tween(220)) +
                                fadeOut(tween(220)))
            },
            label = "countdown",
        ) { count ->
            Text(
                text       = count.toString(),
                fontSize   = 96.sp,
                fontWeight = FontWeight.Black,
                color      = MaterialTheme.colorScheme.primary,
            )
        }
    }
}