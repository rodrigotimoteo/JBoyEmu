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

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KBoyEmuTheme {
    }
}