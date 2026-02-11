package com.github.rodrigotimoteo.kboyemucore.cpu

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses

/**
 * Class purposed with handling everything that needs timings inside the CPU total Cycles, interrupts
 * and others
 *
 * @author rodrigotimoteo
 **/
class Timers(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * Stores whether the timer is currently enabled (true if enabled false otherwise)
     */
    private var timerEnabled: Boolean = true

    /**
     * Stores if there has been a timer overflow
     */
    private var handleOverflow: Boolean = false

    /**
     * Stores the amount of executed machine cycles
     */
    private var _machineCycles: Int = 0

    /**
     * Internal getter for the [_machineCycles] variable
     */
    internal val machineCycles
        get() = _machineCycles

    /**
     * Stores the cycles when halt was last triggered
     */
    private var _haltCycleCounter: Int = 0

    /**
     * Internal getter for the [_haltCycleCounter] variable
     */
    internal val haltCycleCounter
        get() = _haltCycleCounter

    /**
     * Stores the cycles when interrupt status was last changed
     */
    private var _interruptChangedCounter: Int = 0

    /**
     * Internal getter for the [_interruptChangedCounter] variable
     */
    internal val interruptChangedCounter
        get() = _interruptChangedCounter

    /**
     * Stores the "normal" cycles performed by the timer
     */
    private var _timerClockCounter: Int = 0

    /**
     * Stores the divider cycles performed by the timer
     */
    private var _dividerClockTimer: Int = 0

    /**
     * Stores the total divider cycles performed by the timer
     */
    private var _totalDividerTimer: Int = 0

    /**
     * Frequency of the timer indicates when the timer should throw a interrupt
     */
    private var _timerFrequency: Int = 256

    private var shouldHandleOverflow: Boolean = false

    /**
     * Advances the timers by one unit
     */
    fun tick() {
        _machineCycles++

        tickDividerTimer()
        tickNormalTimer()
    }

    private fun tickDividerTimer() {
        _dividerClockTimer++
        _totalDividerTimer++

        while (_dividerClockTimer >= 64) {
            _dividerClockTimer -= 64
            val divCounter = bus.getValue(ReservedAddresses.DIV.memoryAddress).toInt()
            bus.setDIV((divCounter + 1).toUByte())
        }

        if (_totalDividerTimer >= _timerFrequency) {
            _totalDividerTimer = 0
        }
    }

    private fun tickNormalTimer() {
        readTACRegister()

        if (handleOverflow) {
            val tmaRegister = bus.getValue(ReservedAddresses.TMA.memoryAddress)
            bus.setValueFromPPU(ReservedAddresses.TIMA.memoryAddress, tmaRegister)
            cpu.interrupts.requestInterrupt(InterruptNames.TIMER_INT.testBit)
            handleOverflow = false
        }
        if (timerEnabled) {
            _timerClockCounter++
            while (_timerClockCounter >= _timerFrequency) {
                _timerClockCounter -= _timerFrequency
                if (bus.getValue(ReservedAddresses.TIMA.memoryAddress).toInt() == 0xFF) {
                    handleOverflow = true
                } else {
                    val timaRegister = bus.getValue(ReservedAddresses.TIMA.memoryAddress)
                    bus.setValueFromPPU(
                        ReservedAddresses.TIMA.memoryAddress,
                        (timaRegister + 1u).toUByte()
                    )

                }
            }
        }
    }

    private fun readTACRegister() {

        //Timer Enabled
        val tacRegister = bus.getValue(ReservedAddresses.TAC.memoryAddress)
        timerEnabled = tacRegister.testBit(2)

        //Timer Input Clock Select
        val previousFrequency = _timerFrequency
        when (tacRegister.toInt() and 0x03) {
            0x00 -> _timerFrequency = 256
            0x01 -> _timerFrequency = 4
            0x02 -> _timerFrequency = 16
            0x03 -> _timerFrequency = 64
        }
        if (previousFrequency != _timerFrequency) _timerClockCounter = 0
    }

    /**
     * Halt cycles counter for when Halt is triggered
     */
    fun setHaltCycleCounter() {
        _haltCycleCounter = _machineCycles
    }

    /**
     * Setter for the last time interrupt status was changed
     */
    fun setInterruptChangedCounter() {
        _interruptChangedCounter = _machineCycles
    }
}
