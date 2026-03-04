package com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase

import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for restoring battery-backed save game data. Reads the persisted ERAM dump and loads
 * it into the emulator. Should be called right after a ROM is loaded so that previous in-game
 * saves are available immediately.
 *
 * No-op when no save file exists for the current ROM.
 *
 * @author rodrigotimoteo
 */
@Single
class LoadGameUseCase(
    private val emulator: KBoyEmulator,
    private val saveGameRepository: SaveGameRepository,
) {

    /**
     * Loads persisted ERAM into the emulator. No-op if no save file exists.
     */
    operator fun invoke() {
        val data = saveGameRepository.load() ?: return
        emulator.loadEram(data)
    }
}
