package com.github.rodrigotimoteo.kboyemucore.bus

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.controller.Controller
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
import com.github.rodrigotimoteo.kboyemucore.memory.CpuMemoryOperations
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryManager
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryModule
import com.github.rodrigotimoteo.kboyemucore.memory.PpuMemoryOperations
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU
import com.github.rodrigotimoteo.kboyemucore.ppu.PPUModes
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_LOWER_BITS
import com.github.rodrigotimoteo.kboyemucore.util.FILTER_TOP_BITS
import com.github.rodrigotimoteo.kboyemucore.util.FRAME_DURATION_MS_60FPS
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import com.github.rodrigotimoteo.kboyemucore.util.MutableUByte
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Suppress("TooManyFunctions")
@OptIn(ExperimentalUnsignedTypes::class)
class Bus(
    rom: MemoryModule,
    val isCGB: Boolean,
    private val logger: Logger,
) : CpuMemoryOperations, PpuMemoryOperations {

    private var _runningJob: Job? = null

    val runningJob: Job?
        get() = _runningJob

    /**
     * Memory Manager reference
     */
    private val memoryManager = MemoryManager(this, logger, rom)

    /**
     * CPU reference
     */
    private val cpu = CPU(this, logger)

    /**
     * PPU reference
     */
    private val ppu = PPU(this, logger)

    /**
     * Controller reference
     */
    private val controller = Controller(this, logger)

    /**
     * [StateFlow] of [FrameBuffer] for use in Emulator implementation
     */
    val frameBuffer = ppu.painting

    internal val ppuMode: PPUModes
        get() = ppu.ppuRegisters.mode

    /**
     * Starts the emulation by launching a new coroutine that ticks the CPU and PPU in the correct
     * order and timing
     */
    fun run() {
        if (_runningJob?.isActive == true) {
            logger.i("Emulator job is already active quitting run()")
            return
        }
        _runningJob = CoroutineScope(Dispatchers.Default).launch {
            logger.i("Starting emulator job")
            cpu.tick()
            ppu.tick()

            var lastRtcMs = System.currentTimeMillis()
            var frameStartMs = lastRtcMs

            while (true) {
                try {
                    val cpuCounter: Int = cpu.getCounter()
                    if (!ppu.lcdOn) {
                        cpu.tick()
                        ppu.checkLCDStatus()
                    } else {
                        cpu.tick()
                        repeat(cpu.getCounter() - cpuCounter) {
                            ppu.tick()
                        }
                    }

                    val now = System.currentTimeMillis()

                    if (now - lastRtcMs >= 1000) {
                        memoryManager.tickRtc()
                        lastRtcMs = now
                    }

                    // Sleep at the end of each VBlank to cap at 60fps
                    if (ppu.isVBlankStart()) {
                        val elapsed = System.currentTimeMillis() - frameStartMs
                        val sleepMs = FRAME_DURATION_MS_60FPS - elapsed
                        if (sleepMs > 0) delay(sleepMs)
                        frameStartMs = System.currentTimeMillis()
                    }
                } catch (e: InterruptedException) {
                    logger.e("Emulator job crashed, exiting", e)
                    exitProcess(-1)
                }
            }
        }
    }

    /**
     * Stops the [_runningJob] causing the emulation to stop entirely
     */
    fun stop() {
        logger.i("Emulator job is being stopped")
        _runningJob?.cancel()
    }

    /**
     * CPU-only write access that ticks timers
     */
    override fun setValueFromCPU(memoryAddress: Int, value: UByte) {
        cpu.timers.tick()
        memoryManager.setValue(memoryAddress, value)
    }

    /**
     * Changes value of specific word based on its memory address without limitation (because PPU
     * has unrestricted access to every memory address) "I Think" (for now only check bottom registers)
     *
     * @param memoryAddress where to change the value
     * @param value to assign
     */
    override fun setValueFromPPU(memoryAddress: Int, value: UByte) {
        memoryManager.setValueFromPPU(memoryAddress, value)
    }

    /**
     * Gets a reference to a mutable value of a specific word based on its memory address without
     * limitation (because PPU has unrestricted access to every memory address) "I Think" (for now
     * only check bottom registers)
     *
     * @param memoryAddress where to get the value
     *
     * @return reference to a mutable value of a specific word based on its memory address without
     * limitation (because PPU has unrestricted access to every memory address) "I Think" (for now
     * only check bottom registers)
     */
    fun getPermanentRegister(memoryAddress: Int): MutableUByte {
        return memoryManager.getPermanentRegister(memoryAddress)
    }

    /**
     * CPU-only read access that ticks timers
     */
    override fun getValueFromCPU(memoryAddress: Int): UByte {
        cpu.timers.tick()
        return memoryManager.getValue(memoryAddress)
    }

    /**
     * PPU-only read access that does not tick timers
     */
    override fun getValueFromPPU(memoryAddress: Int): UByte {
        return memoryManager.getValueFromPPU(memoryAddress)
    }

    /**
     * Get the value of the joypad register based on the provided joyPadRegister (used for
     * instructions that need to check the state of the joypad)
     *
     * @param joyPadRegister to check
     *
     * @return value of the joypad register based on the provided joyPadRegister
     */
    fun getJoypad(joyPadRegister: UByte) = controller.getJoypad(joyPadRegister)

    /**
     * Presses the provided button on the controller
     *
     * @param button to press
     */
    fun press(button: Button) = controller.buttonPressed(button)

    /**
     * Releases the provided button on the controller
     *
     * @param button to release
     */
    fun release(button: Button) = controller.buttonReleased(button)

    /**
     * Stores the program counter in the stack pointer and decreases its pointer by 2
     */
    fun storeProgramCounterInStackPointer() {
        val stackPointer = cpu.cpuRegisters.getStackPointer()
        val programCounter = cpu.cpuRegisters.getProgramCounter()

        setValueFromCPU(stackPointer - 1, ((programCounter and FILTER_TOP_BITS) shr 8).toUByte())
        setValueFromCPU(stackPointer - 2, (programCounter and FILTER_LOWER_BITS).toUByte())

        cpu.cpuRegisters.incrementStackPointer(-2)
    }

    /**
     * Calculates a new memory address to fetch memory for specific instructions uses a 16 bit word
     * produced by the sum of lower nibble of PC + 1 and higher nibble of PC + 2
     *
     * @return calculated address
     */
    fun calculateNN(): Int {
        val programCounter = cpu.cpuRegisters.getProgramCounter()

        val lowerAddress = getValueFromCPU(programCounter + 1).toInt()
        val upperAddress = getValueFromCPU(programCounter + 2).toInt() shl 8

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
