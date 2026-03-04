package com.github.rodrigotimoteo.kboyemucore.util

import com.github.rodrigotimoteo.kboyemucore.ktx.resetBit
import com.github.rodrigotimoteo.kboyemucore.ktx.setBit
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit

/**
 * A mutable wrapper for [UByte] that provides bit manipulation functions.
 *
 * @author rodrigotimoteo
 */
data class MutableUByte(var value: UByte = 0.toUByte()) {

    /** Tests if the bit at the specified position is set (1) or not (0).
     *
     * @param bit The position of the bit to test (0-7).
     * @return `true` if the bit is set, `false` otherwise.
     * @throws IllegalArgumentException if the bit position is out of range.
     */
    fun testBit(bit: Int): Boolean {
        return value.testBit(bit)
    }

    /** Sets the bit at the specified position to 1.
     *
     * @param bit The position of the bit to set (0-7).
     * @throws IllegalArgumentException if the bit position is out of range.
     */
    fun setBit(bit: Int) {
        value = value.setBit(bit)
    }

    /** Resets the bit at the specified position to 0.
     *
     * @param bit The position of the bit to reset (0-7).
     * @throws IllegalArgumentException if the bit position is out of range.
     */
    fun resetBit(bit: Int) {
        value = value.resetBit(bit)
    }
}
