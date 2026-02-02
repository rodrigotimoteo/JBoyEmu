package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.ktx.resetBit
import com.github.rodrigotimoteo.kboyemucore.ktx.setBit
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses

class PPURegisters(
    private val ppu: PPU,
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

    private var _scrollY = 0
    internal val scrollY get() = _scrollY

    private var _scrollX = 0
    internal val scrollX get() = _scrollX

    private var _windowX = 0
    internal val windowX get() = _windowX

    private var _windowY = 0
    internal val windowY get() = _windowY

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

    internal var negativeTiles = false

    /**
     * Reads the content of [ReservedAddresses.LCDC] and translates it into easily accessible flags
     */
    fun readLCDControl() {
        val lcdcValue = bus.getValue(ReservedAddresses.LCDC.memoryAddress)

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
        _windowOn = lcdcValue.testBit(0)
    }

    /**
     * Updates the current [PPUModes] based on the value stored in [ReservedAddresses.LCDC]
     */
    internal fun readLCDStatus() {
        mode = PPUModes.entries.find { it ->
            bus.getValue(ReservedAddresses.LCDC.memoryAddress).toInt() and 0x03 == it.bit
        } ?: return

        // Needs to be addressed later
        // memory.setPpuMode(mode)
    }

    /**
     * Updates the [windowY] and [windowX] value by getting them from their respective registers
     */
    internal fun readWindow() {
        _windowY = bus.getValue(ReservedAddresses.WY.memoryAddress).toInt()
        _windowX = (bus.getValue(ReservedAddresses.WX.memoryAddress).toInt() - 7)
    }

    /**
     * TODO NEEDS TO CHECK THIS DOCUMENTATION
     */
    internal fun treatLYC(): Boolean {
        val lyc = bus.getValue(ReservedAddresses.LYC.memoryAddress)

        if (currentLine == lyc.toInt()) {
            bus.setValueFromPPU(ReservedAddresses.LYC.memoryAddress, lyc.setBit(2))
            return (bus.getValue(ReservedAddresses.LCDC.memoryAddress).toInt() and 0x40) != 0
        } else {
            bus.setValueFromPPU(ReservedAddresses.LYC.memoryAddress, lyc.resetBit(2))
        }

        return false
    }

    /**
     * Sets both [scrollX] and [scrollY] to the value stored in their respective registers
     */
    fun setScrolls() {
        readScrollX()
        readScrollY()
    }

    /**
     * Reads and sets the SCY register to [scrollY]
     */
    private fun readScrollY() {
        _scrollY = bus.getValue(ReservedAddresses.SCY.memoryAddress).toInt()
    }

    /**
     * Reads and sets the SCX register to [scrollX]
     */
    private fun readScrollX() {
        _scrollY = bus.getValue(ReservedAddresses.SCX.memoryAddress).toInt()
    }

    /**
     * Reads the LY register and assigns it to currentLine variable
     */
    fun readLY() {
        currentLine = bus.getValue(ReservedAddresses.LY.memoryAddress).toInt()
    }

}
