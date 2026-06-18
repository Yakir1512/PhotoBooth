package com.photobooth.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photobooth.ui.screen.*
import com.photobooth.ui.viewmodel.PhotoBoothViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Type-safe route definitions.
// Adding a new screen = add a Route + one composable {} block below.
// Nothing else changes.
// ─────────────────────────────────────────────────────────────────────────────
sealed class Route(val path: String) {
    data object Welcome  : Route("welcome")
    data object Capture  : Route("capture")
    data object Review   : Route("review/{sessionId}") {
        fun withSessionId(id: String) = "review/$id"
    }
    data object Settings : Route("settings")
    data object PinEntry : Route("pin_entry")
}

@Composable
fun PhotoBoothNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    // ─────────────────────────────────────────────────────────────────────────
    // FIX #1 – SHARED VIEWMODEL
    //
    // Calling hiltViewModel() HERE (inside PhotoBoothNavGraph but OUTSIDE any
    // individual composable { } destination) scopes the ViewModel to whatever
    // ViewModelStoreOwner owns this composable – which is MainActivity.
    //
    // Both WelcomeScreen and CaptureScreen receive the EXACT SAME instance,
    // so session state (countdown, captured bitmaps, etc.) is never lost when
    // navigating between the two screens.
    // ─────────────────────────────────────────────────────────────────────────
    val sharedPhotoBoothViewModel: PhotoBoothViewModel = hiltViewModel()

    NavHost(
        navController    = navController,
        startDestination = Route.Welcome.path,
    ) {

        composable(Route.Welcome.path) {
            WelcomeScreen(
                onStartSession = { navController.navigate(Route.Capture.path) },
                onOpenSettings = { navController.navigate(Route.PinEntry.path) },
                viewModel      = sharedPhotoBoothViewModel,  // shared instance
            )
        }

        composable(Route.Capture.path) {
            CaptureScreen(
                onSessionComplete = { sessionId ->
                    navController.navigate(Route.Review.withSessionId(sessionId)) {
                        // Remove CaptureScreen from back-stack so Back doesn't re-enter it
                        popUpTo(Route.Capture.path) { inclusive = true }
                    }
                },
                onCancel  = { navController.popBackStack() },
                viewModel = sharedPhotoBoothViewModel,  // same shared instance
            )
        }

        composable(Route.Review.path) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ReviewScreen(
                sessionId = sessionId,
                onDone    = {
                    navController.navigate(Route.Welcome.path) {
                        popUpTo(Route.Welcome.path) { inclusive = true }
                    }
                },
                viewModel = sharedPhotoBoothViewModel
            )
        }

        composable(Route.PinEntry.path) {
            PinEntryScreen(
                onCorrectPin = {
                    navController.navigate(Route.Settings.path) {
                        popUpTo(Route.PinEntry.path) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = {
                    navController.navigate(Route.Welcome.path) {
                        popUpTo(Route.Welcome.path) { inclusive = true }
                    }
                },
            )
        }
    }
}