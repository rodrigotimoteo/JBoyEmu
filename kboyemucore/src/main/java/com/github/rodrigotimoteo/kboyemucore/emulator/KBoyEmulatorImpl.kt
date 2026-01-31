package com.github.rodrigotimoteo.kboyemucore.emulator

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import kotlinx.coroutines.flow.Flow

class KBoyEmulatorImpl: KBoyEmulator {
    override fun loadRom(rom: Rom) {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
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
