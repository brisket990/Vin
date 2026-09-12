package com.xothiques.vin.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xothiques.vin.ui.bottle.BottleDetailScreen
import com.xothiques.vin.ui.bottle.BottleFormScreen
import com.xothiques.vin.ui.cellar.CellarGridScreen
import com.xothiques.vin.ui.dashboard.DashboardScreen
import com.xothiques.vin.ui.pairing.PairingScreen
import com.xothiques.vin.ui.scan.ScanScreen
import com.xothiques.vin.ui.settings.AiProviderSettingsScreen
import com.xothiques.vin.ui.settings.SettingsScreen
import com.xothiques.vin.ui.wishlist.WishlistScreen

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS = listOf(
    BottomTab(MainRoutes.CELLAR, "Cave", Icons.Filled.WineBar),
    BottomTab(MainRoutes.WISHLIST, "Envies", Icons.Filled.CardGiftcard),
    BottomTab(MainRoutes.PAIRING, "Accords", Icons.Filled.Restaurant),
    BottomTab(MainRoutes.DASHBOARD, "Tableau de bord", Icons.Filled.SpaceDashboard),
    BottomTab(MainRoutes.SETTINGS, "Réglages", Icons.Filled.Settings),
)

/**
 * Main graph: cave / envies / accords / tableau de bord / réglages live
 * behind a bottom bar. Bottle detail/form, the scan flow and AI provider
 * settings are pushed on top of it.
 */
@Composable
fun MainNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { MainBottomBar(navController) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = navController, startDestination = MainRoutes.CELLAR) {
                composable(MainRoutes.CELLAR) {
                    CellarGridScreen(
                        onOpenBottle = { bottleId ->
                            navController.navigate(MainRoutes.bottleDetail(bottleId))
                        },
                        onAddBottle = { locationId ->
                            navController.navigate(MainRoutes.bottleForm(locationId))
                        },
                        onScan = { navController.navigate(MainRoutes.scan()) },
                    )
                }
                composable(MainRoutes.WISHLIST) { WishlistScreen() }
                composable(MainRoutes.PAIRING) { PairingScreen() }
                composable(MainRoutes.DASHBOARD) { DashboardScreen() }
                composable(MainRoutes.SETTINGS) {
                    SettingsScreen(
                        onSignedOut = {
                            // SessionManager's DataStore flips isLoggedIn to false, which
                            // VinNavHost observes to swap this whole graph out for
                            // AuthNavGraph -- nothing to navigate to here.
                        },
                        onOpenAiProviderSettings = {
                            navController.navigate(MainRoutes.AI_PROVIDER_SETTINGS)
                        },
                    )
                }
                composable(MainRoutes.AI_PROVIDER_SETTINGS) {
                    AiProviderSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = MainRoutes.SCAN,
                    arguments = listOf(
                        navArgument("locationId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) {
                    ScanScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable(
                    route = MainRoutes.BOTTLE_DETAIL,
                    arguments = listOf(navArgument("bottleId") { type = NavType.StringType }),
                ) {
                    BottleDetailScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { bottleId ->
                            navController.navigate(MainRoutes.bottleFormEdit(bottleId))
                        },
                    )
                }
                composable(
                    route = MainRoutes.BOTTLE_FORM_EDIT,
                    arguments = listOf(navArgument("bottleId") { type = NavType.StringType }),
                ) {
                    BottleFormScreen(
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = MainRoutes.BOTTLE_FORM,
                    arguments = listOf(
                        navArgument("locationId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) {
                    BottleFormScreen(
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        BOTTOM_TABS.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}
