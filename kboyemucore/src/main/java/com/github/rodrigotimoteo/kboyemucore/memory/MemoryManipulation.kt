package com.github.rodrigotimoteo.kboyemucore.memory

/**
 * Base functions implemented by classes that interact with Memory
 *
 * @author rodrigotimoteo
 **/
interface MemoryManipulation {

    /**
     * Assigns a memory address a new value
     *
     * @param memoryAddress memory location where to set value
     * @param value value to put inside address
     */
    fun setValue(memoryAddress: Int, value: UByte)

    /**
     * Gets the value inside a memory address
     *
     * @param memoryAddress memory location to fetch content from
     * @return content inside given memory location
     */
    fun getValue(memoryAddress: Int): UByte
}
