package com.github.rodrigotimoteo.kboyemucore.spu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AudioRingBuffer]
 *
 * @author rodrigotimoteo
 */
class AudioRingBufferTest {

    // ── construction ─────────────────────────────────────────────────────────

    @Test
    fun `when created then buffer is empty`() {
        val sut = AudioRingBuffer(64)
        val dst = ShortArray(64)

        val read = sut.read(dst, 0, dst.size)

        assertEquals(0, read)
    }

    @Test
    fun `when capacity is not power of two then throws`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { AudioRingBuffer(10) }
    }

    @Test
    fun `when capacity is less than four then throws`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { AudioRingBuffer(2) }
    }

    // ── write and read ───────────────────────────────────────────────────────

    @Test
    fun `when writing one sample then read returns two shorts`() {
        val sut = AudioRingBuffer(64)

        sut.writeSample(100, 200)

        val dst = ShortArray(64)
        val n = sut.read(dst, 0, dst.size)

        assertEquals(2, n)
        assertEquals(100.toShort(), dst[0])
        assertEquals(200.toShort(), dst[1])
    }

    @Test
    fun `when writing multiple samples then read returns all in order`() {
        val sut = AudioRingBuffer(64)

        sut.writeSample(1, 2)
        sut.writeSample(3, 4)
        sut.writeSample(5, 6)

        val dst = ShortArray(64)
        val n = sut.read(dst, 0, dst.size)

        assertEquals(6, n)
        assertEquals(1.toShort(), dst[0])
        assertEquals(2.toShort(), dst[1])
        assertEquals(3.toShort(), dst[2])
        assertEquals(4.toShort(), dst[3])
        assertEquals(5.toShort(), dst[4])
        assertEquals(6.toShort(), dst[5])
    }

    @Test
    fun `when reading with limited maxSamples then returns only that many`() {
        val sut = AudioRingBuffer(64)

        sut.writeSample(1, 2)
        sut.writeSample(3, 4)

        val dst = ShortArray(64)
        val n = sut.read(dst, 0, 2)

        assertEquals(2, n)
        assertEquals(1.toShort(), dst[0])
        assertEquals(2.toShort(), dst[1])
    }

    @Test
    fun `when read returns even count to maintain stereo alignment`() {
        val sut = AudioRingBuffer(64)

        sut.writeSample(1, 2)
        sut.writeSample(3, 4)

        val dst = ShortArray(64)
        val n = sut.read(dst, 0, 3)

        assertEquals(2, n)
    }

    @Test
    fun `when reading with offset then data is placed at offset`() {
        val sut = AudioRingBuffer(64)

        sut.writeSample(10, 20)

        val dst = ShortArray(64)
        val n = sut.read(dst, 4, dst.size)

        assertEquals(2, n)
        assertEquals(10.toShort(), dst[4])
        assertEquals(20.toShort(), dst[5])
    }

    @Test
    fun `when buffer is empty after read then subsequent read returns zero`() {
        val sut = AudioRingBuffer(64)

        sut.writeSample(1, 2)

        val dst = ShortArray(64)
        sut.read(dst, 0, dst.size)
        val n = sut.read(dst, 0, dst.size)

        assertEquals(0, n)
    }

    // ── full buffer ──────────────────────────────────────────────────────────

    @Test
    fun `when buffer is full then write returns false`() {
        val sut = AudioRingBuffer(4)

        val first = sut.writeSample(1, 2)
        val second = sut.writeSample(3, 4)

        assertTrue(first)
        assertFalse(second)
    }

    @Test
    fun `when buffer wraps around then data is still correct`() {
        val sut = AudioRingBuffer(8)

        sut.writeSample(1, 2)
        sut.writeSample(3, 4)
        sut.writeSample(5, 6)

        val dst = ShortArray(4)
        sut.read(dst, 0, 4)

        sut.writeSample(7, 8)
        sut.writeSample(9, 10)

        val dst2 = ShortArray(8)
        val n = sut.read(dst2, 0, 8)

        assertEquals(5.toShort(), dst2[0])
        assertEquals(6.toShort(), dst2[1])
        assertEquals(7.toShort(), dst2[2])
        assertEquals(8.toShort(), dst2[3])
        assertEquals(9.toShort(), dst2[4])
        assertEquals(10.toShort(), dst2[5])
        assertEquals(6, n)
    }
}

