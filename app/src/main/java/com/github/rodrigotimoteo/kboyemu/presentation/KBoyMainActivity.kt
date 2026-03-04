package com.github.rodrigotimoteo.kboyemu.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.github.rodrigotimoteo.kboyemu.presentation.navigation.KBoyNavHost
import com.github.rodrigotimoteo.kboyemu.presentation.theme.KBoyEmuTheme

/**
 * Main activity for the KBoy emulator app. Hosts the navigation graph and applies the theme.
 *
 * @author rodrigotimoteo
 */
class KBoyMainActivity : ComponentActivity() {

    /**
     * Called when the activity is created. Sets up edge-to-edge display and the Compose content.
     *
     * @param savedInstanceState the saved instance state bundle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Content()
        }
    }

    /**
     * Root composable that wires up the theme, scaffold, and navigation host
     */
    @Composable
    private fun Content() {
        KBoyEmuTheme {
            val navController = rememberNavController()

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                KBoyNavHost(
                    navController = navController,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }
        }
    }
}
