package com.zestyy.struct.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zestyy.struct.location.TrackingManager
import com.zestyy.struct.location.TrackingState
import com.zestyy.struct.ui.screens.*

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Tracking : Screen("tracking")
    data object Summary : Screen("summary/{routeId}") {
        fun of(routeId: Long) = "summary/$routeId"
    }
    data object RouteBuilder : Screen("route_builder")
    data object Library : Screen("library")
    data object Follow : Screen("follow/{routeId}") {
        fun of(routeId: Long) = "follow/$routeId"
    }
}

@Composable
fun StructNavGraph() {
    val navController = rememberNavController()

    // If a recording is already in progress (service survives app-swipe / relaunch), open
    // straight into the tracking screen instead of Home — matches tapping the persistent
    // notification, and means force-closing/reopening the app doesn't lose your place.
    val startDestination = if (TrackingManager.state.value.state != TrackingState.IDLE)
        Screen.Tracking.route else Screen.Home.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartTracking = { navController.navigate(Screen.Tracking.route) },
                onBuildRoute = { navController.navigate(Screen.RouteBuilder.route) },
                onOpenLibrary = { navController.navigate(Screen.Library.route) }
            )
        }
        composable(Screen.Tracking.route) {
            TrackingScreen(
                onFinished = { routeId ->
                    navController.navigate(Screen.Summary.of(routeId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onCancel = {
                    if (!navController.popBackStack()) {
                        // we were launched straight into Tracking (resumed session) — there's
                        // nothing to pop back to, so go to Home instead of leaving a dead end
                        navController.navigate(Screen.Home.route) { popUpTo(0) }
                    }
                }
            )
        }
        composable(
            Screen.Summary.route,
            arguments = listOf(navArgument("routeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getLong("routeId") ?: -1L
            SummaryScreen(
                routeId = routeId,
                onBack = { navController.popBackStack(Screen.Home.route, false) },
                onFollow = { navController.navigate(Screen.Follow.of(routeId)) }
            )
        }
        composable(Screen.RouteBuilder.route) {
            RouteBuilderScreen(
                onSaved = { navController.popBackStack(Screen.Home.route, false) },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onOpenRoute = { id -> navController.navigate(Screen.Summary.of(id)) },
                onFollowRoute = { id -> navController.navigate(Screen.Follow.of(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Follow.route,
            arguments = listOf(navArgument("routeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getLong("routeId") ?: -1L
            FollowRouteScreen(routeId = routeId, onExit = { navController.popBackStack() })
        }
    }
}
