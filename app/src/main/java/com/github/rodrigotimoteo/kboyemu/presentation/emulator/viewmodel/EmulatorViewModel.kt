package com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.kboyemu.domain.buttons.usecase.PressButtonUseCase
import com.github.rodrigotimoteo.kboyemu.domain.buttons.usecase.ReleaseButtonUseCase
import com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase.ResumeEmulationUseCase
import com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase.StopEmulationUseCase
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.util.translateCgbPixelsToArgb
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.util.translateGbPixelsToArgb
import com.github.rodrigotimoteo.kboyemu.util.HEIGHT
import com.github.rodrigotimoteo.kboyemu.util.WIDTH
import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for the emulator screen. Manages frame rendering, button input, and
 * lifecycle-aware pause/resume of the emulation loop.
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalUnsignedTypes::class)
@KoinViewModel
class EmulatorViewModel(
    private val emulator: KBoyEmulator,
    private val pressButtonUseCase: PressButtonUseCase,
    private val releaseButtonUseCase: ReleaseButtonUseCase,
    private val stopEmulationUseCase: StopEmulationUseCase,
    private val resumeEmulationUseCase: ResumeEmulationUseCase,
) : ViewModel() {

    /** Buffer for translating emulator pixel data to ARGB format for Android Bitmap rendering */
    private val argbBuffer = IntArray(WIDTH * HEIGHT)

    /** Current frame bitmap for display */
    val frameBitmap = MutableStateFlow(ImageBitmap(WIDTH, HEIGHT))

    /** Job collecting frames from the emulator */
    private var frameCollectorJob: Job? = null

    /**
     * Starts collecting frames from the emulator. Called after a ROM is loaded and whenever the
     * app returns to the foreground.
     */
    fun resumeEmulation() {
        resumeEmulationUseCase()
        startFrameCollection()
    }

    /**
     * Pauses emulation and stops audio when the app goes to the background
     */
    fun pauseEmulation() {
        frameCollectorJob?.cancel()
        frameCollectorJob = null
        stopEmulationUseCase()
    }

    /**
     * Sends a button press to the emulator
     *
     * @param button the button to press
     */
    fun press(button: Button) = pressButtonUseCase(button)

    /**
     * Sends a button release to the emulator
     *
     * @param button the button to release
     */
    fun release(button: Button) = releaseButtonUseCase(button)

    /**
     * Stops the emulator and audio playback when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        stopEmulationUseCase()
    }

    /**
     * Starts or restarts the coroutine that collects frames from the emulator and translates
     * them into [ImageBitmap] for the UI
     */
    private fun startFrameCollection() {
        frameCollectorJob?.cancel()
        frameCollectorJob = viewModelScope.launch {
            emulator.frames.collect { frameBuffer ->
                val colorPixels = frameBuffer.colorPixels
                if (colorPixels != null) {
                    translateCgbPixelsToArgb(colorPixels, argbBuffer)
                } else {
                    translateGbPixelsToArgb(frameBuffer.pixels, argbBuffer)
                }

                val bitmap = createBitmap(WIDTH, HEIGHT)
                bitmap.setPixels(argbBuffer, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
                frameBitmap.value = bitmap.asImageBitmap()
            }
        }
    }
}
