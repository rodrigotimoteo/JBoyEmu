package com.github.rodrigotimoteo.kboyemucore.memory.rom

import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.memory.MemoryModule
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge.MBC0
import com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge.MBC1
import com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge.MBC2
import com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge.MBC3
import com.github.rodrigotimoteo.kboyemucore.util.Logger

/**
 * Class responsible for reading a file and parsing its rom, creating a MBC for the given rom
 *
 * @author rodrigotimoteo
 **/
@OptIn(ExperimentalUnsignedTypes::class)
class RomReader(
    private val logger: Logger,
) {

    /**
     * Holds the number of Rom banks for a given Rom size (coded internally by Game Boy)
     */
    private val romBanksMap: HashMap<Int, Int> = hashMapOf(
        Pair(0, 2),
        Pair(1, 4),
        Pair(2, 8),
        Pair(3, 16),
        Pair(4, 32),
        Pair(5, 64),
        Pair(6, 128),
        Pair(7, 256),
        Pair(8, 512)
    )

    /**
     * Holds the number of Ram banks for a given Ram size (coded internally by Game Boy)
     */
    private val ramBanksMap: HashMap<Int, Int> = hashMapOf(
        Pair(0, 0),
        Pair(1, 1),
        Pair(2, 1),
        Pair(3, 4),
        Pair(4, 16),
        Pair(5, 8)
    )

    /**
     * Holds the content of the rom
     */
    private var romContent: UByteArray = ubyteArrayOf()

    /**
     * Loads a Rom into rom content
     */
    fun loadRom(rom: Rom) {
        logger.i("Loaded rom with size ${rom.bytes.size} bytes")
        romContent = rom.bytes
    }

    /**
     * Checks if the Rom is CGB or DMG Rom. 0x80 = CGB+DMG compatible, 0xC0 = CGB only.
     */
    fun isCgb(): Boolean {
        val flag = romContent[ReservedAddresses.CONSOLE_TYPE.memoryAddress].toInt() and 0xFF
        return flag == 0x80 || flag == 0xC0
    }

    /**
     * Reads the game title from the ROM header (0x0134–0x0142). Trims trailing null bytes
     * and non-printable characters.
     *
     * @return game title string, or "Unknown" if the ROM is not loaded
     */
    fun getTitle(): String {
        if (romContent.isEmpty()) return "Unknown"

        val start = ReservedAddresses.TITLE_START.memoryAddress
        val end = ReservedAddresses.TITLE_END.memoryAddress

        return romContent.sliceArray(start..end)
            .map { it.toByte().toInt().toChar() }
            .filter { it.code in 0x20..0x7E }
            .joinToString("")
            .trim()
            .ifEmpty { "Unknown" }
    }

    /**
     * Build and return a new MemoryModule for the given rom
     */
    fun getRomModule(): MemoryModule =
        when (romContent[ReservedAddresses.CARTRIDGE_TYPE.memoryAddress].toInt()) {
            0x00, 0x08, 0x09 ->
                MBC0(getRomSize(), getRamSize(), romContent)

            0x01, 0x02, 0x03 ->
                MBC1(getRomSize(), getRamSize(), romContent)

            0x05, 0x06 ->
                MBC2(getRomSize(), romContent)

            0x0F, 0x10, 0x11, 0x12, 0x13 ->
                MBC3(getRomSize(), getRamSize(), hasRtc(), romContent)

            0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E ->
                //should be 5
                MBC1(getRomSize(), getRamSize(), romContent)

            else ->
                MBC1(getRomSize(), getRamSize(), romContent)
        }

    /**
     * Responsible for translating the hashmap into the number of rom banks the rom needs
     */
    private fun getRomSize(): Int {
        val romSize = romContent[ReservedAddresses.ROM_SIZE.memoryAddress].toInt()

        return romBanksMap[romSize] ?: 0
    }

    /**
     * Responsible for translating the hashmap into the number of ram banks the rom needs
     */
    private fun getRamSize(): Int {
        val ramSize = romContent[ReservedAddresses.RAM_SIZE.memoryAddress].toInt()

        return ramBanksMap[ramSize] ?: 0
    }

    /**
     * Returns whether the cartridge has a Real Time Clock.
     * Only MBC3 cartridge types 0x0F and 0x10 include an RTC.
     */
    private fun hasRtc(): Boolean =
        romContent[ReservedAddresses.CARTRIDGE_TYPE.memoryAddress].toInt() in setOf(0x0F, 0x10)

    /**
     * Returns whether the cartridge has battery-backed SRAM that should persist across sessions.
     * Cartridge types: 0x03 (MBC1+RAM+BATT), 0x06 (MBC2+BATT), 0x09 (ROM+RAM+BATT),
     * 0x0F/0x10 (MBC3+TIMER+BATT), 0x13 (MBC3+RAM+BATT), 0x1B/0x1E (MBC5+RAM+BATT).
     */
    fun hasBattery(): Boolean =
        romContent[ReservedAddresses.CARTRIDGE_TYPE.memoryAddress].toInt() in
            setOf(0x03, 0x06, 0x09, 0x0F, 0x10, 0x13, 0x1B, 0x1E)
}
