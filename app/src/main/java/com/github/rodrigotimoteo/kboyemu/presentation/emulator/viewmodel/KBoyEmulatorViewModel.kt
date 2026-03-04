package com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.kboyemu.domain.buttons.usecase.PressButtonUseCase
import com.github.rodrigotimoteo.kboyemu.domain.buttons.usecase.ReleaseButtonUseCase
import com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase.ResumeEmulationUseCase
import com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase.StopEmulationUseCase
import com.github.rodrigotimoteo.kboyemu.domain.rom.usecase.LoadRomUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase.LoadSaveStateUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase.SaveSaveStateUseCase
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.util.translateCgbPixelsToArgb
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.util.translateGbPixelsToArgb
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.uistate.EmulatorUiState
import com.github.rodrigotimoteo.kboyemu.util.HEIGHT
import com.github.rodrigotimoteo.kboyemu.util.WIDTH
import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for the emulator screen. Delegates all data and platform concerns to the domain and
 * data layers and focuses solely on UI state management and frame rendering.
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalUnsignedTypes::class)
@KoinViewModel
class KBoyEmulatorViewModel(
    private val emulator: KBoyEmulator,
    private val loadRomUseCase: LoadRomUseCase,
    private val pressButtonUseCase: PressButtonUseCase,
    private val releaseButtonUseCase: ReleaseButtonUseCase,
    private val saveSaveStateUseCase: SaveSaveStateUseCase,
    private val loadSaveStateUseCase: LoadSaveStateUseCase,
    private val stopEmulationUseCase: StopEmulationUseCase,
    private val resumeEmulationUseCase: ResumeEmulationUseCase,
) : ViewModel() {

    /** Buffer for translating emulator pixel data to ARGB format for Android Bitmap rendering */
    private val argbBuffer = IntArray(WIDTH * HEIGHT)

    /** Current state of the emulator — waiting for a ROM or running */
    private val _state = MutableStateFlow<EmulatorUiState>(EmulatorUiState.WaitingForRom)
    val state: StateFlow<EmulatorUiState> = _state.asStateFlow()

    /** Current frame bitmap for display */
    internal val frameBitmap = MutableStateFlow(ImageBitmap(WIDTH, HEIGHT))

    /** Job collecting frames from the emulator */
    private var frameCollectorJob: Job? = null

    /**
     * Loads a ROM from the given content [Uri], starts the emulation and begins collecting frames
     *
     * @param uri content URI of the ROM file selected by the user
     */
    fun loadRom(uri: Uri) {
        if (!loadRomUseCase(uri.toString())) return

        startFrameCollection()
        _state.value = EmulatorUiState.Running
    }

    /**
     * Resumes emulation and audio after the app returns to the foreground. Only takes effect
     * if a ROM was already loaded.
     */
    fun resumeEmulation() {
        if (_state.value != EmulatorUiState.Running) return

        resumeEmulationUseCase()
        startFrameCollection()
    }

    /**
     * Pauses emulation and stops audio when the app goes to the background. Only takes effect
     * if the emulator is currently running.
     */
    fun pauseEmulation() {
        if (_state.value != EmulatorUiState.Running) return

        frameCollectorJob?.cancel()
        frameCollectorJob = null
        stopEmulationUseCase()
    }

    /**
     * Sends a button press to the emulator via [PressButtonUseCase]
     *
     * @param button the button to press
     */
    fun press(button: Button) = pressButtonUseCase(button)

    /**
     * Sends a button release to the emulator via [ReleaseButtonUseCase]
     *
     * @param button the button to release
     */
    fun release(button: Button) = releaseButtonUseCase(button)

    /**
     * Saves the current state of the emulator via [SaveSaveStateUseCase]
     */
    fun saveState() = saveSaveStateUseCase()

    /**
     * Loads the saved state of the emulator via [LoadSaveStateUseCase]
     */
    fun loadState() = loadSaveStateUseCase()

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
