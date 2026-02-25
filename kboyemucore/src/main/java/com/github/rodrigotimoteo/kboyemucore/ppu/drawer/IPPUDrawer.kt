package com.github.rodrigotimoteo.kboyemucore.ppu.drawer

/**
 * Interface for PPU drawing operations shared between DMG and CGB implementations. Each scanline
 * the PPU calls the enabled layer methods in order: background, window, then sprites.
 *
 * @author rodrigotimoteo
 */
interface IPPUDrawer {

    /**
     * Draws the background layer for the current scanline
     *
     * @param tileMapAddress base address of the background tile map
     * @param tileDataAddress base address of the tile data region
     */
    fun drawBackground(tileMapAddress: Int, tileDataAddress: Int)

    /**
     * Draws the window layer for the current scanline
     *
     * @param tileMapAddress base address of the window tile map
     * @param tileDataAddress base address of the tile data region
     */
    fun drawWindow(tileMapAddress: Int, tileDataAddress: Int)

    /**
     * Draws the sprite (OBJ) layer for the current scanline
     */
    fun drawSprite()

    /**
     * Triggers a full frame repaint, pushing the current pixel buffer to the PPU output. Clears
     * the buffer when the LCD is off.
     */
    fun requestRepaint()
}