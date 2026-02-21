package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH

class PPUDrawer(
    private val ppu: PPU,
    private val bus: Bus,
) {

    /**
     * Holds the temporary [ByteArray] used to update the screen when an update is triggered
     */
    private val painting = ByteArray(WIDTH * HEIGHT)

    /**
     * Pre-decoded palette caches - decoded once per scanline, reused per pixel
     */
    private val bgPalette = ByteArray(4)
    private val obp0Palette = ByteArray(4)
    private val obp1Palette = ByteArray(4)

    /**
     * Decodes a Game Boy palette register into 4 color values and stores them in [dest]
     */
    private fun decodePalette(palette: Int, dest: ByteArray) {
        dest[0] = ((palette and 0x03)).toByte()
        dest[1] = (((palette shr 2) and 0x03)).toByte()
        dest[2] = (((palette shr 4) and 0x03)).toByte()
        dest[3] = (((palette shr 6) and 0x03)).toByte()
    }

    /**
     * Draws the background part of the screen based on specification provided by the GameBoy PPU
     */
    internal fun drawBackground(tileMapAddress: Int, tileDataAddress: Int) {
        val tempY = (ppu.ppuRegisters.currentLine + ppu.ppuRegisters.scrollY) and 0xFF

        decodePalette(ppu.ppuRegisters.bgpRegister.value.toInt(), bgPalette)

        for (x in 0 until WIDTH) {
            val tempX = (ppu.ppuRegisters.scrollX + x) % 0x0100

            val address = tileMapAddress + ((tempY / 8) * 0x20)
            var tile = bus.getValueFromPPU(address + (tempX) / 8).toInt()

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
            val colorNum =
                (((bus.getValueFromPPU(tileLine).toInt() and (1 shl offset)) shr offset) +
                        (((bus.getValueFromPPU(tileLine + 1)
                            .toInt() and (1 shl offset)) shr offset) * 2))

            painting[ppu.ppuRegisters.currentLine * WIDTH + x] = bgPalette[colorNum]
        }
    }

    /**
     * Draws the window part of the screen based on specification provided by the GameBoy PPU
     */
    internal fun drawWindow(tileMapAddress: Int, tileDataAddress: Int) { // NOSONAR
        val tempY = ppu.ppuRegisters.currentLineWindow

        // WY condition: window not yet reached
        if (ppu.ppuRegisters.currentLine < ppu.ppuRegisters.windowY) {
            return
        }

        // WX >= 167 raw (windowX = WX-7 >= 160) means window is fully off-screen —
        // no pixels drawn and WLY does NOT increment, per hardware behaviour.
        if (ppu.ppuRegisters.windowX >= WIDTH) {
            return
        }

        decodePalette(ppu.ppuRegisters.bgpRegister.value.toInt(), bgPalette)

        for (x in 0 until WIDTH) {
            if (x < ppu.ppuRegisters.windowX) continue

            val tempX = x - ppu.ppuRegisters.windowX

            val tile = bus.getValueFromPPU(tileMapAddress + ((tempY / 8) * 0x20) + (tempX / 8)).toInt()
            val tileLine = if (ppu.ppuRegisters.negativeTiles) {
                val signedTile = tile.toByte().toInt()
                if (signedTile >= 0) {
                    tileDataAddress + signedTile * 0x10 + (tempY % 8) * 2
                } else {
                    ReservedAddresses.TILE_DATA_1.memoryAddress + (signedTile + 128) * 0x10 + (tempY % 8) * 2
                }
            } else {
                tileDataAddress + tile * 0x10 + (tempY % 8) * 2
            }

            val offset = 7 - (tempX % 8)
            val colorNum =
                (((bus.getValueFromPPU(tileLine).toInt() and (1 shl offset)) shr offset) +
                        (((bus.getValueFromPPU(tileLine + 1)
                            .toInt() and (1 shl offset)) shr offset) * 2))

            painting[ppu.ppuRegisters.currentLine * WIDTH + x] = bgPalette[colorNum]
        }

        ppu.ppuRegisters.currentLineWindow++
    }

    /**
     * Draws the sprite part of the screen based on specification provided by the GameBoy PPU
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
    internal fun drawSprite() { // NOSONAR
        val drawnX = IntArray(10)

        decodePalette(ppu.ppuRegisters.obp0Register.value.toInt(), obp0Palette)
        decodePalette(ppu.ppuRegisters.obp1Register.value.toInt(), obp1Palette)

        var drawnSprites = 0
        val spriteOffset = if (ppu.ppuRegisters.spriteSize) 16 else 8

        var spriteNumber = 0
        while (spriteNumber < 40 && drawnSprites < 10) {
            val tempY =
                bus.getValueFromPPU(ReservedAddresses.OAM_START.memoryAddress + (spriteNumber * 4))
                    .toInt() - 16
            val tempX =
                bus.getValueFromPPU(ReservedAddresses.OAM_START.memoryAddress + (spriteNumber * 4) + 1)
                    .toInt() - 8
            var tile =
                bus.getValueFromPPU(ReservedAddresses.OAM_START.memoryAddress + (spriteNumber * 4) + 2)
                    .toInt()

            var quit = false
            if (drawnSprites > 1) for (x in drawnX) if (x == tempX) quit = true
            if (quit) {
                spriteNumber++
                continue
            }

            if (spriteOffset == 16) tile = tile and 0xFE

            val attributesAddress =
                ReservedAddresses.OAM_START.memoryAddress + (spriteNumber * 4) + 3

            if ((ppu.ppuRegisters.currentLine >= tempY) && (ppu.ppuRegisters.currentLine < (tempY + spriteOffset))) {
                val attributes = bus.getValueFromPPU(attributesAddress)
                val priority: Boolean = attributes.testBit(7)
                val yFlipped: Boolean = attributes.testBit(6)
                val xFlipped: Boolean = attributes.testBit(5)

                val spritePalette = if (attributes.testBit(4)) obp1Palette else obp0Palette

                val tileLine = spriteOffset - (ppu.ppuRegisters.currentLine - tempY)
                val offset: Int = if (!yFlipped) {
                    2 * (ppu.ppuRegisters.currentLine - tempY)
                } else {
                    2 * (tileLine - 1)
                }

                val pixelDataAddress =
                    ReservedAddresses.SWITCH_ROM_END.memoryAddress + tile * 16 + offset

                @Suppress("LoopWithTooManyJumpStatements")
                for (pixelPrinted in 0..7) {
                    if (tempX + pixelPrinted !in 0..<160) continue
                    if (priority && painting[ppu.ppuRegisters.currentLine * WIDTH + tempX + pixelPrinted] > 0) continue

                    val x = if (xFlipped) pixelPrinted else 7 - pixelPrinted
                    val colorNum: Int =
                        ((bus.getValueFromPPU(pixelDataAddress).toInt() and (1 shl x)) shr x) +
                                (((bus.getValueFromPPU(pixelDataAddress + 1)
                                    .toInt() and (1 shl x)) shr x) * 2)

                    if ((tempX + pixelPrinted < 160) && (tempX + pixelPrinted >= 0) && (colorNum != 0)) {
                        painting[ppu.ppuRegisters.currentLine * WIDTH + tempX + pixelPrinted] =
                            spritePalette[colorNum]
                    }
                }

                drawnX[drawnSprites] = tempX
                drawnSprites++
            }
            spriteNumber++
        }
    }

    fun requestRepaint() {
        if (!ppu.ppuRegisters.lcdOn) {
            painting.fill(0)
        }

        ppu.propagatePaintingUpdate(painting)
    }
}
