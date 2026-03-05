package com.github.rodrigotimoteo.kboyemu.domain.savestate

import android.content.Context
import com.github.rodrigotimoteo.kboyemu.util.md5Hex
import com.github.rodrigotimoteo.kboyemucore.api.SaveState
import com.github.rodrigotimoteo.kboyemucore.api.SaveState.Companion.toByteArray
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Unit tests for [SaveStateRepositoryImpl]
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalUnsignedTypes::class)
class SaveStateRepositoryImplTest {

    @TempDir
    lateinit var tempDir: File

    /** Mock for [Context] returning [tempDir] as filesDir */
    private val contextMock: Context = mockk()

    /** Mock for [Logger] */
    private val loggerMock: Logger = mockk(relaxed = true)

    /** Instance of the repository being tested */
    private lateinit var sut: SaveStateRepositoryImpl

    /** Sample ROM bytes used to generate a deterministic hash */
    private val sampleRomBytes = ubyteArrayOf(0x00u, 0x01u, 0x02u, 0x03u)

    /** Serialized representation of a mock save state */
    private val fakeSerializedBytes = byteArrayOf(10, 20, 30, 40, 50)

    @BeforeEach
    fun setup() {
        every { contextMock.filesDir } returns tempDir
        sut = SaveStateRepositoryImpl(contextMock, loggerMock)
        mockkObject(SaveState.Companion)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SaveState.Companion)
    }

    @Test
    fun `when setting rom hash then it is computed deterministically for the same rom bytes`() {
        sut.setRomHash(sampleRomBytes)

        val state = mockk<SaveState>()
        every { state.toByteArray() } returns fakeSerializedBytes

        sut.save(state)

        val expectedFile = tempDir.listFiles()?.firstOrNull()
        assertNotNull(expectedFile)
        assertTrue(expectedFile!!.name.startsWith("savestate_"))
        assertTrue(expectedFile.name.endsWith(".bin"))
    }

    @Test
    fun `when setting rom hash with different bytes then different hashes are produced`() {
        val state = mockk<SaveState>()
        every { state.toByteArray() } returns fakeSerializedBytes

        sut.setRomHash(ubyteArrayOf(0xAAu, 0xBBu))
        sut.save(state)
        val firstFile = tempDir.listFiles()!!.first().name

        sut.setRomHash(ubyteArrayOf(0xCCu, 0xDDu))
        sut.save(state)
        val files = tempDir.listFiles()!!.map { it.name }

        assertEquals(2, files.size)
        assertTrue(files.contains(firstFile))
        assertTrue(files.any { it != firstFile })
    }

    @Test
    fun `when saving without rom hash then returns false and logs message`() {
        val state = mockk<SaveState>()

        val result = sut.save(state)

        assertFalse(result)
        verify { loggerMock.i("Cannot save state — no ROM loaded") }
    }

    @Test
    fun `when saving with rom hash set then writes file and returns true`() {
        sut.setRomHash(sampleRomBytes)
        val state = mockk<SaveState>()
        every { state.toByteArray() } returns fakeSerializedBytes

        val result = sut.save(state)

        assertTrue(result)
        val savedFile = tempDir.listFiles()?.firstOrNull()
        assertNotNull(savedFile)
        assertTrue(savedFile!!.readBytes().contentEquals(fakeSerializedBytes))
    }

    @Test
    fun `when saving then logs the file size`() {
        sut.setRomHash(sampleRomBytes)
        val state = mockk<SaveState>()
        every { state.toByteArray() } returns fakeSerializedBytes

        sut.save(state)

        verify { loggerMock.i(match { it.contains("Save state written") && it.contains("bytes") }) }
    }

    @Test
    fun `when saving and serialization throws then returns false and logs error`() {
        sut.setRomHash(sampleRomBytes)
        val state = mockk<SaveState>()
        val exception = RuntimeException("Serialization failure")
        every { state.toByteArray() } throws exception

        val result = sut.save(state)

        assertFalse(result)
        verify { loggerMock.e("Failed to write save state", exception) }
    }

    @Test
    fun `when saving and filesDir is not writable then returns false and logs error`() {
        sut.setRomHash(sampleRomBytes)
        val state = mockk<SaveState>()
        every { state.toByteArray() } returns fakeSerializedBytes

        val readOnlyDir = File(tempDir, "readonly").apply {
            mkdir()
            setReadOnly()
        }
        every { contextMock.filesDir } returns readOnlyDir

        val result = sut.save(state)

        assertFalse(result)
        verify { loggerMock.e(eq("Failed to write save state"), any()) }

        readOnlyDir.setWritable(true)
    }

    @Test
    fun `when saving multiple times then the file is overwritten with latest state`() {
        sut.setRomHash(sampleRomBytes)

        val firstState = mockk<SaveState>()
        val firstBytes = byteArrayOf(1, 2, 3)
        every { firstState.toByteArray() } returns firstBytes
        sut.save(firstState)

        val secondState = mockk<SaveState>()
        val secondBytes = byteArrayOf(4, 5, 6, 7)
        every { secondState.toByteArray() } returns secondBytes
        sut.save(secondState)

        val savedFile = tempDir.listFiles()?.firstOrNull()
        assertNotNull(savedFile)
        assertTrue(savedFile!!.readBytes().contentEquals(secondBytes))
    }

    @Test
    fun `when loading without rom hash then returns null and logs message`() {
        val result = sut.load()

        assertNull(result)
        verify { loggerMock.i("Cannot load state — no ROM loaded") }
    }

    @Test
    fun `when loading and no file exists then returns null and logs message`() {
        sut.setRomHash(sampleRomBytes)

        val result = sut.load()

        assertNull(result)
        verify { loggerMock.i("No save state found") }
    }

    @Test
    fun `when loading a previously saved state then returns the deserialized state`() {
        sut.setRomHash(sampleRomBytes)

        val state = mockk<SaveState>()
        every { state.toByteArray() } returns fakeSerializedBytes
        sut.save(state)

        val expectedState = mockk<SaveState>()
        every { SaveState.fromByteArray(fakeSerializedBytes) } returns expectedState

        val result = sut.load()

        assertNotNull(result)
        assertEquals(expectedState, result)
        verify { loggerMock.i("Save state loaded") }
    }

    @Test
    fun `when loading and deserialization throws then returns null and logs error`() {
        sut.setRomHash(sampleRomBytes)

        val savedFile = tempDir.resolve("savestate_${md5Hex(sampleRomBytes)}.bin")
        savedFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xFF.toByte()))

        val exception = RuntimeException("Corrupt data")
        every { SaveState.fromByteArray(any()) } throws exception

        val result = sut.load()

        assertNull(result)
        verify { loggerMock.e("Failed to load save state", exception) }
    }

    @Test
    fun `when saving and loading then the same serialized bytes are passed to deserialization`() {
        sut.setRomHash(sampleRomBytes)

        val state = mockk<SaveState>()
        every { state.toByteArray() } returns fakeSerializedBytes
        sut.save(state)

        val restoredState = mockk<SaveState>()
        every { SaveState.fromByteArray(fakeSerializedBytes) } returns restoredState

        val result = sut.load()

        assertEquals(restoredState, result)
    }

    @Test
    fun `when changing rom hash between save and load then load returns null`() {
        sut.setRomHash(sampleRomBytes)

        val state = mockk<SaveState>()
        every { state.toByteArray() } returns fakeSerializedBytes
        sut.save(state)

        sut.setRomHash(ubyteArrayOf(0xFFu, 0xFEu))

        val result = sut.load()

        assertNull(result)
        verify { loggerMock.i("No save state found") }
    }
}