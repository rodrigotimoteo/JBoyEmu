package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge.MBC0
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalUnsignedTypes::class)
class ControlTest {

    /** CPU reference for testing */
    private lateinit var cpu: CPU

    /** Control reference for testing */
    private lateinit var control: Control

    /** Bus reference for testing */
    private lateinit var bus: Bus

    @BeforeEach
    fun setUp() {
        val romContent = UByteArray(0x8000)
        val rom = MBC0(romBanks = 2, ramBanks = 0, romContent = romContent)
        bus = Bus(rom = rom, isCGB = false)
        cpu = CPU(bus)
        control = Control(cpu, bus)
    }

    @Test
    fun `when Nop is executed then the programCounter increments`() {
        cpu.cpuRegisters.setProgramCounter(0xC000)

        control.nop()

        assertEquals(0xC001, cpu.cpuRegisters.getProgramCounter())
    }

    @Test
    fun `when Ccf is executed then carry toggles and half and subtract clear`() {
        cpu.cpuRegisters.flags.setFlags(zero = true, subtract = true, half = true, carry = true)
        cpu.cpuRegisters.setProgramCounter(0xC000)

        control.ccf()

        assertTrue(cpu.cpuRegisters.flags.getZeroFlag())
        assertFalse(cpu.cpuRegisters.flags.getSubtractFlag())
        assertFalse(cpu.cpuRegisters.flags.getHalfCarryFlag())
        assertFalse(cpu.cpuRegisters.flags.getCarryFlag())
        assertEquals(0xC001, cpu.cpuRegisters.getProgramCounter())
    }

    @Test
    fun `when Ccf is executed with carry clear then carry becomes set`() {
        cpu.cpuRegisters.flags.setFlags(zero = false, subtract = null, half = null, carry = false)

        control.ccf()

        assertTrue(cpu.cpuRegisters.flags.getCarryFlag())
    }

    @Test
    fun `when Ccf is executed then zero flag is preserved when clear`() {
        cpu.cpuRegisters.flags.setFlags(zero = false, subtract = null, half = null, carry = true)

        control.ccf()

        assertFalse(cpu.cpuRegisters.flags.getZeroFlag())
    }

    @Test
    fun `when Scf is executed then carry sets and half and subtract clear`() {
        cpu.cpuRegisters.flags.setFlags(zero = false, subtract = true, half = true, carry = false)
        cpu.cpuRegisters.setProgramCounter(0xC000)

        control.scf()

        assertFalse(cpu.cpuRegisters.flags.getSubtractFlag())
        assertFalse(cpu.cpuRegisters.flags.getHalfCarryFlag())
        assertTrue(cpu.cpuRegisters.flags.getCarryFlag())
        assertEquals(0xC001, cpu.cpuRegisters.getProgramCounter())
    }

    @Test
    fun `when Scf is executed then zero flag is preserved`() {
        cpu.cpuRegisters.flags.setFlags(zero = true, subtract = null, half = null, carry = false)

        control.scf()

        assertTrue(cpu.cpuRegisters.flags.getZeroFlag())
    }

    @Test
    fun `when Scf is executed then carry remains set`() {
        cpu.cpuRegisters.flags.setFlags(zero = null, subtract = null, half = null, carry = true)

        control.scf()

        assertTrue(cpu.cpuRegisters.flags.getCarryFlag())
    }

    @Test
    fun `when Halt is executed then cpu is halted and program counter increments`() {
        cpu.cpuRegisters.setProgramCounter(0xC000)

        control.halt()

        assertTrue(cpu.isHalted())
        assertEquals(0xC001, cpu.cpuRegisters.getProgramCounter())
    }

    @Test
    fun `when Stop is executed then cpu stops and program counter increments`() {
        cpu.cpuRegisters.setProgramCounter(0xC000)
        bus.setValueFromPPU(ReservedAddresses.DIV.memoryAddress, 0x12u)

        control.stop()

        assertTrue(cpu.isStopped())
        assertEquals(0xC001, cpu.cpuRegisters.getProgramCounter())
    }

    @Test
    fun `when Di is executed then interrupt disable is requested and change cycle marked`() {
        cpu.timers.tick()
        val cycle = cpu.timers.machineCycles

        control.di()

        assertTrue(cpu.interrupts.requestedInterruptChange())
        assertEquals(cycle, cpu.timers.interruptChangedCounter)
        assertEquals(0x0101, cpu.cpuRegisters.getProgramCounter())
    }

    @Test
    fun `when Ei is executed then interrupt enable is requested and change cycle marked`() {
        cpu.timers.tick()
        val cycle = cpu.timers.machineCycles

        control.ei()

        assertTrue(cpu.interrupts.requestedInterruptChange())
        assertEquals(cycle, cpu.timers.interruptChangedCounter)
        assertEquals(0x0101, cpu.cpuRegisters.getProgramCounter())
    }
}
