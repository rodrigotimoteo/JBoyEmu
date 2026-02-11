package com.github.rodrigotimoteo.kboyemu.presentation.emulator

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    )
}
