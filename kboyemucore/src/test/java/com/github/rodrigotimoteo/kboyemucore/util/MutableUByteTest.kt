package com.github.rodrigotimoteo.kboyemucore.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Unit tests for [MutableUByte]
 *
 * @author rodrigotimoteo
 */
class MutableUByteTest {

    // ── initial state ────────────────────────────────────────────────────────

    @Test
    fun `when created with default then value is zero`() {
        val sut = MutableUByte()

        assertEquals(0x00u.toUByte(), sut.value)
    }

    @Test
    fun `when created with explicit value then value matches`() {
        val sut = MutableUByte(0xABu)

        assertEquals(0xABu.toUByte(), sut.value)
    }

    // ── testBit ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when bit is set then testBit returns true`(bit: Int) {
        val sut = MutableUByte((1 shl bit).toUByte())

        assertTrue(sut.testBit(bit))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when bit is cleared then testBit returns false`(bit: Int) {
        val sut = MutableUByte(0x00u)

        assertFalse(sut.testBit(bit))
    }

    @Test
    fun `when testing bit out of range then throws`() {
        val sut = MutableUByte()

        assertThrows<IllegalArgumentException> { sut.testBit(8) }
        assertThrows<IllegalArgumentException> { sut.testBit(-1) }
    }

    // ── setBit ───────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when setting a bit then that bit becomes set`(bit: Int) {
        val sut = MutableUByte(0x00u)

        sut.setBit(bit)

        assertTrue(sut.testBit(bit))
        assertEquals((1 shl bit).toUByte(), sut.value)
    }

    @Test
    fun `when setting all bits sequentially then value becomes 0xFF`() {
        val sut = MutableUByte(0x00u)

        for (bit in 0..7) sut.setBit(bit)

        assertEquals(0xFFu.toUByte(), sut.value)
    }

    // ── resetBit ─────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when resetting a bit then that bit becomes cleared`(bit: Int) {
        val sut = MutableUByte(0xFFu)

        sut.resetBit(bit)

        assertFalse(sut.testBit(bit))
    }

    @Test
    fun `when resetting all bits sequentially then value becomes 0x00`() {
        val sut = MutableUByte(0xFFu)

        for (bit in 0..7) sut.resetBit(bit)

        assertEquals(0x00u.toUByte(), sut.value)
    }

    // ── data class equality ──────────────────────────────────────────────────

    @Test
    fun `when two instances have same value then they are equal`() {
        val a = MutableUByte(0x42u)
        val b = MutableUByte(0x42u)

        assertEquals(a, b)
    }

    @Test
    fun `when copying then copy is independent`() {
        val original = MutableUByte(0x42u)
        val copy = original.copy()

        copy.setBit(0)

        assertFalse(original.testBit(0))
        assertTrue(copy.testBit(0))
    }
}

