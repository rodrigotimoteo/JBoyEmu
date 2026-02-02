package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH
import kotlin.collections.set
import kotlin.text.compareTo
import kotlin.text.get

class PPUDrawer(
    private val ppu: PPU,
    private val bus: Bus,
) {

    private val painting = IntArray(WIDTH * HEIGHT)

    internal fun drawBackground(tileMapAddress: Int, tileDataAddress: Int) {
        val tempY = (ppu.ppuRegisters.currentLine + ppu.ppuRegisters.scrollY) and 0xFF

        //System.out.println(currentLine + "  " + tempY);
        for (x in 0..159) {
            val tempX = (ppu.ppuRegisters.scrollX + x) % 0x100

            val address = tileMapAddress + ((tempY / 8) * 0x20)
            var tile = bus.getValue(address + (tempX) / 8).toInt()

            if (ppu.ppuRegisters.negativeTiles) {
                tile = if (((tile and 0x80) shr 7) == 0) {
                    tile and 0x7f
                } else {
                    (tile and 0x7f) - 0x80
                }
            }

            val tileLine: Int
            val i = tileDataAddress + ((tile and 0xff) * 0x10) + ((tempY % 8) * 2)
            tileLine = if (ppu.ppuRegisters.negativeTiles) {
                if (((tile and 0x80) shr 7) == 0) {
                    i
                } else {
                    ReservedAddresses.TILE_DATA_1.memoryAddress + (((tile and 0xff) - 128) * 0x10) + ((tempY % 8) * 2)
                }
            } else i

            val offset = 7 - (tempX % 8)
            val colorNum = (((bus.getValue(tileLine).toInt() and (1 shl offset)) shr offset) +
                    (((bus.getValue(tileLine + 1).toInt() and (1 shl offset)) shr offset) * 2))
            val color = decodeColor(
                colorNum,
                bus.getValue(ReservedAddresses.BGP.memoryAddress).toInt()
            )

            painting[x * WIDTH][currentLine] = color
        }
    }

    internal fun drawWindow(tileMapAddress: Int, tileDataAddress: Int) {
        if (ppu.ppuRegisters.windowY < 0 || ppu.ppuRegisters.windowX > 166 ||
            ppu.ppuRegisters.currentLine < ppu.ppuRegisters.windowY
        ) {
            return
        }

        val tempY = ppu.ppuRegisters.currentLineWindow
        for (x in 0..159) {
            val tempX: Int
            if (x < ppu.ppuRegisters.windowX) continue
            else tempX = x - ppu.ppuRegisters.windowX

            val address = tileMapAddress + ((tempY / 8) * 0x20)
            var tile = bus.getValue(address + (tempX) / 8).toInt()

            if (ppu.ppuRegisters.negativeTiles) {
                tile = if (((tile and 0x80) shr 7) == 0) tile and 0x7f
                else (tile and 0x7f) - 0x80
            }

            val tileLine = if (ppu.ppuRegisters.negativeTiles) {
                if (((tile and 0x80) shr 7) == 0) {
                    tileDataAddress + ((tile and 0xff) * 0x10) + ((ppu.ppuRegisters.currentLine % 8) * 2)
                } else {
                    ReservedAddresses.TILE_DATA_1.memoryAddress + (((tile and 0xff) - 128) * 0x10) +
                            ((ppu.ppuRegisters.currentLine % 8) * 2)
                }
            } else {
                tileDataAddress + ((tile and 0xff) * 0x10) + ((ppu.ppuRegisters.currentLine % 8) * 2)
            }

            val offset = 7 - (tempX % 8)
            val colorNum =
                (((bus.getValue(tileLine).toInt() and (1 shl offset)) shr offset) +
                        (((bus.getValue(tileLine + 1).toInt() and (1 shl offset)) shr offset) * 2))
            val color = decodeColor(
                colorNum,
                bus.getValue(ReservedAddresses.BGP.memoryAddress).toInt()
            )

            painting[x]!![currentLine] = color
        }

        ppu.ppuRegisters.currentLineWindow++
    }

    internal fun drawSprite() {
        var tempY: Int
        var tempX: Int
        var tile: Int
        var attributesAddress: Int
        val drawnX = IntArray(10)

        var drawnSprites = 0
        val spriteOffset = if (ppu.ppuRegisters.spriteSize) 16 else 8

        var spriteNumber = 0
        while (spriteNumber < 40 && drawnSprites < 10) {

            tempY = bus.getValue(ReservedAddresses.OAM_START.memoryAddress + (spriteNumber * 4))
                .toInt() - 16
            tempX = bus.getValue(ReservedAddresses.OAM_START.memoryAddress + (spriteNumber * 4) + 1)
                .toInt() - 8
            tile = bus.getValue(ReservedAddresses.OAM_START.memoryAddress + (spriteNumber * 4) + 2)
                .toInt()

            var quit = false
            if (drawnSprites > 1) for (x in drawnX) if (x == tempX) quit = true
            if (quit) {
                spriteNumber++
                continue
            }

            if (spriteOffset == 16) tile = tile and 0xfe

            attributesAddress = ReservedAddresses.OAM_START.memoryAddress + (spriteNumber * 4) + 3

            if ((ppu.ppuRegisters.currentLine >= tempY) && (ppu.ppuRegisters.currentLine < (tempY + spriteOffset))) {
                val attributes = bus.getValue(attributesAddress)
                val priority: Boolean = attributes.testBit(7)
                val yFlipped: Boolean = attributes.testBit(6)
                val xFlipped: Boolean = attributes.testBit(5)

                val paletteAddress = if (attributes.testBit(4)) {
                    ReservedAddresses.OBP1.memoryAddress
                } else {
                    ReservedAddresses.OBP0.memoryAddress
                }

                val palette: Int = bus.getValue(paletteAddress).toInt()

                val tileLine = spriteOffset - (ppu.ppuRegisters.currentLine - tempY)
                val offset: Int = if (!yFlipped) {
                    2 * (ppu.ppuRegisters.currentLine - tempY)
                } else {
                    2 * (tileLine - 1)
                }

                val pixelDataAddress =
                    ReservedAddresses.SWITCH_ROM_END.memoryAddress + tile * 16 + offset

                for (pixelPrinted in 0..7) {
                    if (tempX + pixelPrinted < 0 || tempX + pixelPrinted >= 160) continue
                    if (priority && painting[tempX + pixelPrinted]!![currentLine] > 0) continue

                    val x = if (xFlipped) pixelPrinted else 7 - pixelPrinted
                    val colorNum: Int =
                        ((bus.getValue(pixelDataAddress).toInt() and (1 shl x)) shr x) +
                                (((bus.getValue(pixelDataAddress + 1)
                                    .toInt() and (1 shl x)) shr x) * 2)
                    val color = decodeColor(colorNum, palette)

                    if ((tempX + pixelPrinted < 160) && (tempX + pixelPrinted >= 0) && (colorNum != 0)) {
                        painting[tempX + pixelPrinted]!![currentLine] = color
                    }
                }

                drawnX[drawnSprites] = tempX
                drawnSprites++
            }
            spriteNumber++
        }
    }

    private fun decodeColor(index: Int, palette: Int): Byte {
        val colors = ByteArray(4)
        colors[3] = (((palette and 0x80) shr 6) + ((palette and 0x40) shr 6)).toByte()
        colors[2] = (((palette and 0x20) shr 4) + ((palette and 0x10) shr 4)).toByte()
        colors[1] = (((palette and 0x08) shr 2) + ((palette and 0x04) shr 2)).toByte()
        colors[0] = ((palette and 0x02) + (palette and 0x01)).toByte()

        return colors[index]
    }


    fun requestRepaint() {
        ppu.if (!lcdOn) display!!.drawBlankImage()
        display.repaint()
    }
}