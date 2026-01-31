package com.github.rodrigotimoteo.kboyemucore.cpu

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.Registers

class CPU(
    private val bus: Bus
) {

    internal val registers = Registers(bus)

    internal val timers = Timers()
//
//    internal val interrupts = Interrupts()
//
//    internal val decoder = Decoder(bus)
}
