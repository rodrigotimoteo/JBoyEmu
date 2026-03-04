package com.github.rodrigotimoteo.kboyemu.presentation.emulator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.uistate.EmulatorUiState
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel.KBoyEmulatorViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EmulatorScreen(
    modifier: Modifier = Modifier,
    viewModel: KBoyEmulatorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.pauseEmulation()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeEmulation()
    }

    when (state) {
        EmulatorUiState.WaitingForRom -> RomPickerScreen(
            modifier = modifier,
            onRomSelected = { uri -> viewModel.loadRom(uri) }
        )

        EmulatorUiState.Running -> RunningEmulatorScreen(
            modifier = modifier,
            viewModel = viewModel
        )
    }
}

@Composable
private fun RomPickerScreen(
    modifier: Modifier = Modifier,
    onRomSelected: (android.net.Uri) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onRomSelected) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "KBoy Emulator",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select a Game Boy ROM to play",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { launcher.launch(arrayOf("*/*")) }
            ) {
                Text("Select ROM")
            }
        }
    }
}

@Composable
private fun RunningEmulatorScreen(
    modifier: Modifier = Modifier,
    viewModel: KBoyEmulatorViewModel,
) {
    val image by viewModel.frameBitmap.collectAsState()

    Column(modifier = modifier) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(160f / 144f)
        )

        EmulatorControls(viewModel = viewModel)
    }
}
