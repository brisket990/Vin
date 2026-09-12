package com.xothiques.vin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xothiques.vin.ui.navigation.VinNavHost
import com.xothiques.vin.ui.theme.VinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VinApp()
        }
    }
}

@Composable
private fun VinApp() {
    VinTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            VinNavHost()
        }
    }
}
