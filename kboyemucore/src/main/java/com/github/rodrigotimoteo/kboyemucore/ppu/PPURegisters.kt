package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses

class PPURegisters(
    private val ppu: PPU,
    private val bus: Bus,
) {

    /**
     * Counts the amount of cycles the PPU has performed (this value normally oscillates between 0
     * and 114 because that's the maximum cycles a [PPUModes] can have
     */
    internal var counter = 0

    internal var currentLine = 0
    internal var currentLineWindow = 0

    private var _scrollY = 0
    internal val scrollY = _scrollY

    private var _scrollX = 0
    internal val scrollX = _scrollX

    private var _windowX = 0
    internal val windowX = _windowX

    private var _windowY = 0
    internal val windowY = _windowY

    private var _lcdOn: Boolean = false
    internal val lcdOn = _lcdOn

    private var _windowOn: Boolean = false
    internal val windowOn = _windowOn

    private var _backgroundOn: Boolean = false
    internal val backgroundOn = _backgroundOn

    private var _windowTileMap: Boolean = false
    internal val windowTileMap = _windowTileMap

    private var _tileData: Boolean = false
    internal val tileData = _tileData

    private var _backgroundTileMap: Boolean = false
    internal val backgroundTileMap = _backgroundTileMap

    private var _spriteSize: Boolean = false
    internal val spriteSize = _spriteSize

    private var _spriteOn: Boolean = false
    internal val spriteOn = _spriteOn

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
