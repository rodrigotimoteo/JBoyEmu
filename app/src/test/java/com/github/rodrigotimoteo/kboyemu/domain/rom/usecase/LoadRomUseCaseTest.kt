package com.github.rodrigotimoteo.kboyemu.domain.rom.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.data.rom.RomRepository
import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemu.data.savestate.SaveStateRepository
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.LoadGameUseCase
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [LoadRomUseCase]
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalUnsignedTypes::class)
class LoadRomUseCaseTest {

    private val romRepositoryMock: RomRepository = mockk(relaxed = true)
    private val saveStateRepositoryMock: SaveStateRepository = mockk(relaxed = true)
    private val saveGameRepositoryMock: SaveGameRepository = mockk(relaxed = true)
    private val emulatorMock: KBoyEmulator = mockk(relaxed = true)
    private val audioPlayerMock: AudioPlayer = mockk(relaxed = true)
    private val loadGameUseCaseMock: LoadGameUseCase = mockk(relaxed = true)

    private val sut: LoadRomUseCase = LoadRomUseCase(
        romRepositoryMock,
        saveStateRepositoryMock,
        saveGameRepositoryMock,
        emulatorMock,
        audioPlayerMock,
        loadGameUseCaseMock,
    )

    private val testUri = "content://com.example/rom.gb"
    private val testRomBytes = ubyteArrayOf(0x00u, 0x01u, 0x02u, 0x03u)

    @Test
    fun `when invoking with valid uri then returns true`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes

        val result = sut(testUri)

        assertTrue(result)
    }

    @Test
    fun `when invoking with valid uri then rom hash is set on save state repository`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes

        sut(testUri)

        verify(exactly = 1) { saveStateRepositoryMock.setRomHash(testRomBytes) }
    }

    @Test
    fun `when invoking with valid uri then rom hash is set on save game repository`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes

        sut(testUri)

        verify(exactly = 1) { saveGameRepositoryMock.setRomHash(testRomBytes) }
    }

    @Test
    fun `when invoking with valid uri then rom is loaded into emulator`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes

        sut(testUri)

        verify(exactly = 1) { emulatorMock.loadRom(any<Rom>()) }
    }

    @Test
    fun `when invoking with valid uri then save game is loaded`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes

        sut(testUri)

        verify(exactly = 1) { loadGameUseCaseMock() }
    }

    @Test
    fun `when invoking with valid uri then audio playback is started with emulator ring buffer`() {
        val ringBuffer = mockk<AudioRingBuffer>()
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes
        every { emulatorMock.audioRingBuffer } returns ringBuffer

        sut(testUri)

        verify(exactly = 1) { audioPlayerMock.start(ringBuffer) }
    }

    @Test
    fun `when invoking with valid uri then emulator run is called`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes

        sut(testUri)

        verify(exactly = 1) { emulatorMock.run() }
    }

    @Test
    fun `when invoking with valid uri then operations happen in correct order`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes

        sut(testUri)

        verifyOrder {
            romRepositoryMock.loadFromUri(testUri)
            saveStateRepositoryMock.setRomHash(testRomBytes)
            saveGameRepositoryMock.setRomHash(testRomBytes)
            emulatorMock.loadRom(any<Rom>())
            loadGameUseCaseMock()
            audioPlayerMock.start(any())
            emulatorMock.run()
        }
    }

    @Test
    fun `when invoking and repository returns null then returns false`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns null

        val result = sut(testUri)

        assertFalse(result)
    }

    @Test
    fun `when invoking and repository returns null then rom hash is not set`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns null

        sut(testUri)

        verify(exactly = 0) { saveStateRepositoryMock.setRomHash(any()) }
        verify(exactly = 0) { saveGameRepositoryMock.setRomHash(any()) }
    }

    @Test
    fun `when invoking and repository returns null then emulator is not touched`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns null

        sut(testUri)

        verify(exactly = 0) { emulatorMock.loadRom(any()) }
        verify(exactly = 0) { emulatorMock.run() }
    }

    @Test
    fun `when invoking and repository returns null then audio is not started`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns null

        sut(testUri)

        verify(exactly = 0) { audioPlayerMock.start(any()) }
    }

    @Test
    fun `when invoking and repository returns null then save game is not loaded`() {
        every { romRepositoryMock.loadFromUri(testUri) } returns null

        sut(testUri)

        verify(exactly = 0) { loadGameUseCaseMock() }
    }

    @Test
    fun `when invoking multiple times with different uris then each rom is loaded independently`() {
        val secondUri = "content://com.example/other.gb"
        val secondRomBytes = ubyteArrayOf(0xAAu, 0xBBu)

        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes
        every { romRepositoryMock.loadFromUri(secondUri) } returns secondRomBytes

        assertTrue(sut(testUri))
        assertTrue(sut(secondUri))

        verify(exactly = 1) { saveStateRepositoryMock.setRomHash(testRomBytes) }
        verify(exactly = 1) { saveStateRepositoryMock.setRomHash(secondRomBytes) }
        verify(exactly = 1) { saveGameRepositoryMock.setRomHash(testRomBytes) }
        verify(exactly = 1) { saveGameRepositoryMock.setRomHash(secondRomBytes) }
        verify(exactly = 2) { emulatorMock.loadRom(any<Rom>()) }
        verify(exactly = 2) { emulatorMock.run() }
        verify(exactly = 2) { loadGameUseCaseMock() }
    }

    @Test
    fun `when invoking with valid uri after a failed attempt then succeeds normally`() {
        every { romRepositoryMock.loadFromUri("bad://uri") } returns null
        every { romRepositoryMock.loadFromUri(testUri) } returns testRomBytes

        assertFalse(sut("bad://uri"))
        assertTrue(sut(testUri))

        verify(exactly = 1) { saveStateRepositoryMock.setRomHash(testRomBytes) }
        verify(exactly = 1) { saveGameRepositoryMock.setRomHash(testRomBytes) }
        verify(exactly = 1) { emulatorMock.loadRom(any<Rom>()) }
        verify(exactly = 1) { emulatorMock.run() }
        verify(exactly = 1) { loadGameUseCaseMock() }
    }
}