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

/**
 * Checks if the PPU mode allows VRAM access. This is true for HBLANK, VBLANK and OAM modes.
 *
 * @return true if the PPU mode allows VRAM access, false otherwise
 */
fun PPUModes.writeVRAMAble(): Boolean {
    return this == PPUModes.HBLANK || this == PPUModes.VBLANK || this == PPUModes.OAM
}

/**
 * Checks if the PPU mode allows OAM access. This is true for HBLANK and VBLANK modes.
 *
 * @return true if the PPU mode allows OAM access, false otherwise
 */
fun PPUModes.writeOAMAble(): Boolean {
    return this == PPUModes.HBLANK || this == PPUModes.VBLANK
}

/**
 * Checks if the PPU mode allows CGB palette access. This is true for HBLANK and VBLANK modes.
 *
 * @return true if the PPU mode allows CGB palette access, false otherwise
 */
fun PPUModes.writeCGBPalettesAble(): Boolean {
    return this == PPUModes.HBLANK || this == PPUModes.VBLANK
}
