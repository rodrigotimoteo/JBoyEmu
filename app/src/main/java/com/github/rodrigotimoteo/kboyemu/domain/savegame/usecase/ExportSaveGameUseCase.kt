package com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase

import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for exporting the emulator's current ERAM to an external .sav file.
 * Dumps the ERAM and writes it to the given content URI via [SaveGameRepository].
 *
 * @author rodrigotimoteo
 */
@Single
class ExportSaveGameUseCase(
    private val emulator: KBoyEmulator,
    private val saveGameRepository: SaveGameRepository,
) {

    /**
     * Exports the current ERAM to the given content URI
     *
     * @param uri content URI where the .sav file should be written
     * @return true if the export succeeded
     */
    operator fun invoke(uri: String): Boolean {
        val data = emulator.dumpEram() ?: return false
        return saveGameRepository.exportTo(uri, data)
    }
}

