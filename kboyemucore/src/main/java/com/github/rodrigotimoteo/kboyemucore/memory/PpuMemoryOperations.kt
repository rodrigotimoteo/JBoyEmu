package com.github.rodrigotimoteo.kboyemucore.memory

/**
 * PPU-facing memory operations. These accesses must not tick CPU timers.
 *
 * @author rodrigotimoteo
 */
interface PpuMemoryOperations {

    /**
     * Gets a value from the given memory address. This must not tick CPU timers.
     *
     * @param memoryAddress the memory address to read from
     * @return the value at the given memory address
     */
    fun getValueFromPPU(memoryAddress: Int): UByte

    /**
     * Sets a value at the given memory address. This must not tick CPU timers.
     *
     * @param memoryAddress the memory address to write to
     * @param value the value to write at the given memory address
     */
    fun setValueFromPPU(memoryAddress: Int, value: UByte)
}
