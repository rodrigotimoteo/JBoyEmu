package com.github.rodrigotimoteo.kboyemucore.ppu.drawer

import com.github.rodrigotimoteo.kboyemucore.api.PpuDrawerState
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.ktx.testBit
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU
import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH

/**
 * CGB (Game Boy Color) PPU drawer implementation. Renders background, window, and sprite layers
 * using 15-bit RGB palettes stored in dedicated CGB palette RAM. Supports VRAM bank 1 tile
 * attributes including per-tile palette selection, X/Y flip, VRAM bank, and BG-to-OBJ priority.
 *
 * The pixel buffer stores 16-bit RGB555 values packed into two bytes per pixel. The Android side
 * converts these to ARGB for display.
 *
 * @param ppu reference to the PPU for register access
 * @param bus reference to the Bus for memory reads and CGB palette RAM access
 *
 * @author rodrigotimoteo
 */
class CGBPPUDrawer(
    private val ppu: PPU,
    private val bus: Bus,
) : IPPUDrawer {

    /**
     * Pixel buffer holding the current frame. Each pixel is an index into a flat 15-bit RGB
     * color table. Stored as bytes where the value encodes palette index + color index so the
     * Android side can map it. For CGB we store the raw 15-bit RGB555 color directly as an
     * Int in a separate buffer and convert to the byte buffer on repaint.
     */
    private val painting = ByteArray(WIDTH * HEIGHT)

    /**
     * 15-bit RGB555 color buffer — each pixel stores the actual color value. On repaint this
     * is converted to the [painting] byte array using a simple intensity mapping.
     */
    private val colorBuffer = IntArray(WIDTH * HEIGHT)

    /**
     * Stores per-pixel BG priority info for sprite-behind-BG logic. True means the BG pixel
     * has priority over sprites at that position.
     */
    private val bgPriority = BooleanArray(WIDTH * HEIGHT)

    /**
     * Stores per-pixel BG color index (0-3) for the BG-over-OBJ priority rule:
     * sprites are hidden behind BG when BG color index is non-zero and priority is set.
     */
    private val bgColorIndex = IntArray(WIDTH * HEIGHT)

    /** CGB BG palette RAM — 8 palettes × 4 colors × 2 bytes = 64 bytes */
    private val bgPaletteRam = IntArray(64)

    /** CGB OBJ palette RAM — 8 palettes × 4 colors × 2 bytes = 64 bytes */
    private val objPaletteRam = IntArray(64)

    /**
     * Writes a byte to the CGB BG palette RAM at the given index
     *
     * @param index byte offset into BG palette RAM (0-63)
     * @param value byte value to write
     */
    fun writeBgPalette(index: Int, value: Int) {
        bgPaletteRam[index and 0x3F] = value and 0xFF
    }

    /**
     * Reads a byte from the CGB BG palette RAM at the given index
     *
     * @param index byte offset into BG palette RAM (0-63)
     * @return byte value at the given index
     */
    fun readBgPalette(index: Int): Int = bgPaletteRam[index and 0x3F]

    /**
     * Writes a byte to the CGB OBJ palette RAM at the given index
     *
     * @param index byte offset into OBJ palette RAM (0-63)
     * @param value byte value to write
     */
    fun writeObjPalette(index: Int, value: Int) {
        objPaletteRam[index and 0x3F] = value and 0xFF
    }

    /**
     * Reads a byte from the CGB OBJ palette RAM at the given index
     *
     * @param index byte offset into OBJ palette RAM (0-63)
     * @return byte value at the given index
     */
    fun readObjPalette(index: Int): Int = objPaletteRam[index and 0x3F]

    override fun saveState(): PpuDrawerState = PpuDrawerState(
        bgPaletteRam = bgPaletteRam.copyOf(),
        objPaletteRam = objPaletteRam.copyOf(),
    )

    override fun loadState(state: PpuDrawerState) {
        state.bgPaletteRam?.copyInto(bgPaletteRam, endIndex = minOf(state.bgPaletteRam.size, bgPaletteRam.size))
        state.objPaletteRam?.copyInto(objPaletteRam, endIndex = minOf(state.objPaletteRam.size, objPaletteRam.size))
    }

    /**
     * Resolves a 15-bit RGB555 color from the given palette RAM at the specified palette and
     * color index
     *
     * @param paletteRam either [bgPaletteRam] or [objPaletteRam]
     * @param paletteNumber palette index (0-7)
     * @param colorIndex color within the palette (0-3)
     * @return 15-bit RGB555 color value
     */
    private fun getColor(paletteRam: IntArray, paletteNumber: Int, colorIndex: Int): Int {
        val offset = paletteNumber * 8 + colorIndex * 2
        return paletteRam[offset] or (paletteRam[offset + 1] shl 8)
    }

    /**
     * Converts a 15-bit RGB555 color to a 2-bit grayscale approximation for the byte buffer.
     * This allows the existing Android rendering pipeline to display CGB output while a proper
     * RGB pipeline is not yet available.
     *
     * @param rgb555 15-bit color value
     * @return 2-bit intensity (0 = lightest, 3 = darkest)
     */
    private fun rgb555ToGray(rgb555: Int): Byte {
        val r = rgb555 and 0x1F
        val g = (rgb555 shr 5) and 0x1F
        val b = (rgb555 shr 10) and 0x1F
        val luminance = (r * 30 + g * 59 + b * 11) / 100
        return when {
            luminance >= 24 -> 0
            luminance >= 16 -> 1
            luminance >= 8  -> 2
            else            -> 3
        }.toByte()
    }

    /**
     * Resolves the VRAM address of a tile line, handling both signed and unsigned addressing modes.
     * The returned address is always in the 0x8000-0x9FFF range — VRAM bank selection is handled
     * separately by the caller.
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

    /**
     * Extracts the 2-bit color number from two tile data bytes at the given bit position,
     * reading from a specific VRAM bank.
     *
     * @param address VRAM address of the first tile data byte
     * @param bitOffset bit position within the tile line (7 = leftmost, 0 = rightmost)
     * @param vramBank VRAM bank to read from (0 or 1)
     * @return color number in the range 0-3
     */
    private fun getColorNumber(address: Int, bitOffset: Int, vramBank: Int): Int {
        val low = bus.getValueFromPPUBank(address, vramBank).toInt()
        val high = bus.getValueFromPPUBank(address + 1, vramBank).toInt()
        return ((low shr bitOffset) and 1) + (((high shr bitOffset) and 1) shl 1)
    }

    override fun drawBackground(tileMapAddress: Int, tileDataAddress: Int) {
        val tempY = (ppu.ppuRegisters.currentLine + ppu.ppuRegisters.scrollY) and 0xFF
        val tileRow = tempY / 8
        val lineInTile = tempY % 8

        for (x in 0 until WIDTH) {
            val tempX = (ppu.ppuRegisters.scrollX + x) % 0x0100
            val tileCol = tempX / 8

            val mapOffset = tileMapAddress + tileRow * 0x20 + tileCol

            val tile = bus.getValueFromPPU(mapOffset).toInt()
            val attributes = bus.getValueFromPPUBank1(mapOffset).toInt()

            val bgPaletteNum = attributes and 0x07
            val vramBank = (attributes shr 3) and 1
            val xFlip = (attributes and 0x20) != 0
            val yFlip = (attributes and 0x40) != 0
            val priority = (attributes and 0x80) != 0

            val actualLine = if (yFlip) 7 - lineInTile else lineInTile
            val tileLine = resolveTileLineAddress(tile, tileDataAddress, actualLine)

            val bitOffset = if (xFlip) tempX % 8 else 7 - (tempX % 8)
            val colorIdx = getColorNumber(tileLine, bitOffset, vramBank)

            val color = getColor(bgPaletteRam, bgPaletteNum, colorIdx)
            val pixelPos = ppu.ppuRegisters.currentLine * WIDTH + x

            colorBuffer[pixelPos] = color
            bgPriority[pixelPos] = priority
            bgColorIndex[pixelPos] = colorIdx
        }
    }

    override fun drawWindow(tileMapAddress: Int, tileDataAddress: Int) {
        val tempY = ppu.ppuRegisters.currentLineWindow

        if (ppu.ppuRegisters.currentLine < ppu.ppuRegisters.windowY) return
        if (ppu.ppuRegisters.windowX >= WIDTH) return

        val lineInTile = tempY % 8

        for (x in 0 until WIDTH) {
            if (x < ppu.ppuRegisters.windowX) continue

            val tempX = x - ppu.ppuRegisters.windowX
            val tileCol = tempX / 8
            val tileRow = tempY / 8

            val mapOffset = tileMapAddress + tileRow * 0x20 + tileCol

            val tile = bus.getValueFromPPU(mapOffset).toInt()
            val attributes = bus.getValueFromPPUBank1(mapOffset).toInt()

            val bgPaletteNum = attributes and 0x07
            val vramBank = (attributes shr 3) and 1
            val xFlip = (attributes and 0x20) != 0
            val yFlip = (attributes and 0x40) != 0
            val priority = (attributes and 0x80) != 0

            val actualLine = if (yFlip) 7 - lineInTile else lineInTile
            val tileLine = resolveTileLineAddress(tile, tileDataAddress, actualLine)

            val bitOffset = if (xFlip) tempX % 8 else 7 - (tempX % 8)
            val colorIdx = getColorNumber(tileLine, bitOffset, vramBank)

            val color = getColor(bgPaletteRam, bgPaletteNum, colorIdx)
            val pixelPos = ppu.ppuRegisters.currentLine * WIDTH + x

            colorBuffer[pixelPos] = color
            bgPriority[pixelPos] = priority
            bgColorIndex[pixelPos] = colorIdx
        }

        ppu.ppuRegisters.currentLineWindow++
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
    override fun drawSprite() {
        var drawnSprites = 0
        val spriteOffset = if (ppu.ppuRegisters.spriteSize) 16 else 8
        val masterPriority = ppu.ppuRegisters.masterPriority

        // OPRI register (0xFF6C) bit 0: 0 = OAM position priority, 1 = X-coordinate priority (DMG)
        val opri = bus.getValueFromPPU(ReservedAddresses.OPRI.memoryAddress).toInt() and 0x01
        val useOamPriority = opri == 0

        // Track which screen pixels already have a sprite drawn (for OAM priority mode)
        val spriteDrawn = BooleanArray(WIDTH)

        var spriteNumber = 0
        while (spriteNumber < 40 && drawnSprites < 10) {
            val oamBase = ReservedAddresses.OAM_START.memoryAddress + spriteNumber * 4

            val tempY = bus.getValueFromPPU(oamBase).toInt() - 16
            val tempX = bus.getValueFromPPU(oamBase + 1).toInt() - 8
            var tile = bus.getValueFromPPU(oamBase + 2).toInt()

            if (spriteOffset == 16) tile = tile and 0xFE

            if (ppu.ppuRegisters.currentLine >= tempY &&
                ppu.ppuRegisters.currentLine < tempY + spriteOffset
            ) {
                val attributes = bus.getValueFromPPU(oamBase + 3)
                val behindBg: Boolean = attributes.testBit(7)
                val yFlipped: Boolean = attributes.testBit(6)
                val xFlipped: Boolean = attributes.testBit(5)
                val vramBank = if (attributes.testBit(3)) 1 else 0
                val objPaletteNum = attributes.toInt() and 0x07

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
                    val screenX = tempX + pixelPrinted
                    if (screenX !in 0 until WIDTH) continue

                    // In OAM priority mode, first sprite in OAM wins per pixel
                    if (useOamPriority && spriteDrawn[screenX]) continue

                    val pixelPos = ppu.ppuRegisters.currentLine * WIDTH + screenX

                    // Master priority: when LCDC bit 0 is set, BG can have priority over sprites
                    // When LCDC bit 0 is clear, sprites always draw on top of BG
                    if (masterPriority) {
                        if (behindBg && bgColorIndex[pixelPos] != 0) continue
                        if (bgPriority[pixelPos] && bgColorIndex[pixelPos] != 0) continue
                    }

                    val bitPos = if (xFlipped) pixelPrinted else 7 - pixelPrinted
                    val colorIdx =
                        ((bus.getValueFromPPUBank(pixelDataAddress, vramBank).toInt() shr bitPos) and 1) +
                                (((bus.getValueFromPPUBank(pixelDataAddress + 1, vramBank).toInt() shr bitPos) and 1) shl 1)

                    if (colorIdx != 0) {
                        colorBuffer[pixelPos] = getColor(objPaletteRam, objPaletteNum, colorIdx)
                        spriteDrawn[screenX] = true
                    }
                }

                drawnSprites++
            }
            spriteNumber++
        }
    }

    override fun requestRepaint() {
        if (!ppu.ppuRegisters.lcdOn) {
            painting.fill(0)
            colorBuffer.fill(0x7FFF)
            bgPriority.fill(false)
            bgColorIndex.fill(0)
        } else {
            for (i in colorBuffer.indices) {
                painting[i] = rgb555ToGray(colorBuffer[i])
            }
        }

        ppu.propagatePaintingUpdate(painting, colorBuffer)
    }
}





