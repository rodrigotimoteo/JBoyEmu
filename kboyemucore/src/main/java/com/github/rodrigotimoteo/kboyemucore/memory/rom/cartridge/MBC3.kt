package com.github.rodrigotimoteo.kboyemucore.memory.rom.cartridge

import com.github.rodrigotimoteo.kboyemucore.memory.MemoryModule
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomModule

/**
 * Represents the MBC3 (Memory Bank Controller 3).
 *
 * MBC3 supports up to 2 MiB ROM (128 banks of 16 KiB) and up to 32 KiB RAM (4 banks).
 * It also contains a Real Time Clock (RTC) with 5 registers (seconds, minutes, hours,
 * lower day counter, upper day counter + flags).
 *
 * Key differences from MBC1:
 *  - ROM bank is 7-bit (0x00–0x7F), written directly to 0x2000–0x3FFF; 0 is remapped to 1
 *  - No upper ROM bank bits / no advanced banking mode
 *  - 0x4000–0x5FFF: values 0x00–0x03 select a RAM bank; values 0x08–0x0C map an RTC register
 *  - 0x6000–0x7FFF: writing 0x00 then 0x01 latches the current RTC time
 *
 * @param romBanks the number of ROM banks in the cartridge
 * @param ramBanks the number of RAM banks in the cartridge
 * @param hasRtc whether this cartridge has a Real Time Clock
 * @param romContent the content of the ROM
 *
 * @author rodrigotimoteo
 **/
@OptIn(ExperimentalUnsignedTypes::class)
class MBC3(
    override val romBanks: Int,
    override val ramBanks: Int,
    private val hasRtc: Boolean,
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

    override val ramStatus get() = _ramStatus
    override val ramBankNumber get() = _ramBankNumber

    // RTC register indices: 0=seconds, 1=minutes, 2=hours, 3=day-low, 4=day-high+flags
    private val rtcRegisters = IntArray(5)
    private val rtcLatched = IntArray(5)
    private var rtcSelected = -1      // -1 = RAM mapped, 0–4 = RTC register index
    private var rtcLatchState = 0xFF   // tracks the 0x00→0x01 latch sequence

    // ── RomModule RTC interface ──────────────────────────────────────────────

    override val hasRtcMapped: Boolean
        get() = hasRtc && rtcSelected >= 0

    override fun readRtcRegister(): UByte = rtcLatched[rtcSelected].toUByte()

    override fun writeRtcRegister(value: UByte) {
        rtcRegisters[rtcSelected] = value.toInt()
    }

    /**
     * Advances the RTC by one second. Call this once per real-time second from the emulation loop.
     * Handles carry propagation: seconds→minutes→hours→day counter.
     * The halt flag (bit 6 of day-high register) stops counting when set.
     */
    @Suppress("ReturnCount")
    override fun tickRtc() {
        if (!hasRtc) return
        if ((rtcRegisters[4] and 0x40) != 0) return  // halt flag set — RTC stopped

        rtcRegisters[0]++                             // seconds
        if (rtcRegisters[0] < 60) return
        rtcRegisters[0] = 0

        rtcRegisters[1]++                             // minutes
        if (rtcRegisters[1] < 60) return
        rtcRegisters[1] = 0

        rtcRegisters[2]++                             // hours
        if (rtcRegisters[2] < 24) return
        rtcRegisters[2] = 0

        val day = ((rtcRegisters[4] and 0x01) shl 8) or rtcRegisters[3]
        val newDay = day + 1
        rtcRegisters[3] = newDay and 0xFF             // day-low
        rtcRegisters[4] = (rtcRegisters[4] and 0xFE) or ((newDay shr 8) and 0x01)  // day-high bit
        if (newDay > 511) {
            rtcRegisters[4] = rtcRegisters[4] or 0x80  // day counter carry / overflow flag
            rtcRegisters[3] = 0
            rtcRegisters[4] = rtcRegisters[4] and 0xFE  // reset day-high bit
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun setValue(memoryAddress: Int, value: UByte) {
        when (memoryAddress) {
            // 0x0000–0x1FFF: RAM + RTC enable (0x0A enables, anything else disables)
            in 0 until ReservedAddresses.RAM_ENABLE.memoryAddress -> {
                _ramStatus = (value.toInt() and 0x0F) == 0x0A
            }

            // 0x2000–0x3FFF: ROM bank select (7-bit, 0 remapped to 1)
            in ReservedAddresses.RAM_ENABLE.memoryAddress until ReservedAddresses.ROM_BANK0_END.memoryAddress -> {
                var bank = value.toInt() and 0x7F
                if (bank == 0) bank = 1
                val max = if (romBanks == 0) 1 else romBanks
                activeBank = bank % max
            }

            // 0x4000–0x5FFF: RAM bank select (0x00–0x03) or RTC register select (0x08–0x0C)
            in ReservedAddresses.ROM_BANK0_END.memoryAddress until ReservedAddresses.RAM_BANK.memoryAddress -> {
                val v = value.toInt()
                if (v in 0x00..0x03) {
                    rtcSelected = -1
                    _ramBankNumber = if (ramBanks == 0) 0 else v % ramBanks
                } else if (hasRtc && v in 0x08..0x0C) {
                    rtcSelected = v - 0x08
                }
            }

            // 0x6000–0x7FFF: RTC latch — write 0x00 then 0x01 to latch current time
            in ReservedAddresses.RAM_BANK.memoryAddress until ReservedAddresses.SWITCH_ROM_END.memoryAddress -> {
                if (hasRtc) {
                    val v = value.toInt() and 0x01
                    if (rtcLatchState == 0x00 && v == 0x01) {
                        rtcLatched.indices.forEach { rtcLatched[it] = rtcRegisters[it] }
                    }
                    rtcLatchState = v
                }
            }

            else -> super.setValue(memoryAddress, value)
        }
    }
}
