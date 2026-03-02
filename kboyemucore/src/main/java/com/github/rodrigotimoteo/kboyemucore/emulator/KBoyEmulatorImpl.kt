package com.github.rodrigotimoteo.kboyemucore.emulator

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.api.SaveState
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomReader
import com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer
import com.github.rodrigotimoteo.kboyemucore.util.ACCESSING_FRAME_BEFORE_READY
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
    private val logger: Logger,
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

    /**
     * Stores the [AudioRingBuffer] for a consumer to play audio
     */
    private var _audioRingBuffer: AudioRingBuffer? = null

    override fun loadRom(rom: Rom) {
        romReader.loadRom(rom)
        bus = Bus(romReader.getRomModule(), romReader.isCgb(), logger)
        _frames = bus?.frameBuffer
        _audioRingBuffer = bus?.audioRingBuffer
    }

    override fun reset() {
        bus = Bus(romReader.getRomModule(), romReader.isCgb(), logger)
        _frames = bus?.frameBuffer
        _audioRingBuffer = bus?.audioRingBuffer
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

    override fun saveState(): SaveState? = bus?.saveState()

    override fun loadState(state: SaveState) {
        bus?.loadState(state)
    }

    override val frames: Flow<FrameBuffer>
        get() = _frames ?: error(ACCESSING_FRAME_BEFORE_READY)

    override val audioRingBuffer: AudioRingBuffer
        get() = _audioRingBuffer ?: error(ACCESSING_FRAME_BEFORE_READY)
}
