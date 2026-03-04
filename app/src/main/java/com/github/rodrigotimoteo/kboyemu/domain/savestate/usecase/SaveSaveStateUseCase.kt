package com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase

import com.github.rodrigotimoteo.kboyemu.data.savestate.SaveStateRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for saving the current state of the emulator. Captures the emulator snapshot and
 * delegates persistence to the [SaveStateRepository].
 *
 * @author rodrigotimoteo
 */
@Single
class SaveSaveStateUseCase(
    private val emulator: KBoyEmulator,
    private val saveStateRepository: SaveStateRepository,
) {

    /**
     * Saves the current emulator state to a per-ROM file via the repository
     */
    operator fun invoke() {
        val state = emulator.saveState() ?: return
        saveStateRepository.save(state)
    }
}
