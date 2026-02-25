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
import com.github.rodrigotimoteo.kboyemucore.ppu.drawer.CGBPPUDrawer
import com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer
import com.github.rodrigotimoteo.kboyemucore.spu.SPU
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
     * SPU (Sound Processing Unit) reference
     */
    internal val spu = SPU(logger)

    /**
     * Audio ring buffer — read from the Android audio thread
     */
    val audioRingBuffer: AudioRingBuffer = spu.ringBuffer

    /**
     * [StateFlow] of [FrameBuffer] for use in Emulator implementation
     */
    val frameBuffer = ppu.painting

    internal val ppuMode: PPUModes
        get() = ppu.ppuRegisters.mode

    /** Returns the CGB PPU drawer if running in CGB mode, null otherwise */
    internal val cgbDrawer: CGBPPUDrawer?
        get() = ppu.ppuDrawer as? CGBPPUDrawer

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

            // ── Debug: detect hangs ──────────────────────────────────────────
            var debugLastLogMs = System.currentTimeMillis()
            var debugLastPC = -1
            var debugSamePcCount = 0
            var debugHaltTicks = 0L
            var debugStopTicks = 0L
            var debugInstrCount = 0L
            // ─────────────────────────────────────────────────────────────────

            while (true) {
                try {
                    val cpuCounter: Int = cpu.getCounter()
                    if (!ppu.lcdOn) {
                        cpu.tick()
                        ppu.checkLCDStatus()
                        spu.tick((cpu.getCounter() - cpuCounter) * 4)
                    } else {
                        cpu.tick()
                        val elapsed = cpu.getCounter() - cpuCounter
                        repeat(elapsed) {
                            ppu.tick()
                        }
                        spu.tick(elapsed * 4)
                    }

                    // ── Debug tracking ───────────────────────────────────────
                    debugInstrCount++
                    if (cpu.isHalted()) debugHaltTicks++
                    if (cpu.isStopped()) debugStopTicks++

                    val currentPC = cpu.cpuRegisters.getProgramCounter()
                    if (currentPC == debugLastPC) {
                        debugSamePcCount++
                    } else {
                        debugSamePcCount = 0
                        debugLastPC = currentPC
                    }

                    val debugNow = System.currentTimeMillis()
                    if (debugNow - debugLastLogMs >= 2000) {
                        val ie = memoryManager.getValue(0xFFFF).toInt()
                        val iff = memoryManager.getValue(0xFF0F).toInt()
                        val lcdc = memoryManager.getValue(0xFF40).toInt()
                        val stat = memoryManager.getValue(0xFF41).toInt()
                        val ly = memoryManager.getValue(0xFF44).toInt()
                        val opcode = memoryManager.getValue(currentPC).toInt()

                        logger.d(
                            "DBG: PC=%04X op=%02X halted=%b stopped=%b IME=%b IE=%02X IF=%02X LCDC=%02X STAT=%02X LY=%d samePc=%d haltT=%d stopT=%d instr=%d".format(
                                currentPC, opcode,
                                cpu.isHalted(), cpu.isStopped(),
                                cpu.interrupts.isImeEnabled,
                                ie, iff, lcdc, stat, ly,
                                debugSamePcCount, debugHaltTicks, debugStopTicks, debugInstrCount
                            )
                        )
                        debugHaltTicks = 0
                        debugStopTicks = 0
                        debugInstrCount = 0
                        debugLastLogMs = debugNow
                    }

                    if (debugSamePcCount > 500_000) {
                        val ie = memoryManager.getValue(0xFFFF).toInt()
                        val iff = memoryManager.getValue(0xFF0F).toInt()
                        val opcode = memoryManager.getValue(currentPC).toInt()
                        logger.e(
                            "HANG DETECTED: PC=%04X op=%02X halted=%b stopped=%b IME=%b IE=%02X IF=%02X".format(
                                currentPC, opcode,
                                cpu.isHalted(), cpu.isStopped(),
                                cpu.interrupts.isImeEnabled,
                                ie, iff
                            ),
                            null
                        )
                        debugSamePcCount = 0
                    }
                    // ─────────────────────────────────────────────────────────

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
     * PPU-only read from VRAM bank 1, used by the CGB drawer to fetch tile map attributes.
     * On DMG this always returns 0.
     *
     * @param memoryAddress VRAM address to read from bank 1
     * @return value stored in VRAM bank 1 at the given address
     */
    fun getValueFromPPUBank1(memoryAddress: Int): UByte {
        return memoryManager.getValueFromPPUBank1(memoryAddress)
    }

    /**
     * PPU-only read from a specific VRAM bank, used by the CGB drawer to fetch tile data
     * from the bank specified in the tile attribute byte.
     *
     * @param memoryAddress VRAM address to read
     * @param bank VRAM bank number (0 or 1)
     * @return value stored in the specified VRAM bank at the given address
     */
    fun getValueFromPPUBank(memoryAddress: Int, bank: Int): UByte {
        return memoryManager.getValueFromPPUBank(memoryAddress, bank)
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
     * Performs a CGB speed switch. Called by the STOP instruction when KEY1 bit 0 is set.
     * Toggles KEY1 bit 7 (current speed indicator) and clears bit 0 (switch request).
     * Does nothing if bit 0 is not set.
     */
    fun performSpeedSwitch() {
        memoryManager.performSpeedSwitch()
    }

    /**
     * Called by the PPU at the start of each HBlank to tick an active CGB HBlank HDMA transfer.
     */
    fun tickHdma() {
        memoryManager.tickHdma()
    }

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
    // Debug: track first bad RST 38h
    @Volatile private var rstTrapFired = false

    fun calculateNN(): Int {
        val programCounter = cpu.cpuRegisters.getProgramCounter()

        val lowerAddress = getValueFromCPU(programCounter + 1).toInt()
        val upperAddress = getValueFromCPU(programCounter + 2).toInt() shl 8

        val result = lowerAddress + upperAddress

        // Debug: log when JP at an interrupt vector goes to unexpected address
        if (programCounter in intArrayOf(0x0040, 0x0048, 0x0050, 0x0058, 0x0060)) {
            // Also read raw bytes from memory (no timer tick) for comparison
            val raw0 = memoryManager.getValue(programCounter).toInt()
            val raw1 = memoryManager.getValue(programCounter + 1).toInt()
            val raw2 = memoryManager.getValue(programCounter + 2).toInt()
            val rawTarget = raw1 + (raw2 shl 8)
            if (result != rawTarget || result >= 0x8000) {
                println("BAD JP at vec %04X → %04X (lo=%02X hi=%02X) raw=[%02X %02X %02X]→%04X romBank=%d".format(
                    programCounter, result, lowerAddress, upperAddress shr 8,
                    raw0, raw1, raw2, rawTarget,
                    memoryManager.romActiveBank()
                ))
            }
        }

        return result
    }

    /**
     * Called from CPU when executing an opcode that is 0xFF (RST 38h) at an unexpected address.
     * Logs once to help debug the hang.
     */
    fun trapRst38(pc: Int) {
        if (!rstTrapFired && pc != 0x0038) {
            rstTrapFired = true
            val sp = cpu.cpuRegisters.getStackPointer()
            val ie = memoryManager.getValue(0xFFFF).toInt()
            val iff = memoryManager.getValue(0xFF0F).toInt()
            println("RST38 TRAP: PC=%04X SP=%04X IE=%02X IF=%02X romBank=%d wramBank=%d".format(
                pc, sp, ie, iff,
                memoryManager.romActiveBank(),
                memoryManager.wramActiveBank()
            ))
        }
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
