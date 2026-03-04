package com.github.rodrigotimoteo.kboyemucore.api

import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH

/**
 * Data class used to expose the user of this Core access to what needs to be drawn to the screen.
 * DMG games populate [pixels] with 2-bit grayscale indices (0-3).
 * CGB games populate [colorPixels] with 15-bit RGB555 colors.
 *
 * @param width of the screen defaults to [WIDTH]
 * @param height of the screen default to [HEIGHT]
 * @param pixels array with 2-bit color indices for DMG rendering
 * @param colorPixels array with 15-bit RGB555 colors for CGB rendering (null on DMG)
 *
 * @author rodrigotimoteo
 */
data class FrameBuffer(
    val width: Int = WIDTH,
    val height: Int = HEIGHT,
    val pixels: ByteArray,
    val colorPixels: IntArray? = null,
) {

    /** Whether this frame contains CGB color data */
    val isCGB: Boolean get() = colorPixels != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FrameBuffer

        if (width != other.width) return false
        if (height != other.height) return false
        if (!pixels.contentEquals(other.pixels)) return false
        if (colorPixels != null) {
            if (other.colorPixels == null) return false
            if (!colorPixels.contentEquals(other.colorPixels)) return false
        } else if (other.colorPixels != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        result = 31 * result + (colorPixels?.contentHashCode() ?: 0)
        return result
    }
}
