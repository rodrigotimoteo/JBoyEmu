package com.github.rodrigotimoteo.kboyemucore.ppu

/**
 * Stores all modes that PPU uses while operating
 *
 * @author rodrigotimoteo
 */
enum class PPUModes(val bit: Int) {
    HBLANK(0),
    VBLANK(1),
    OAM(2),
    PIXEL_TRANSFER(3)
}
