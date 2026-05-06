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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.photobooth.domain.service.CameraService
import com.photobooth.ui.viewmodel.PhotoBoothViewModel
import com.photobooth.ui.viewmodel.SettingsViewModel
import javax.inject.Inject

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onSessionComplete: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: PhotoBoothViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val session  by viewModel.session.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Navigate away when session complete
    LaunchedEffect(session.sessionState) {
        if (session.sessionState == SessionState.REVIEW) {
            onSessionComplete(session.id)
        }
    }

    // Start session as soon as we have permission
    LaunchedEffect(cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted) {
            viewModel.startSession()
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera preview ──────────────────────────────────────────────
        if (cameraPermission.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    // CameraService is injected into ViewModel; we bind here via a side-effect
                    // In a real project, expose a bindCamera method on the ViewModel
                }
            )
        }

        // ── Close button ────────────────────────────────────────────────
        IconButton(
            onClick = { viewModel.resetSession(); onCancel() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
        }

        // ── Photo counter (top-center) ──────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            val label = when (settings.appLanguage) {
                AppLanguage.HEBREW    -> "צילום ${session.currentPhotoIndex + 1} מתוך 3"
                AppLanguage.ENGLISH   -> "Photo ${session.currentPhotoIndex + 1} of 3"
                AppLanguage.BILINGUAL -> "צילום ${session.currentPhotoIndex + 1} / Photo ${session.currentPhotoIndex + 1} of 3"
            }
            Text(
                text  = label,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Countdown overlay ───────────────────────────────────────────
        AnimatedVisibility(
            visible = session.sessionState == SessionState.COUNTING_DOWN,
            enter   = scaleIn() + fadeIn(),
            exit    = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CountdownCircle(number = session.countdownValue)
        }

        // ── "SMILE!" flash label ────────────────────────────────────────
        AnimatedVisibility(
            visible = session.sessionState == SessionState.CAPTURING,
            enter   = fadeIn(),
            exit    = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "😊 תחייכו!"
                    AppLanguage.ENGLISH   -> "😊 SMILE!"
                    AppLanguage.BILINGUAL -> "😊 SMILE! • תחייכו!"
                },
                fontSize   = 56.sp,
                fontWeight = FontWeight.Black,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                modifier   = Modifier
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            )
        }

        // ── Processing indicator ────────────────────────────────────────
        AnimatedVisibility(
            visible = session.sessionState == SessionState.PROCESSING,
            enter   = fadeIn(),
            exit    = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = when (settings.appLanguage) {
                        AppLanguage.HEBREW    -> "מעבד תמונות..."
                        AppLanguage.ENGLISH   -> "Processing..."
                        AppLanguage.BILINGUAL -> "Processing... · מעבד..."
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // ── Strip preview (bottom) – thumbnails of captured photos ──────
        if (session.currentPhotoIndex > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (index < session.currentPhotoIndex)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else
                                    Color.White.copy(alpha = 0.1f)
                            )
                            .border(
                                width = if (index < session.currentPhotoIndex) 2.dp else 1.dp,
                                color = if (index < session.currentPhotoIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (index < session.currentPhotoIndex) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                        } else {
                            Text("${index + 1}", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownCircle(number: Int) {
    val animatedScale by animateFloatAsState(
        targetValue = if (number > 0) 1f else 0.5f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "countdownScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(animatedScale)
            .size(180.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f))
            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
    ) {
        AnimatedContent(
            targetState = number,
            transitionSpec = {
                scaleIn(initialScale = 1.4f) + fadeIn() togetherWith
                scaleOut(targetScale = 0.6f) + fadeOut()
            },
            label = "countdownNumber"
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
