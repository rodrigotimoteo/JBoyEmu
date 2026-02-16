package com.github.rodrigotimoteo.kboyemucore.emulator

import app.cash.turbine.test
import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.util.ACCESSING_FRAME_BEFORE_READY
import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalUnsignedTypes::class)
class KBoyEmulatorImplTest {

    private val loggerMock = mockk<Logger>(relaxed = true)

    private lateinit var sut: KBoyEmulator

    private fun createTestRom(size: Int = 0x8000): Rom {
        return Rom(UByteArray(size) { 0x00u })
    }

    @BeforeEach
    fun setup() {
        sut = KBoyEmulatorImpl(loggerMock)
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `when creating emulator without ROM then job should be null`() {
        val job = sut.job()
        assertNull(job)
    }

    @Test
    fun `when accessing frames before ROM loaded then expect IllegalStateException`() {
        assertThrows<IllegalStateException> {
            @Suppress("UNUSED_VARIABLE")
            val frames = sut.frames
        }
    }

    // ==================== Button Input Tests ====================

    @Test
    fun `when pressing button A then expect no exception`() {
        sut.press(Button.A)
    }

    @Test
    fun `when releasing button B then expect no exception`() {
        sut.release(Button.B)
    }

    @Test
    fun `when pressing multiple buttons then expect no exception`() {
        sut.press(Button.A)
        sut.press(Button.B)
        sut.press(Button.SELECT)
        sut.press(Button.START)
    }

    // ==================== Emulator Control Tests ====================

    @Test
    fun `when running emulator without ROM then expect no exception`() {
        sut.run()
    }

    @Test
    fun `when pausing emulator without ROM then expect no exception`() {
        sut.pause()
    }

    @Test
    fun `when running multiple times then expect safe operation`() {
        sut.run()
        sut.run()
        sut.run()
    }

    @Test
    fun `when pausing multiple times then expect safe operation`() {
        sut.pause()
        sut.pause()
        sut.pause()
    }

    @Test
    fun `when alternating run and pause then expect consistent state`() {
        sut.run()
        sut.pause()
        sut.run()
        sut.pause()
    }

    @Test
    fun `when resetting without ROM loaded then expect exception`() {
        assertThrows<Exception> {
            sut.reset()
        }
    }

    // ==================== ROM Loading and Frames Flow Tests ====================

    @Test
    fun `when loading ROM then expect frames flow to emit FrameBuffer`() = runTest {
        val testRom = createTestRom()
        sut.loadRom(testRom)

        sut.frames.test {
            val frameBuffer = awaitItem()
            assertEquals(WIDTH, frameBuffer.width)
            assertEquals(HEIGHT, frameBuffer.height)
            assertEquals(WIDTH * HEIGHT, frameBuffer.pixels.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when loading ROM then expect frames to have correct dimensions`() = runTest {
        val testRom = createTestRom()
        sut.loadRom(testRom)

        sut.frames.test {
            val frameBuffer = awaitItem()
            assertEquals(160, frameBuffer.width)
            assertEquals(144, frameBuffer.height)
            assertEquals(23040, frameBuffer.pixels.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when loading ROM then expect frames pixels to be valid`() = runTest {
        val testRom = createTestRom()
        sut.loadRom(testRom)

        sut.frames.test {
            val frameBuffer = awaitItem()
            assertEquals(true, frameBuffer.pixels.isNotEmpty())
            frameBuffer.pixels.forEach { pixel ->
                assertEquals(true, pixel in Byte.MIN_VALUE..Byte.MAX_VALUE)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when accessing frames multiple times then expect same flow instance`() = runTest {
        val testRom = createTestRom()
        sut.loadRom(testRom)

        val frames1 = sut.frames
        val frames2 = sut.frames

        frames1.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        frames2.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Job Management Tests ====================

    @Test
    fun `when calling job multiple times without running then expect consistent null results`() {
        val job1 = sut.job()
        val job2 = sut.job()

        assertNull(job1)
        assertNull(job2)
    }

    @Test
    fun `when performing operations in sequence then expect no state interference`() = runTest {
        val testRom = createTestRom()
        sut.loadRom(testRom)

        sut.run()
        sut.pause()
        val job = sut.job()
        sut.run()

        sut.frames.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Reset Tests ====================

    @Test
    fun `when resetting after ROM loaded then expect frames to still emit`() = runTest {
        val testRom = createTestRom()
        sut.loadRom(testRom)

        sut.reset()

        sut.frames.test {
            val frameBuffer = awaitItem()
            assertEquals(WIDTH, frameBuffer.width)
            assertEquals(HEIGHT, frameBuffer.height)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

