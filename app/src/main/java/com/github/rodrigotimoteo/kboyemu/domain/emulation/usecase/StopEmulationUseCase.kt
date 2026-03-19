package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.SaveGameUseCase
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for stopping emulation and audio playback. Auto-saves battery-backed ERAM before
 * pausing the emulator loop and releasing all audio resources. Intended to be called when the
 * user leaves the app or the ViewModel is cleared.
 *
 * @author rodrigotimoteo
 */
@Single
class StopEmulationUseCase(
    private val emulator: KBoyEmulator,
    private val audioPlayer: AudioPlayer,
    private val saveGameUseCase: SaveGameUseCase,
) {

    /**
     * Auto-saves battery RAM, pauses the emulator, and stops audio playback
     */
    operator fun invoke() {
        saveGameUseCase()
        emulator.pause()
        audioPlayer.stop()
    }
}

