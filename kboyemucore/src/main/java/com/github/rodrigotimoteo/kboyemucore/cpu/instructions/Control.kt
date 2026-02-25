package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU

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
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Complements the carry flag
     */
    fun ccf() {
        cpu.cpuRegisters.flags.setFlags(
            zero = null,
            subtract = false,
            half = false,
            carry = !cpu.cpuRegisters.flags.getCarryFlag()
        )

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Sets the carry flag
     */
    fun scf() {
        cpu.cpuRegisters.flags.setFlags(zero = null, subtract = false, half = false, carry = true)

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Powers down the CPU until the next interrupt occurs. The behavior depends on the IME flag:
     * - IME=1: Normal halt, CPU sleeps until an interrupt wakes it and the interrupt is serviced
     * - IME=0, no pending interrupts: CPU halts, wakes on next interrupt but does not service it
     * - IME=0, pending interrupts (IE & IF != 0): Halt bug — CPU does NOT halt, and the next
     *   instruction's fetch will fail to increment PC (the byte after HALT is read twice)
     */
    fun halt() {
        if (!cpu.interrupts.isImeEnabled && cpu.interrupts.hasPendingInterrupts) {
            cpu.interrupts.enableHaltBug()
        } else {
            cpu.setHalted(true)
        }

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Stops the CPU and LCD until a button is pressed. On CGB, if KEY1 bit 0 is set, performs a
     * speed switch instead: toggles KEY1 bit 7 (current speed) and clears bit 0 (request).
     */
    fun stop() {
        if (bus.isCGB) {
            bus.performSpeedSwitch()
        } else {
            cpu.setStopped(true)
        }

        cpu.cpuRegisters.incrementProgramCounter(2)
    }

    /**
     * Disables interrupts immediately and cancels any pending EI change
     */
    fun di() {
        cpu.interrupts.disableIme()
        cpu.interrupts.cancelPendingChange()

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Enables interrupts after execution
     */
    fun ei() {
        cpu.interrupts.setInterruptChange(true)
        cpu.timers.setInterruptChangedCounter()

        cpu.cpuRegisters.incrementProgramCounter(1)
    }
}
