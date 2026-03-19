package com.github.rodrigotimoteo.kboyemu.domain.rom

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Unit tests for [RomRepositoryImpl]
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalUnsignedTypes::class)
class RomRepositoryImplTest {

    /** Mock for [ContentResolver] */
    private val contentResolverMock: ContentResolver = mockk()

    /** Mock for [Context] */
    private val contextMock: Context = mockk {
        every { this@mockk.contentResolver } returns this@RomRepositoryImplTest.contentResolverMock
    }

    /** Mock for [Logger] */
    private val loggerMock: Logger = mockk(relaxed = true)

    /** Instance of the repository being tested */
    private val sut: RomRepositoryImpl = RomRepositoryImpl(contextMock, loggerMock)

    /** Fake parsed URI returned by [Uri.parse] */
    private val fakeUri: Uri = mockk()

    private val testUriString = "content://com.example.provider/rom.gb"
    private val testRomBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(testUriString) } returns fakeUri
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `when loading from valid uri then returns rom bytes as UByteArray`() {
        every { contentResolverMock.openInputStream(fakeUri) } returns ByteArrayInputStream(testRomBytes)

        val result = sut.loadFromUri(testUriString)

        assertNotNull(result)
        assertArrayEquals(testRomBytes.toUByteArray().toByteArray(), result!!.toByteArray())
    }

    @Test
    fun `when loading from valid uri then returned bytes match original content`() {
        val romContent = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        every { contentResolverMock.openInputStream(fakeUri) } returns ByteArrayInputStream(romContent)

        val result = sut.loadFromUri(testUriString)

        assertNotNull(result)
        assertArrayEquals(romContent, result!!.toByteArray())
    }

    @Test
    fun `when loading an empty rom then returns empty UByteArray`() {
        every { contentResolverMock.openInputStream(fakeUri) } returns ByteArrayInputStream(byteArrayOf())

        val result = sut.loadFromUri(testUriString)

        assertNotNull(result)
        assertArrayEquals(byteArrayOf(), result!!.toByteArray())
    }

    @Test
    fun `when content resolver returns null stream then returns null`() {
        every { contentResolverMock.openInputStream(fakeUri) } returns null

        val result = sut.loadFromUri(testUriString)

        assertNull(result)
    }

    @Test
    fun `when content resolver throws IOException then returns null and logs error`() {
        val exception = IOException("File not found")
        every { contentResolverMock.openInputStream(fakeUri) } throws exception

        val result = sut.loadFromUri(testUriString)

        assertNull(result)
        verify { loggerMock.e("Failed to read ROM from URI: $testUriString", exception) }
    }

    @Test
    fun `when content resolver throws SecurityException then returns null and logs error`() {
        val exception = SecurityException("Permission denied")
        every { contentResolverMock.openInputStream(fakeUri) } throws exception

        val result = sut.loadFromUri(testUriString)

        assertNull(result)
        verify { loggerMock.e("Failed to read ROM from URI: $testUriString", exception) }
    }

    @Test
    fun `when uri parsing throws then returns null and logs error`() {
        val badUri = "not a valid uri %%"
        val exception = IllegalArgumentException("Bad URI")
        every { Uri.parse(badUri) } throws exception

        val result = sut.loadFromUri(badUri)

        assertNull(result)
        verify { loggerMock.e("Failed to read ROM from URI: $badUri", exception) }
    }

    @Test
    fun `when loading from same uri twice then both calls resolve independently`() {
        every { contentResolverMock.openInputStream(fakeUri) } returns ByteArrayInputStream(testRomBytes)

        val first = sut.loadFromUri(testUriString)

        every { contentResolverMock.openInputStream(fakeUri) } returns ByteArrayInputStream(testRomBytes)

        val second = sut.loadFromUri(testUriString)

        assertNotNull(first)
        assertNotNull(second)
        assertArrayEquals(first!!.toByteArray(), second!!.toByteArray())
    }

    @Test
    fun `when loading fails then succeeds on retry then returns bytes on second call`() {
        every { contentResolverMock.openInputStream(fakeUri) } throws IOException("Temporary error")

        val firstResult = sut.loadFromUri(testUriString)
        assertNull(firstResult)

        every { contentResolverMock.openInputStream(fakeUri) } returns ByteArrayInputStream(testRomBytes)

        val secondResult = sut.loadFromUri(testUriString)
        assertNotNull(secondResult)
        assertArrayEquals(testRomBytes, secondResult!!.toByteArray())
    }
}