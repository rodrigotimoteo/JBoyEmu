package com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase

import com.github.rodrigotimoteo.kboyemu.data.savestate.SaveStateRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.SaveState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SaveSaveStateUseCase]
 *
 * @author rodrigotimoteo
 */
class SaveSaveStateUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Mock for [SaveStateRepository] */
    private val saveStateRepositoryMock: SaveStateRepository = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: SaveSaveStateUseCase = SaveSaveStateUseCase(emulatorMock, saveStateRepositoryMock)

    @Test
    fun `when invoking and emulator returns a snapshot then it is saved to the repository`() {
        val snapshot = mockk<SaveState>()
        every { emulatorMock.saveState() } returns snapshot

        sut()

        verify(exactly = 1) { emulatorMock.saveState() }
        verify(exactly = 1) { saveStateRepositoryMock.save(snapshot) }
    }

    @Test
    fun `when invoking and emulator returns null then repository save is not called`() {
        every { emulatorMock.saveState() } returns null

        sut()

        verify(exactly = 1) { emulatorMock.saveState() }
        verify(exactly = 0) { saveStateRepositoryMock.save(any()) }
    }

    @Test
    fun `when invoking multiple times then each snapshot is saved independently`() {
        val firstSnapshot = mockk<SaveState>()
        val secondSnapshot = mockk<SaveState>()

        every { emulatorMock.saveState() } returns firstSnapshot
        sut()

        every { emulatorMock.saveState() } returns secondSnapshot
        sut()

        verify(exactly = 2) { emulatorMock.saveState() }
        verify(exactly = 1) { saveStateRepositoryMock.save(firstSnapshot) }
        verify(exactly = 1) { saveStateRepositoryMock.save(secondSnapshot) }
    }

    @Test
    fun `when invoking with alternating valid and null snapshots then only valid ones are saved`() {
        val snapshot = mockk<SaveState>()

        every { emulatorMock.saveState() } returns snapshot
        sut()

        every { emulatorMock.saveState() } returns null
        sut()

        every { emulatorMock.saveState() } returns snapshot
        sut()

        verify(exactly = 3) { emulatorMock.saveState() }
        verify(exactly = 2) { saveStateRepositoryMock.save(snapshot) }
    }
}