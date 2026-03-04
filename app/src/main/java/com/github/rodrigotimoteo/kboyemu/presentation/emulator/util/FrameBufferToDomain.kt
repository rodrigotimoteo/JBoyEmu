package com.github.rodrigotimoteo.kboyemu.presentation.emulator.util

/** * Default DMG palette mapping 2-bit color indices to ARGB values for Android rendering.
 * The colors are defined as follows:
 * 0 - White (0xFFFFFFFF)
 * 1 - Light gray (0xFFCCCCCC)
 * 2 - Dark gray (0xFF777777)
 * 3 - Black (0xFF000000)
 *
 * This palette can be overridden by providing a custom palette to the translation function.
 */
private val defaultGbPalette = intArrayOf(
    0xFFFFFFFF.toInt(), // 0 - White
    0xFFCCCCCC.toInt(), // 1 - Light gray
    0xFF777777.toInt(), // 2 - Dark gray
    0xFF000000.toInt()  // 3 - Black
)

/**
 * Converts DMG 2-bit color indices to ARGB using the given palette
 *
 * @param src Source array of DMG pixel data (2-bit color indices)
 * @param dst Destination array for ARGB pixel data, must be at least as large as src
 * @param palette Optional palette mapping DMG color indices to ARGB values, defaults to a standard
 * grayscale palette
 */
fun translateGbPixelsToArgb(
    src: ByteArray,
    dst: IntArray,
    palette: IntArray = defaultGbPalette
) {
    require(dst.size >= src.size)

    for (i in src.indices) {
        dst[i] = palette[src[i].toInt() and 0x03]
    }
}

/**
 * Converts CGB 15-bit RGB555 color values to 32-bit ARGB for Android rendering.
 * Each RGB555 component (5 bits, 0-31) is expanded to 8 bits (0-255) using the
 * standard (val * 255 / 31) formula for accurate color reproduction.
 *
 * @param src Source array of RGB555 pixel data from the emulator
 * @param dst Destination array for ARGB pixel data, must be at least as large as src
 */
fun translateCgbPixelsToArgb(
    src: IntArray,
    dst: IntArray
) {
    require(dst.size >= src.size)

    for (i in src.indices) {
        val rgb555 = src[i]
        val r5 = rgb555 and 0x1F
        val g5 = (rgb555 shr 5) and 0x1F
        val b5 = (rgb555 shr 10) and 0x1F

        val r8 = (r5 * 255 + 15) / 31
        val g8 = (g5 * 255 + 15) / 31
        val b8 = (b5 * 255 + 15) / 31

        dst[i] = (0xFF shl 24) or (r8 shl 16) or (g8 shl 8) or b8
    }
}

