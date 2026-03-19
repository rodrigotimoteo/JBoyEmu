package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for resuming emulation and audio playback after a pause. Restarts the emulator loop
 * and reconnects the audio consumer thread. Intended to be called when the user returns to the
 * app while a ROM is loaded.
 *
 * @author rodrigotimoteo
 */
@Single
class ResumeEmulationUseCase(
    private val emulator: KBoyEmulator,
    private val audioPlayer: AudioPlayer,
) {

    /**
     * Resumes the emulator and restarts audio playback
     */
    operator fun invoke() {
        audioPlayer.start(emulator.audioRingBuffer)
        emulator.run()
    }
}

