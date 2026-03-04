package com.github.rodrigotimoteo.kboyemu.data.savegame

/**
 * Contract for persisting and retrieving battery-backed save games (ERAM dumps).
 * Each save file is stored per-ROM so that different games keep independent saves,
 * similar to how a real Game Boy cartridge battery preserves SRAM.
 *
 * @author rodrigotimoteo
 */
interface SaveGameRepository {

    /**
     * Computes and stores an identifier for the given ROM bytes. Must be called after loading a
     * ROM and before any save/load operations.
     *
     * @param romBytes raw ROM content
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun setRomHash(romBytes: UByteArray)

    /**
     * Writes the given ERAM dump to persistent storage
     *
     * @param data flat byte array of all ERAM banks
     * @return true if the save succeeded, false otherwise
     */
    fun save(data: ByteArray): Boolean

    /**
     * Reads the saved ERAM dump from persistent storage
     *
     * @return previously saved ERAM bytes, or null if no save exists
     */
    fun load(): ByteArray?
}

