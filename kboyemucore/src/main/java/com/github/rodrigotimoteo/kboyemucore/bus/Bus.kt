package com.github.rodrigotimoteo.kboyemucore.bus

import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryManager
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryManipulation
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryModule
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_LOWER_BITS
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_TOP_BITS

class Bus(
    rom: MemoryModule,
    private val isCGB: Boolean
) : MemoryManipulation {

    /**
     * Memory Manager reference
     */
    private val memoryManager = MemoryManager(this, rom)

    /**
     * CPU reference
     */
    private val cpu = CPU(this)

    /**
     * Returns whether or not the current rom is from Color Game Boy or not
     */
    fun isCGB() = isCGB

    /**
     * Changes value of specific word based on its memory address
     *
     * @param memoryAddress where to change the value
     * @param value to assign
     */
    override fun setValue(memoryAddress: Int, value: UByte) {
        memoryManager.setValue(memoryAddress, value)
    }

    /**
     * Gets the value of specific word based on its address
     *
     * @param memoryAddress where to get the value
     * @return value stored in specific address
     */
    override fun getValue(memoryAddress: Int): UByte {
        return memoryManager.getValue(memoryAddress)
    }

    /**
     * Stores the program counter in the stack pointer and decreases its pointer by 2
     */
    fun storeProgramCounterInStackPointer() {
        val stackPointer = cpu.registers.getStackPointer()
        val programCounter = cpu.registers.getProgramCounter()

        setValue(stackPointer - 1, (programCounter and FILTER_TOP_BITS) shr 8)
        setValue(stackPointer - 2, programCounter and FILTER_LOWER_BITS)

        cpu.registers.incrementStackPointer(-2)
    }

    /**
     * Executes an action on the CPU of the Game Boy based on predifined options
     *
     * @param action which action to perform
     * @param parameters to use
     */
    @Suppress("CyclomaticComplexMethod")
    fun executeFromCPU(action: BusConstants, parameters: Any) {
        when (action) {
            BusConstants.TICK_TIMERS -> cpu.timers.tick()
            BusConstants.SET_REGISTER -> (parameters as Array<*>).let {
                cpu.registers.setRegister(it[0] as RegisterNames, it[1] as Int)
            }
            BusConstants.INCR_PC -> cpu.registers.incrementProgramCounter(parameters as Int)
            BusConstants.SET_PC -> cpu.registers.setProgramCounter(parameters as Int)
            BusConstants.INCR_SP -> cpu.registers.incrementStackPointer(parameters as Int)
            BusConstants.SET_SP -> cpu.registers.setStackPointer(parameters as Int)
            BusConstants.SET_AF -> cpu.registers.setAF(parameters as Int)
            BusConstants.SET_BC -> cpu.registers.setBC(parameters as Int)
            BusConstants.SET_DE -> cpu.registers.setDE(parameters as Int)
            BusConstants.SET_HL -> cpu.registers.setHL(parameters as Int)
            BusConstants.DISABLE_INT -> cpu.interrupts.setInterruptChange(false)
            BusConstants.ENABLE_INT -> cpu.interrupts.setInterruptChange(true)
            BusConstants.REQUEST_INT -> cpu.interrupts.requestInterrupt(parameters as Int)
            BusConstants.HALT -> cpu.setHalted(true)
            BusConstants.UNHALT -> cpu.setHalted(false)
            BusConstants.STOP -> cpu.setStopped(true)
            else -> Logger.getGlobal().log(Level.SEVERE, "Executing invalid action! Needs fix!")
        }
    }

    /**
     * Gets values from the CPU from any part of the architecure
     *
     * @param action which value to get
     * @param parameters to use
     */
    fun getFromCPU(action: BusConstants, parameters: Any): Any {
        return when (action) {
            BusConstants.GET_FLAGS -> cpu.registers.flags
            BusConstants.GET_REGISTER -> cpu.registers.getRegister(parameters as RegisterNames)
            BusConstants.GET_AF -> cpu.registers.getAF()
            BusConstants.GET_BC -> cpu.registers.getBC()
            BusConstants.GET_DE -> cpu.registers.getDE()
            BusConstants.GET_HL -> cpu.registers.getHL()
            BusConstants.GET_PC -> cpu.registers.getProgramCounter()
            BusConstants.GET_SP -> cpu.registers.getStackPointer()
            BusConstants.GET_HALTED -> cpu.isHalted()
            BusConstants.GET_STOPPED -> cpu.isStopped()
            BusConstants.GET_MC -> cpu.timers.getMachineCycles()
            BusConstants.GET_HALT_MC -> cpu.timers.getHaltCycleCounter()
            else -> error("Unexpected value! $action")
        }
    }

    /**
     * Calculates a new memory address to fetch memory for specific instructions uses a 16 bit word
     * produced by the sum of lower nibble of PC + 1 and higher nibble of PC + 2
     *
     * @return calculated address
     */
    fun calculateNN(): Int {
        repeat(2) { cpu.timers.tick() }

        val programCounter = cpu.registers.getProgramCounter()

        val lowerAddress = getValue(programCounter + 1).toInt()
        val upperAddress = getValue(programCounter + 2).toInt() shl 8

        return lowerAddress + upperAddress
    }
}
