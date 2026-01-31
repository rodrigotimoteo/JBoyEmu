package com.github.rodrigotimoteo.kboyemucore.cpu.registers

import com.github.rodrigotimoteo.kboyemucore.ktx.resetBit
import com.github.rodrigotimoteo.kboyemucore.ktx.setBit
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit
import com.github.rodrigotimoteo.kboyemucore.util.CARRY_BIT
import com.github.rodrigotimoteo.kboyemucore.util.HALF_CARRY_BIT
import com.github.rodrigotimoteo.kboyemucore.util.MutableUByte
import com.github.rodrigotimoteo.kboyemucore.util.SUBTRACT_BIT
import com.github.rodrigotimoteo.kboyemucore.util.ZERO_BIT

/**
 * This class is abstraction of the Flags used by the CPU to check for special operations such as
 * checking if output is Zero, checking the Carry and Half Carry as well as the Subtract flag if a
 * subtraction is performed
 *
 * Bit 7 - Zero Flag
 * Bit 6 - Subtraction Flag (BCD)
 * Bit 5 - Half Carry Flag (BCD)
 * Bit 4 - Carry Flag
 *
 * @author rodrigotimoteo
 */
@Suppress("TooManyFunctions")
class Flags(
    private var content: MutableUByte
) {

    /**
     * Sets all the flags based on the given values where 0 is false (reset), 1
     * is true (set) and other values (normally should be used 2) won't have any
     * effect in the flag
     *
     * @param zero status of zero flag
     * @param subtract status of subtract flag
     * @param half status of half carry flag
     * @param carry status of carry flag
     */
    fun setFlags(zero: Boolean?, subtract: Boolean?, half: Boolean?, carry: Boolean?) {
        if (zero == true) setZeroFlag() else if (zero == false) resetZeroFlag()
        if (subtract == true) setSubtractFlag() else if (subtract == false) resetSubtractFlag()
        if (half == true) setHalfCarryFlag() else if (half == false) resetHalfCarryFlag()
        if (carry == true) setCarryFlag() else if (carry == false) resetCarryFlag()
    }

    /**
     * Sets the bit correspondent to the Zero Flag
     */
    private fun setZeroFlag() {
        content.value = content.value.setBit(ZERO_BIT)
    }

    /**
     * Sets the bit correspondent to the Subtract Flag
     */
    private fun setSubtractFlag() {
        content.value = content.value.setBit(SUBTRACT_BIT)
    }

    /**
     * Sets the bit correspondent to the Half Carry Flag
     */
    private fun setHalfCarryFlag() {
        content.value = content.value.setBit(HALF_CARRY_BIT)
    }

    /**
     * Sets the bit correspondent to the Carry Flag
     */
    private fun setCarryFlag() {
        content.value = content.value.setBit(CARRY_BIT)
    }

    /**
     * Resets the bit correspondent to the Zero Flag
     */
    private fun resetZeroFlag() {
        content.value = content.value.resetBit(ZERO_BIT)
    }

    /**
     * Resets the bit correspondent to the Subtract Flag
     */
    private fun resetSubtractFlag() {
        content.value = content.value.resetBit(SUBTRACT_BIT)
    }

    /**
     * Resets the bit correspondent to the Half Carry Flag
     */
    private fun resetHalfCarryFlag() {
        content.value = content.value.resetBit(HALF_CARRY_BIT)
    }

    /**
     * Resets the bit correspondent to the Carry Flag
     */
    private fun resetCarryFlag() {
        content.value = content.value.resetBit(CARRY_BIT)
    }

    /**
     * Zero flag boolean value getter
     *
     * @return test of Zero Flag bit (true if 1 false otherwise)
     */
    fun getZeroFlag(): Boolean {
        return content.value.testBit(ZERO_BIT)
    }

    /**
     * Subtract flag boolean value getter
     *
     * @return test of Subtract Flag bit (true if 1 false otherwise)
     */
    fun getSubtractFlag(): Boolean {
        return content.value.testBit(SUBTRACT_BIT)
    }

    /**
     * Half Carry flag boolean value getter
     *
     * @return test of Half Carry Flag bit (true if 1 false otherwise)
     */
    fun getHalfCarryFlag(): Boolean {
        return content.value.testBit(HALF_CARRY_BIT)
    }

    /**
     * Carry flag boolean value getter
     *
     * @return test of Carry Flag bit (true if 1 false otherwise)
     */
    fun getCarryFlag(): Boolean {
        return content.value.testBit(CARRY_BIT)
    }

    /**
     * Converts the flags values into a readable string containing all the names
     * of flag followed by their value (boolean)
     *
     * @return String with full flags dump
     */
    override fun toString(): String {
        return "Flag Z: " + getZeroFlag() + " | N: " + getSubtractFlag() + " | H: " +
                getHalfCarryFlag() + " | C: " + getCarryFlag()
    }
}