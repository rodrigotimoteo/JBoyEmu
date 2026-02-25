package com.github.rodrigotimoteo.kboyemucore.memory

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.memory.rom.RomModule
import com.github.rodrigotimoteo.kboyemucore.ppu.PPUModes
import com.github.rodrigotimoteo.kboyemucore.ppu.writeOAMAble
import com.github.rodrigotimoteo.kboyemucore.ppu.writeVRAMAble
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import com.github.rodrigotimoteo.kboyemucore.util.MutableUByte
import com.github.rodrigotimoteo.kboyemucore.util.REGISTER_DOES_NOT_EXIST

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
    private val logger: Logger,
    private val rom: MemoryModule,
) : MemoryManipulation {

    /**
     * Whether or not the current rom is Color Game Boy or not
     */
    private val isCGB: Boolean = bus.isCGB

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
    private val bottomRegisters = Array(0x100) { MutableUByte() }

    private val ppuMode: PPUModes
        get() = bus.ppuMode

    // ── CGB palette RAM ──────────────────────────────────────────────────────
    /** BG palette auto-increment flag and index (written via BCPS 0xFF68) */
    private var bgPaletteIndex: Int = 0
    private var bgPaletteAutoInc: Boolean = false

    /** OBJ palette auto-increment flag and index (written via OCPS 0xFF6A) */
    private var objPaletteIndex: Int = 0
    private var objPaletteAutoInc: Boolean = false

    /**
     * Responsible for initializing the memory with the default values assign by the boot rom
     * therefore skipping its necessity
     *
     * Uses - ReservedAddresses.JOYP.memoryAddress as the base address for the bottom registers as
     * they are the last part of the memory map
     */
    init {
        bottomRegisters[ReservedAddresses.NR10.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0x80u
        bottomRegisters[ReservedAddresses.NR11.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xBFu
        bottomRegisters[ReservedAddresses.NR12.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xF3u
        bottomRegisters[ReservedAddresses.NR14.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xBFu
        bottomRegisters[ReservedAddresses.NR21.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0x3Fu
        bottomRegisters[ReservedAddresses.NR24.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xBFu
        bottomRegisters[ReservedAddresses.NR30.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0x7Fu
        bottomRegisters[ReservedAddresses.NR31.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xFFu
        bottomRegisters[ReservedAddresses.NR32.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0x9Fu
        bottomRegisters[ReservedAddresses.NR34.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xBFu
        bottomRegisters[ReservedAddresses.NR41.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xFFu
        bottomRegisters[ReservedAddresses.NR44.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xBFu
        bottomRegisters[ReservedAddresses.NR50.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0x77u
        bottomRegisters[ReservedAddresses.NR51.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xF3u
        bottomRegisters[ReservedAddresses.NR52.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xF1u
        bottomRegisters[ReservedAddresses.LCDC.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0x91u
        bottomRegisters[ReservedAddresses.STAT.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0x80u
        bottomRegisters[ReservedAddresses.BGP.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xFCu
        bottomRegisters[ReservedAddresses.OBP0.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xFFu
        bottomRegisters[ReservedAddresses.OBP1.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
            0xFFu

        if (isCGB) {
            bottomRegisters[ReservedAddresses.VBK.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                0xFEu
            bottomRegisters[ReservedAddresses.SVBK.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                0xF8u
            bottomRegisters[ReservedAddresses.HDMA5.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                0xFFu
            bottomRegisters[ReservedAddresses.KEY1.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                0x00u
            bottomRegisters[ReservedAddresses.OPRI.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                0xFEu
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun setValue(memoryAddress: Int, value: UByte) {
        val addr = memoryAddress and 0xFFFF
        when (addr) {
            in 0 until ReservedAddresses.SWITCH_ROM_END.memoryAddress -> {
                rom.setValue(addr, value).also {
                    eram?.let { eram.activeBank = (rom as RomModule).ramBankNumber }
                }
            }

            in ReservedAddresses.SWITCH_ROM_END.memoryAddress until ReservedAddresses.VRAM_END.memoryAddress -> {
                if (ppuMode.writeVRAMAble()) {
                    vram.setValue(addr, value)
                } else {
                    Unit
                }
            }

            in ReservedAddresses.VRAM_END.memoryAddress until ReservedAddresses.ERAM_END.memoryAddress -> {
                val romModule = rom as RomModule
                if (romModule.ramStatus) {
                    if (romModule.hasRtcMapped) {
                        romModule.writeRtcRegister(value)
                    } else {
                        eram?.setValue(addr, value) ?: Unit
                    }
                } else Unit
            }

            in ReservedAddresses.ERAM_END.memoryAddress until ReservedAddresses.WRAM_END.memoryAddress -> {
                wram.setValue(addr, value)
            }

            in ReservedAddresses.WRAM_END.memoryAddress until ReservedAddresses.OAM_START.memoryAddress -> {
                // Echo RAM — ignore writes
            }

            in ReservedAddresses.OAM_START.memoryAddress until ReservedAddresses.OAM_END.memoryAddress -> {
                if (ppuMode.writeOAMAble()) {
                    oam.setValue(addr, value)
                } else {
                    Unit
                }
            }

            in ReservedAddresses.OAM_END.memoryAddress until ReservedAddresses.JOYP.memoryAddress -> {
                // Unusable memory — ignore writes
            }

            else -> {
                setBottomRegisters(addr, value)
            }
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
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value = value
        }

        else -> {
            /** Nothing needs to be done */
        }
    }

    /**
     * Advances the RTC by one second if the current cartridge has one.
     */
    fun tickRtc() {
        (rom as? RomModule)?.tickRtc()
    }

    /**
     * This method is responsible for handling the assignment of new values to the bottom registers, according to all
     * their quirks (these registers have special conditions that must be respected)
     *
     * @param memoryAddress memory location where value should be written
     * @param value content that needs to be written to given address
     */
    @Suppress("CyclomaticComplexMethod")
    private fun setBottomRegisters(memoryAddress: Int, value: UByte) = when (memoryAddress) {
        ReservedAddresses.DIV.memoryAddress, ReservedAddresses.LY.memoryAddress ->
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value = 0x00u

        ReservedAddresses.DMA.memoryAddress -> {
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value = value
            performDmaTransfer(value.toInt())
        }

        in ReservedAddresses.NR10.memoryAddress..ReservedAddresses.WAVE_END.memoryAddress -> {
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value = value
            bus.spu.writeRegister(memoryAddress, value.toInt())
        }

        ReservedAddresses.IF.memoryAddress -> {
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                (value.toInt() and 0x1F).toUByte()
        }

        // ── CGB-only registers ───────────────────────────────────────────────
        ReservedAddresses.VBK.memoryAddress -> if (isCGB) {
            val bank = value.toInt() and 0x01
            vram.activeBank = bank
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                (0xFE or bank).toUByte()
        } else Unit

        ReservedAddresses.SVBK.memoryAddress -> if (isCGB) {
            val bank = (value.toInt() and 0x07).let { if (it == 0) 1 else it }
            wram.activeBank = bank
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                (0xF8 or (value.toInt() and 0x07)).toUByte()
        } else Unit

        ReservedAddresses.BCPS.memoryAddress -> if (isCGB) {
            bgPaletteIndex = value.toInt() and 0x3F
            bgPaletteAutoInc = (value.toInt() and 0x80) != 0
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value = value
        } else Unit

        ReservedAddresses.BCPD.memoryAddress -> if (isCGB) {
            bus.cgbDrawer?.writeBgPalette(bgPaletteIndex, value.toInt())
            if (bgPaletteAutoInc) bgPaletteIndex = (bgPaletteIndex + 1) and 0x3F
            Unit
        } else Unit

        ReservedAddresses.OCPS.memoryAddress -> if (isCGB) {
            objPaletteIndex = value.toInt() and 0x3F
            objPaletteAutoInc = (value.toInt() and 0x80) != 0
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value = value
        } else Unit

        ReservedAddresses.OCPD.memoryAddress -> if (isCGB) {
            bus.cgbDrawer?.writeObjPalette(objPaletteIndex, value.toInt())
            if (objPaletteAutoInc) objPaletteIndex = (objPaletteIndex + 1) and 0x3F
            Unit
        } else Unit

        ReservedAddresses.OPRI.memoryAddress -> if (isCGB) {
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                (0xFE or (value.toInt() and 0x01)).toUByte()
        } else Unit

        ReservedAddresses.KEY1.memoryAddress -> if (isCGB) {
            val current = bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value.toInt()
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value =
                ((current and 0xFE) or (value.toInt() and 0x01)).toUByte()
        } else Unit

        ReservedAddresses.HDMA5.memoryAddress -> if (isCGB) {
            performHdmaTransfer(value.toInt())
        } else Unit

        else -> {
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value = value
        }
    }

    // ── CGB HBlank HDMA state ────────────────────────────────────────────────
    /** Whether an HBlank HDMA transfer is currently active */
    private var hdmaActive = false
    /** Current source address for the next HBlank chunk */
    private var hdmaSource = 0
    /** Current destination offset (within VRAM) for the next HBlank chunk */
    private var hdmaDest = 0
    /** Remaining blocks to transfer (each block = 0x10 bytes) */
    private var hdmaRemaining = 0

    /**
     * Performs a CGB speed switch. Toggles KEY1 bit 7 (current speed) and clears bit 0 (request).
     * Called by STOP instruction when KEY1 bit 0 is set.
     */
    fun performSpeedSwitch() {
        val key1Offset = ReservedAddresses.KEY1.memoryAddress - ReservedAddresses.JOYP.memoryAddress
        val current = bottomRegisters[key1Offset].value.toInt()
        if ((current and 0x01) != 0) {
            bottomRegisters[key1Offset].value = ((current xor 0x80) and 0xFE).toUByte()
        }
    }

    /**
     * Performs an OAM DMA transfer. When the game writes X to 0xFF46, copies 0xA0 bytes
     * from address (X * 0x100) into OAM (0xFE00–0xFE9F).
     *
     * @param source high byte of the source address (source address = source * 0x100)
     */
    private fun performDmaTransfer(source: Int) {
        val baseAddress = source * 0x100
        for (i in 0 until 0xA0) {
            oam.setValue(ReservedAddresses.OAM_START.memoryAddress + i, getValue(baseAddress + i))
        }
    }

    /**
     * Handles a write to HDMA5 (0xFF55).
     *
     * - Bit 7 = 0, no active HBlank DMA: general-purpose (immediate) transfer
     * - Bit 7 = 0, active HBlank DMA: cancel the ongoing HBlank DMA
     * - Bit 7 = 1: start a new HBlank DMA (0x10 bytes copied per HBlank)
     *
     * @param controlByte the value written to HDMA5
     */
    private fun performHdmaTransfer(controlByte: Int) {
        val hdma5Offset = ReservedAddresses.HDMA5.memoryAddress - ReservedAddresses.JOYP.memoryAddress

        // Cancel active HBlank DMA when bit 7 is clear
        if ((controlByte and 0x80) == 0 && hdmaActive) {
            hdmaActive = false
            // HDMA5 reads back remaining blocks with bit 7 set (inactive flag)
            bottomRegisters[hdma5Offset].value = (0x80 or hdmaRemaining).toUByte()
            return
        }

        val sourceHigh = bottomRegisters[ReservedAddresses.HDMA1.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value.toInt()
        val sourceLow = bottomRegisters[ReservedAddresses.HDMA2.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value.toInt() and 0xF0
        val destHigh = bottomRegisters[ReservedAddresses.HDMA3.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value.toInt() and 0x1F
        val destLow = bottomRegisters[ReservedAddresses.HDMA4.memoryAddress - ReservedAddresses.JOYP.memoryAddress].value.toInt() and 0xF0

        val source = (sourceHigh shl 8) or sourceLow
        val dest = (destHigh shl 8) or destLow
        val blocks = (controlByte and 0x7F) + 1
        val length = blocks * 0x10

        if ((controlByte and 0x80) == 0) {
            // General-purpose DMA — immediate copy
            for (i in 0 until length) {
                val byte = getValue(source + i)
                val vramAddr = 0x8000 or ((dest + i) and 0x1FFF)
                vram.setValue(vramAddr, byte)
            }
            bottomRegisters[hdma5Offset].value = 0xFFu
        } else {
            // HBlank DMA — store state, PPU will call tickHdma() each HBlank
            hdmaActive = true
            hdmaSource = source
            hdmaDest = dest
            hdmaRemaining = blocks - 1  // HDMA5 reads back remaining-1 during transfer
            // HDMA5 reads back remaining blocks with bit 7 clear (active)
            bottomRegisters[hdma5Offset].value = hdmaRemaining.toUByte()
        }
    }

    /**
     * Called by the PPU at the start of each HBlank to transfer one 0x10-byte block if an HBlank
     * HDMA is active. Returns true if a transfer was performed.
     */
    fun tickHdma(): Boolean {
        if (!hdmaActive) return false

        // Copy one 0x10-byte block
        for (i in 0 until 0x10) {
            val byte = getValue(hdmaSource + i)
            val vramAddr = 0x8000 or ((hdmaDest + i) and 0x1FFF)
            vram.setValue(vramAddr, byte)
        }

        hdmaSource += 0x10
        hdmaDest += 0x10

        val hdma5Offset = ReservedAddresses.HDMA5.memoryAddress - ReservedAddresses.JOYP.memoryAddress

        if (hdmaRemaining == 0) {
            hdmaActive = false
            bottomRegisters[hdma5Offset].value = 0xFFu
        } else {
            hdmaRemaining--
            bottomRegisters[hdma5Offset].value = hdmaRemaining.toUByte()
        }

        return true
    }

    override fun getValue(memoryAddress: Int): UByte {
        val addr = memoryAddress and 0xFFFF
        return when (addr) {
            in 0 until ReservedAddresses.SWITCH_ROM_END.memoryAddress -> {
                rom.getValue(addr)
            }

            in ReservedAddresses.SWITCH_ROM_END.memoryAddress until ReservedAddresses.VRAM_END.memoryAddress -> {
                if (ppuMode.writeVRAMAble()) {
                    vram.getValue(addr)
                } else {
                    0xFFu
                }
            }

            in ReservedAddresses.VRAM_END.memoryAddress until ReservedAddresses.ERAM_END.memoryAddress -> {
                val romModule = rom as RomModule
                if (romModule.ramStatus) {
                    if (romModule.hasRtcMapped) {
                        romModule.readRtcRegister()
                    } else {
                        eram?.getValue(addr) ?: 0x00u
                    }
                } else {
                    0x00u
                }
            }

            in ReservedAddresses.ERAM_END.memoryAddress until ReservedAddresses.WRAM_END.memoryAddress -> {
                wram.getValue(addr)
            }

            in ReservedAddresses.WRAM_END.memoryAddress until ReservedAddresses.OAM_START.memoryAddress -> {
                wram.getValue(addr - 0x2000)
            }

            in ReservedAddresses.OAM_START.memoryAddress until ReservedAddresses.OAM_END.memoryAddress -> {
                if (ppuMode.writeOAMAble()) {
                    oam.getValue(addr)
                } else {
                    0xFFu
                }
            }

            in ReservedAddresses.OAM_END.memoryAddress until ReservedAddresses.JOYP.memoryAddress -> {
                0x00u
            }

            else -> {
                getBottomRegisters(addr)
            }
        }
    }

    /**
     * This is a special method designed to be used by the PPU only for now and it provides free read
     * access to each and every memory location currently only bottomRegisters suffer this limitation
     *
     * @param memoryAddress memory location where to get value
     * @return value stored in given address
     */
    fun getPermanentRegister(memoryAddress: Int): MutableUByte = when (memoryAddress) {
        in ReservedAddresses.JOYP.memoryAddress..ReservedAddresses.IE.memoryAddress -> {
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress]
        }

        else -> error(REGISTER_DOES_NOT_EXIST)
    }

    /**
     * This method is responsible for handling the retrieval of values from the bottom registers,
     * according to all their quirks (these registers have special conditions that must be respected)
     *
     * @param memoryAddress memory location where value should be retrieved
     * @return value stored in given address
     */
    @Suppress("CyclomaticComplexMethod")
    private fun getBottomRegisters(memoryAddress: Int): UByte {
        if (memoryAddress == ReservedAddresses.JOYP.memoryAddress) {
            return bus.getJoypad(bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value)
        }

        if (memoryAddress == ReservedAddresses.IF.memoryAddress) {
            return (bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value.toInt() or 0xE0).toUByte()
        }

        if (memoryAddress in ReservedAddresses.NR10.memoryAddress..ReservedAddresses.WAVE_END.memoryAddress) {
            return bus.spu.readRegister(memoryAddress).toUByte()
        }

        // CGB palette data reads
        if (isCGB && memoryAddress == ReservedAddresses.BCPD.memoryAddress) {
            return (bus.cgbDrawer?.readBgPalette(bgPaletteIndex) ?: 0xFF).toUByte()
        }

        if (isCGB && memoryAddress == ReservedAddresses.OCPD.memoryAddress) {
            return (bus.cgbDrawer?.readObjPalette(objPaletteIndex) ?: 0xFF).toUByte()
        }

        return bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value
    }

    /**
     * This is a special method designed to be used by the PPU only and it provides free read access
     * to each and every memory location currently only bottomRegisters suffer this limitation
     * Used as an optimization to avoid checking every condition
     *
     * @param memoryAddress memory location where to get value
     * @return value stored in given address
     */
    fun getValueFromPPU(memoryAddress: Int): UByte = when (memoryAddress) {
        in ReservedAddresses.SWITCH_ROM_END.memoryAddress until ReservedAddresses.VRAM_END.memoryAddress -> {
            vram.getValue(memoryAddress)
        }

        in ReservedAddresses.OAM_START.memoryAddress until ReservedAddresses.OAM_END.memoryAddress -> {
            oam.getValue(memoryAddress)
        }

        in ReservedAddresses.JOYP.memoryAddress..ReservedAddresses.IE.memoryAddress -> {
            bottomRegisters[memoryAddress - ReservedAddresses.JOYP.memoryAddress].value
        }

        else -> {
            error("This should not be accessed here")
        }
    }

    /**
     * Reads a value from VRAM bank 1 directly, used by the CGB PPU drawer to fetch tile map
     * attributes. On DMG (single VRAM bank) this returns 0.
     *
     * @param memoryAddress VRAM address to read from bank 1
     * @return value stored in VRAM bank 1, or 0 if not CGB
     */
    fun getValueFromPPUBank1(memoryAddress: Int): UByte {
        if (!isCGB) return 0u
        if (memoryAddress !in ReservedAddresses.SWITCH_ROM_END.memoryAddress until ReservedAddresses.VRAM_END.memoryAddress) return 0u
        return vram.getValueFromBank(memoryAddress, 1)
    }

    /**
     * Reads a value from a specific VRAM bank, used by the CGB PPU drawer to fetch tile data
     * from the bank specified in the tile attribute byte. On DMG this reads from the only bank.
     *
     * @param memoryAddress VRAM address to read
     * @param bank VRAM bank number (0 or 1)
     * @return value stored in the specified VRAM bank
     */
    fun getValueFromPPUBank(memoryAddress: Int, bank: Int): UByte {
        if (!isCGB) return vram.getValue(memoryAddress)
        if (memoryAddress !in ReservedAddresses.SWITCH_ROM_END.memoryAddress until ReservedAddresses.VRAM_END.memoryAddress) return 0u
        return vram.getValueFromBank(memoryAddress, bank)
    }

    /**
     * Returns the currently active ROM bank number (for debug logging).
     */
    fun romActiveBank(): Int = rom.activeBank

    /**
     * Returns the currently active WRAM bank number (for debug logging).
     */
    fun wramActiveBank(): Int = wram.activeBank

    /**
     * Converts the full memory map into a readable string containing all the memory address' content
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
