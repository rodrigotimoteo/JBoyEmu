package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for stopping emulation and audio playback. Pauses the emulator loop and releases
 * all audio resources. Intended to be called when the user leaves the app or the ViewModel
 * is cleared.
 *
 * @author rodrigotimoteo
 */
@Single
class StopEmulationUseCase(
    private val emulator: KBoyEmulator,
    private val audioPlayer: AudioPlayer,
) {

    /**
     * Pauses the emulator and stops audio playback
     */
    operator fun invoke() {
        emulator.pause()
        audioPlayer.stop()
    }
}

