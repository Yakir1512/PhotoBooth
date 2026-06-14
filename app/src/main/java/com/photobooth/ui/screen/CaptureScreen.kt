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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(session.sessionState) {
        if (session.sessionState == SessionState.REVIEW) {
            onSessionComplete(session.id)
        }
    }

    LaunchedEffect(cameraPermission.status.isGranted) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermission.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    viewModel.bindCamera(lifecycleOwner, previewView)
                }
            )
        }

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

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text  = when (settings.appLanguage) {
                    AppLanguage.HEBREW    -> "צילום ${session.currentPhotoIndex + 1} מתוך 3"
                    AppLanguage.ENGLISH   -> "Photo ${session.currentPhotoIndex + 1} of 3"
                    AppLanguage.BILINGUAL -> "צילום ${session.currentPhotoIndex + 1} / 3"
                },
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        AnimatedVisibility(
            visible = session.sessionState == SessionState.COUNTING_DOWN,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CountdownCircle(number = session.countdownValue)
        }

        if (session.sessionState == SessionState.CAPTURING) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.4f)))
        }
    }
}

@Composable
private fun CountdownCircle(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f))
            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
    ) {
        Text(
            text = number.toString(),
            fontSize = 96.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
