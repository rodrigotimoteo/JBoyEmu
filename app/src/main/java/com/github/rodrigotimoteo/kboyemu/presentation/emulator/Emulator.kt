package com.github.rodrigotimoteo.kboyemu.presentation.emulator

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel.KBoyEmulatorViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EmulatorScreen(
    modifier: Modifier = Modifier,
    viewModel: KBoyEmulatorViewModel = koinViewModel(),
) {
    val image by viewModel.frameBitmap.collectAsState()

    Image(
        bitmap = image,
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 3f
                scaleY = 3f
            }
    )

}