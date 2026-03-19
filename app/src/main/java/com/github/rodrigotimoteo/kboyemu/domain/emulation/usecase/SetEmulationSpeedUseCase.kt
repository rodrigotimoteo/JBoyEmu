package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.domain.emulation.EmulationSpeed
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for changing the emulation speed. Updates the emulator core's frame pacing multiplier
 * so the change takes effect immediately without restarting.
 *
 * @author rodrigotimoteo
 */
@Single
class SetEmulationSpeedUseCase(
    private val emulator: KBoyEmulator,
) {

    /**
     * Applies the given [speed] to the emulator
     *
     * @param speed desired emulation speed
     */
    operator fun invoke(speed: EmulationSpeed) {
        emulator.speedMultiplier = speed.multiplier
    }
}

