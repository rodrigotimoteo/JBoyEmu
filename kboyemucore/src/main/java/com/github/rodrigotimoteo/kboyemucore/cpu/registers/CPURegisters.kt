package com.github.rodrigotimoteo.kboyemucore.cpu.registers

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.util.AF_INITIAL_VALUE
import com.github.rodrigotimoteo.kboyemucore.util.BC_INITIAL_VALUE
import com.github.rodrigotimoteo.kboyemucore.util.DE_INITIAL_VALUE
import com.github.rodrigotimoteo.kboyemucore.util.EIGHT_BITS
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_16_BITS
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_LOWER_BITS
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_TOP_BITS
import com.github.rodrigotimoteo.kboyemucore.util.HL_INITIAL_VALUE
import com.github.rodrigotimoteo.kboyemucore.util.MutableUByte
import com.github.rodrigotimoteo.kboyemucore.util.PROGRAM_COUNTER_INITIAL_VALUE
import com.github.rodrigotimoteo.kboyemucore.util.STACK_POINTER_INITIAL_VALUE

/**
 * This abstraction holds the registers inside the SHARP CPU as well as the ability to return the
 * [Flags] from [RegisterNames.F]
 *
 * @author rodrigotimoteo
 **/
@Suppress("TooManyFunctions")
class CPURegisters(
    private val bus: Bus
) {
    /**
     * Stores all the registers with their associated name (register names kept as enum due to their limited nature)
     */
    private val registersArray = Array(RegisterNames.entries.size) { MutableUByte() }

    /**
     * Stores the CPU flags hold by register F
     */
    internal val flags: Flags = Flags(getRegister(RegisterNames.F))

    /**
     * Stores the PC (default value at the end of boot rom is 0x0100)
     */
    private var programCounter: Int = PROGRAM_COUNTER_INITIAL_VALUE

    /**
     * Stores the SP (default value at the end of boot rom is 0xFFFE)
     */
    private var stackPointer: Int = STACK_POINTER_INITIAL_VALUE

    /**
     * Initializes the register to their default values
     */
    init {
        setAF(AF_INITIAL_VALUE)
        setBC(BC_INITIAL_VALUE)
        setDE(DE_INITIAL_VALUE)
        setHL(HL_INITIAL_VALUE)
    }

    /**
     * Getter for the PC
     *
     * @return PC value
     */
    fun getProgramCounter(): Int = programCounter

    /**
     * Increments the program counter by the given value
     *
     * @param value to increase program counter
     */
    fun incrementProgramCounter(value: Int) {
        programCounter += value
    }

    /**
     * Sets the value of program counter to given one
     *
     * @param value to assign program counter
     */
    fun setProgramCounter(value: Int) {
        programCounter = value
    }

    /**
     * Getter for the SP
     *
     * @return SP value
     */
    fun getStackPointer(): Int {
        return stackPointer
    }

    /**
     * Increments the stack pointer by the given value
     *
     * @param value to increase stack pointer
     */
    fun incrementStackPointer(value: Int) {
        stackPointer = (stackPointer + value) and FILTER_16_BITS
    }

    /**
     * Sets the value of stack pointer to given one
     *
     * @param value to assign stack pointer
     */
    fun setStackPointer(value: Int) {
        stackPointer = value
    }

    /**
     * Register getter based on given name (uses !! due to there not existing a possibility where a register is null)
     *
     * @param register name
     * @return content of given register
     */
    fun getRegister(register: RegisterNames): MutableUByte = registersArray[register.ordinal]

    /**
     * Sets the register content to the given value
     *
     * @param register name
     * @param value to assign to the given register
     */
    fun setRegister(register: RegisterNames, value: UByte) {
        getRegister(register).value = value
    }

    /**
     * Returns the result of the aggregation of register A (as the left value) and register F (as the right value)
     * creating a 16bit Word
     *
     * @return register A and F together
     */
    fun getAF(): Int = (getRegister(RegisterNames.A).value.toInt() shl EIGHT_BITS) +
            getRegister(RegisterNames.F).value.toInt()

    /**
     * Returns the result of the aggregation of register B (as the left value) and register C (as the right value)
     * creating a 16bit Word
     *
     * @return register B and C together
     */
    fun getBC(): Int = (getRegister(RegisterNames.B).value.toInt() shl EIGHT_BITS) +
            getRegister(RegisterNames.C).value.toInt()

    /**
     * Returns the result of the aggregation of register D (as the left value) and register E (as the right value)
     * creating a 16bit Word
     *
     * @return register D and E together
     */
    fun getDE(): Int = (getRegister(RegisterNames.D).value.toInt() shl EIGHT_BITS) +
            getRegister(RegisterNames.E).value.toInt()

    /**
     * Returns the result of the aggregation of register H (as the left value) and register L (as the right value)
     * creating a 16bit Word
     *
     * @return register H and L together
     */
    fun getHL(): Int = (getRegister(RegisterNames.H).value.toInt() shl EIGHT_BITS) +
            getRegister(RegisterNames.L).value.toInt()

    /**
     * Sets the value of register A (with high 8 bits of value) and F (with low 8 bits of value)
     *
     * @param value to assign
     */

    fun setAF(value: Int) {
        setRegister(RegisterNames.A, ((value and FILTER_TOP_BITS) shr EIGHT_BITS).toUByte())
        setRegister(RegisterNames.F, (value and 0x00F0).toUByte())
    }

    /**
     * Sets the value of register B (with high 8 bits of value) and C (with low 8 bits of value)
     *
     * @param value to assign
     */
    fun setBC(value: Int) {
        setRegister(RegisterNames.B, ((value and FILTER_TOP_BITS) shr EIGHT_BITS).toUByte())
        setRegister(RegisterNames.C, (value and FILTER_LOWER_BITS).toUByte())
    }

    /**
     * Sets the value of register D (with high 8 bits of value) and E (with low 8 bits of value)
     *
     * @param value to assign
     */
    fun setDE(value: Int) {
        setRegister(RegisterNames.D, ((value and FILTER_TOP_BITS) shr EIGHT_BITS).toUByte())
        setRegister(RegisterNames.E, (value and FILTER_LOWER_BITS).toUByte())
    }

    /**
     * Sets the value of register H (with high 8 bits of value) and L (with low 8 bits of value)
     *
     * @param value to assign
     */
    fun setHL(value: Int) {
        setRegister(RegisterNames.H, ((value and FILTER_TOP_BITS) shr EIGHT_BITS).toUByte())
        setRegister(RegisterNames.L, (value and FILTER_LOWER_BITS).toUByte())
    }

    /**
     * Builds the content of all CPU registers in the following manner
     * RegisterName: RegisterValue ... Next 4 instructions to execute
     *
     * @return String with debug dump of registers
     */
    @Suppress("ImplicitDefaultLocale")
    override fun toString(): String {
        val stringBuilder = StringBuilder();

        stringBuilder.append(RegisterNames.A).append(": ")
            .append(String.format("%02X", getRegister(RegisterNames.A).value.toInt()))
            .append(" ");
        stringBuilder.append(RegisterNames.F).append(": ")
            .append(String.format("%02X", getRegister(RegisterNames.F).value.toInt()))
            .append(" ");
        stringBuilder.append(RegisterNames.B).append(": ")
            .append(String.format("%02X", getRegister(RegisterNames.B).value.toInt()))
            .append(" ");
        stringBuilder.append(RegisterNames.C).append(": ")
            .append(String.format("%02X", getRegister(RegisterNames.C).value.toInt()))
            .append(" ");
        stringBuilder.append(RegisterNames.D).append(": ")
            .append(String.format("%02X", getRegister(RegisterNames.D).value.toInt()))
            .append(" ");
        stringBuilder.append(RegisterNames.E).append(": ")
            .append(String.format("%02X", getRegister(RegisterNames.E).value.toInt()))
            .append(" ");
        stringBuilder.append(RegisterNames.H).append(": ")
            .append(String.format("%02X", getRegister(RegisterNames.H).value.toInt()))
            .append(" ");
        stringBuilder.append(RegisterNames.L).append(": ")
            .append(String.format("%02X", getRegister(RegisterNames.L).value.toInt()))
            .append(" ");

        stringBuilder.append("SP: ").append(String.format("%04X", stackPointer)).append(" ");
        stringBuilder.append("PC: 00:").append(String.format("%04X", programCounter)).append(" ");

        stringBuilder.append("(").append(String.format("%02X", bus.getValue(programCounter).toInt()));
        stringBuilder.append(" ").append(String.format("%02X", bus.getValue(programCounter + 1).toInt()));
        stringBuilder.append(" ").append(String.format("%02X", bus.getValue(programCounter + 2).toInt()));
        stringBuilder.append(" ").append(String.format("%02X", bus.getValue(programCounter + 3).toInt()));
        stringBuilder.append(")");

        return stringBuilder.toString();
    }
}