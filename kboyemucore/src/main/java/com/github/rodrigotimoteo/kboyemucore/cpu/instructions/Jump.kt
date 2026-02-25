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
@Suppress("TooManyFunctions")
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
            JumpConstants.NZ -> !cpu.cpuRegisters.flags.getZeroFlag()
            JumpConstants.Z -> cpu.cpuRegisters.flags.getZeroFlag()
            JumpConstants.NC -> !cpu.cpuRegisters.flags.getCarryFlag()
            JumpConstants.C -> cpu.cpuRegisters.flags.getCarryFlag()
        }
    }

    /**
     * Jumps to the address given by the two words after the program counter
     */
    fun jp() {
        cpu.timers.tick()
        val jumpAddress = bus.calculateNN()

        // Debug: trap when JP goes to VRAM or other non-executable areas
        if (jumpAddress in 0x8000..0x9FFF) {
            val pc = cpu.cpuRegisters.getProgramCounter()
            println("JP TO VRAM: from PC=%04X to %04X".format(pc, jumpAddress))
        }

        cpu.cpuRegisters.setProgramCounter(jumpAddress)
    }

    /**
     * Jumps to the address given by the two words after the program counter if the given condition is satisfied
     *
     * @param condition which condition to test for
     * @see jp()
     */
    fun jpCond(condition: JumpConstants) {
        val conditionalValue = getConditionalValue(condition)

        if (conditionalValue) {
            jp()
        } else {
            repeat(2) { cpu.timers.tick() }
            cpu.cpuRegisters.incrementProgramCounter(3)
        }
    }

    /**
     * Jumps to the address contained inside the HL register
     */
    fun jpHL() {
        cpu.cpuRegisters.setProgramCounter(cpu.cpuRegisters.getHL())
    }

    /**
     * Adds a given value to the program counter taken from the value after the program counter, this value is signed
     */
    fun jr() {
        cpu.timers.tick()
        val programCounter = cpu.cpuRegisters.getProgramCounter()
        val checkValue = bus.getValueFromCPU(programCounter + 1).toByte().toInt()

        cpu.cpuRegisters.incrementProgramCounter(2)
        cpu.cpuRegisters.incrementProgramCounter(checkValue)
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

        if (conditionalValue) {
            jr()
        } else {
            cpu.timers.tick()
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Pushes the address of the next instruction onto the stack and then jumps to given NN address
     */
    fun call() {
        cpu.timers.tick()

        val programCounter = cpu.cpuRegisters.getProgramCounter()
        val stackPointer = cpu.cpuRegisters.getStackPointer()
        val jumpAddress = bus.calculateNN()

        // Debug: trap when CALL goes to VRAM
        if (jumpAddress in 0x8000..0x9FFF) {
            println("CALL TO VRAM: from PC=%04X to %04X".format(programCounter, jumpAddress))
        }

        bus.setValueFromCPU(stackPointer - 1, (((programCounter + 3) and 0xFF00) shr 8).toUByte())
        bus.setValueFromCPU(stackPointer - 2, ((programCounter + 3) and 0x00FF).toUByte())

        cpu.cpuRegisters.setProgramCounter(jumpAddress)
        cpu.cpuRegisters.incrementStackPointer(-2)
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

        if (conditionalValue) {
            call()
        } else {
            repeat(2) { cpu.timers.tick() }
            cpu.cpuRegisters.incrementProgramCounter(3)
        }
    }

    /**
     * This operation pops two bytes from the stack and jumps to that address
     */
    fun ret() {
        cpu.timers.tick()

        val stackPointer = cpu.cpuRegisters.getStackPointer()
        val jumpAddress = bus.getValueFromCPU(stackPointer).toInt() +
                (bus.getValueFromCPU(stackPointer + 1).toInt() shl 8)

        // Debug: trap when RET returns to VRAM
        if (jumpAddress in 0x8000..0x9FFF) {
            println("RET TO VRAM: SP=%04X retAddr=%04X".format(stackPointer, jumpAddress))
        }

        cpu.cpuRegisters.setProgramCounter(jumpAddress)
        cpu.cpuRegisters.incrementStackPointer(2)
    }

    /**
     * This operation pops two bytes from the stack and jumps to that address
     *
     * @param condition which condition to test for
     * @see ret()
     */
    fun retCond(condition: JumpConstants) {
        val conditionalValue = getConditionalValue(condition)

        cpu.timers.tick()
        if (conditionalValue) {
            ret()
        } else {
            cpu.cpuRegisters.incrementProgramCounter(1)
        }
    }

    /**
     * Pops two bytes from the stack and jumps to that address and immediately enables interrupts.
     * Unlike EI which delays IME enable by one instruction, RETI enables IME right away.
     *
     * @see ret()
     */
    fun reti() {
        ret()

        cpu.interrupts.enableIme()
    }

    /**
     * Pushes the present address onto the stack and jump to $0000 plus the given address
     *
     * @param jumpAddress offset of where to jump to
     */
    fun rst(jumpAddress: Int) {
        cpu.timers.tick()

        val programCounter = cpu.cpuRegisters.getProgramCounter()

        // Debug: trap when RST 38h is executed from unexpected address
        if (jumpAddress == 0x38) {
            bus.trapRst38(programCounter)
        }

        val stackPointer = cpu.cpuRegisters.getStackPointer()

        bus.setValueFromCPU(stackPointer - 1, (((programCounter + 1) and 0xFF00) shr 8).toUByte())
        bus.setValueFromCPU(stackPointer - 2, ((programCounter + 1) and 0x00FF).toUByte())

        cpu.cpuRegisters.setProgramCounter(jumpAddress)
        cpu.cpuRegisters.incrementStackPointer(-2)
    }
}
