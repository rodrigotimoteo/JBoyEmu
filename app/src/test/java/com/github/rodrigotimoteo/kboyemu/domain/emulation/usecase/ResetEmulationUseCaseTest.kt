package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.LoadGameUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.SaveGameUseCase
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ResetEmulationUseCase]
 *
 * @author rodrigotimoteo
 */
class ResetEmulationUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Mock for [AudioPlayer] */
    private val audioPlayerMock: AudioPlayer = mockk(relaxed = true)

    /** Mock for [SaveGameUseCase] */
    private val saveGameUseCaseMock: SaveGameUseCase = mockk(relaxed = true)

    /** Mock for [SaveGameUseCase] */
    private val loadGameUseCaseMock: LoadGameUseCase = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: ResetEmulationUseCase =
        ResetEmulationUseCase(
            emulatorMock,
            audioPlayerMock,
            saveGameUseCaseMock,
            loadGameUseCaseMock
        )

    @Test
    fun `when invoking then emulator is reset`() {
        sut()

        verify(exactly = 1) { saveGameUseCaseMock() }
        verify(exactly = 1) { emulatorMock.pause() }
        verify(exactly = 1) { audioPlayerMock.stop() }
        verify(exactly = 1) { emulatorMock.reset() }
        verify(exactly = 1) { loadGameUseCaseMock() }
        verify(exactly = 1) { audioPlayerMock.start(emulatorMock.audioRingBuffer) }
        verify(exactly = 1) { emulatorMock.run() }
    }
}