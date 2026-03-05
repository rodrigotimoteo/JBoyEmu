package com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for importing a .sav file from an external source into the emulator's ERAM.
 * Reads the file contents via [SaveGameRepository], persists them internally, then resets
 * the emulator so the imported save takes effect cleanly without corrupting game state.
 *
 * @author rodrigotimoteo
 */
@Single
class ImportSaveGameUseCase(
    private val emulator: KBoyEmulator,
    private val saveGameRepository: SaveGameRepository,
    private val audioPlayer: AudioPlayer,
) {

    /**
     * Imports a .sav file from the given content URI, resets the emulator, and loads the
     * imported ERAM so the game boots with the new save.
     *
     * @param uri content URI of the .sav file
     * @return true if the import succeeded
     */
    operator fun invoke(uri: String): Boolean {
        val data = saveGameRepository.importFrom(uri) ?: return false
        saveGameRepository.save(data)

        emulator.pause()
        audioPlayer.stop()
        emulator.reset()
        emulator.loadEram(data)
        audioPlayer.start(emulator.audioRingBuffer)
        emulator.run()

        return true
    }
}
