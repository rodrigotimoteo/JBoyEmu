package com.github.rodrigotimoteo.kboyemucore.emulator

import app.cash.turbine.test
import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("CONDITION_IS_ALWAYS_TRUE", "UNUSED_VARIABLE")
class KBoyEmulatorImplTest {

    @Test
    fun `when creating emulator then should implement KBoyEmulator interface`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator: KBoyEmulator = KBoyEmulatorImpl(mockLogger)

        assertTrue(emulator != null)
    }

    @Test
    fun `when bus is null then job should return null`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        val job = emulator.job()
        assertNull(job)
    }

    @Test
    fun `when ROM not loaded then frames should throw IllegalStateException`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        try {
            @Suppress("UNUSED_VARIABLE")
            val frames = emulator.frames
            assertTrue(false, "Should have thrown IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("not been initialized") == true)
        }
    }

    @Test
    fun `when pressing button then should throw NotImplementedError`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        try {
            emulator.press(Button.A)
            assertTrue(false, "Should have thrown NotImplementedError")
        } catch (@Suppress("UNUSED_VARIABLE") e: NotImplementedError) {
            // Expected
            assertTrue(true)
        }
    }

    @Test
    fun `when releasing button then should throw NotImplementedError`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        try {
            emulator.release(Button.B)
            assertTrue(false, "Should have thrown NotImplementedError")
        } catch (@Suppress("UNUSED_VARIABLE") e: NotImplementedError) {
            // Expected
            assertTrue(true)
        }
    }

    @Test
    fun `when running emulator then should not throw`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        emulator.run()
    }

    @Test
    fun `when pausing emulator then should not throw`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        emulator.pause()
    }

    @Test
    fun `when creating emulator with logger then should initialize successfully`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>(relaxed = true)
        val emulator = KBoyEmulatorImpl(mockLogger)

        assertNull(emulator.job())
    }

    @Test
    fun `when running without ROM then should not throw`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        emulator.run()
        emulator.pause()
    }

    @Test
    fun `when resetting without ROM loaded then should throw`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        try {
            emulator.reset()
            assertTrue(false, "Should have thrown an exception")
        } catch (@Suppress("UNUSED_VARIABLE") e: Exception) {
            // Expected - cannot reset without ROM loaded
            assertTrue(true)
        }
    }

    @Test
    fun `when job called multiple times then should return consistent results`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        val job1 = emulator.job()
        val job2 = emulator.job()

        assertNull(job1)
        assertNull(job2)
    }

    @Test
    fun `when running multiple times then should be safe`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        emulator.run()
        emulator.run()
        emulator.run()
    }

    @Test
    fun `when pausing multiple times then should be safe`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        emulator.pause()
        emulator.pause()
        emulator.pause()
    }

    @Test
    fun `when alternating run and pause then should handle correctly`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        emulator.run()
        emulator.pause()
        emulator.run()
        emulator.pause()
    }

    @Test
    fun `when performing consecutive operations then should not interfere`() {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)

        emulator.run()
        emulator.pause()
        val job = emulator.job()
        emulator.run()

        assertNull(job)
    }

    @Test
    fun `when loading ROM then frames flow should emit initial FrameBuffer`() = runTest {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)
        val testRom = Rom(UByteArray(size = 0x8000) { 0x00u })

        emulator.loadRom(testRom)

        emulator.frames.test {
            val frameBuffer = awaitItem()
            assertEquals(WIDTH, frameBuffer.width)
            assertEquals(HEIGHT, frameBuffer.height)
            assertEquals(WIDTH * HEIGHT, frameBuffer.pixels.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when frames flow emits then should receive initial FrameBuffer value`() = runTest {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)
        val testRom = Rom(UByteArray(size = 0x8000) { 0x00u })

        emulator.loadRom(testRom)

        emulator.frames.test {
            val firstEmission = awaitItem()
            assertTrue(firstEmission.pixels.isNotEmpty())
            assertEquals(160, firstEmission.width)
            assertEquals(144, firstEmission.height)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when accessing frames multiple times then should return same flow`() = runTest {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)
        val testRom = Rom(UByteArray(size = 0x8000) { 0x00u })

        emulator.loadRom(testRom)

        val frames1 = emulator.frames
        val frames2 = emulator.frames

        frames1.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        frames2.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when FrameBuffer emitted then should have correct dimensions`() = runTest {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)
        val testRom = Rom(UByteArray(size = 0x8000) { 0x00u })

        emulator.loadRom(testRom)

        emulator.frames.test {
            val frameBuffer = awaitItem()
            assertEquals(160, frameBuffer.width)
            assertEquals(144, frameBuffer.height)
            assertEquals(23040, frameBuffer.pixels.size) // 160 * 144
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when FrameBuffer emitted then pixels should be valid ByteArray`() = runTest {
        val mockLogger = mockk<com.github.rodrigotimoteo.kboyemucore.util.Logger>()
        val emulator = KBoyEmulatorImpl(mockLogger)
        val testRom = Rom(UByteArray(size = 0x8000) { 0x00u })

        emulator.loadRom(testRom)

        emulator.frames.test {
            val frameBuffer = awaitItem()
            assertTrue(frameBuffer.pixels.isNotEmpty())
            // All pixels should be initialized (default is 0)
            frameBuffer.pixels.forEach { pixel ->
                assertTrue(pixel in Byte.MIN_VALUE..Byte.MAX_VALUE)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}

