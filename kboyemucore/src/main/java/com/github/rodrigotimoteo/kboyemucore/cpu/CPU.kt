package com.github.rodrigotimoteo.kboyemucore.cpu

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.instructions.Decoder
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.Interrupts
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.CPURegisters

class CPU(
    private val bus: Bus
) {

    /** Reference to the [CPURegisters] */
    internal val cpuRegisters = CPURegisters(bus)

    /** Reference to the [Timers] */
    internal val timers = Timers(this, bus)

    /** Reference to the [Interrupts] */
    internal val interrupts = Interrupts(this, bus)

    /** Reference to the [Decoder] */
    internal val decoder = Decoder(this, bus)

    /**
     * Stores whether the CPU is currently halted
     */
    private var isHalted = false

    /**
     * Stores whether the CPU is currently stopped
     */
    private var isStopped = false

    /**
     * Executes a CPU tick, this is the main function of the CPU and is responsible for executing
     * instructions, handling interrupts and updating timers
     */
    fun tick() {
        interrupts.checkJoypadInterrupt()

        if (!isStopped) {
            if (!isHalted) {
//                println(cpuRegisters)
                executeOperation()

                val imeChange = interrupts.requestedInterruptChange()
                val interruptChangeCounter = timers.interruptChangedCounter
                val machineCycles = timers.machineCycles

                if (imeChange && interruptChangeCounter < machineCycles) {
                    interrupts.triggerImeChange()
                }
            } else {
                timers.tick()
            }

            interrupts.handleInterrupt()
        }
    }

    /**
     * Returns the amount of machineCycles that the CPU has executed
     */
    fun getCounter() = timers.machineCycles

    /**
     * Executes the operation that is currently pointed by the program counter, if the halt bug is
     * active it decodes the instruction at the program counter and then decrements the program counter
     * by 1 to make it point to the same instruction for the next tick
     */
    private fun executeOperation() {
        val programCounter = cpuRegisters.getProgramCounter()

        if (interrupts.haltBug) {
            decoder.decode(bus.getValueFromCPU(programCounter).toInt())
            cpuRegisters.incrementProgramCounter(-1)
            interrupts.disableHaltBug()
        } else {
            decoder.decode(bus.getValueFromCPU(programCounter).toInt())
        }
    }

    /**
     * Getter for the isHalted flag
     *
     * @return true if CPU is halted false otherwise
     */
    fun isHalted(): Boolean = isHalted

    /**
     * Sets the halted state to the provided one
     *
     * @param haltedState should be halted or not (true if should false otherwise)
     */
    fun setHalted(haltedState: Boolean) {
        isHalted = haltedState
        timers.setHaltCycleCounter()
    }

    /**
     * Getter for the isStopped flag
     *
     * @return true if CPU is stopped false otherwise
     */
    fun isStopped(): Boolean = isStopped

    /**
     * Sets the stopped state to the provided one
     *
     * @param stoppedState should be stopped or not (true if should false otherwise)
     */
    fun setStopped(stoppedState: Boolean) {
        isStopped = stoppedState
    }
}
