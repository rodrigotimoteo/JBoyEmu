package com.github.rodrigotimoteo.kboyemucore.memory

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.Timers
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomModule

/**
 * Class responsible for managing everything interacting directly with the
 * emulator memory space, keeps all other modules and manages memoryAddresses
 *
 *   0x0000 to 0x4000 - 16kb ROM Bank #0
 *   0x4000 to 0x8000 - 16kb switchable ROM Bank
 *   0x8000 to 0xA000 - 8kb Video RAM
 *   0xA000 to 0xC000 - 8kb switchable RAM Bank
 *   0xC000 to 0xE000 - 8kb Internal RAM
 *   0xE000 to 0xFE00 - Echo of 8kb Internal RAM
 *   0xFE00 to 0xFEA0 - Sprite Attrib Memory (OAM)
 *   0xFEA0 to 0xFF00 - Empty but unusable for I/O
 *   0xFF00 to 0xFF4C - I/O Ports
 *   0xFF4C to 0xFF80 - Empty but unusable for I/O
 *   0xFF80 to 0xFFFF - Internal RAM
 *   0xFFFF           - Interrupt Enable Register
 *
 * @author rodrigotimoteo
 */

class MemoryManager(
    private val bus: Bus,
    private val rom: MemoryModule
) : MemoryManipulation {

    /**
     * Whether or not the current rom is Color Game Boy or not
     */
    private val isCGB: Boolean = bus.isCGB()

    /**
     * Reference to the VRAM [MemoryModule]
     */
    private val vram: MemoryModule = if (isCGB) {
        MemoryModule(0x2000, 1, 0x8000, 2)
    } else {
        MemoryModule(0x2000, 0x8000)
    }

    /**
     * Reference to the ERAM [MemoryModule]
     */
    private val eram: MemoryModule? = when (val ramBanks = (rom as RomModule).ramBanks) {
        0 -> null
        else -> MemoryModule(0x2000, 1, 0xA000, ramBanks)
    }

    /**
     * Reference to the WRAM [MemoryModule]
     */
    private val wram: MemoryModule = if (isCGB) {
        MemoryModule(0x1000, 2, 0xC000, 8)
    } else {
        MemoryModule(0x1000, 2, 0xC000, 2)
    }

    /**
     * Reference to the OAM [MemoryModule]
     */
    private val oam: MemoryModule = MemoryModule(0xA0, ReservedAddresses.OAM_START.memoryAddress)

    /**
     * Reference to the BottomRegisters [MemoryModule]
     */
    private val bottomRegisters: MemoryModule =
        MemoryModule(0x100, ReservedAddresses.JOYP.memoryAddress)

    /**
     * Responsible for initializing the memory with the default values assign by the boot rom
     * therefore skipping its necessity
     */
    init {
        bottomRegisters.setValue(ReservedAddresses.NR10.memoryAddress, 0x80u)
        bottomRegisters.setValue(ReservedAddresses.NR11.memoryAddress, 0xBFu)
        bottomRegisters.setValue(ReservedAddresses.NR12.memoryAddress, 0xF3u)
        bottomRegisters.setValue(ReservedAddresses.NR14.memoryAddress, 0xBFu)
        bottomRegisters.setValue(ReservedAddresses.NR21.memoryAddress, 0x3Fu)
        bottomRegisters.setValue(ReservedAddresses.NR24.memoryAddress, 0xBFu)
        bottomRegisters.setValue(ReservedAddresses.NR30.memoryAddress, 0x7Fu)
        bottomRegisters.setValue(ReservedAddresses.NR31.memoryAddress, 0xFFu)
        bottomRegisters.setValue(ReservedAddresses.NR32.memoryAddress, 0x9Fu)
        bottomRegisters.setValue(ReservedAddresses.NR34.memoryAddress, 0xBFu)
        bottomRegisters.setValue(ReservedAddresses.NR41.memoryAddress, 0xFFu)
        bottomRegisters.setValue(ReservedAddresses.NR44.memoryAddress, 0xBFu)
        bottomRegisters.setValue(ReservedAddresses.NR50.memoryAddress, 0x77u)
        bottomRegisters.setValue(ReservedAddresses.NR51.memoryAddress, 0xF3u)
        bottomRegisters.setValue(ReservedAddresses.NR52.memoryAddress, 0xF1u)
        bottomRegisters.setValue(ReservedAddresses.LCDC.memoryAddress, 0x91u)
        bottomRegisters.setValue(ReservedAddresses.STAT.memoryAddress, 0x80u)
        bottomRegisters.setValue(ReservedAddresses.BGP.memoryAddress, 0xFCu)
        bottomRegisters.setValue(ReservedAddresses.OBP0.memoryAddress, 0xFFu)
        bottomRegisters.setValue(ReservedAddresses.OBP1.memoryAddress, 0xFFu)
    }

    override fun setValue(memoryAddress: Int, value: UByte) = when (memoryAddress) {
        in 0 until ReservedAddresses.SWITCH_ROM_END.memoryAddress -> {
            rom.setValue(memoryAddress, value).also {
                eram?.let { eram.activeBank = (rom as RomModule).ramBankNumber }
            }
        }

        in ReservedAddresses.SWITCH_ROM_END.memoryAddress until ReservedAddresses.VRAM_END.memoryAddress -> {
            vram.setValue(memoryAddress, value)
        }

        in ReservedAddresses.VRAM_END.memoryAddress until ReservedAddresses.ERAM_END.memoryAddress -> {
            eram?.let {
                if ((rom as RomModule).ramStatus) eram.setValue(memoryAddress, value)
            } ?: Unit
        }

        in ReservedAddresses.ERAM_END.memoryAddress until ReservedAddresses.WRAM_END.memoryAddress -> {
            wram.setValue(memoryAddress, value)
        }

        in ReservedAddresses.WRAM_END.memoryAddress until ReservedAddresses.OAM_START.memoryAddress -> {
            // Nothing should be done here its unusable
        }

        in ReservedAddresses.OAM_START.memoryAddress until ReservedAddresses.OAM_END.memoryAddress -> {
            oam.setValue(memoryAddress, value)
        }

        in ReservedAddresses.OAM_END.memoryAddress until ReservedAddresses.JOYP.memoryAddress -> {
            // Nothing should be done here its unusable
        }

        else -> {
            setBottomRegisters(memoryAddress, value)
        }
    }

    /**
     * This is a special method designed to be used by the PPU only and it provides free write access
     * to each and every memory location currently only bottomRegisters suffer this limitation
     *
     * @param memoryAddress memory location where to set value
     * @param value value to put inside address
     */
    fun setValueFromPPU(memoryAddress: Int, value: UByte) = when (memoryAddress) {
        in ReservedAddresses.JOYP.memoryAddress until ReservedAddresses.IE.memoryAddress -> {
            bottomRegisters.setValue(memoryAddress, value)
        }

        else -> {
            /** Nothing needs to be done */
        }
    }

    /**
     * This method is responsible for handling the assignment of new values to the bottom registers, according to all
     * their quirks (these registers have special conditions that must be respected)
     *
     * @param memoryAddress memory location where value should be written
     * @param value content that needs to be written to given address
     */
    private fun setBottomRegisters(memoryAddress: Int, value: UByte) = when (memoryAddress) {
        ReservedAddresses.DIV.memoryAddress, ReservedAddresses.LY.memoryAddress ->
            bottomRegisters.setValue(memoryAddress, 0x00.toUByte())

        else -> {
            bottomRegisters.setValue(memoryAddress, value)
        }
    }

    override fun getValue(memoryAddress: Int): UByte = when (memoryAddress) {
        in 0 until ReservedAddresses.SWITCH_ROM_END.memoryAddress -> {
            rom.getValue(memoryAddress)
        }

        in ReservedAddresses.SWITCH_ROM_END.memoryAddress until ReservedAddresses.VRAM_END.memoryAddress -> {
            vram.getValue(memoryAddress)
        }

        in ReservedAddresses.VRAM_END.memoryAddress until ReservedAddresses.ERAM_END.memoryAddress -> {
            if (eram != null && (rom as RomModule).ramStatus) {
                eram.getValue(memoryAddress)
            } else {
                0x00u
            }
        }

        in ReservedAddresses.ERAM_END.memoryAddress until ReservedAddresses.WRAM_END.memoryAddress -> {
            wram.getValue(memoryAddress)
        }

        in ReservedAddresses.WRAM_END.memoryAddress until ReservedAddresses.OAM_START.memoryAddress -> {
            wram.getValue(memoryAddress - 0x2000)
        }

        in ReservedAddresses.OAM_START.memoryAddress until ReservedAddresses.OAM_END.memoryAddress -> {
            oam.getValue(memoryAddress)
        }

        in ReservedAddresses.OAM_END.memoryAddress until ReservedAddresses.JOYP.memoryAddress -> {
            0x00u
        }

        else -> {
            getBottomRegisters(memoryAddress)
        }
    }

    /**
     * This method is responsible for handling the retrieval of values from the bottom registers,
     * according to all their quirks (these registers have special conditions that must be respected)
     *
     * @param memoryAddress memory location where value should be retrieved
     * @return value stored in given address
     */
    private fun getBottomRegisters(memoryAddress: Int): UByte {
        if (memoryAddress == ReservedAddresses.JOYP.memoryAddress) {
            return bus.getJoypad(bottomRegisters.getValue(memoryAddress))
        }

        return bottomRegisters.getValue(memoryAddress)
    }

    /**
     * Sets the value of the [ReservedAddresses.DIV] memory address, this method is design for giving
     * the [Timers] access to writing to this address as it should normally reset to 0 when written
     * to
     *
     * @param value to write to [ReservedAddresses.DIV]
     */
    internal fun setDiv(value: UByte) {
        bottomRegisters.setValue(ReservedAddresses.DIV.memoryAddress, value)
    }

    /**
     * Converts the full memory map into a readable string containing all the memory addrress' content
     *
     * @return memory dump of GB
     */
    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("0 ")

        for (i in 0..0xFFFF) {
            if (i % 16 == 0 && i != 0) {
                stringBuilder.append(" \n")
                stringBuilder.append(Integer.toHexString(i)).append(" ")
                stringBuilder.append(Integer.toHexString(getValue(i).toInt())).append(" ")
            } else {
                stringBuilder.append(Integer.toHexString(getValue(i).toInt())).append(" ")
            }
        }

        return stringBuilder.toString()
    }
}
