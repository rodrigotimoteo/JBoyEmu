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
    internal var activeBank: Int = 0,
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

    /**
     * The bank index used for the lower (fixed) region when [simultaneousBanks] == 2.
     * Defaults to 0 (bank 0 is always fixed). Subclasses that need dynamic fixed-bank
     * remapping (e.g. MBC1 advanced banking mode) can override this.
     */
    protected open val fixedBank: Int = 0

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
                    memory[fixedBank][realIndex]
                }
            }
        }
    }

    /**
     * Reads a value from a specific bank regardless of which bank is currently active.
     * Used by the CGB PPU drawer to access VRAM bank 1 for tile map attributes.
     *
     * @param memoryAddress the absolute memory address to read
     * @param bank the bank number to read from
     * @return the value at the given address in the specified bank
     */
    fun getValueFromBank(memoryAddress: Int, bank: Int): UByte {
        val realIndex = memoryAddress - memoryOffset
        return memory[bank.coerceIn(0, numberOfBanks - 1)][realIndex]
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
