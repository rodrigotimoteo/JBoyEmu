package com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase

import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SaveGameUseCase]
 *
 * @author rodrigotimoteo
 */
class SaveGameUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Mock for [SaveGameRepository] */
    private val saveGameRepositoryMock: SaveGameRepository = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: SaveGameUseCase = SaveGameUseCase(emulatorMock, saveGameRepositoryMock)

    private val testData = byteArrayOf(10, 20, 30, 40)

    @Test
    fun `when invoking and dumpEram returns null then save is not called`() {
        every { emulatorMock.dumpEram() } returns null

        sut()

        verify(exactly = 0) { saveGameRepositoryMock.save(any()) }
    }

    @Test
    fun `when invoking and dumpEram returns data then save is called with that data`() {
        every { emulatorMock.dumpEram() } returns testData

        sut()

        verify(exactly = 1) { saveGameRepositoryMock.save(testData) }
    }

    @Test
    fun `when invoking and dumpEram returns data then dump happens before save`() {
        every { emulatorMock.dumpEram() } returns testData

        sut()

        verifyOrder {
            emulatorMock.dumpEram()
            saveGameRepositoryMock.save(testData)
        }
    }

    @Test
    fun `when invoking with empty data then save is still called`() {
        val emptyData = byteArrayOf()
        every { emulatorMock.dumpEram() } returns emptyData

        sut()

        verify(exactly = 1) { saveGameRepositoryMock.save(emptyData) }
    }
}