package com.github.rodrigotimoteo.kboyemucore.ktx

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Unit tests for the [UByte] bit-manipulation extension functions
 *
 * @author rodrigotimoteo
 */
class UByteExtensionsTest {

    // ── testBit ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when testing a set bit then returns true`(bit: Int) {
        val value = (1 shl bit).toUByte()

        assertTrue(value.testBit(bit))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when testing a cleared bit then returns false`(bit: Int) {
        val value: UByte = 0x00u

        assertFalse(value.testBit(bit))
    }

    @Test
    fun `when all bits set then every bit test returns true`() {
        val value: UByte = 0xFFu

        for (bit in 0..7) {
            assertTrue(value.testBit(bit))
        }
    }

    @Test
    fun `when testing bit out of range then throws`() {
        val value: UByte = 0xFFu

        assertThrows<IllegalArgumentException> { value.testBit(8) }
        assertThrows<IllegalArgumentException> { value.testBit(-1) }
    }

    // ── setBit ───────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when setting a bit on zero then only that bit is set`(bit: Int) {
        val result = (0x00u).toUByte().setBit(bit)

        assertEquals((1 shl bit).toUByte(), result)
    }

    @Test
    fun `when setting an already set bit then value is unchanged`() {
        val value: UByte = 0xFFu

        assertEquals(0xFFu.toUByte(), value.setBit(3))
    }

    @Test
    fun `when setting bit out of range then throws`() {
        val value: UByte = 0x00u

        assertThrows<IllegalArgumentException> { value.setBit(8) }
        assertThrows<IllegalArgumentException> { value.setBit(-1) }
    }

    // ── resetBit ─────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when resetting a bit on 0xFF then only that bit is cleared`(bit: Int) {
        val result = (0xFFu).toUByte().resetBit(bit)

        val expected = (0xFF and (1 shl bit).inv()).toUByte()
        assertEquals(expected, result)
    }

    @Test
    fun `when resetting an already cleared bit then value is unchanged`() {
        val value: UByte = 0x00u

        assertEquals(0x00u.toUByte(), value.resetBit(5))
    }

    @Test
    fun `when resetting bit out of range then throws`() {
        val value: UByte = 0xFFu

        assertThrows<IllegalArgumentException> { value.resetBit(8) }
        assertThrows<IllegalArgumentException> { value.resetBit(-1) }
    }

    // ── round-trip ───────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when setting then resetting a bit then original value is restored`(bit: Int) {
        val original: UByte = 0x00u

        val set = original.setBit(bit)
        val restored = set.resetBit(bit)

        assertEquals(original, restored)
    }
}

