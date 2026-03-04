package com.github.rodrigotimoteo.kboyemu.domain.buttons

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for releasing a button on the emulator
 *
 * @author rodrigotimoteo
 */
@Single
class ReleaseButtonUseCase(
    private val emulator: KBoyEmulator
) {

    /**
     * Releases the specified button on the emulator
     *
     * @param button The button to release
     */
    operator fun invoke(button: Button) = emulator.release(button)
}
