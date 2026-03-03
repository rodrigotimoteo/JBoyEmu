package com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase

import com.github.rodrigotimoteo.kboyemu.data.savestate.SaveStateRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for loading a previously saved emulator state. Reads the save file from the
 * [SaveStateRepository] and restores it into the emulator.
 *
 * @author rodrigotimoteo
 */
@Single
class LoadSaveStateUseCase(
    private val emulator: KBoyEmulator,
    private val saveStateRepository: SaveStateRepository,
) {

    /**
     * Loads the save state for the currently loaded ROM and restores it into the emulator
     */
    operator fun invoke() {
        val state = saveStateRepository.load() ?: return
        emulator.loadState(state)
    }
}