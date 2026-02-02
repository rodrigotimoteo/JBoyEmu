package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.RegisterNames
import com.github.rodrigotimoteo.kboyemucore.ktx.resetBit
import com.github.rodrigotimoteo.kboyemucore.ktx.setBit
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit

/**
 * Class responsible for handling all single bit operation in the Game Boy [CPU]
 *
 * @param cpu used for cpu communication
 * @param bus used for communication with other parts in this case accessing memory
 *
 * @author rodrigotimoteo
 */
class SingleBit(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * Test given bit of a specific register given
     *
     * @param bit to test
     * @param register to test
     */
    fun bit(bit: Int, register: RegisterNames) {
        val givenRegister = cpu.cpuRegisters.getRegister(register)
        val testResult = givenRegister.value.testBit(bit)

        cpu.cpuRegisters.flags.setFlags(
            zero = !testResult,
            subtract = false,
            half = true,
            carry = null
        )

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Test given bit of a specific memory address contained in (HL)
     *
     * @param bit to test
     * @param memoryAddress retrieve memory to test bit
     */
    fun bitHL(bit: Int, memoryAddress: Int) {
        cpu.timers.tick()

        val testResult = bus.getValue(memoryAddress).testBit(bit)

        cpu.cpuRegisters.flags.setFlags(
            zero = !testResult,
            subtract = false,
            half = true,
            carry = null
        )


        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Sets a bit on the given cpu register
     *
     * @param bit to set
     * @param register to change the given bit
     */
    fun set(bit: Int, register: RegisterNames) {
        val givenRegister = cpu.cpuRegisters.getRegister(register)
        givenRegister.value = givenRegister.value.setBit(bit)

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Sets a bit of a specific memory address contained in (HL)
     *
     * @param bit to set
     * @param memoryAddress retrieve memory to test bit
     */
    fun setHL(bit: Int, memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        bus.setValue(memoryAddress, bus.getValue(memoryAddress).setBit(bit))

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Resets a bit on the given cpu register
     *
     * @param bit to set
     * @param register to change the given bit
     */
    fun res(bit: Int, register: RegisterNames) {
        val givenRegister = cpu.cpuRegisters.getRegister(register)
        givenRegister.value = givenRegister.value.resetBit(bit)

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Resets a bit of a specific memory address contained in (HL)
     *
     * @param bit to set
     * @param memoryAddress retrieve memory to test bit
     */
    fun resHL(bit: Int, memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        bus.setValue(memoryAddress, bus.getValue(memoryAddress).resetBit(bit))

        cpu.cpuRegisters.incrementProgramCounter(1)
    }
}
