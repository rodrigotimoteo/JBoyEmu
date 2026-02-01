package com.github.rodrigotimoteo.kboyemucore.cpu.registers

import com.github.rodrigotimoteo.kboyemucore.util.MutableUByte
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlagsTest {

    private lateinit var sut: Flags

    @BeforeEach
    fun setup() {
        sut = Flags(MutableUByte())
    }

    @Test
    fun `when flags is created then all flags are false`() {
        assertFalse(sut.getCarryFlag())
        assertFalse(sut.getHalfCarryFlag())
        assertFalse(sut.getSubtractFlag())
        assertFalse(sut.getZeroFlag())
    }

    @Test
    fun `when carry flag is set then its value is true`() {
        sut.setFlags(
            zero = null,
            subtract = null,
            half = null,
            carry = true
        )

        assertTrue(sut.getCarryFlag())
        assertFalse(sut.getHalfCarryFlag())
        assertFalse(sut.getSubtractFlag())
        assertFalse(sut.getZeroFlag())
    }

    @Test
    fun `when carry flag is reset then its value is true`() {
        sut = Flags(MutableUByte(0xFF.toUByte()))
        sut.setFlags(
            zero = null,
            subtract = null,
            half = null,
            carry = false
        )

        assertFalse(sut.getCarryFlag())
        assertTrue(sut.getHalfCarryFlag())
        assertTrue(sut.getSubtractFlag())
        assertTrue(sut.getZeroFlag())
    }

    @Test
    fun `when half carry flag is set then its value is true`() {
        sut.setFlags(
            zero = null,
            subtract = null,
            half = true,
            carry = null
        )

        assertFalse(sut.getCarryFlag())
        assertTrue(sut.getHalfCarryFlag())
        assertFalse(sut.getSubtractFlag())
        assertFalse(sut.getZeroFlag())
    }

    @Test
    fun `when half carry flag is reset then its value is true`() {
        sut = Flags(MutableUByte(0xFF.toUByte()))
        sut.setFlags(
            zero = null,
            subtract = null,
            half = false,
            carry = null
        )

        assertTrue(sut.getCarryFlag())
        assertFalse(sut.getHalfCarryFlag())
        assertTrue(sut.getSubtractFlag())
        assertTrue(sut.getZeroFlag())
    }

    @Test
    fun `when subtract flag is set then its value is true`() {
        sut.setFlags(
            zero = null,
            subtract = true,
            half = null,
            carry = null
        )

        assertFalse(sut.getCarryFlag())
        assertFalse(sut.getHalfCarryFlag())
        assertTrue(sut.getSubtractFlag())
        assertFalse(sut.getZeroFlag())
    }

    @Test
    fun `when subtract flag is reset then its value is true`() {
        sut = Flags(MutableUByte(0xFF.toUByte()))
        sut.setFlags(
            zero = null,
            subtract = false,
            half = null,
            carry = null
        )

        assertTrue(sut.getCarryFlag())
        assertTrue(sut.getHalfCarryFlag())
        assertFalse(sut.getSubtractFlag())
        assertTrue(sut.getZeroFlag())
    }

    @Test
    fun `when zero flag is set then its value is true`() {
        sut.setFlags(
            zero = true,
            subtract = null,
            half = null,
            carry = null
        )

        assertFalse(sut.getCarryFlag())
        assertFalse(sut.getHalfCarryFlag())
        assertFalse(sut.getSubtractFlag())
        assertTrue(sut.getZeroFlag())
    }

    @Test
    fun `when zero flag is reset then its value is true`() {
        sut = Flags(MutableUByte(0xFF.toUByte()))
        sut.setFlags(
            zero = false,
            subtract = null,
            half = null,
            carry = null
        )

        assertTrue(sut.getCarryFlag())
        assertTrue(sut.getHalfCarryFlag())
        assertTrue(sut.getSubtractFlag())
        assertFalse(sut.getZeroFlag())
    }
}
