package com.github.rodrigotimoteo.kboyemu.domain.buttons

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import org.koin.core.annotation.Single

/**
 * Use case for pressing a button on the emulator
 *
 * @author rodrigotimoteo
 */
@Single
class PressButtonUseCase(
    private val emulator: KBoyEmulator
) {

    /**
     * Presses the specified button on the emulator
     *
     * @param button The button to press
     */
    operator fun invoke(button: Button) = emulator.press(button)
}
