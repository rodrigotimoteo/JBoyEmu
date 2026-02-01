package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses

/**
 * Responsible for handling control operations on the CPU
 *
 * @param cpu used for cpu communication
 * @param bus used for communication with other parts in this case accessing memory
 *
 * @author rodrigotimoteo
 **/
class Control(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * Does nothing
     */
    fun nop() {
        cpu.registers.incrementProgramCounter(1)
    }

    /**
     * Complements the carry flag
     */
    fun ccf() {
        cpu.registers.flags.setFlags(
            zero = null,
            subtract = false,
            half = false,
            carry = !cpu.registers.flags.getCarryFlag()
        )

        cpu.registers.incrementProgramCounter(1)
    }

    /**
     * Sets the carry flag
     */
    fun scf() {
        cpu.registers.flags.setFlags(zero = null, subtract = false, half = false, carry = true)

        cpu.registers.incrementProgramCounter(1)
    }

    /**
     * Powers down the CPU until the next interrupt occurs. Reduces power consumption
     */
    fun halt() {
        cpu.setHalted(true)

        cpu.registers.incrementProgramCounter(1)
    }

    /**
     * Stops the CPU and LCD until a button is pressed
     */
    fun stop() {
        cpu.setStopped(true)
        bus.setValue(ReservedAddresses.DIV.memoryAddress, 0x00u)

        cpu.registers.incrementProgramCounter(1)
    }

    /**
     * Disables interrupts after execution
     */
    fun di() {
        cpu.interrupts.setInterruptChange(false)

        cpu.registers.incrementProgramCounter(1)
    }

    /**
     * Enables interrupts after execution
     */
    fun ei() {
        cpu.interrupts.setInterruptChange(true)

        cpu.registers.incrementProgramCounter(1)
    }
}
