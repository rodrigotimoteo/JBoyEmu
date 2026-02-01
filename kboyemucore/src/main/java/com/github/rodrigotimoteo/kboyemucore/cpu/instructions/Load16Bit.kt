package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU

/**
 * Class responsible for handling all things that deal with 16 bit load operations in CPU instruction set
 *
 * @param cpu used for cpu communication
 * @param bus used for communication with other parts in this case accessing memory
 *
 * @author rodrigotimoteo
 **/
class Load16Bit(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * Loads a 16bit immediate value after the program counter onto a register set, BC, DE or HL,
     * which is used is defined by the argument
     *
     * @param type selects which register set to use
     */
    fun ld16bit(type: Int) {
        repeat(2) { cpu.timers.tick() }

        val value = bus.calculateNN()

        when (type) {
            0 -> cpu.registers.setBC(value)
            1 -> cpu.registers.setDE(value)
            2 -> cpu.registers.setHL(value)
        }

        cpu.registers.incrementProgramCounter(3)
    }

    /**
     * Puts the two immediate words after the program counter onto the Stack Pointer
     */
    fun ldSPUU() {
        repeat(2) { cpu.timers.tick() }

        val value = bus.calculateNN()

        cpu.registers.setStackPointer(value)
        cpu.registers.incrementProgramCounter(3)
    }

    /**
     * Put the HL 16bit register onto the stack pointer
     */
    fun ldSPHL() {
        val value = cpu.registers.getHL()

        cpu.timers.tick()
        cpu.registers.setStackPointer(value)
        cpu.registers.incrementProgramCounter(1)
    }

    /**
     * Puts the SP value plus the immediate word after the program counter into HL 16 bit register
     * (computes cpu flags)
     */
    fun ldHL() {
        repeat(2) { cpu.timers.tick() }

        val stackPointer = cpu.registers.getStackPointer()
        val programCounter = cpu.registers.getProgramCounter()

        val signedValue = bus.getValue(programCounter + 1).toByte().toInt()

        val finalAddress = (stackPointer + signedValue) and 0xFFFF
        val valueToAssign = bus.getValue(programCounter + 1).toInt()

        val halfCarry = ((stackPointer and 0xF) + (valueToAssign and 0xF) and 0x10) == 0x10
        val carry = (((stackPointer and 0xFF) + valueToAssign) and 0x100) == 0x100

        cpu.registers.flags.setFlags(
            zero = false,
            subtract = false,
            half = halfCarry,
            carry = carry
        )

        cpu.registers.setStackPointer(finalAddress)
        cpu.registers.incrementProgramCounter(2)
    }

    /**
     * Put the stack pointer at 16 bit address immediately after the program counter
     */
    fun ldNNSP() {
        repeat(2) { cpu.timers.tick() }

        val address = bus.calculateNN()
        val stackPointer = cpu.registers.getStackPointer()
        bus.setValue(address, (stackPointer and 0xFF).toUByte())
        cpu.registers.incrementProgramCounter(3)
    }

    /**
     * Push register pair given onto the stack
     *
     * @param register pair of register to use as input
     */
    fun push(register: Int) {
        repeat(3) { cpu.timers.tick() }

        val registerValue = when (register) {
            0 -> cpu.registers.getAF()
            1 -> cpu.registers.getBC()
            2 -> cpu.registers.getDE()
            3 -> cpu.registers.getHL()
            else -> return
        }
        val stackPointer = cpu.registers.getStackPointer()

        bus.setValue(stackPointer - 1, ((registerValue and 0xFF00) shr 8).toUByte())
        bus.setValue(stackPointer - 2, (registerValue and 0x00FF).toUByte())

        cpu.registers.incrementStackPointer(-2)
        cpu.registers.incrementStackPointer(-2)
        cpu.registers.incrementProgramCounter(1)
    }

    /**
     * Pop two bytes of stack into the given register pair
     *
     * @param register pair of register to use as input
     */
    fun pop(register: Int) {
        repeat(2) { cpu.timers.tick() }

        val stackPointer = cpu.registers.getStackPointer()
        val wordToInsert = bus.getValue(stackPointer + 1).toInt() shl 8 +
                bus.getValue(stackPointer).toInt()

        when (register) {
            0 -> cpu.registers.setAF(wordToInsert)
            1 -> cpu.registers.setBC(wordToInsert)
            2 -> cpu.registers.setDE(wordToInsert)
            3 -> cpu.registers.setHL(wordToInsert)
            else -> return
        }

        cpu.registers.incrementStackPointer(-2)
        cpu.registers.incrementProgramCounter(1)
    }
}
