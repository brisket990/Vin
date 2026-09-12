package com.xothiques.vin.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.xothiques.vin.ui.bottle.BottleDetailScreen
import com.xothiques.vin.ui.bottle.BottleFormScreen
import com.xothiques.vin.ui.cellar.CellarGridScreen
import com.xothiques.vin.ui.settings.SettingsScreen

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS = listOf(
    BottomTab(MainRoutes.CELLAR, "Cave", Icons.Filled.WineBar),
    BottomTab(MainRoutes.SETTINGS, "Réglages", Icons.Filled.Settings),
)

/**
 * v1 of the main graph: the cave grid and settings live behind a bottom bar,
 * bottle detail/form are pushed on top. Pairing, wishlist, dashboard, scan
 * and AI provider settings (all in the next batch of screens) will join the
 * bottom bar / be pushed from Settings the same way once they exist.
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
                        onOpenSettings = { navController.navigate(MainRoutes.SETTINGS) },
                    )
                }
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
                    AiProviderSettingsPlaceholder()
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

@Composable
private fun AiProviderSettingsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "La configuration des fournisseurs IA arrive dans le prochain lot d'écrans.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
