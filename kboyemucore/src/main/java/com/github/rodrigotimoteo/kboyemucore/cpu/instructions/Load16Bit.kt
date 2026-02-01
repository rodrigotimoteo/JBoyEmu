package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU

class Load16Bit(
    private val cpu: CPU,
    private val bus: Bus
) {

    fun ld16bit(type: Int) {
        repeat(2) { cpu.timers.tick() }

        val value = bus.calculateNN()

        when (type) {
            0 -> cpu.registers.setBC(value)
            1 -> cpu.registers.setDE(value)
            2 -> cpu.registers.setHL(value)
        }

        cpu.registers.incrementProgramCounter(3)
    }
}