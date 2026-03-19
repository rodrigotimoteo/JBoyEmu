package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.LoadGameUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.SaveGameUseCase
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for resetting the emulator to its initial state. Persists the current save game,
 * stops emulation and audio, recreates the emulator internals from the same ROM, reloads
 * the battery save, and restarts emulation.
 *
 * @author rodrigotimoteo
 */
@Single
class ResetEmulationUseCase(
    private val emulator: KBoyEmulator,
    private val audioPlayer: AudioPlayer,
    private val saveGameUseCase: SaveGameUseCase,
    private val loadGameUseCase: LoadGameUseCase,
) {

    /**
     * Saves the current ERAM, resets the emulator, reloads the save game, and restarts
     */
    operator fun invoke() {
        saveGameUseCase()
        emulator.pause()
        audioPlayer.stop()
        emulator.reset()
        loadGameUseCase()
        audioPlayer.start(emulator.audioRingBuffer)
        emulator.run()
    }
}

