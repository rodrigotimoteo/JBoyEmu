package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.DisplayFrame
import com.github.rodrigotimoteo.kboyemucore.DisplayPanel
import com.github.rodrigotimoteo.kboyemucore.cpu.OldCPU
import com.github.rodrigotimoteo.kboyemucore.memory.Memory
import kotlin.and

class PPU(
    private val cpu: OldCPU,
    private val memory: Memory,
) {
//
//    private var displayFrame: DisplayFrame? = null
//    private var display: DisplayPanel? = null
//
//    private val HBLANK = 0
//    private val VBLANK = 1
//    private val OAM = 2
//    private val PIXEL_TRANSFER = 3
//
//    private val TILE_DATA_0 = 0x8000
//    private val TILE_DATA_1 = 0x8800
//    private val TILE_DATA_2 = 0x9000
//
//    private val TILE_MAP_0 = 0x9800
//    private val TILE_MAP_1 = 0x9c00
//
//    private val OAM_START = 0xfe00
//    private val LCDC_CONTROL = 0xff40
//    private val LCDC_STATUS = 0xff41
//    private val SCROLL_Y_REGISTER = 0xff42
//    private val SCROLL_X_REGISTER = 0xff43
//    private val LY_REGISTER = 0xff44
//    private val LYC_REGISTER = 0xff45
//    private val BG_PALETTE = 0xff47
//    private val OBJECT_PALETTE_0 = 0xff48
//    private val OBJECT_PALETTE_1 = 0xff49
//    private val WINDOW_Y_REGISTER = 0xff4a
//    private val WINDOW_X_REGISTER = 0xff4b
//
//    private val VBLANK_INTERRUPT = 0
//    private val STAT_INTERRUPT = 1
//
//    private var counter = 0
//
//    private var mode = 0
//    private var currentLine = 0
//    private var currentLineWindow = 0
//
//    private var scrollY = 0
//    private var scrollX = 0
//
//    private var windowX = 0
//    private var windowY = 0
//
//    private val painting = Array<ByteArray?>(160) { ByteArray(144) }
//    private var getToSleep = false
//
//    private var lcdOn = false
//    private var windowOn = false
//    private var backgroundOn = false
//    private var windowTileMap = false
//    private var tileData = false
//    private var backgroundTileMap = false
//    private var spriteSize = false
//    private var spriteOn = false
//    private var cgb = false
//
//
//    private var negativeTiles = false
//
//
//    //Getters
//    fun getLcdOn(): Boolean {
//        return lcdOn
//    }
//
//    fun getCounter(): Int {
//        return counter
//    }
//
//    fun isGetToSleep(): Boolean {
//        return getToSleep
//    }
//
//
//    //Setters
//    fun setDisplayFrame(display: DisplayFrame?) {
//        displayFrame = display
//    }
//
//    fun setDisplayPanel(display: DisplayPanel) {
//        this.display = display
//    }
//
//    fun setCgbMode() {
//        cgb = true
//        display!!.setCgbMode()
//    }
//
//    fun setScrolls() {
//        this.scrollX = readScrollX().code
//        this.scrollY = readScrollY().code
//    }
//
//    fun reset() {
//        counter = 0
//    }
//
//    fun cycle() {
//        readLCDControl()
//        readLCDStatus()
//        draw()
//    }
//
//    fun readLCDControl() {
//        var bit: Int
//        val LCDControl: Char = memory.getMemory(LCDC_CONTROL)
//
//        //Read LCD and main.kotlin.PPU enabled bit
//        bit = (LCDControl.code and 0x80) shr 7
//        lcdOn = bit == 1
//        memory.setLcdOn(lcdOn)
//
//        //Read Window Tile Map (where window tiles are located if enabled)
//        bit = (LCDControl.code and 0x40) shr 6
//        windowTileMap = bit == 1
//
//        //Read Window and Background Tile Data
//        bit = (LCDControl.code and 0x10) shr 4
//        tileData = bit == 1
//
//        //Read Background tile Area
//        bit = (LCDControl.code and 0x08) shr 3
//        backgroundTileMap = bit == 1
//
//        //Read Sprite Size (0 = 8x8, 1 = 8x16)
//        bit = (LCDControl.code and 0x04) shr 2
//        spriteSize = bit == 1
//
//        //Read Sprite Enabled Status
//        bit = (LCDControl.code and 0x02) shr 1
//        spriteOn = bit == 1
//
//        //Read Background and window Enabled Status
//        bit = LCDControl.code and 0x01
//        backgroundOn = bit == 1
//        windowOn = bit == 1
//
//        //Read Window Enabled state
//        bit = (LCDControl.code and 0x20) shr 5
//        windowOn = windowOn && bit == 1
//    }
//
//    private fun readLCDStatus() {
//        val bit: Int
//
//        val LCD: Char = memory.getMemory(LCDC_STATUS)
//        //Read bit 1 and 0
//        bit = LCD.code and 0x03
//        mode = bit
//        memory.setPpuMode(mode)
//    }
//
//    private fun readWindow() {
//        this.windowY = memory.getMemory(WINDOW_Y_REGISTER)
//        this.windowX = memory.getMemory(WINDOW_X_REGISTER) - 7
//    }
//
//    fun readScrollY(): Char {
//        return memory.getMemory(SCROLL_Y_REGISTER)
//    }
//
//    fun readScrollX(): Char {
//        return memory.getMemory(SCROLL_X_REGISTER)
//    }
//
//    fun readLY(): Char {
//        return memory.getMemory(LY_REGISTER)
//    }
//
//    private fun treatLYC(): Boolean {
//        val lyc: Int = memory.getMemory(LYC_REGISTER)
//
//        if (currentLine == lyc) {
//            memory.setBit(LCDC_STATUS, 2)
//            return (memory.getMemory(LCDC_STATUS) and 0x40) !== 0
//        } else {
//            memory.resetBit(LCDC_STATUS, 2)
//        }
//
//        return false
//    }
//
//    private fun changeMode(mode: Int) {
//        var lcdStatus: Int = memory.getMemory(LCDC_STATUS) and 0xfc
//        var requestInterrupt = false
//
//        when (mode) {
//            HBLANK -> {
//                if ((lcdStatus and 0x08) != 0) requestInterrupt = true
//            }
//
//            VBLANK -> {
//                lcdStatus = lcdStatus or 0x01
//                if ((lcdStatus and 0x10) != 0) requestInterrupt = true
//            }
//
//            OAM -> {
//                lcdStatus = lcdStatus or 0x02
//                if ((lcdStatus and 0x20) != 0) requestInterrupt = true
//            }
//
//            PIXEL_TRANSFER -> {
//                lcdStatus = lcdStatus or 0x03
//            }
//        }
//        memory.writePriv(LCDC_STATUS, lcdStatus.toChar())
//        //System.out.println(currentLine + "   " + Integer.toHexString(memory.getMemory(LYC_REGISTER)));
//        val lycInterrupt = treatLYC()
//        if (requestInterrupt || lycInterrupt) cpu!!.setInterrupt(STAT_INTERRUPT)
//    }
//
//    var lol: Int = 0
//
//    private fun draw() {
//        counter++
//
//        currentLine = readLY().code
//
//        when (mode) {
//            0 -> { //H-BLANK
//                if (counter == 114) {
//                    counter = 0
//                    currentLine++
//                    memory.writePriv(LY_REGISTER, currentLine.toChar())
//                    if (currentLine > 143) {
//                        display!!.drawImage(painting)
//                        lol++
//                        requestRepaint()
//                        changeMode(VBLANK)
//                        cpu!!.setInterrupt(VBLANK_INTERRUPT)
//                    } else changeMode(OAM)
//                }
//            }
//
//            1 -> { //V-BLANK
//                if (counter == 114) {
//                    counter = 0
//                    currentLine++
//                    memory.writePriv(LY_REGISTER, currentLine.toChar())
//                    if (currentLine > 153) {
//                        currentLine = 0
//                        currentLineWindow = 0
//                        memory.writePriv(LY_REGISTER, currentLine.toChar())
//                        changeMode(OAM)
//                        getToSleep = true
//                    }
//                }
//            }
//
//            2 -> { //OAM Search
//                if (getToSleep) getToSleep = false
//                if (counter == 20) {
//                    changeMode(PIXEL_TRANSFER)
//                }
//            }
//
//            3 -> { //Pixel Transfer
//                if (counter == 63) {
//                    val backgroundMapAddress: Int
//                    val windowMapAddress: Int
//                    windowMapAddress = if (windowTileMap) TILE_MAP_1 else TILE_MAP_0
//                    backgroundMapAddress = if (backgroundTileMap) TILE_MAP_1 else TILE_MAP_0
//
//                    val tileDataAddress = if (tileData) TILE_DATA_0 else TILE_DATA_2
//
//                    if (tileDataAddress == TILE_DATA_2) negativeTiles = true
//
//
//                    //System.out.println(currentLine + "  " + Integer.toHexString(backgroundMapAddress));
////                    System.out.println(scrollX + " " + scrollY + "  " + cpu.getIsHalted());
//                    setScrolls()
//                    readWindow()
//                    if (backgroundOn) drawBackground(backgroundMapAddress, tileDataAddress)
//                    if (windowOn) drawWindow(windowMapAddress, tileDataAddress)
//                    if (spriteOn) drawSprite()
//
//                    changeMode(HBLANK)
//                }
//            }
//        }
//    }
//
//    private fun drawBackground(tileMapAddress: Int, tileDataAddress: Int) {
//        val tempY = (currentLine + scrollY) and 0xff
//
//        //System.out.println(currentLine + "  " + tempY);
//        for (x in 0..159) {
//            val tempX = (scrollX + x) % 0x100
//
//            val address = tileMapAddress + ((tempY / 8) * 0x20)
//            var tile: Int = memory.getMemoryPriv(address + (tempX) / 8)
//
//            if (negativeTiles) {
//                if (((tile and 0x80) shr 7) == 0) tile = tile and 0x7f
//                else tile = (tile and 0x7f) - 0x80
//            }
//
//            val tileLine: Int
//            val i = tileDataAddress + ((tile and 0xff) * 0x10) + ((tempY % 8) * 2)
//            if (negativeTiles) {
//                if (((tile and 0x80) shr 7) == 0) tileLine = i
//                else tileLine = TILE_DATA_1 + (((tile and 0xff) - 128) * 0x10) + ((tempY % 8) * 2)
//            } else tileLine = i
//
//            val offset = 7 - (tempX % 8)
//            val color_num =
//                ((((memory.getMemoryPriv(tileLine) and (1 shl offset)) shr offset) + (((memory.getMemoryPriv(
//                    tileLine + 1
//                ) and (1 shl offset)) shr offset) * 2)) as Byte).toInt()
//            val color = decodeColor(color_num, memory.getMemoryPriv(BG_PALETTE))
//            painting[x]!![currentLine] = color
//        }
//    }
//
//    private fun drawWindow(tileMapAddress: Int, tileDataAddress: Int) {
//        if (windowY < 0 || windowX > 166 || currentLine < windowY) {
//            return
//        }
//
//        val tempY = currentLineWindow
//        for (x in 0..159) {
//            val tempX: Int
//            if (x < windowX) continue
//            else tempX = x - windowX
//
//            val address = tileMapAddress + ((tempY / 8) * 0x20)
//            var tile: Int = memory.getMemoryPriv(address + (tempX) / 8)
//
//            if (negativeTiles) {
//                if (((tile and 0x80) shr 7) == 0) tile = tile and 0x7f
//                else tile = (tile and 0x7f) - 0x80
//            }
//
//            val tileLine: Int
//            if (negativeTiles) {
//                if (((tile and 0x80) shr 7) == 0) tileLine =
//                    tileDataAddress + ((tile and 0xff) * 0x10) + ((currentLine % 8) * 2)
//                else tileLine =
//                    TILE_DATA_1 + (((tile and 0xff) - 128) * 0x10) + ((currentLine % 8) * 2)
//            } else tileLine = tileDataAddress + ((tile and 0xff) * 0x10) + ((currentLine % 8) * 2)
//
//            val offset = 7 - (tempX % 8)
//            val color_num =
//                ((((memory.getMemoryPriv(tileLine) and (1 shl offset)) shr offset) + (((memory.getMemoryPriv(
//                    tileLine + 1
//                ) and (1 shl offset)) shr offset) * 2)) as Byte).toInt()
//            val color = decodeColor(color_num, memory.getMemoryPriv(BG_PALETTE))
//            painting[x]!![currentLine] = color
//        }
//
//        currentLineWindow++
//    }
//
//    private fun drawSprite() {
//        var tempY: Int
//        var tempX: Int
//        var tile: Int
//        var attributesAddress: Int
//        val drawnX = IntArray(10)
//
//        var drawnSprites = 0
//        val spriteOffset = if (this.spriteSize) 16 else 8
//
//        var spriteNumber = 0
//        while (spriteNumber < 40 && drawnSprites < 10) {
//            tempY = memory.getMemoryPriv(OAM_START + (spriteNumber * 4)) - 16
//            tempX = memory.getMemoryPriv(OAM_START + (spriteNumber * 4) + 1) - 8
//            tile = memory.getMemoryPriv(OAM_START + (spriteNumber * 4) + 2)
//
//            var quit = false
//            if (drawnSprites > 1) for (x in drawnX) if (x == tempX) quit = true
//            if (quit) {
//                spriteNumber++
//                continue
//            }
//
//            if (spriteOffset == 16) tile = tile and 0xfe
//
//            attributesAddress = OAM_START + (spriteNumber * 4) + 3
//
//            if ((currentLine >= tempY) && (currentLine < (tempY + spriteOffset))) {
//                val priority: Boolean = memory.testBit(attributesAddress, 7)
//                val yFlipped: Boolean = memory.testBit(attributesAddress, 6)
//                val xFlipped: Boolean = memory.testBit(attributesAddress, 5)
//                val paletteAddress =
//                    if (memory.testBit(attributesAddress, 4)) OBJECT_PALETTE_1 else OBJECT_PALETTE_0
//                val palette: Int = memory.getMemoryPriv(paletteAddress)
//
//                val tileLine = spriteOffset - (currentLine - tempY)
//
//                val offset: Int
//                if (!yFlipped) offset = 2 * (currentLine - tempY)
//                else offset = 2 * (tileLine - 1)
//
//                val pixelDataAddress = TILE_DATA_0 + tile * 16 + offset
//
//                for (pixelPrinted in 0..7) {
//                    if (tempX + pixelPrinted < 0 || tempX + pixelPrinted >= 160) continue
//                    if (priority && painting[tempX + pixelPrinted]!![currentLine] > 0) continue
//
//                    val x = if (xFlipped) pixelPrinted else 7 - pixelPrinted
//                    val color_num: Int =
//                        ((memory.getMemoryPriv(pixelDataAddress) and (1 shl x)) shr x) + (((memory.getMemoryPriv(
//                            pixelDataAddress + 1
//                        ) and (1 shl x)) shr x) * 2)
//                    val color = decodeColor(color_num, palette)
//
//                    if ((tempX + pixelPrinted < 160) && (tempX + pixelPrinted >= 0) && (color_num != 0)) {
//                        painting[tempX + pixelPrinted]!![currentLine] = color
//                    }
//                }
//
//                drawnX[drawnSprites] = tempX
//                drawnSprites++
//            }
//            spriteNumber++
//        }
//    }
//
//    private fun decodeColor(index: Int, palette: Int): Byte {
//        val colors = ByteArray(4)
//        colors[3] = (((palette and 0x80) shr 6) + ((palette and 0x40) shr 6)).toByte()
//        colors[2] = (((palette and 0x20) shr 4) + ((palette and 0x10) shr 4)).toByte()
//        colors[1] = (((palette and 0x08) shr 2) + ((palette and 0x04) shr 2)).toByte()
//        colors[0] = ((palette and 0x02) + (palette and 0x01)).toByte()
//
//        return colors[index]
//    }
//
//
//    fun requestRepaint() {
//        if (!lcdOn) display!!.drawBlankImage()
//        display.repaint()
//    }
}