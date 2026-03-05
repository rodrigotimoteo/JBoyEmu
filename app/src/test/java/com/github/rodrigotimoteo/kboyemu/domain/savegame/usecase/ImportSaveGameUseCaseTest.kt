package com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ImportSaveGameUseCase]
 *
 * @author rodrigotimoteo
 */
class ImportSaveGameUseCaseTest {

    /** Mock for [KBoyEmulator] */
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)

    /** Mock for [SaveGameRepository] */
    private val saveGameRepositoryMock: SaveGameRepository = mockk(relaxed = true)

    /** Mock for [AudioPlayer] */
    private val audioPlayerMock: AudioPlayer = mockk(relaxed = true)

    /** Instance of the use case being tested */
    private val sut: ImportSaveGameUseCase =
        ImportSaveGameUseCase(emulatorMock, saveGameRepositoryMock, audioPlayerMock)

    private val testUri = "content://com.example/save.sav"
    private val testData = byteArrayOf(10, 20, 30, 40)

    @Test
    fun `when invoking and importFrom returns null then returns false`() {
        every { saveGameRepositoryMock.importFrom(testUri) } returns null

        val result = sut(testUri)

        assertFalse(result)
    }

    @Test
    fun `when invoking and importFrom returns null then emulator is not touched`() {
        every { saveGameRepositoryMock.importFrom(testUri) } returns null

        sut(testUri)

        verify(exactly = 0) { emulatorMock.pause() }
        verify(exactly = 0) { emulatorMock.reset() }
        verify(exactly = 0) { emulatorMock.loadEram(any()) }
        verify(exactly = 0) { emulatorMock.run() }
    }

    @Test
    fun `when invoking and importFrom returns null then audio is not touched`() {
        every { saveGameRepositoryMock.importFrom(testUri) } returns null

        sut(testUri)

        verify(exactly = 0) { audioPlayerMock.stop() }
        verify(exactly = 0) { audioPlayerMock.start(any()) }
    }

    @Test
    fun `when invoking and importFrom returns null then save is not called`() {
        every { saveGameRepositoryMock.importFrom(testUri) } returns null

        sut(testUri)

        verify(exactly = 0) { saveGameRepositoryMock.save(any()) }
    }

    @Test
    fun `when invoking and importFrom returns data then returns true`() {
        every { saveGameRepositoryMock.importFrom(testUri) } returns testData

        val result = sut(testUri)

        assertTrue(result)
    }

    @Test
    fun `when invoking and importFrom returns data then data is persisted internally`() {
        every { saveGameRepositoryMock.importFrom(testUri) } returns testData

        sut(testUri)

        verify(exactly = 1) { saveGameRepositoryMock.save(testData) }
    }

    @Test
    fun `when invoking and importFrom returns data then emulator is reset with imported eram`() {
        every { saveGameRepositoryMock.importFrom(testUri) } returns testData

        sut(testUri)

        verify(exactly = 1) { emulatorMock.pause() }
        verify(exactly = 1) { emulatorMock.reset() }
        verify(exactly = 1) { emulatorMock.loadEram(testData) }
        verify(exactly = 1) { emulatorMock.run() }
    }

    @Test
    fun `when invoking and importFrom returns data then audio is restarted`() {
        val ringBuffer = mockk<AudioRingBuffer>()
        every { saveGameRepositoryMock.importFrom(testUri) } returns testData
        every { emulatorMock.audioRingBuffer } returns ringBuffer

        sut(testUri)

        verify(exactly = 1) { audioPlayerMock.stop() }
        verify(exactly = 1) { audioPlayerMock.start(ringBuffer) }
    }

    @Test
    fun `when invoking and importFrom returns data then operations happen in correct order`() {
        every { saveGameRepositoryMock.importFrom(testUri) } returns testData

        sut(testUri)

        verifyOrder {
            saveGameRepositoryMock.importFrom(testUri)
            saveGameRepositoryMock.save(testData)
            emulatorMock.pause()
            audioPlayerMock.stop()
            emulatorMock.reset()
            emulatorMock.loadEram(testData)
            audioPlayerMock.start(any())
            emulatorMock.run()
        }
    }

    @Test
    fun `when invoking with empty data then still resets and loads`() {
        val emptyData = byteArrayOf()
        every { saveGameRepositoryMock.importFrom(testUri) } returns emptyData

        val result = sut(testUri)

        assertTrue(result)
        verify(exactly = 1) { saveGameRepositoryMock.save(emptyData) }
        verify(exactly = 1) { emulatorMock.loadEram(emptyData) }
    }
}