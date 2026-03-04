package com.github.rodrigotimoteo.kboyemu.presentation.emulator.controls

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.rodrigotimoteo.kboyemu.presentation.theme.GBButtonA
import com.github.rodrigotimoteo.kboyemu.presentation.theme.GBButtonStart
import com.github.rodrigotimoteo.kboyemu.presentation.theme.GBDPadColor
import com.github.rodrigotimoteo.kboyemucore.api.Button
import timber.log.Timber

private const val OPAQUE = 1f

/**
 * Game Boy-inspired control layout with a cross-shaped D-pad, circular A/B buttons,
 * and pill-shaped Start/Select buttons. Supports a configurable alpha for overlay use
 * in landscape mode.
 *
 * @param onPress callback invoked when a button is pressed
 * @param onRelease callback invoked when a button is released
 * @param alpha opacity of the control elements (1f = opaque, 0f = invisible)
 * @param modifier modifier applied to the root layout
 *
 * @author rodrigotimoteo
 */
@Composable
fun EmulatorControls(
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = OPAQUE,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DPad(
                onPress = onPress,
                onRelease = onRelease,
                alpha = alpha,
                modifier = Modifier.padding(start = 16.dp),
            )

            ActionButtons(
                onPress = onPress,
                onRelease = onRelease,
                alpha = alpha,
                modifier = Modifier.padding(end = 16.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        StartSelectButtons(
            onPress = onPress,
            onRelease = onRelease,
            alpha = alpha,
        )
    }
}

/**
 * Cross-shaped D-pad made of overlapping vertical and horizontal bars
 */
@Composable
private fun DPad(
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val armWidth = 52.dp

    Box(
        modifier = modifier.size(armWidth * 3),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .width(armWidth)
                .height(armWidth * 3)
                .alpha(alpha),
            shape = RoundedCornerShape(6.dp),
            color = GBDPadColor,
            shadowElevation = 4.dp,
        ) {}

        Surface(
            modifier = Modifier
                .width(armWidth * 3)
                .height(armWidth)
                .alpha(alpha),
            shape = RoundedCornerShape(6.dp),
            color = GBDPadColor,
            shadowElevation = 4.dp,
        ) {}

        DPadTouchZone(
            button = Button.UP,
            onPress = onPress,
            onRelease = onRelease,
            modifier = Modifier
                .size(armWidth)
                .align(Alignment.TopCenter),
        )

        DPadTouchZone(
            button = Button.DOWN,
            onPress = onPress,
            onRelease = onRelease,
            modifier = Modifier
                .size(armWidth)
                .align(Alignment.BottomCenter),
        )

        DPadTouchZone(
            button = Button.LEFT,
            onPress = onPress,
            onRelease = onRelease,
            modifier = Modifier
                .size(armWidth)
                .align(Alignment.CenterStart),
        )

        DPadTouchZone(
            button = Button.RIGHT,
            onPress = onPress,
            onRelease = onRelease,
            modifier = Modifier
                .size(armWidth)
                .align(Alignment.CenterEnd),
        )
    }
}

/**
 * Invisible touch zone for a single D-pad direction
 */
@Composable
private fun DPadTouchZone(
    button: Button,
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.pointerInput(button) {
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
                },
            )
        },
    )
}

/**
 * Circular A and B buttons arranged at a slight diagonal like the original Game Boy
 */
@Composable
private fun ActionButtons(
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(
                button = Button.B,
                onPress = onPress,
                onRelease = onRelease,
                size = 60.dp,
                color = GBButtonA,
                alpha = alpha,
                modifier = Modifier.offset(y = 16.dp),
            )

            CircleButton(
                button = Button.A,
                onPress = onPress,
                onRelease = onRelease,
                size = 60.dp,
                color = GBButtonA,
                alpha = alpha,
                modifier = Modifier.offset(y = (-16).dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 36.dp)
                .alpha(alpha),
            horizontalArrangement = Arrangement.spacedBy(44.dp),
        ) {
            Text(
                text = "B",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "A",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * A circular pressable button with configurable opacity
 */
@Suppress("LongParameterList")
@Composable
private fun CircleButton(
    button: Button,
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    size: Dp,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(size)
            .alpha(alpha)
            .clip(CircleShape)
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
                    },
                )
            },
        shape = CircleShape,
        color = color,
        shadowElevation = 6.dp,
    ) {}
}

/**
 * Pill-shaped Start and Select buttons
 */
@Composable
private fun StartSelectButtons(
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillButton(
            label = "SELECT",
            button = Button.SELECT,
            onPress = onPress,
            onRelease = onRelease,
            alpha = alpha,
        )

        PillButton(
            label = "START",
            button = Button.START,
            onPress = onPress,
            onRelease = onRelease,
            alpha = alpha,
        )
    }
}

/**
 * A small pill-shaped pressable button for Start/Select
 */
@Composable
private fun PillButton(
    label: String,
    button: Button,
    onPress: (Button) -> Unit,
    onRelease: (Button) -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = modifier
                .width(56.dp)
                .height(20.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(50))
                .rotate(-25f)
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
                        },
                    )
                },
            shape = RoundedCornerShape(50),
            color = GBButtonStart,
            shadowElevation = 2.dp,
        ) {}

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            modifier = Modifier.alpha(alpha),
        )
    }
}
