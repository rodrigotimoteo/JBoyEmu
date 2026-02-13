package com.github.rodrigotimoteo.kboyemucore.controller

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge.MBC0
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalUnsignedTypes::class)
class ControllerTest {

    /** Bus reference for testing */
    private lateinit var bus: Bus

    @BeforeEach
    fun setUp() {
        val romContent = UByteArray(0x8000)
        val rom = MBC0(romBanks = 2, ramBanks = 0, romContent = romContent)
        bus = Bus(rom = rom, isCGB = false)
    }

    @Test
    fun `when no joypad group is selected then joypad returns zero`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x30u)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x00u.toUByte(), value)
    }

    @Test
    fun `when direction group is selected and no input then joypad returns all released`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x20u)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x0Fu.toUByte(), value)
    }

    @Test
    fun `when right is pressed and direction group selected then joypad returns right cleared`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x20u)
        bus.press(Button.RIGHT)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x0Eu.toUByte(), value)
    }

    @Test
    fun `when button group is selected and a is pressed then joypad returns a cleared`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x10u)
        bus.press(Button.A)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x0Eu.toUByte(), value)
    }

    @Test
    fun `when a is released then joypad returns a set again`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x10u)
        bus.press(Button.A)
        bus.release(Button.A)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x0Fu.toUByte(), value)
    }

    @Test
    fun `when select and start are pressed then joypad returns both cleared`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x10u)
        bus.press(Button.SELECT)
        bus.press(Button.START)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x03u.toUByte(), value)
    }

    @Test
    fun `when a button is pressed then joypad interrupt bit is set`() {
        val before = bus.getValue(ReservedAddresses.IF.memoryAddress).toInt()

        bus.press(Button.A)

        val after = bus.getValue(ReservedAddresses.IF.memoryAddress).toInt()
        val joypadMask = 1 shl InterruptNames.JOYPAD_INT.testBit
        assertTrue((before and joypadMask) == 0)
        assertTrue((after and joypadMask) != 0)
    }

    @Test
    fun `when button group is selected then direction presses do not change the readout`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x10u)
        bus.press(Button.RIGHT)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x0Fu.toUByte(), value)
    }

    @Test
    fun `when direction group is selected then button presses do not change the readout`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x20u)
        bus.press(Button.A)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x0Fu.toUByte(), value)
    }

    @Test
    fun `when both groups are selected then direction group has priority`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x00u)
        bus.press(Button.RIGHT)
        bus.press(Button.A)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x0Eu.toUByte(), value)
    }

    @Test
    fun `when no joypad group is selected and a button is pressed then joypad returns zero`() {
        bus.setValue(ReservedAddresses.JOYP.memoryAddress, 0x30u)
        bus.press(Button.A)

        val value = bus.getValue(ReservedAddresses.JOYP.memoryAddress)

        assertEquals(0x00u.toUByte(), value)
    }
}
