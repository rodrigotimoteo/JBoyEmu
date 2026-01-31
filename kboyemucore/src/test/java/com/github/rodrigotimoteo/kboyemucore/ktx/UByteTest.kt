package com.github.rodrigotimoteo.kboyemucore.ktx

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals


class UByteTest {

    var sut = 0x00.toUByte()

    @Test
    fun `when calling setBit with invalid bit then throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            sut.setBit(-1)
        }
        assertThrows<IllegalArgumentException> {
            sut.setBit(8)
        }
    }

    @Test
    fun `when calling resetBit with invalid bit then throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            sut.resetBit(-1)
        }
        assertThrows<IllegalArgumentException> {
            sut.resetBit(8)
        }
    }

    @Test
    fun `when calling testBit with invalid bit then throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            sut.testBit(-1)
        }
        assertThrows<IllegalArgumentException> {
            sut.testBit(8)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when setting bit then correct value should be returned`(bit: Int) {
        assertEquals(expected = (1 shl bit).toUByte(), actual = sut.setBit(bit))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when resetting bit then correct value should be returned`(bit: Int) {
        sut = 0xFF.toUByte()
        assertEquals(expected = (0xFF - (1 shl bit)).toUByte(), actual = sut.resetBit(bit))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when testing bit and value is 0xFF then correct status should be returned`(bit: Int) {
        sut = 0xFF.toUByte()
        assertTrue(sut.testBit(bit))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `when testing bit and value is 0x00 then correct status should be returned`(bit: Int) {
        assertFalse(sut.testBit(bit))
    }
}
