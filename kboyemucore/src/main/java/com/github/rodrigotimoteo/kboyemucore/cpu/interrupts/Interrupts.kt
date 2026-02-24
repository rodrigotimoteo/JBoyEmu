package com.github.rodrigotimoteo.kboyemucore.cpu.interrupts

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses

/**
 * Class responsible for handling all the CPU interrupts, these are responsible for servicing hardware
 * timers, such as PPU timers, input and CPU timers
 *
 * @author rodrigotimoteo
 **/
class Interrupts(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * Always has the value at the [ReservedAddresses.IE] memory address
     */
    private val ieRegister = bus.getPermanentRegister(ReservedAddresses.IE.memoryAddress)

    /**
     * Always has the value at the [ReservedAddresses.IF] memory address
     */
    private val ifRegister = bus.getPermanentRegister(ReservedAddresses.IF.memoryAddress)

    /**
     * Stores whether the CPU is currently reacting to interrupts true if so false otherwise
     */
    private var interruptMasterEnabled: Boolean = false

    /** Whether the interrupt master enable flag is currently active */
    val isImeEnabled: Boolean
        get() = interruptMasterEnabled

    /** Whether there are any pending interrupts that can be serviced (IE & IF != 0) */
    val hasPendingInterrupts: Boolean
        get() = decodeServiceableInterrupts() != 0

    /**
     * Stores a test for the bug that exists on the halt mode of the CPU, that if the interrupt master enabled flag is
     * inactive and the value of the IE register and IF register with an and operation is different then 0 the
     * instruction ends and the PC fails to be incremented
     */
    private var _haltBug: Boolean = false

    /** Value getter for the halt bug variable */
    val haltBug: Boolean
        get() = _haltBug

    /**
     * Stores whether an interrupt state change (enabling/disabling IME) is queried
     */
    private var interruptChange: Boolean = false

    /**
     * Stores the value of which to change IME to true if enabling false otherwise when interrupt change is queried
     */
    private var changeToState: Boolean = false

    /**
     * Handles the interrupt process. If IME is active and there are serviceable interrupts (IE & IF
     * != 0), it wakes the CPU from halt, disables IME, pushes PC to the stack and jumps to the
     * appropriate interrupt vector. If IME is inactive but the CPU is halted and there are pending
     * interrupts, it simply wakes the CPU without servicing the interrupt.
     */
    fun handleInterrupt() {
        val availableInterrupts = decodeServiceableInterrupts()

        if (interruptMasterEnabled) {
            if (availableInterrupts != 0x00) {
                cpu.setHalted(false)
                disableIme()

                repeat(2) { cpu.timers.tick() }
                bus.storeProgramCounterInStackPointer()
                cpu.timers.tick()

                checkInterruptTypes(availableInterrupts)
            }
        } else if (cpu.isHalted() && availableInterrupts != 0x00) {
            cpu.setHalted(false)
        }
    }

    /**
     * Checks if the joypad interrupt is being requested, if so it changes the stopped state of the
     * CPU to false. Checks only IF register since IE may be 0 during STOP mode.
     */
    fun checkJoypadInterrupt() {
        if (ifRegister.value.toInt().toUByte().testBit(InterruptNames.JOYPAD_INT.testBit)) {
            cpu.setStopped(false)
        }
    }

    /**
     * Decodes the interrupts being requested, this is obtained from the IE and IF register
     *
     * @return value of IE register and IF register after AND operation
     */
    private fun decodeServiceableInterrupts(): Int =
        ieRegister.value.toInt() and ifRegister.value.toInt() and 0x1F

    /**
     * Based on the available given interrupts to be serviced provided by the integer received that
     * combines the values of IE and IF register. Serves the first available interrupt and then quits
     *
     * @param availableInterrupts combination of IE and IF register
     */
    private fun checkInterruptTypes(availableInterrupts: Int) {
        InterruptNames.entries.forEachIndexed { index, interrupt ->
            if (availableInterrupts.toUByte().testBit(interrupt.testBit)) {
                cpu.cpuRegisters.setProgramCounter(0x40 + 0x8 * index)
                ifRegister.resetBit(interrupt.testBit)

                return
            }
        }
    }

    /**
     * Request an interrupt based on given value (these values are defined in InterruptNames enum)
     *
     * @param interrupt bit to set in the IF Register
     */
    fun requestInterrupt(interrupt: Int) {
        if (interrupt !in 0..4) return

        ifRegister.setBit(interrupt)
    }

    /**
     * Request a new interrupt state change (IME change)
     *
     * @param changeToState which state to change the IME flag to (true if enable false otherwise)
     */
    fun setInterruptChange(changeToState: Boolean) {
        interruptChange = true
        this.changeToState = changeToState
    }

    /**
     * Checks if IME change should be performed
     *
     * @return if there is IME change request
     */
    fun requestedInterruptChange(): Boolean = interruptChange

    /**
     * Change the IME state after the request is made
     */
    fun triggerImeChange() {
        interruptMasterEnabled = changeToState
        interruptChange = false
    }

    /**
     * Disables the Ime flag
     */
    fun disableIme() {
        interruptMasterEnabled = false
    }

    /**
     * Cancels any pending IME change, used by DI to ensure a queued EI does not re-enable interrupts
     */
    fun cancelPendingChange() {
        interruptChange = false
    }

    /**
     * Enables the halt bug flag, called from HALT when IME=0 and there are pending interrupts
     */
    fun enableHaltBug() {
        _haltBug = true
    }

    /**
     * Disables the halt bug
     */
    fun disableHaltBug() {
        _haltBug = false
    }
}
