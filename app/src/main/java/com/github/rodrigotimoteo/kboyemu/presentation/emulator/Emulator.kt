package com.github.rodrigotimoteo.kboyemu.presentation.emulator

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.controls.EmulatorControls
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel.EmulatorViewModel
import org.koin.androidx.compose.koinViewModel

private const val LANDSCAPE_CONTROLS_ALPHA = 0.35f
private const val GAME_ASPECT_RATIO = 160f / 144f

/**
 * Emulator screen showing the game display and controls. In portrait mode the game screen sits
 * at the top and the controls are pushed toward the bottom for comfortable grip. In landscape
 * mode the game fills the screen and semi-transparent controls overlay on top.
 *
 * @param onNavigateToSettings callback invoked when the user taps the settings gear icon
 * @param modifier modifier applied to the root layout
 *
 * @author rodrigotimoteo
 */
@Composable
fun EmulatorScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmulatorViewModel = koinViewModel(),
) {
    val image by viewModel.frameBitmap.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.pauseEmulation()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeEmulation()
    }

    if (isLandscape) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Image(
                bitmap = image,
                contentDescription = "Game screen",
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(GAME_ASPECT_RATIO)
                    .align(Alignment.Center),
            )

            EmulatorControls(
                onPress = viewModel::press,
                onRelease = viewModel::release,
                alpha = LANDSCAPE_CONTROLS_ALPHA,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomCenter),
            )

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
            ) {
                Image(
                    bitmap = image,
                    contentDescription = "Game screen",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(GAME_ASPECT_RATIO),
                )

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            EmulatorControls(
                onPress = viewModel::press,
                onRelease = viewModel::release,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}
