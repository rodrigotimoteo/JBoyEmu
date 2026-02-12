package com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge

import com.github.rodrigotimoteo.kboyemucore.memory.MemoryModule
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomModule

/**
 * Represents the MBC1 (Memory Bank Controller) one of many types of controllers used by the Game Boy
 * This controller can have up to 2MiB of ROM and 32Kib of RAM, but the actual amount depends on the cartridge
 *
 * @param romBanks the number of ROM banks in the cartridge
 * @param ramBanks the number of RAM banks in the cartridge
 * @param romContent the content of the ROM, which will be used to initialize the memory
 *
 * @author rodrigotimoteo
 **/
@OptIn(ExperimentalUnsignedTypes::class)
class MBC1(
    override val romBanks: Int,
    override val ramBanks: Int,
    romContent: UByteArray
) : MemoryModule(
    content = romContent,
    size = 0x4000,
    simultaneousBanks = 2,
    memoryOffset = 0x0,
    numberOfBanks = romBanks
), RomModule {
    private var _ramStatus = false
    private var _ramBankNumber = 0
    private var romBankLow = 1
    private var romBankHigh = 0
    private var bankingMode = 0

    override val ramStatus
        get() = _ramStatus

    override val ramBankNumber
        get() = _ramBankNumber

    override fun setValue(memoryAddress: Int, value: UByte) = when (memoryAddress) {
        in 0 until ReservedAddresses.RAM_ENABLE.memoryAddress -> {
            _ramStatus = (value.toInt() and 0x0F) == 0x0A
        }

        in ReservedAddresses.RAM_ENABLE.memoryAddress until ReservedAddresses.ROM_BANK0_END.memoryAddress -> {
            romBankLow = value.toInt() and 0x1F
            if (romBankLow == 0) romBankLow = 1
            updateActiveRomBank()
        }

        in ReservedAddresses.ROM_BANK0_END.memoryAddress until ReservedAddresses.RAM_BANK.memoryAddress -> {
            val bankBits = value.toInt() and 0x03
            if (bankingMode == 0) {
                romBankHigh = bankBits
                updateActiveRomBank()
            } else {
                _ramBankNumber = if (ramBanks == 0) 0 else bankBits % ramBanks
            }
        }

        in ReservedAddresses.RAM_BANK.memoryAddress until ReservedAddresses.SWITCH_ROM_END.memoryAddress -> {
            bankingMode = value.toInt() and 0x01
            updateActiveRomBank()
        }

        else -> super.setValue(memoryAddress, value)
    }

    /**
     * Updates the active ROM bank based on the current values of romBankLow, romBankHigh, and bankingMode.
     */
    private fun updateActiveRomBank() {
        val selected = if (bankingMode == 0) {
            (romBankHigh shl 5) or romBankLow
        } else {
            romBankLow
        }

        val max = if (romBanks == 0) 1 else romBanks
        activeBank = (selected % max).coerceAtLeast(1)
    }
}
