package com.github.rodrigotimoteo.kboyemucore.ppu.drawer

import com.github.rodrigotimoteo.kboyemucore.api.PpuDrawerState
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU
import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH

/**
 * DMG (original Game Boy) PPU drawer implementation. Renders background, window, and sprite layers
 * using 4-shade monochrome palettes (BGP, OBP0, OBP1).
 *
 * @param ppu reference to the PPU for register access
 * @param bus reference to the Bus for memory reads
 *
 * @author rodrigotimoteo
 */
class DMGPPUDrawer(
    private val ppu: PPU,
    private val bus: Bus,
) : IPPUDrawer {

    /** Pixel buffer holding the current frame as 2-bit color indices */
    private val painting = ByteArray(WIDTH * HEIGHT)

    /** Pre-decoded palette caches — decoded once per scanline, reused per pixel */
    private val bgPalette = ByteArray(4)
    private val obp0Palette = ByteArray(4)
    private val obp1Palette = ByteArray(4)

    /**
     * Decodes a Game Boy palette register into 4 color values and stores them in [dest]
     */
    private fun decodePalette(palette: Int, dest: ByteArray) {
        dest[0] = (palette and 0x03).toByte()
        dest[1] = ((palette shr 2) and 0x03).toByte()
        dest[2] = ((palette shr 4) and 0x03).toByte()
        dest[3] = ((palette shr 6) and 0x03).toByte()
    }

    /**
     * Resolves the VRAM address of a tile line given a tile index, handling both signed and unsigned
     * tile addressing modes
     *
     * @param tile tile index from the tile map
     * @param tileDataAddress base address of the tile data region
     * @param lineOffset vertical pixel offset within the tile (0-7)
     * @return VRAM address of the two bytes that define the tile line
     */
    private fun resolveTileLineAddress(tile: Int, tileDataAddress: Int, lineOffset: Int): Int {
        return if (ppu.ppuRegisters.negativeTiles) {
            val signedTile = tile.toByte().toInt()
            if (signedTile >= 0) {
                tileDataAddress + signedTile * 0x10 + lineOffset * 2
            } else {
                ReservedAddresses.TILE_DATA_1.memoryAddress + (signedTile + 128) * 0x10 + lineOffset * 2
            }
        } else {
            tileDataAddress + tile * 0x10 + lineOffset * 2
        }
    }

    override fun drawBackground(tileMapAddress: Int, tileDataAddress: Int) {
        val tempY = (ppu.ppuRegisters.currentLine + ppu.ppuRegisters.scrollY) and 0xFF
        val lineBase = ppu.ppuRegisters.currentLine * WIDTH
        val tileRow = tempY shr 3            // tempY / 8
        val tileLineOffset = tempY and 7     // tempY % 8
        val mapRowBase = tileMapAddress + (tileRow shl 5) // tileRow * 32

        decodePalette(ppu.ppuRegisters.bgpRegister.value.toInt(), bgPalette)

        val scrollX = ppu.ppuRegisters.scrollX

        // Cache tile data bytes so they are only read once per 8-pixel tile
        var cachedTileCol = -1
        var low = 0
        var high = 0

        var x = 0
        while (x < WIDTH) {
            val tempX = (scrollX + x) and 0xFF
            val tileCol = tempX shr 3   // tempX / 8
            val bitOffset = 7 - (tempX and 7)

            if (tileCol != cachedTileCol) {
                cachedTileCol = tileCol
                val tile = bus.getValueFromPPU(mapRowBase + tileCol).toInt()
                val tileAddr = resolveTileLineAddress(tile, tileDataAddress, tileLineOffset)
                low = bus.getValueFromPPU(tileAddr).toInt()
                high = bus.getValueFromPPU(tileAddr + 1).toInt()
            }

            val colorNum = ((low shr bitOffset) and 1) or (((high shr bitOffset) and 1) shl 1)
            painting[lineBase + x] = bgPalette[colorNum]
            x++
        }
    }

    override fun drawWindow(tileMapAddress: Int, tileDataAddress: Int) {
        val tempY = ppu.ppuRegisters.currentLineWindow

        if (ppu.ppuRegisters.currentLine < ppu.ppuRegisters.windowY) return
        val winX = ppu.ppuRegisters.windowX
        if (winX >= WIDTH) return

        val lineBase = ppu.ppuRegisters.currentLine * WIDTH
        val tileRow = tempY shr 3
        val tileLineOffset = tempY and 7
        val mapRowBase = tileMapAddress + (tileRow shl 5)

        decodePalette(ppu.ppuRegisters.bgpRegister.value.toInt(), bgPalette)

        // Cache tile data bytes per tile column
        var cachedTileCol = -1
        var low = 0
        var high = 0

        // Start at winX directly instead of iterating from 0 and skipping
        var x = if (winX > 0) winX else 0
        while (x < WIDTH) {
            val localX = x - winX
            val tileCol = localX shr 3
            val bitOffset = 7 - (localX and 7)

            if (tileCol != cachedTileCol) {
                cachedTileCol = tileCol
                val tile = bus.getValueFromPPU(mapRowBase + tileCol).toInt()
                val tileAddr = resolveTileLineAddress(tile, tileDataAddress, tileLineOffset)
                low = bus.getValueFromPPU(tileAddr).toInt()
                high = bus.getValueFromPPU(tileAddr + 1).toInt()
            }

            val colorNum = ((low shr bitOffset) and 1) or (((high shr bitOffset) and 1) shl 1)
            painting[lineBase + x] = bgPalette[colorNum]
            x++
        }

        ppu.ppuRegisters.currentLineWindow++
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
    override fun drawSprite() {
        val drawnX = IntArray(10)
        val currentLine = ppu.ppuRegisters.currentLine
        val lineBase = currentLine * WIDTH

        decodePalette(ppu.ppuRegisters.obp0Register.value.toInt(), obp0Palette)
        decodePalette(ppu.ppuRegisters.obp1Register.value.toInt(), obp1Palette)

        var drawnSprites = 0
        val spriteOffset = if (ppu.ppuRegisters.spriteSize) 16 else 8
        val oamBase = ReservedAddresses.OAM_START.memoryAddress
        val vramBase = ReservedAddresses.SWITCH_ROM_END.memoryAddress

        var spriteNumber = 0
        while (spriteNumber < 40 && drawnSprites < 10) {
            val oamAddr = oamBase + (spriteNumber shl 2) // spriteNumber * 4
            val tempY = bus.getValueFromPPU(oamAddr).toInt() - 16
            val tempX = bus.getValueFromPPU(oamAddr + 1).toInt() - 8
            var tile = bus.getValueFromPPU(oamAddr + 2).toInt()

            // Check for duplicate X positions among already-drawn sprites
            var quit = false
            if (drawnSprites > 1) {
                for (i in 0 until drawnSprites) {
                    if (drawnX[i] == tempX) { quit = true; break }
                }
            }
            if (quit) {
                spriteNumber++
                continue
            }

            if (spriteOffset == 16) tile = tile and 0xFE

            if (currentLine >= tempY && currentLine < tempY + spriteOffset) {
                val attributes = bus.getValueFromPPU(oamAddr + 3).toInt()
                val priority = (attributes and 0x80) != 0
                val yFlipped = (attributes and 0x40) != 0
                val xFlipped = (attributes and 0x20) != 0

                val spritePalette = if ((attributes and 0x10) != 0) obp1Palette else obp0Palette

                val tileLine = spriteOffset - (currentLine - tempY)
                val offset: Int = if (!yFlipped) {
                    2 * (currentLine - tempY)
                } else {
                    2 * (tileLine - 1)
                }

                val pixelDataAddress = vramBase + tile * 16 + offset
                // Hoist tile data reads out of the per-pixel loop
                val lowByte = bus.getValueFromPPU(pixelDataAddress).toInt()
                val highByte = bus.getValueFromPPU(pixelDataAddress + 1).toInt()

                @Suppress("LoopWithTooManyJumpStatements")
                for (pixelPrinted in 0..7) {
                    val screenX = tempX + pixelPrinted
                    if (screenX !in 0 until 160) continue
                    if (priority && painting[lineBase + screenX] > 0) continue

                    val bitPos = if (xFlipped) pixelPrinted else 7 - pixelPrinted
                    val colorNum = ((lowByte shr bitPos) and 1) or
                            (((highByte shr bitPos) and 1) shl 1)

                    if (colorNum != 0) {
                        painting[lineBase + screenX] = spritePalette[colorNum]
                    }
                }

                drawnX[drawnSprites] = tempX
                drawnSprites++
            }
            spriteNumber++
        }
    }

    override fun requestRepaint() {
        if (!ppu.ppuRegisters.lcdOn) {
            painting.fill(0)
        }

        ppu.propagatePaintingUpdate(painting)
    }

    override fun saveState(): PpuDrawerState = PpuDrawerState()

    override fun loadState(state: PpuDrawerState) {
        // DMG drawer has no persistent state beyond what memory registers already capture
    }
}
