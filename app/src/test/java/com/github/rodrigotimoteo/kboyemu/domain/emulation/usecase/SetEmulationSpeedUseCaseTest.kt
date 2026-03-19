package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.domain.emulation.EmulationSpeed
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.SaveGameUseCase
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Unit tests for [SetEmulationSpeedUseCase]
 *
 * @author rodrigotimoteo
 */
class SetEmulationSpeedUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: SetEmulationSpeedUseCase =
        SetEmulationSpeedUseCase(emulatorMock)

    @ParameterizedTest
    @EnumSource(EmulationSpeed::class)
    fun `when invoking then emulation speed changes`(emulationSpeed: EmulationSpeed) {
        sut(emulationSpeed)

        verify(exactly = 1) { emulatorMock.speedMultiplier = emulationSpeed.multiplier }
    }
}
