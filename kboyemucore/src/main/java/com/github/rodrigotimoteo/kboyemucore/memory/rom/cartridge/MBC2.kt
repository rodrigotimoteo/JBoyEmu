package com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge

import com.github.rodrigotimoteo.kboyemucore.memory.MemoryModule
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomModule

/**
 * Represents the MBC2 (Memory Bank Controller 2).
 *
 * MBC2 supports up to 256 KiB ROM (16 banks of 16 KiB) and has 512×4-bit built-in RAM
 * (only the lower nibble of each byte is valid). It has no external RAM banks and no
 * banking mode register.
 *
 * Write routing in 0x0000–0x3FFF is controlled by bit 8 of the write address:
 *  - Bit 8 CLEAR → RAM enable/disable (value 0x0A enables, anything else disables)
 *  - Bit 8 SET   → ROM bank select (lower 4 bits; 0 is remapped to 1)
 *
 * @param romBanks the number of ROM banks in the cartridge
 * @param romContent the content of the ROM
 *
 * @author rodrigotimoteo
 **/
@OptIn(ExperimentalUnsignedTypes::class)
class MBC2(
    override val romBanks: Int,
    romContent: UByteArray
) : MemoryModule(
    content = romContent,
    size = 0x4000,
    simultaneousBanks = 2,
    memoryOffset = 0x0,
    numberOfBanks = romBanks
), RomModule {

    // MBC2 has no external RAM banks — built-in 512×4-bit only
    override val ramBanks: Int = 0

    private var _ramStatus = false
    override val ramStatus get() = _ramStatus

    // MBC2 has no RAM banking
    override val ramBankNumber: Int = 0

    override fun setValue(memoryAddress: Int, value: UByte) = when (memoryAddress) {
        in 0 until ReservedAddresses.ROM_BANK0_END.memoryAddress -> {
            // Bit 8 of the address selects the operation
            if ((memoryAddress and 0x100) != 0) {
                // ROM bank select — lower 4 bits only, 0 remapped to 1
                var bank = value.toInt() and 0x0F
                if (bank == 0) bank = 1
                val max = if (romBanks == 0) 1 else romBanks
                activeBank = bank % max
            } else {
                // RAM enable — 0x0A enables, anything else disables
                _ramStatus = (value.toInt() and 0x0F) == 0x0A
            }
        }

        else -> super.setValue(memoryAddress, value)
    }
}

