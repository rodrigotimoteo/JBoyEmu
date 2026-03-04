package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * Unit tests for [StopEmulationUseCase]
 *
 * @author rodrigotimoteo
 */
class StopEmulationUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Mock for [AudioPlayer] */
    private val audioPlayerMock: AudioPlayer = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: StopEmulationUseCase = StopEmulationUseCase(emulatorMock, audioPlayerMock)

    @Test
    fun `when invoking then both audio player and emulator are started`() {
        sut()

        verify(exactly = 1) { audioPlayerMock.stop() }
        verify(exactly = 1) { emulatorMock.pause() }
    }
}