package com.github.rodrigotimoteo.kboyemucore.cpu

import com.github.rodrigotimoteo.kboyemucore.api.TimerState
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
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

    /**
     * Permanent storage of the [ReservedAddresses.TAC] register
     */
    private val tacRegister = bus.getPermanentRegister(ReservedAddresses.TAC.memoryAddress)

    /**
     * Permanent storage of the [ReservedAddresses.DIV] register
     */
    private val divRegister = bus.getPermanentRegister(ReservedAddresses.DIV.memoryAddress)

    /**
     * Permanent storage of the [ReservedAddresses.TMA] register
     */
    private val tmaRegister = bus.getPermanentRegister(ReservedAddresses.TMA.memoryAddress)

    /**
     * Permanent storage of the [ReservedAddresses.TIMA] register
     */
    private val timaRegister = bus.getPermanentRegister(ReservedAddresses.TIMA.memoryAddress)

    /**
     * Advances the timers by one unit
     */
    fun tick() {
        _machineCycles++

        tickNormalTimer()
        tickDividerTimer()
    }

    /**
     * Advances the divider timer, this timer is responsible for incrementing the DIV register at a
     * fixed frequency
     */
    private fun tickDividerTimer() {
        _dividerClockTimer++
        _totalDividerTimer++

        while (_dividerClockTimer >= 64) {
            _dividerClockTimer -= 64
            val divCounter = divRegister.value.toInt()
            divRegister.value = (divCounter + 1).toUByte()
        }

        if (_totalDividerTimer >= _timerFrequency) {
            _totalDividerTimer = 0
        }
    }

    /**
     * Advances the normal timer, this timer is responsible for incrementing the TIMA register and
     * throwing interrupts when it overflows
     */
    private fun tickNormalTimer() {
        readTACRegister()

        if (handleOverflow) {
            timaRegister.value = tmaRegister.value
            cpu.interrupts.requestInterrupt(InterruptNames.TIMER_INT.testBit)
            handleOverflow = false
        }
        if (timerEnabled) {
            _timerClockCounter++
            while (_timerClockCounter >= _timerFrequency) {
                _timerClockCounter -= _timerFrequency
                if (timaRegister.value.toInt() == 0xFF) {
                    handleOverflow = true
                } else {
                    timaRegister.value = (timaRegister.value + 1u).toUByte()
                }
            }
        }
    }

    /**
     * Reads the TAC register to update the timer enabled status and frequency
     */
    private fun readTACRegister() {
        timerEnabled = tacRegister.testBit(2)

        val previousFrequency = _timerFrequency
        when (tacRegister.value.toInt() and 0x03) {
            0x00 -> _timerFrequency = 256
            0x01 -> _timerFrequency = 4
            0x02 -> _timerFrequency = 16
            0x03 -> _timerFrequency = 64
        }
        if (previousFrequency != _timerFrequency) _timerClockCounter = 0
    }


    /**
     * Setter for the last time interrupt status was changed
     */
    fun setInterruptChangedCounter() {
        _interruptChangedCounter = _machineCycles
    }

    /**
     * Captures the current timer state for save state serialization
     *
     * @return snapshot of all timer counters and configuration
     */
    fun saveState(): TimerState = TimerState(
        _machineCycles, _interruptChangedCounter, _timerClockCounter,
        _dividerClockTimer, _totalDividerTimer, _timerFrequency,
        timerEnabled, handleOverflow,
    )

    /**
     * Restores the timers from a previously captured save state
     *
     * @param s saved timer state to restore
     */
    fun loadState(s: TimerState) {
        _machineCycles = s.machineCycles
        _interruptChangedCounter = s.interruptChangedCounter
        _timerClockCounter = s.timerClockCounter
        _dividerClockTimer = s.dividerClockTimer
        _totalDividerTimer = s.totalDividerTimer
        _timerFrequency = s.timerFrequency
        timerEnabled = s.timerEnabled
        handleOverflow = s.handleOverflow
    }
}
