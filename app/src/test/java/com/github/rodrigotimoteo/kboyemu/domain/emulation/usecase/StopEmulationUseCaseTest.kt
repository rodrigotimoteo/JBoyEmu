package com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.SaveGameUseCase
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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

    /** Mock for [SaveGameUseCase] */
    private val saveGameUseCaseMock: SaveGameUseCase = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: StopEmulationUseCase =
        StopEmulationUseCase(emulatorMock, audioPlayerMock, saveGameUseCaseMock)

    @Test
    fun `when invoking then emulator is paused`() {
        sut()

        verify(exactly = 1) { emulatorMock.pause() }
    }

    @Test
    fun `when invoking then audio player is stopped`() {
        sut()

        verify(exactly = 1) { audioPlayerMock.stop() }
    }

    @Test
    fun `when invoking then save game is persisted`() {
        sut()

        verify(exactly = 1) { saveGameUseCaseMock() }
    }

    @Test
    fun `when invoking then save game happens before pause and audio stop`() {
        sut()

        verifyOrder {
            saveGameUseCaseMock()
            emulatorMock.pause()
            audioPlayerMock.stop()
        }
    }
}