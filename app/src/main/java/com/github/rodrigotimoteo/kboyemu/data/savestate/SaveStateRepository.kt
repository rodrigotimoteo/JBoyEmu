package com.github.rodrigotimoteo.kboyemu.data.savestate

import com.github.rodrigotimoteo.kboyemucore.api.SaveState

/**
 * Contract for persisting and retrieving emulator save states. Each save state is stored per-ROM
 * so that different games keep independent save slots.
 *
 * @author rodrigotimoteo
 */
interface SaveStateRepository {

    /**
     * Computes and stores an identifier for the given ROM bytes. Must be called after loading a
     * ROM and before any save/load operations.
     *
     * @param romBytes raw ROM content
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun setRomHash(romBytes: UByteArray)

    /**
     * Writes the given [SaveState] to persistent storage
     *
     * @param state emulator state to persist
     * @return true if the save succeeded, false otherwise
     */
    fun save(state: SaveState): Boolean

    /**
     * Reads a previously persisted [SaveState] for the currently loaded ROM
     *
     * @return the deserialized save state, or null if no save exists or an error occurs
     */
    fun load(): SaveState?
}