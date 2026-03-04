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
import androidx.compose.ui.tooling.preview.Preview
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.EmulatorScreen
import com.github.rodrigotimoteo.kboyemu.presentation.theme.KBoyEmuTheme

/**
 * Main activity for the KBoy emulator app
 *
 * @author rodrigotimoteo
 */
class KBoyMainActivity : ComponentActivity() {

    /**
     * Called when the activity is created. Sets up the content view and enables edge-to-edge display.
     *
     * @param savedInstanceState The saved instance state bundle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Content()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    /**
     * Composable function that sets up the main content of the activity
     */
    @Composable
    fun Content() {
        KBoyEmuTheme {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                EmulatorScreen(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KBoyEmuTheme {
    }
}
