package com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge

import com.github.rodrigotimoteo.kboyemucore.memory.MemoryModule
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomModule

/**
 * Represents the MBC0 (Memory Bank Controller) one of many types of controllers used by the Game Boy
 * This controller normally has 32Kib of ROM and if (most of the time it doesn't) RAM exists 8Kib
 *
 * @param romBanks the number of ROM banks in the cartridge
 * @param ramBanks the number of RAM banks in the cartridge
 * @param romContent the content of the ROM, which will be used to initialize the memory
 *
 * @author rodrigotimoteo
 **/
@OptIn(ExperimentalUnsignedTypes::class)
class MBC0(
    override val romBanks: Int,
    override val ramBanks: Int,
    romContent: UByteArray,
) : MemoryModule(
    content = romContent,
    size = 0x4000,
    simultaneousBanks = 2,
    memoryOffset = 0x0,
    numberOfBanks = romBanks
), RomModule {
    private var _ramStatus = false

    override val ramStatus
        get() = _ramStatus

    override val ramBankNumber = 0
}
