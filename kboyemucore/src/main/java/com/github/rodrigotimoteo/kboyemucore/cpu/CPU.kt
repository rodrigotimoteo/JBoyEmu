package com.github.rodrigotimoteo.kboyemucore.cpu

import com.github.rodrigotimoteo.kboyemucore.api.CpuState
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.instructions.Decoder
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.Interrupts
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.CPURegisters
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.RegisterNames
import com.github.rodrigotimoteo.kboyemucore.util.Logger

class CPU(
    private val bus: Bus,
    private val logger: Logger,
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
     * Executes the operation that is currently pointed by the program counter. When the halt bug is
     * active the opcode fetch does not increment PC. Since instructions read operands relative to
     * the current PC (e.g. calculateNN reads PC+1 and PC+2), we decrement PC by 1 before executing
     * so the operand reads start from the opcode byte itself, matching real hardware behavior.
     */
    private fun executeOperation() {
        val programCounter = cpuRegisters.getProgramCounter()

        if (interrupts.haltBug) {
            interrupts.disableHaltBug()
            cpuRegisters.setProgramCounter(programCounter - 1)
            decoder.decode(bus.getValueFromCPU(programCounter).toInt())
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

    /**
     * Captures the complete CPU state for save state serialization
     *
     * @return snapshot of registers, timers, interrupts, and execution flags
     */
    fun saveState(): CpuState = CpuState(
        a = cpuRegisters.getRegister(RegisterNames.A).value.toInt(),
        f = cpuRegisters.getRegister(RegisterNames.F).value.toInt(),
        b = cpuRegisters.getRegister(RegisterNames.B).value.toInt(),
        c = cpuRegisters.getRegister(RegisterNames.C).value.toInt(),
        d = cpuRegisters.getRegister(RegisterNames.D).value.toInt(),
        e = cpuRegisters.getRegister(RegisterNames.E).value.toInt(),
        h = cpuRegisters.getRegister(RegisterNames.H).value.toInt(),
        l = cpuRegisters.getRegister(RegisterNames.L).value.toInt(),
        programCounter = cpuRegisters.getProgramCounter(),
        stackPointer = cpuRegisters.getStackPointer(),
        halted = isHalted,
        stopped = isStopped,
    )

    /**
     * Restores the CPU from a previously captured save state
     *
     * @param s saved CPU state to restore
     */
    fun loadState(s: CpuState) {
        cpuRegisters.setRegister(RegisterNames.A, s.a.toUByte())
        cpuRegisters.setRegister(RegisterNames.F, (s.f and 0xF0).toUByte())
        cpuRegisters.setRegister(RegisterNames.B, s.b.toUByte())
        cpuRegisters.setRegister(RegisterNames.C, s.c.toUByte())
        cpuRegisters.setRegister(RegisterNames.D, s.d.toUByte())
        cpuRegisters.setRegister(RegisterNames.E, s.e.toUByte())
        cpuRegisters.setRegister(RegisterNames.H, s.h.toUByte())
        cpuRegisters.setRegister(RegisterNames.L, s.l.toUByte())
        cpuRegisters.setProgramCounter(s.programCounter)
        cpuRegisters.setStackPointer(s.stackPointer)
        isHalted = s.halted
        isStopped = s.stopped
    }
}
