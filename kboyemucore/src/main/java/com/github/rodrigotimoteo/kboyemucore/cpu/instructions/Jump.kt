package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU

/**
 * This class is responsible for handling all jump operations, calls and the restart operation
 *
 * @param cpu used for cpu communication
 * @param bus used for communication with other parts in this case accessing memory
 *
 * @author rodrigotimoteo
 **/
class Jump(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * Gets the boolean correspondent to the given condition
     *
     * @param condition which condition to test
     * @return boolean of condition test
     */
    private fun getConditionalValue(condition: JumpConstants): Boolean {
        return when (condition) {
            JumpConstants.NZ -> !cpu.CPURegisters.flags.getZeroFlag()
            JumpConstants.Z -> cpu.CPURegisters.flags.getZeroFlag()
            JumpConstants.NC -> !cpu.CPURegisters.flags.getCarryFlag()
            JumpConstants.C -> cpu.CPURegisters.flags.getCarryFlag()
        }
    }

    /**
     * Jumps to the address given by the two words after the program counter
     */
    fun jp() {
        val jumpAddress = bus.calculateNN()

        cpu.CPURegisters.setProgramCounter(jumpAddress)
        cpu.timers.tick()
    }

    /**
     * Jumps to the address given by the two words after the program counter if the given condition is statisfied
     *
     * @param condition which condition to test for
     * @see jp()
     */
    fun jpCond(condition: JumpConstants) {
        val conditionalValue = getConditionalValue(condition)

        if (conditionalValue) jp()
        else {
            cpu.CPURegisters.incrementProgramCounter(3)
            repeat(2) { cpu.timers.tick() }
        }
    }

    /**
     * Jumps to the address contained inside the HL register
     */
    fun jpHL() {
        cpu.CPURegisters.setProgramCounter(cpu.CPURegisters.getHL())
    }

    /**
     * Adds a given value to the program counter taken from the value after the program counter, this value is signed
     */
    fun jr() {
        repeat(2) { cpu.timers.tick() }

        val programCounter = cpu.CPURegisters.getProgramCounter()
        val checkValue = bus.getValue(programCounter + 1).toInt()

        if (checkValue shr 7 == 0) {
            cpu.CPURegisters.incrementProgramCounter(checkValue and 0x7F)
        } else {
            cpu.CPURegisters.incrementProgramCounter((checkValue and 0x7F) - 128)
        }

        cpu.CPURegisters.incrementProgramCounter(2)
    }

    /**
     * Adds a given value to the program counter taken from the value after the program counter, if the given condition
     * is satisfied, given value is signed
     *
     * @param condition which condition to test for
     * @see jr()
     */
    fun jrCond(condition: JumpConstants) {
        val conditionalValue = getConditionalValue(condition)

        if (conditionalValue) jr()
        else {
            cpu.CPURegisters.incrementProgramCounter(3)
            cpu.timers.tick()
        }
    }

    /**
     * Pushes the address of the next instruction onto the stack and then jumps to given NN address
     */
    fun call() {
        repeat(3) { cpu.timers.tick() }

        val programCounter = cpu.CPURegisters.getProgramCounter()
        val stackPointer = cpu.CPURegisters.getStackPointer()
        val jumpAddress = bus.calculateNN()

        bus.setValue(stackPointer - 1, (((programCounter + 3) and 0xFF00) shr 8).toUByte())
        bus.setValue(stackPointer - 2, ((programCounter + 3) and 0x00FF).toUByte())

        cpu.CPURegisters.setProgramCounter(jumpAddress)
        cpu.CPURegisters.incrementProgramCounter(-2)
    }

    /**
     * Pushes the address of the next instruction onto the stack and then jumps to given NN address if given condition
     * is satisfied
     *
     * @param condition which condition to test for
     * @see call()
     */
    fun callCond(condition: JumpConstants) {
        val conditionalValue = getConditionalValue(condition)

        if (conditionalValue) call()
        else {
            cpu.CPURegisters.incrementProgramCounter(3)
            repeat(2) { cpu.timers.tick() }
        }
    }

    /**
     * This operation pops two bytes from the stack and jumps to that address
     */
    fun ret() {
        repeat(3) { cpu.timers.tick() }

        val stackPointer = cpu.CPURegisters.getStackPointer()
        val jumpAddress = bus.getValue(stackPointer).toInt() +
                bus.getValue(stackPointer + 1).toInt() shl 8

        cpu.CPURegisters.setProgramCounter(jumpAddress)
        cpu.CPURegisters.incrementProgramCounter(2)
    }

    /**
     * This operation pops two bytes from the stack and jumps to that address
     *
     * @param condition which condition to test for
     * @see ret()
     */
    fun retCond(condition: JumpConstants) {
        val conditionalValue = getConditionalValue(condition)

        if (conditionalValue) {
            ret()
        } else {
            cpu.CPURegisters.incrementProgramCounter(1)
        }
    }

    /**
     * Pops two bytes from the stack and jumps to that address and enables interrupts
     *
     * @see ret()
     */
    fun reti() {
        ret()
        cpu.interrupts.setInterruptChange(true)
    }

    /**
     * Pushes the present address onto the stack and jump to $0000 plus the given address
     *
     * @param jumpAddress offset of where to jump to
     */
    fun rst(jumpAddress: Int) {
        repeat(3) { cpu.timers.tick() }

        val programCounter = cpu.CPURegisters.getProgramCounter()
        val stackPointer = cpu.CPURegisters.getStackPointer()

        bus.setValue(stackPointer - 1, (((programCounter + 1) and 0xFF00) shr 8).toUByte())
        bus.setValue(stackPointer - 2, ((programCounter + 1) and 0x00FF).toUByte())

        cpu.CPURegisters.setProgramCounter(jumpAddress)
        cpu.CPURegisters.incrementProgramCounter(-2)
    }
}
