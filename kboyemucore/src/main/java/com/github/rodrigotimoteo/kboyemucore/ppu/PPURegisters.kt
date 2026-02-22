package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.ktx.resetBit
import com.github.rodrigotimoteo.kboyemucore.ktx.setBit
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses

class PPURegisters(
    private val bus: Bus,
) {

    /**
     * Current [PPUModes] the PPU is running on
     */
    internal var mode = PPUModes.HBLANK

    /**
     * Counts the amount of cycles the PPU has performed (this value normally oscillates between 0
     * and 114 because that's the maximum cycles a [PPUModes] can have
     */
    internal var counter = 0

    internal var currentLine = 0
    internal var currentLineWindow = 0

    private var _scrollY = bus.getPermanentRegister(ReservedAddresses.SCY.memoryAddress)
    internal val scrollY get() = _scrollY.value.toInt()

    private var _scrollX = bus.getPermanentRegister(ReservedAddresses.SCX.memoryAddress)
    internal val scrollX get() = _scrollX.value.toInt()

    private val _windowX = bus.getPermanentRegister(ReservedAddresses.WX.memoryAddress)
    internal val windowX get() = (_windowX.value.toInt() - 7) and 0xFF

    private val _windowY = bus.getPermanentRegister(ReservedAddresses.WY.memoryAddress)
    internal val windowY get() = _windowY.value.toInt()

    private var _lcdOn: Boolean = false
    internal val lcdOn get() = _lcdOn

    private var _windowOn: Boolean = false
    internal val windowOn get() = _windowOn

    private var _backgroundOn: Boolean = false
    internal val backgroundOn get() = _backgroundOn

    private var _windowTileMap: Boolean = false
    internal val windowTileMap get() = _windowTileMap

    private var _tileData: Boolean = false
    internal val tileData get() = _tileData

    private var _backgroundTileMap: Boolean = false
    internal val backgroundTileMap get() = _backgroundTileMap

    private var _spriteSize: Boolean = false
    internal val spriteSize get() = _spriteSize

    private var _spriteOn: Boolean = false
    internal val spriteOn get() = _spriteOn

    /**
     * This variable is used to determine whether the tile data should be treated as signed or unsigned,
     * this is determined by the value of the tile data bit in the LCDC register
     */
    internal var negativeTiles = false

    /** Always has the value at the [ReservedAddresses.LCDC] memory address */
    private val lcdcRegister = bus.getPermanentRegister(ReservedAddresses.LCDC.memoryAddress)

    /** Always has the value at the [ReservedAddresses.STAT] memory address */
    internal val statRegister = bus.getPermanentRegister(ReservedAddresses.STAT.memoryAddress)

    /** Always has the value at the [ReservedAddresses.LY] memory address */
    internal val lyRegister = bus.getPermanentRegister(ReservedAddresses.LY.memoryAddress)

    /** Always has the value at the [ReservedAddresses.LYC] memory address */
    internal val lycRegister = bus.getPermanentRegister(ReservedAddresses.LYC.memoryAddress)

    /** Always has the value at the [ReservedAddresses.LYC] memory address */
    internal val bgpRegister = bus.getPermanentRegister(ReservedAddresses.BGP.memoryAddress)

    /** Always has the value at the [ReservedAddresses.OBP0] memory address */
    internal val obp0Register = bus.getPermanentRegister(ReservedAddresses.OBP0.memoryAddress)

    /** Always has the value at the [ReservedAddresses.OBP1] memory address */
    internal val obp1Register = bus.getPermanentRegister(ReservedAddresses.OBP1.memoryAddress)

    /**
     * Reads the content of [ReservedAddresses.LCDC] and translates it into easily accessible flags
     */
    fun readLCDControl() {
        val lcdcValue = lcdcRegister

        //Read LCD and main.kotlin.PPU enabled bit
        _lcdOn = lcdcValue.testBit(7)
        // memory needs to be aware of status of lcdOn to determine what to return in some memory accesses
        // memory.setLcdOn(lcdOn)

        //Read Window Tile Map (where window tiles are located if enabled)
        _windowTileMap = lcdcValue.testBit(6)

        //Read Window Enabled state
        _windowOn = lcdcValue.testBit(5)

        //Read Window and Background Tile Data
        _tileData = lcdcValue.testBit(4)

        //Read Background tile Area
        _backgroundTileMap = lcdcValue.testBit(3)

        //Read Sprite Size (0 = 8x8, 1 = 8x16)
        _spriteSize = lcdcValue.testBit(2)

        //Read Sprite Enabled Status
        _spriteOn = lcdcValue.testBit(1)

        //Read Background and window Enabled Status
        _backgroundOn = lcdcValue.testBit(0)

        if (bus.isCGB) {

        } else {
            _windowOn = _windowOn && _backgroundOn
        }
    }

    /**
     * Updates the current [PPUModes] based on the value stored in [ReservedAddresses.LCDC]
     */
    internal fun readLCDStatus() {
        val modeBits = statRegister.value.toInt() and 0x03
        mode = when (modeBits) {
            PPUModes.HBLANK.bit -> PPUModes.HBLANK
            PPUModes.VBLANK.bit -> PPUModes.VBLANK
            PPUModes.OAM.bit -> PPUModes.OAM
            PPUModes.PIXEL_TRANSFER.bit -> PPUModes.PIXEL_TRANSFER
            else -> return
        }
    }

    /**
     * Compares the current line with the value stored in the LYC register. Sets bit 2 of STAT
     * when LY==LYC and clears it otherwise. The STAT interrupt is edge-triggered: it fires only
     * on the 0→1 transition of bit 2, not on every subsequent call while the match holds.
     *
     * @return true if a STAT LYC interrupt should be requested, false otherwise
     */
    internal fun treatLYC(): Boolean {
        if (currentLine == lycRegister.value.toInt()) {
            val alreadySet = (statRegister.value.toInt() and 0x04) != 0
            statRegister.value = statRegister.value.setBit(2)
            if (alreadySet) return false
            return (statRegister.value.toInt() and 0x40) != 0
        } else {
            statRegister.value = statRegister.value.resetBit(2)
        }

        return false
    }

    /**
     * Reads the LY register and assigns it to currentLine variable
     */
    fun readLY() {
        currentLine = lyRegister.value.toInt()
    }
}
