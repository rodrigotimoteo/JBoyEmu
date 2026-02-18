package com.github.rodrigotimoteo.kboyemucore.memory

/**
 * CPU-facing memory operations. All accesses here should tick CPU timers.
 *
 * @author rodrigotimoteo
 */
interface CpuMemoryOperations {

    /**
     * Gets a value from the given memory address. This should tick CPU timers.
     *
     * @param memoryAddress the memory address to read from
     * @return the value at the given memory address
     */
    fun getValueFromCPU(memoryAddress: Int): UByte

    /**
     * Sets a value at the given memory address. This should tick CPU timers.
     *
     * @param memoryAddress the memory address to write to
     * @param value the value to write at the given memory address
     */
    fun setValueFromCPU(memoryAddress: Int, value: UByte)
}
