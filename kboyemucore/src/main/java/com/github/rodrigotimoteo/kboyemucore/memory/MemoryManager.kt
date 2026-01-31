package com.github.rodrigotimoteo.kboyemucore.memory

import com.github.rodrigotimoteo.kboyemucore.bus.Bus

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

//@Suppress("MagicNumber")
//class MemoryManager(
//    private val bus: Bus,
//    private val rom: MemoryModule
//) : MemoryManipulation {
//    private val isCGB: Boolean = bus.isCGB()
//
//    private val vram: MemoryModule = if (isCGB) {
//        MemoryModule(0x2000, 1, 0x8000, 2)
//    } else {
//        MemoryModule(0x2000, 0x8000)
//    }
//
//    private val eram: MemoryModule? = when (val ramBanks = (rom as RomModule).getRamBanks()) {
//        0 -> null
//        else -> MemoryModule(0x2000, 1, 0xA000, ramBanks)
//    }
//
//    private val wram: MemoryModule = if (isCGB) {
//        MemoryModule(0x1000, 2, 0xC000, 8)
//    } else {
//        MemoryModule(0x1000, 2, 0xC000, 2)
//    }
//
//    private val oam: MemoryModule = MemoryModule(0xA0, ReservedAddresses.OAM_START.memoryAddress)
//
//    private val bottomRegisters: MemoryModule = MemoryModule(0x100, ReservedAddresses.JOYP.memoryAddress)
//
//
//    /**
//     * Responsible for initializing the memory with the default values assign by the boot rom
//     * therefore skipping its necessity
//     */
//    init {
//        getWord(ReservedAddresses.NR10.memoryAddress)?.setValue(0x80)
//        getWord(ReservedAddresses.NR11.memoryAddress)?.setValue(0xBF)
//        getWord(ReservedAddresses.NR12.memoryAddress)?.setValue(0xF3)
//        getWord(ReservedAddresses.NR14.memoryAddress)?.setValue(0xBF)
//        getWord(ReservedAddresses.NR21.memoryAddress)?.setValue(0x3F)
//        getWord(ReservedAddresses.NR24.memoryAddress)?.setValue(0xBF)
//        getWord(ReservedAddresses.NR30.memoryAddress)?.setValue(0x7F)
//        getWord(ReservedAddresses.NR31.memoryAddress)?.setValue(0xFF)
//        getWord(ReservedAddresses.NR32.memoryAddress)?.setValue(0x9F)
//        getWord(ReservedAddresses.NR34.memoryAddress)?.setValue(0xBF)
//        getWord(ReservedAddresses.NR41.memoryAddress)?.setValue(0xFF)
//        getWord(ReservedAddresses.NR44.memoryAddress)?.setValue(0xBF)
//        getWord(ReservedAddresses.NR50.memoryAddress)?.setValue(0x77)
//        getWord(ReservedAddresses.NR51.memoryAddress)?.setValue(0xF3)
//        getWord(ReservedAddresses.NR52.memoryAddress)?.setValue(0xF1)
//        getWord(ReservedAddresses.LCDC.memoryAddress)?.setValue(0x91)
//        getWord(ReservedAddresses.STAT.memoryAddress)?.setValue(0x80)
//        getWord(ReservedAddresses.BGP .memoryAddress)?.setValue(0xFC)
//        getWord(ReservedAddresses.OBP0.memoryAddress)?.setValue(0xFF)
//        getWord(ReservedAddresses.OBP1.memoryAddress)?.setValue(0xFF)
//
//        //Debug Purposes LY
//        getWord(ReservedAddresses.LY.memoryAddress)?.setValue(0x90)
//    }
//
//    override fun setValue(memoryAddress: Int, value: UByte) = when(memoryAddress) {
//
//        if (memoryAddress < ReservedAddresses.SWITCH_ROM_END.memoryAddress)
//            rom.setValue(memoryAddress, value)
//        else if (memoryAddress < ReservedAddresses.VRAM_END.memoryAddress)
//            vram.setValue(memoryAddress, value)
//        else if (memoryAddress < ReservedAddresses.ERAM_END.memoryAddress)
//            eram?.setValue(memoryAddress, value)
//        else if (memoryAddress < ReservedAddresses.WRAM_END.memoryAddress)
//            wram.setValue(memoryAddress, value)
//        else if (memoryAddress < ReservedAddresses.OAM_START.memoryAddress)
//        //Unusable section under development guidelines
//            return
//        else if (memoryAddress < ReservedAddresses.OAM_END.memoryAddress)
//            oam.setValue(memoryAddress, value)
//        else if (memoryAddress < ReservedAddresses.JOYP.memoryAddress)
//        //Unusable section under development guidelines
//            return
//        else
//            handleBottomRegisters(memoryAddress, value)
//
//        else -> handleBottomRegisters(memoryAddress, value)
//    }
//
//    /**
//     * This method is responsible for handling the assignment of new values to the bottom registers, according to all
//     * their quirks (these registers have special conditions that must be respected)
//     *
//     * @param memoryAddress memory location where value should be written
//     * @param value content that needs to be written to given address
//     */
//    private fun handleBottomRegisters(memoryAddress: Int, value: UByte) {
//        if (memoryAddress == ReservedAddresses.DIV.memoryAddress)
//            bottomRegisters.setValue(memoryAddress, 0x00.toUByte())
//        else if(memoryAddress == ReservedAddresses.LY.memoryAddress)
//            bottomRegisters.setValue(memoryAddress, 0x00.toUByte())
//
//        bottomRegisters.setValue(memoryAddress, value)
//    }
//
//    override fun getValue(memoryAddress: Int): UByte {
//        return if (memoryAddress < ReservedAddresses.SWITCH_ROM_END.memoryAddress)
//            rom.getValue(memoryAddress)
//        else if (memoryAddress < ReservedAddresses.VRAM_END.memoryAddress)
//            vram.getValue(memoryAddress)
//        else if (memoryAddress < ReservedAddresses.ERAM_END.memoryAddress)
//            if(eram != null && (rom as RomModule).getRamStatus())
//                eram.getValue(memoryAddress)
//            else
//                0x00
//        else if (memoryAddress < ReservedAddresses.WRAM_END.memoryAddress)
//            wram.getValue(memoryAddress)
//        else if (memoryAddress < ReservedAddresses.OAM_START.memoryAddress)
//            //This will always return the WRAM content minus it's offset to its location
//            wram.getValue(memoryAddress - 0x2000)
//        else if (memoryAddress < ReservedAddresses.OAM_END.memoryAddress)
//            oam.getValue(memoryAddress)
//        else if (memoryAddress < ReservedAddresses.JOYP.memoryAddress)
//            //Unusable section under development guidelines
//            0
//        else
//            bottomRegisters.getValue(memoryAddress)
//    }
//
//    /**
//     * Converts the full memory map into a readable string containing all the memory addrress' content
//     *
//     * @return memory dump of GB
//     */
//    override fun toString(): String {
//        val stringBuilder = StringBuilder()
//        stringBuilder.append("0 ")
//
//        for (i in 0..0xFFFF) {
//            if (i % 16 == 0 && i != 0) {
//                stringBuilder.append(" \n")
//                stringBuilder.append(Integer.toHexString(i)).append(" ")
//                stringBuilder.append(Integer.toHexString(getValue(i).toInt())).append(" ")
//            } else {
//                stringBuilder.append(Integer.toHexString(getValue(i).toInt())).append(" ")
//            }
//        }
//
//        return stringBuilder.toString()
//    }
//}
