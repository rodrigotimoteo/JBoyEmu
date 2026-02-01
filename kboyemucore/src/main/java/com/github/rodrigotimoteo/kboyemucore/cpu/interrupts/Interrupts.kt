package com.github.rodrigotimoteo.kboyemucore.cpu.interrupts

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses

/**
 * Class responsible for handling all the CPU interrupts, these are responsible for servicing hardware
 * timers, such as PPU timers, input and CPU timers
 *
 * @author rodrigotimoteo
 **/
class Interrupts(
    private val bus: Bus
) {

    /**
     * Always has the value at the [ReservedAddresses.IE] memory address
     */
    private val ieRegister: UByte
        get() = bus.getValue(ReservedAddresses.IE.memoryAddress)

    /**
     * Always has the value at the [ReservedAddresses.IF] memory address
     */
    private val ifRegister: UByte
        get() = bus.getValue(ReservedAddresses.IF.memoryAddress)

    /**
     * Stores whether the CPU is currently reacting to interrupts true if so false otherwise
     */
    private var interruptMasterEnabled: Boolean = false

    /**
     * Stores a test for the bug that exists on the halt mode of the CPU, that if the interrupt master enabled flag is
     * inactive and the value of the IE register and IF register with an and operation is different then 0 the
     * instruction ends and the PC fails to be incremented
     */
    private var haltBug: Boolean = false

    /**
     * Stores whether an interrupt state change (enabling/disabling IME) is queried
     */
    private var interruptChange: Boolean = false

    /**
     * Stores the value of which to change IME to true if enabling false otherwise when interrupt change is queried
     */
    private var changeToState: Boolean = false

    public fun handleInterrupt() {
        val availableInterrupts = decodeServiceableInterrupts()

        if (interruptMasterEnabled) {
            if (availableInterrupts.getValue() != 0x00) {
                bus.executeFromCPU(BusConstants.UNHALT, Bus.EMPTY_ARGUMENTS)
                disableIme()

                bus.storeProgramCounterInStackPointer()

                checkInterruptTypes(availableInterrupts)
            }
        } else if (bus.getFromCPU(BusConstants.GET_HALTED, Bus.EMPTY_ARGUMENTS) as Boolean) {
            if (availableInterrupts.getValue() != 0x00) {
                bus.executeFromCPU(BusConstants.HALT, Bus.EMPTY_ARGUMENTS)

                val machineCycles = bus.getFromCPU(BusConstants.GET_MC, Bus.EMPTY_ARGUMENTS) as Int
                val haltMachineCycles = bus.getFromCPU(BusConstants.GET_HALT_MC, Bus.EMPTY_ARGUMENTS) as Int

                if (machineCycles == haltMachineCycles) haltBug = true
            }
        }
    }

    /**
     * Decodes the interrrupts being requested, this is obtained from the IE and IF register
     *
     * @return value of IE register and IF register after AND operation
     */
    private fun decodeServiceableInterrupts(): Word = Word(ieRegister.getValue() and ifRegister.getValue())

    private fun checkInterruptTypes(availableInterrupts: Word) {
        for (interrupt in InterruptNames.entries) {
            if (availableInterrupts.testBit(interrupt.testBit)) {
                bus.executeFromCPU(BusConstants.SET_PC, Bus.EMPTY_ARGUMENTS)
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
    @Suppress("MagicNumber")
    public fun requestInterrupt(interrupt: Int) {
        if (interrupt !in 0..4) return

        ifRegister.setBit(interrupt)
    }

    /**
     * Request a new interrupt state change (IME change)
     *
     * @param changeToState which state to change the IME flag to (true if enable false otherwise)
     */
    public fun setInterruptChange(changeToState: Boolean) {
        interruptChange = true
        this.changeToState = changeToState
    }

    /**
     * Checks if IME change should be performed
     *
     * @return if there is IME change request
     */
    public fun requestedInterruptChange(): Boolean = interruptChange

    /**
     * Change the IME state after the request is made
     */
    public fun triggerImeChange() {
        interruptMasterEnabled = changeToState
        interruptChange = false
    }

    /**
     * Disables the Ime flag
     */
    public fun disableIme() {
        interruptMasterEnabled = false
    }

    /**
     * Public getter for the halt bug flag
     *
     * @return halt bug status, true if enabled false otherwise
     */
    public fun isHaltBug(): Boolean = haltBug

    /**
     * Disables the halt bug
     */
    public fun disableHaltBug() {
        haltBug = false
    }
}