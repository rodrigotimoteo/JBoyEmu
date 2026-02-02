package com.github.rodrigotimoteo.kboyemucore.emulator

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomReader
import kotlinx.coroutines.flow.Flow

class KBoyEmulatorImpl: KBoyEmulator {

    /**
     * Holds reference to the Bus (basically carries all the logic of the Game Boy).
     * Can be created at will in order to generate new instances of this emulator (resetting)
     */
    private var bus: Bus? = null

    /**
     * Used to read file and provide the appropriate memory modules from them
     */
    private val romReader = RomReader()

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
        TODO("Not yet implemented")
    }

    override fun release(button: Button) {
        TODO("Not yet implemented")
    }

    override fun run() {
        bus?.run()
    }

    override fun pause() {
        bus?.stop()
    }

    override val frames: Flow<FrameBuffer>
        get() = _frames ?: error("Emulator has not been initialized first pass a valid ROM")
}
