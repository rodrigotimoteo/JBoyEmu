package com.github.rodrigotimoteo.kboyemu.domain.buttons.usecase

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Unit tests for [PressButtonUseCase]
 *
 * @author rodrigotimoteo
 */
class PressButtonUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: PressButtonUseCase = PressButtonUseCase(emulatorMock)

    @EnumSource(Button::class)
    @ParameterizedTest
    fun `when invoking with a button then button press is sent to emulator`(button: Button) {
        sut(button)

        verify(exactly = 1) { emulatorMock.press(button) }
    }
}
