package com.github.rodrigotimoteo.kboyemu.data.rom

/**
 * Contract for loading ROM data from platform sources such as content URIs or asset files.
 *
 * @author rodrigotimoteo
 */
interface RomRepository {

    /**
     * Reads ROM bytes from a content URI
     *
     * @param uri string representation of the content URI to read
     * @return raw ROM bytes, or null if reading failed
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun loadFromUri(uri: String): UByteArray?
}

