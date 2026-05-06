package com.photobooth.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photobooth.ui.screen.*

/**
 * Sealed class for type-safe routes.
 * Adding a new screen = add a Route + composable() below. Nothing else changes.
 */
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
    NavHost(
        navController = navController,
        startDestination = Route.Welcome.path,
    ) {
        composable(Route.Welcome.path) {
            WelcomeScreen(
                onStartSession  = { navController.navigate(Route.Capture.path) },
                onOpenSettings  = { navController.navigate(Route.PinEntry.path) },
            )
        }

        composable(Route.Capture.path) {
            CaptureScreen(
                onSessionComplete = { sessionId ->
                    navController.navigate(Route.Review.withSessionId(sessionId)) {
                        popUpTo(Route.Capture.path) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(Route.Review.path) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ReviewScreen(
                sessionId = sessionId,
                onDone = {
                    navController.navigate(Route.Welcome.path) {
                        popUpTo(Route.Welcome.path) { inclusive = true }
                    }
                },
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
