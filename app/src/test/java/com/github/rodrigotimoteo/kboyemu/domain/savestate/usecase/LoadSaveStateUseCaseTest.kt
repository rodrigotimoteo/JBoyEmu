package com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase

import com.github.rodrigotimoteo.kboyemu.data.savestate.SaveStateRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.SaveState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * Unit tests for [LoadSaveStateUseCase]
 *
 * @author rodrigotimoteo
 */
class LoadSaveStateUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Mock for [SaveStateRepository] */
    private val saveStateRepositoryMock: SaveStateRepository = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: LoadSaveStateUseCase = LoadSaveStateUseCase(emulatorMock, saveStateRepositoryMock)

    @Test
    fun `when invoking and state exists then loads state from repository and restores it into emulator`() {
        val savedState = mockk<SaveState>()
        every { saveStateRepositoryMock.load() } returns savedState

        sut()

        verify(exactly = 1) { saveStateRepositoryMock.load() }
        verify(exactly = 1) { emulatorMock.loadState(savedState) }
    }

    @Test
    fun `when invoking and repository returns null then does nothing`() {
        every { saveStateRepositoryMock.load() } returns null

        sut()

        verify(exactly = 1) { saveStateRepositoryMock.load() }
        verify(exactly = 0) { emulatorMock.loadState(any()) }
    }

    @Test
    fun `when invoking and other save states become available then the correct one is retrieved`() {
        val firstState = mockk<SaveState>()
        val secondState = mockk<SaveState>()

        every { saveStateRepositoryMock.load() } returns firstState
        sut()

        every { saveStateRepositoryMock.load() } returns secondState
        sut()

        verify(exactly = 2) { saveStateRepositoryMock.load() }
        verify(exactly = 1) { emulatorMock.loadState(firstState) }
        verify(exactly = 1) { emulatorMock.loadState(secondState) }
    }

    @Test
    fun `when invoking with valid save state and null then both cases are handled correctly`() {
        val savedState = mockk<SaveState>()

        every { saveStateRepositoryMock.load() } returns null
        sut()

        every { saveStateRepositoryMock.load() } returns savedState
        sut()

        every { saveStateRepositoryMock.load() } returns null
        sut()

        verify(exactly = 3) { saveStateRepositoryMock.load() }
        verify(exactly = 1) { emulatorMock.loadState(savedState) }
    }
}