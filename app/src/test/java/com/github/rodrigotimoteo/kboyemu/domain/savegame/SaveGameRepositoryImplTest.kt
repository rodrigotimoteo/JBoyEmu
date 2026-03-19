package com.github.rodrigotimoteo.kboyemu.domain.savegame

import android.content.ContentResolver
import android.content.Context
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Unit tests for [SaveGameRepositoryImpl]
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalUnsignedTypes::class)
class SaveGameRepositoryImplTest {

    @TempDir
    lateinit var tempDir: File

    /** Mock for [Context] returning [tempDir] as filesDir */
    private val contextMock: Context = mockk()

    /** Mock for [ContentResolver] used by importFrom / exportTo */
    private val contentResolverMock: ContentResolver = mockk()

    /** Mock for [Logger] */
    private val loggerMock: Logger = mockk(relaxed = true)

    /** Instance of the repository being tested */
    private lateinit var sut: SaveGameRepositoryImpl

    /** Sample ROM bytes used to generate a deterministic hash */
    private val sampleRomBytes = ubyteArrayOf(0x00u, 0x01u, 0x02u, 0x03u)

    /** Sample ERAM data */
    private val sampleEramData = byteArrayOf(10, 20, 30, 40, 50)

    @BeforeEach
    fun setup() {
        every { contextMock.filesDir } returns tempDir
        every { contextMock.contentResolver } returns contentResolverMock
        sut = SaveGameRepositoryImpl(contextMock, loggerMock)
    }

    @Test
    fun `when setting rom hash then it is computed deterministically for the same rom bytes`() {
        sut.setRomHash(sampleRomBytes)
        sut.save(sampleEramData)

        val savedFile = tempDir.listFiles()?.firstOrNull()
        assertNotNull(savedFile)
        assertTrue(savedFile!!.name.startsWith("savegame_"))
        assertTrue(savedFile.name.endsWith(".sav"))
    }

    @Test
    fun `when setting rom hash with different bytes then different hashes are produced`() {
        sut.setRomHash(ubyteArrayOf(0xAAu, 0xBBu))
        sut.save(sampleEramData)
        val firstName = tempDir.listFiles()!!.first().name

        sut.setRomHash(ubyteArrayOf(0xCCu, 0xDDu))
        sut.save(sampleEramData)

        val files = tempDir.listFiles()!!.map { it.name }
        assertTrue(files.size == 2)
        assertTrue(files.contains(firstName))
        assertTrue(files.any { it != firstName })
    }

    @Test
    fun `when saving without rom hash then returns false and logs message`() {
        val result = sut.save(sampleEramData)

        assertFalse(result)
        verify { loggerMock.i("Cannot save game — no ROM loaded") }
    }

    @Test
    fun `when saving with rom hash set then writes file and returns true`() {
        sut.setRomHash(sampleRomBytes)

        val result = sut.save(sampleEramData)

        assertTrue(result)
        val savedFile = tempDir.listFiles()?.firstOrNull()
        assertNotNull(savedFile)
        assertArrayEquals(sampleEramData, savedFile!!.readBytes())
    }

    @Test
    fun `when saving then logs the file size`() {
        sut.setRomHash(sampleRomBytes)

        sut.save(sampleEramData)

        verify { loggerMock.i(match { it.contains("Save game written") && it.contains("bytes") }) }
    }

    @Test
    fun `when saving and filesDir is not writable then returns false and logs error`() {
        sut.setRomHash(sampleRomBytes)

        val readOnlyDir = File(tempDir, "readonly").apply {
            mkdir()
            setReadOnly()
        }
        every { contextMock.filesDir } returns readOnlyDir

        val result = sut.save(sampleEramData)

        assertFalse(result)
        verify { loggerMock.e(eq("Failed to write save game"), any()) }

        readOnlyDir.setWritable(true)
    }

    @Test
    fun `when saving multiple times then the file is overwritten with latest data`() {
        sut.setRomHash(sampleRomBytes)

        sut.save(byteArrayOf(1, 2, 3))
        sut.save(byteArrayOf(4, 5, 6, 7))

        val savedFile = tempDir.listFiles()?.firstOrNull()
        assertNotNull(savedFile)
        assertArrayEquals(byteArrayOf(4, 5, 6, 7), savedFile!!.readBytes())
    }

    @Test
    fun `when saving empty data then writes empty file and returns true`() {
        sut.setRomHash(sampleRomBytes)

        val result = sut.save(byteArrayOf())

        assertTrue(result)
        val savedFile = tempDir.listFiles()?.firstOrNull()
        assertNotNull(savedFile)
        assertTrue(savedFile!!.readBytes().isEmpty())
    }

    @Test
    fun `when loading without rom hash then returns null and logs message`() {
        val result = sut.load()

        assertNull(result)
        verify { loggerMock.i("Cannot load game — no ROM loaded") }
    }

    @Test
    fun `when loading and no file exists then returns null and logs message`() {
        sut.setRomHash(sampleRomBytes)

        val result = sut.load()

        assertNull(result)
        verify { loggerMock.i("No save game found") }
    }

    @Test
    fun `when loading a previously saved game then returns the same bytes`() {
        sut.setRomHash(sampleRomBytes)
        sut.save(sampleEramData)

        val result = sut.load()

        assertNotNull(result)
        assertArrayEquals(sampleEramData, result)
        verify { loggerMock.i(match { it.contains("Save game loaded") && it.contains("bytes") }) }
    }

    @Test
    fun `when loading after saving multiple times then returns the latest data`() {
        sut.setRomHash(sampleRomBytes)
        sut.save(byteArrayOf(1, 2, 3))
        sut.save(byteArrayOf(7, 8, 9, 10))

        val result = sut.load()

        assertArrayEquals(byteArrayOf(7, 8, 9, 10), result)
    }

    @Test
    fun `when changing rom hash between save and load then load returns null`() {
        sut.setRomHash(sampleRomBytes)
        sut.save(sampleEramData)

        sut.setRomHash(ubyteArrayOf(0xFFu, 0xFEu))

        val result = sut.load()

        assertNull(result)
        verify { loggerMock.i("No save game found") }
    }

    @Test
    fun `when importing from valid uri then returns the file bytes`() {
        val data = byteArrayOf(99, 88, 77)
        val inputStream = ByteArrayInputStream(data)
        every { contentResolverMock.openInputStream(any()) } returns inputStream

        val result = sut.importFrom("content://test/save.sav")

        assertNotNull(result)
        assertArrayEquals(data, result)
    }

    @Test
    fun `when importing and content resolver returns null stream then returns null`() {
        every { contentResolverMock.openInputStream(any()) } returns null

        val result = sut.importFrom("content://test/save.sav")

        assertNull(result)
    }

    @Test
    fun `when importing and content resolver throws then returns null and logs error`() {
        every { contentResolverMock.openInputStream(any()) } throws RuntimeException("IO error")

        val result = sut.importFrom("content://test/save.sav")

        assertNull(result)
        verify { loggerMock.e(match { it.contains("Failed to import save game") }, any()) }
    }

    @Test
    fun `when importing with invalid uri then returns null and logs error`() {
        val result = sut.importFrom("")

        assertNull(result)
        verify { loggerMock.e(match { it.contains("Failed to import save game") }, any()) }
    }

    @Test
    fun `when exporting to valid uri then writes data and returns true`() {
        val outputStream = ByteArrayOutputStream()
        every { contentResolverMock.openOutputStream(any()) } returns outputStream

        val result = sut.exportTo("content://test/export.sav", sampleEramData)

        assertTrue(result)
        assertArrayEquals(sampleEramData, outputStream.toByteArray())
        verify { loggerMock.i(match { it.contains("Save game exported") && it.contains("bytes") }) }
    }

    @Test
    fun `when exporting and content resolver returns null stream then returns true but writes nothing`() {
        every { contentResolverMock.openOutputStream(any()) } returns null

        val result = sut.exportTo("content://test/export.sav", sampleEramData)

        assertTrue(result)
    }

    @Test
    fun `when exporting and content resolver throws then returns false and logs error`() {
        every { contentResolverMock.openOutputStream(any()) } throws RuntimeException("IO error")

        val result = sut.exportTo("content://test/export.sav", sampleEramData)

        assertFalse(result)
        verify { loggerMock.e(match { it.contains("Failed to export save game") }, any()) }
    }

    @Test
    fun `when exporting with invalid uri then returns false and logs error`() {
        val result = sut.exportTo("", sampleEramData)

        assertFalse(result)
        verify { loggerMock.e(match { it.contains("Failed to export save game") }, any()) }
    }

    @Test
    fun `when exporting empty data then writes empty bytes and returns true`() {
        val outputStream = ByteArrayOutputStream()
        every { contentResolverMock.openOutputStream(any()) } returns outputStream

        val result = sut.exportTo("content://test/export.sav", byteArrayOf())

        assertTrue(result)
        assertTrue(outputStream.toByteArray().isEmpty())
    }
}