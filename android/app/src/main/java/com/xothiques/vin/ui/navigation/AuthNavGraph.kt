package com.xothiques.vin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xothiques.vin.ui.auth.JoinHouseholdScreen
import com.xothiques.vin.ui.auth.LoginScreen
import com.xothiques.vin.ui.auth.RegisterHouseholdScreen
import com.xothiques.vin.ui.auth.WelcomeScreen

/**
 * Once any of these screens succeeds, SessionManager's DataStore emits a new
 * Session with isLoggedIn = true, which VinNavHost observes to swap this
 * whole graph out for MainNavGraph -- so the on-success callbacks here don't
 * need to navigate anywhere themselves.
 */
@Composable
fun AuthNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AuthRoutes.WELCOME) {
        composable(AuthRoutes.WELCOME) {
            WelcomeScreen(
                onLogin = { navController.navigate(AuthRoutes.LOGIN) },
                onRegisterHousehold = { navController.navigate(AuthRoutes.REGISTER_HOUSEHOLD) },
                onJoinHousehold = { navController.navigate(AuthRoutes.JOIN_HOUSEHOLD) },
            )
        }
        composable(AuthRoutes.LOGIN) {
            LoginScreen(onLoggedIn = {}, onBack = { navController.popBackStack() })
        }
        composable(AuthRoutes.REGISTER_HOUSEHOLD) {
            RegisterHouseholdScreen(onRegistered = {}, onBack = { navController.popBackStack() })
        }
        composable(AuthRoutes.JOIN_HOUSEHOLD) {
            JoinHouseholdScreen(onJoined = {}, onBack = { navController.popBackStack() })
        }
    }
}
