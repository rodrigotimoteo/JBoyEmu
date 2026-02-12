package com.github.rodrigotimoteo.kboyemu.presentation.emulator

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel.KBoyEmulatorViewModel
import com.github.rodrigotimoteo.kboyemucore.api.Button
import timber.log.Timber

@Composable
fun EmulatorControls(
    viewModel: KBoyEmulatorViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .height(180.dp)
                .aspectRatio(2f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(
                onPress = viewModel::press,
                onRelease = viewModel::release
            )

            ActionButtons(
                onPress = viewModel::press,
                onRelease = viewModel::release
            )
        }

        StartSelectButtons(
            onPress = viewModel::press,
            onRelease = viewModel::release
        )
    }
}

@Composable
private fun DPad(
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = 56.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            Spacer(modifier = Modifier.size(size))
            PressableButton("Up", Button.UP, onPress, onRelease, size = size)
            Spacer(modifier = Modifier.size(size))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PressableButton("Left", Button.LEFT, onPress, onRelease, size = size)
            PressableButton("Right", Button.RIGHT, onPress, onRelease, size = size)
        }
        Row(horizontalArrangement = Arrangement.Center) {
            Spacer(modifier = Modifier.size(size))
            PressableButton("Down", Button.DOWN, onPress, onRelease, size = size)
            Spacer(modifier = Modifier.size(size))
        }
    }
}

@Composable
private fun ActionButtons(
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressableButton("B", Button.B, onPress, onRelease, size = 64.dp)
        PressableButton("A", Button.A, onPress, onRelease, size = 72.dp)
    }
}

@Composable
private fun StartSelectButtons(
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressableButton("Select", Button.SELECT, onPress, onRelease, size = 72.dp)
        PressableButton("Start", Button.START, onPress, onRelease, size = 72.dp)
    }
}

@Composable
private fun PressableButton(
    label: String,
    button: Button,
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .pointerInput(button) {
                detectTapGestures(
                    onPress = {
                        Timber.i("Button $button pressed")
                        onPress(button)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onRelease(button)
                            Timber.i("Button $button released")
                        }
                    }
                )
            },
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

