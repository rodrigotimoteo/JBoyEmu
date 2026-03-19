package com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase

import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

/**
 * Unit tests for [LoadGameUseCase]
 *
 * @author rodrigotimoteo
 */
class LoadGameUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Mock for [SaveGameRepository] */
    private val saveGameRepositoryMock: SaveGameRepository = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: LoadGameUseCase = LoadGameUseCase(emulatorMock, saveGameRepositoryMock)

    private val testData = byteArrayOf(10, 20, 30, 40)

    @Test
    fun `when invoking and load returns null then loadEram is not called`() {
        every { saveGameRepositoryMock.load() } returns null

        sut()

        verify(exactly = 0) { emulatorMock.loadEram(any()) }
    }

    @Test
    fun `when invoking and load returns data then loadEram is called with that data`() {
        every { saveGameRepositoryMock.load() } returns testData

        sut()

        verify(exactly = 1) { emulatorMock.loadEram(testData) }
    }

    @Test
    fun `when invoking and load returns data then load happens before loadEram`() {
        every { saveGameRepositoryMock.load() } returns testData

        sut()

        verifyOrder {
            saveGameRepositoryMock.load()
            emulatorMock.loadEram(testData)
        }
    }

    @Test
    fun `when invoking with empty data then loadEram is still called`() {
        val emptyData = byteArrayOf()
        every { saveGameRepositoryMock.load() } returns emptyData

        sut()

        verify(exactly = 1) { emulatorMock.loadEram(emptyData) }
    }
}