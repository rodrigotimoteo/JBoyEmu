package com.github.rodrigotimoteo.kboyemu.presentation.emulator

private val defaultGbPalette = intArrayOf(
    0xFFFFFFFF.toInt(), // 0 - White
    0xFFCCCCCC.toInt(), // 1 - Light gray
    0xFF777777.toInt(), // 2 - Dark gray
    0xFF000000.toInt()  // 3 - Black
)

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
