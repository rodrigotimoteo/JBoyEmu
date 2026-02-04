package com.github.rodrigotimoteo.kboyemucore.bus

import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryManager
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryManipulation
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryModule
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_LOWER_BITS
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_TOP_BITS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class Bus(
    rom: MemoryModule,
    private val isCGB: Boolean
) : MemoryManipulation {

    private var _runningJob: Job? = null

    val runningJob: Job?
        get() = _runningJob

    /**
     * Memory Manager reference
     */
    private val memoryManager = MemoryManager(this, rom)

    /**
     * CPU reference
     */
    private val cpu = CPU(this)

    /**
     * PPU reference
     */
    private val ppu = PPU(this)

    /**
     * [StateFlow] of [FrameBuffer] for use in Emulator implementation
     */
    val frameBuffer = ppu.painting

    fun run() {
        if (_runningJob?.isActive == true) {
            return
        }
        _runningJob = CoroutineScope(Dispatchers.Default).launch {
            cpu.tick()
            ppu.tick()
//        oldTime = System.nanoTime()
            while (true) {
                try {
                    val cpuCounter: Int = cpu.getCounter()
//                    if (ppu.isGetToSleep()) Thread.sleep(getSleepTime(oldTime));
                    if (!ppu.isLCDOn()) {
                        cpu.tick()
                        ppu.checkLCDStatus()
                    } else {
                        cpu.tick()
                        for (i in 0 until (cpu.getCounter() - cpuCounter)) {
                            ppu.tick()
                        }
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    exitProcess(-1)
                }
            }
        }
    }

    /**
     * Stops the [_runningJob] causing the emulation to stop entirely
     */
    fun stop() {
        _runningJob?.cancel()
    }

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
     * Changes value of specific word based on its memory address without limitation (because PPU
     * has unrestricted access to every memory address) "I Think" (for now only check bottom registers)
     *
     * @param memoryAddress where to change the value
     * @param value to assign
     */
    fun setValueFromPPU(memoryAddress: Int, value: UByte) {
        memoryManager.setValueFromPPU(memoryAddress, value)
    }

    /**
     * Sets the value on the DIV register
     *
     * @param value to set
     */
    fun setDIV(value: UByte) {
        memoryManager.setDiv(value)
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
        val stackPointer = cpu.cpuRegisters.getStackPointer()
        val programCounter = cpu.cpuRegisters.getProgramCounter()

        setValue(stackPointer - 1, ((programCounter and FILTER_TOP_BITS) shr 8).toUByte())
        setValue(stackPointer - 2, (programCounter and FILTER_LOWER_BITS).toUByte())

        cpu.cpuRegisters.incrementStackPointer(-2)
    }

    /**
     * Calculates a new memory address to fetch memory for specific instructions uses a 16 bit word
     * produced by the sum of lower nibble of PC + 1 and higher nibble of PC + 2
     *
     * @return calculated address
     */
    fun calculateNN(): Int {
        repeat(2) { cpu.timers.tick() }

        val programCounter = cpu.cpuRegisters.getProgramCounter()

        val lowerAddress = getValue(programCounter + 1).toInt()
        val upperAddress = getValue(programCounter + 2).toInt() shl 8

        return lowerAddress + upperAddress
    }

    /**
     * Sets a specific bit in Interrupts based on the provided interrupt
     *
     * @param interrupt which type of interrupt to request
     */
    fun triggerInterrupt(interrupt: InterruptNames) {
        cpu.interrupts.requestInterrupt(interrupt.testBit)
    }
}
