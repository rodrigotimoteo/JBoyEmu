package com.github.rodrigotimoteo.kboyemucore.emulator

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomReader
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/**
 * Default implementation of [KBoyEmulator] that holds reference to the [Bus] and delegates all the
 * logic to it, while also providing a way to read ROM files and generate the appropriate memory
 * modules for the [Bus] to use
 *
 * @author rodrigotimoteo
 */
class KBoyEmulatorImpl(
    logger: Logger,
): KBoyEmulator {

    /**
     * Holds reference to the Bus (basically carries all the logic of the Game Boy).
     * Can be created at will in order to generate new instances of this emulator (resetting)
     */
    private var bus: Bus? = null

    /**
     * Used to read file and provide the appropriate memory modules from them
     */
    private val romReader = RomReader(logger)

    /**
     * Stores the flow [FrameBuffer] for a consumer to draw
     */
    private var _frames: Flow<FrameBuffer>? = null

    override fun loadRom(rom: Rom) {
        romReader.loadRom(rom)
        bus = Bus(romReader.getRomModule(), romReader.isCgb())
        _frames = bus?.frameBuffer
    }

    override fun reset() {
        bus = Bus(romReader.getRomModule(), romReader.isCgb())
        _frames = bus?.frameBuffer
    }

    override fun press(button: Button) {
        bus?.press(button)
    }

    override fun release(button: Button) {
        bus?.release(button)
    }

    override fun run() {
        bus?.run()
    }

    override fun job(): Job? = bus?.runningJob

    override fun pause() {
        bus?.stop()
    }

    override val frames: Flow<FrameBuffer>
        get() = _frames ?: error("Emulator has not been initialized first pass a valid ROM")
}
