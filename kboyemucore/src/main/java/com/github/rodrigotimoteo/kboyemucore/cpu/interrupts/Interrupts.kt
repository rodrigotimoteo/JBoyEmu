package com.github.rodrigotimoteo.kboyemucore.cpu.interrupts

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.ktx.resetBit
import com.github.rodrigotimoteo.kboyemucore.ktx.setBit
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

    fun handleInterrupt() {
        val availableInterrupts = decodeServiceableInterrupts()

        if (interruptMasterEnabled) {
            if (availableInterrupts != 0x00) {
                cpu.setHalted(false)
                disableIme()

                bus.storeProgramCounterInStackPointer()

                checkInterruptTypes(availableInterrupts)
            }
        } else if (cpu.isHalted() && availableInterrupts != 0x00) {
            cpu.setHalted(true)

            val machineCycles = cpu.timers.getMachineCycles()
            val haltMachineCycles = cpu.timers.getHaltCycleCounter()

            if (machineCycles == haltMachineCycles) _haltBug = true
        }
    }

    /**
     * Decodes the interrupts being requested, this is obtained from the IE and IF register
     *
     * @return value of IE register and IF register after AND operation
     */
    private fun decodeServiceableInterrupts(): Int = ieRegister.toInt() and ifRegister.toInt()

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
                bus.setValue(
                    ReservedAddresses.IF.memoryAddress,
                    ifRegister.resetBit(interrupt.testBit)
                )

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
    fun requestInterrupt(interrupt: Int) {
        if (interrupt !in 0..4) return

        bus.setValue(ReservedAddresses.IF.memoryAddress, ifRegister.setBit(interrupt))
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
     * Disables the halt bug
     */
    fun disableHaltBug() {
        _haltBug = false
    }
}