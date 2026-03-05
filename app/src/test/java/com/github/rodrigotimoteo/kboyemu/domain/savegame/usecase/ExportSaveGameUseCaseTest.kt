package com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase

import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ExportSaveGameUseCase]
 *
 * @author rodrigotimoteo
 */
class ExportSaveGameUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Mock for [SaveGameRepository] */
    private val saveGameRepositoryMock: SaveGameRepository = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: ExportSaveGameUseCase = ExportSaveGameUseCase(emulatorMock, saveGameRepositoryMock)

    private val testUri = "content://com.example/export.sav"
    private val testEramData = byteArrayOf(10, 20, 30, 40)

    @Test
    fun `when invoking and dumpEram returns null then returns false`() {
        every { emulatorMock.dumpEram() } returns null

        val result = sut(testUri)

        assertFalse(result)
    }

    @Test
    fun `when invoking and dumpEram returns null then exportTo is never called`() {
        every { emulatorMock.dumpEram() } returns null

        sut(testUri)

        verify(exactly = 0) { saveGameRepositoryMock.exportTo(any(), any()) }
    }

    @Test
    fun `when invoking and dumpEram returns data then exportTo is called with uri and data`() {
        every { emulatorMock.dumpEram() } returns testEramData

        sut(testUri)

        verify(exactly = 1) { saveGameRepositoryMock.exportTo(testUri, testEramData) }
    }

    @Test
    fun `when invoking and exportTo succeeds then returns true`() {
        every { emulatorMock.dumpEram() } returns testEramData
        every { saveGameRepositoryMock.exportTo(testUri, testEramData) } returns true

        val result = sut(testUri)

        assertTrue(result)
    }

    @Test
    fun `when invoking and exportTo fails then returns false`() {
        every { emulatorMock.dumpEram() } returns testEramData
        every { saveGameRepositoryMock.exportTo(testUri, testEramData) } returns false

        val result = sut(testUri)

        assertFalse(result)
    }

    @Test
    fun `when invoking with empty eram data then exportTo is still called`() {
        val emptyData = byteArrayOf()
        every { emulatorMock.dumpEram() } returns emptyData
        every { saveGameRepositoryMock.exportTo(testUri, emptyData) } returns true

        val result = sut(testUri)

        assertTrue(result)
        verify(exactly = 1) { saveGameRepositoryMock.exportTo(testUri, emptyData) }
    }
}