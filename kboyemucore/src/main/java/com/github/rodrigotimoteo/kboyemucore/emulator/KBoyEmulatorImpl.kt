package com.github.rodrigotimoteo.kboyemucore.emulator

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import kotlinx.coroutines.flow.Flow

class KBoyEmulatorImpl: KBoyEmulator {

    private lateinit var bus: Bus

    override fun loadRom(rom: Rom) {

    }

    override fun reset() {
        bus.reset()
    }

    override fun press(button: Button) {
        TODO("Not yet implemented")
    }

    override fun release(button: Button) {
        TODO("Not yet implemented")
    }

    override fun run() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override val frames: Flow<FrameBuffer>
        get() = TODO("Not yet implemented")
}
