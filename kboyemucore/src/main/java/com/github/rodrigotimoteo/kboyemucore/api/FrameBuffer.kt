package com.github.rodrigotimoteo.kboyemucore.api

import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH

/**
 * Data class used to expose the user of this Core access to what needs to be drawn to the screen
 *
 * @param width of the screen defaults to [WIDTH]
 * @param height of the screen default to [HEIGHT]
 * @param pixels array with the information of each pixel to be drawn that needs to be drawn (color)
 *
 * @author rodrigotimoteo
 */
data class FrameBuffer(
    val width: Int = WIDTH,
    val height: Int = HEIGHT,
    val pixels: ByteArray
) {

    /**
     * Custom equals and hashcode implementation to avoid the default one that does not work with
     * arrays
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FrameBuffer

        if (width != other.width) return false
        if (height != other.height) return false
        if (!pixels.contentEquals(other.pixels)) return false

        return true
    }

    /**
     * Custom hashcode implementation to avoid the default one that does not work with arrays
     */
    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}
