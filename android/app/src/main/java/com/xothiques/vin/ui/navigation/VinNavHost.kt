package com.xothiques.vin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xothiques.vin.ui.auth.ServerSetupScreen
import com.xothiques.vin.ui.common.FullScreenLoading

@Composable
fun VinNavHost(rootViewModel: RootViewModel = hiltViewModel()) {
    val session by rootViewModel.session.collectAsStateWithLifecycle()

    when {
        session == null -> FullScreenLoading()
        session!!.serverBaseUrl.isBlank() -> ServerSetupScreen()
        !session!!.isLoggedIn -> AuthNavGraph()
        else -> MainNavGraph()
    }
}
