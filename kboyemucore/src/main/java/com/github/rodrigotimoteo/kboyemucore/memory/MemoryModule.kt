package com.github.rodrigotimoteo.kboyemucore.memory

/**
 * Represents a memory module, the game boy is divided into several modules, therefore this class
 * absorbs the main logic those modules
 *
 * @author rodrigotimoteo
 **/
@OptIn(ExperimentalUnsignedTypes::class)
open class MemoryModule(
    private val numberOfBanks: Int = 1,
    private val memoryOffset: Int,
    private val simultaneousBanks: Int,
    private val activeBank: Int = 0,
    private val size: Int,
) : MemoryManipulation {
    private val memory: Array<UByteArray> = Array(numberOfBanks) { UByteArray(size) }

    constructor(size: Int, memoryOffset: Int) : this(
        numberOfBanks = 1,
        activeBank = 0,
        simultaneousBanks = 1,
        memoryOffset = memoryOffset,
        size = size
    )

    constructor(size: Int, simultaneousBanks: Int, memoryOffset: Int, numberOfBanks: Int) : this(
        numberOfBanks = numberOfBanks,
        activeBank = if (simultaneousBanks == 2) 1 else 0,
        simultaneousBanks = simultaneousBanks,
        memoryOffset = memoryOffset,
        size = size
    )

    constructor(
        content: UByteArray,
        size: Int,
        simultaneousBanks: Int,
        memoryOffset: Int,
        numberOfBanks: Int
    ) : this(
        numberOfBanks = numberOfBanks,
        activeBank = if (simultaneousBanks == 2) 1 else 0,
        simultaneousBanks = simultaneousBanks,
        memoryOffset = memoryOffset,
        size = size
    ) {
        initializeMemory(content)
    }

    /**
     * Method that initializes all the Word objects to the provided value as ByteArray (used for rom
     * assignment)
     *
     * @param content to assign to memory module
     */
    private fun initializeMemory(content: UByteArray) {
        for ((i, byte) in content.withIndex()) {
            memory[i / size][i % size] = byte
        }
    }

    override fun setValue(memoryAddress: Int, value: UByte) {
        val realIndex = memoryAddress - memoryOffset

        if (numberOfBanks == 1) {
            memory[activeBank][realIndex] = value
        } else {
            if (simultaneousBanks == 1) {
                memory[activeBank][realIndex] = value
            } else {
                val moduleSize = memory[activeBank].size

                if (realIndex >= moduleSize) {
                    memory[activeBank][realIndex - moduleSize] = value
                } else {
                    memory[0][realIndex] = value
                }
            }
        }
    }

    override fun getValue(memoryAddress: Int): UByte {
        val realIndex = memoryAddress - memoryOffset

        return if (numberOfBanks == 1) {
            memory[activeBank][realIndex]
        } else {
            if (simultaneousBanks == 1)
                memory[activeBank][realIndex]
            else {
                val moduleSize = memory[activeBank].size

                if (realIndex >= moduleSize) {
                    memory[activeBank][realIndex - moduleSize]
                } else {
                    memory[0][realIndex]
                }
            }
        }
    }

    /**
     * Override the toString method to better reflect the way the information for this class should
     * be read, therefore enabling the prints of this class as a readable output, that enabled easier
     * debugging
     *
     * @return custom dump of this module
     */
    override fun toString(): String {
        val stringBuilder = StringBuilder()

        stringBuilder.append("0 ")
        for (i in 0 until numberOfBanks) {
            for (j in memory[i].indices) if (i % 16 == 0 && i != 0) {
                stringBuilder.append(" \n")
                stringBuilder.append(Integer.toHexString(i)).append(" ")
                stringBuilder.append(Integer.toHexString(memory[i][j].toInt())).append(" ")
            } else {
                stringBuilder.append(Integer.toHexString(memory[i][j].toInt())).append(" ")
            }
        }

        return stringBuilder.toString()
    }
}
