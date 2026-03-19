package com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase

import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for persisting battery-backed save game data. Dumps the emulator's external RAM
 * and writes it to storage. Should be called whenever emulation is paused or stopped so the
 * player's in-game progress is preserved across sessions.
 *
 * No-op when the cartridge has no external RAM (dumpEram returns null).
 *
 * @author rodrigotimoteo
 */
@Single
class SaveGameUseCase(
    private val emulator: KBoyEmulator,
    private val saveGameRepository: SaveGameRepository,
) {

    /**
     * Dumps ERAM and persists it. No-op if the cartridge has no external RAM.
     */
    operator fun invoke() {
        val data = emulator.dumpEram() ?: return
        saveGameRepository.save(data)
    }
}
