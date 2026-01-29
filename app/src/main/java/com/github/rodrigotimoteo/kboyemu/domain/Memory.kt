package com.github.rodrigotimoteo.kboyemu.domain

import com.github.rodrigotimoteo.kboyemu.kotlin.DisplayFrame
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.Arrays

class Memory {


    private var gameFileName: String? = null

    private val memory = CharArray(0x10000)
    private lateinit var romBank: Array<CharArray?> //Only used when MBC's are needed
    private var ramBank: Array<CharArray?>?

    private var cgbWorkRamBank: Array<CharArray?>
    private var cgbVramBank: Array<CharArray?>

    private const val DIV = 0xff04
    private const val TIMA = 0xff05
    private const val TAC = 0xff07
    private const val SOUND1 = 0xff14

    private var ramOn = false
    private var littleRam = false
    private var hasBattery = false
    private var hasTimer = false
    private var lcdOn = false
    private var cgb = false

    private var memoryModel = 0 //main.kotlin.ROM = 0 RAM = 1
    private var ppuMode = 0
    private const val latchReg = 0
    private var currentRomBank = 0
    private var currentRamBank = 0

    private var currentCgbVramBank = 0
    private var currentCgbWorkRamBank = 1

    private const val ROM_LIMIT = 0x8000

    private val cpu: CPU? = null
    var displayFrame: DisplayFrame? = null

    private var cartridgeType = 0


    //Resets
    private fun resetMemory() {
        Arrays.fill(memory, 0.toChar())
    }


    //Setters
    fun setCartridgeType(cartridgeType: Int) {
        this.cartridgeType = cartridgeType

        try {
            loadRam()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun setHasBattery(state: Boolean) {
        hasBattery = state
    }

    fun setLittleRam(state: Boolean) {
        littleRam = state
    }

    fun setHasTimer(state: Boolean) {
        hasTimer = state
    }

    fun setDisplayFrame(displayFrame: DisplayFrame) {
        this.displayFrame = displayFrame
    }

    fun setLcdOn(state: Boolean) {
        lcdOn = state
    }

    fun setPpuMode(value: Int) {
        ppuMode = value
    }

    fun setGameFileName(name: String?) {
        gameFileName = name
    }

    fun setCgbMode() {
        cgbVramBank = Array<CharArray?>(2) { CharArray(0x2000) }
        cgbWorkRamBank = Array<CharArray?>(7) { CharArray(0x1000) }

        cpu!!.setCgbMode()

        cgb = true
    }


    //Writing to main.kotlin.Memory
    fun writePriv(address: Int, value: Char) {
        memory[address] = (value.code and 0xff).toChar()
    }

    fun setMemory(address: Int, value: Char) {
        setMemoryMBC0(address, value)
        when (cartridgeType) {
            0, 8, 9 -> setMemoryMBC0(address, value)
            1, 2, 3 -> setMemoryMBC1(address, value)
            5, 6 -> setMemoryMBC2(address, value)
            0xf, 0x10, 0x11, 0x12, 0x13 -> setMemoryMBC3(address, value)
            0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e -> setMemoryMBC5(address, value)
            else -> setMemoryMBC0(address, value)
        }
    }

    private fun setMemoryMBC0(address: Int, value: Char) {
        if (address == 0xff44) {
        } //Check currentLine
        else if (address == DIV) {  //Check timer
            cpu!!.resetClocks()
            memory[address] = 0.toChar()
        } else if (address == TAC) {
            val oldTac = memory[address].code and 0x3
            if (oldTac == 0 && (value.code and 0x03) == 1 && (value.code and 0x4) != 0) setMemory(
                TIMA,
                (memory[TIMA].code + 1).toChar()
            )

            memory[address] = (value.code and 0xff).toChar()
        } else if (address == 0xff26) { //Check sound enable/disable
            if (value.code != 0) memory[address] = 0xff.toChar()
            else memory[address] = 0.toChar()
        } else if (address == 0x0143 && !cgb) {
            setCgbMode()
        } else if (address == 0xff46) {
            doDMA(value)
        } else if (address == 0xff0f) {
            memory[address] = ((0xe0 or value.code) and 0xff).toChar()
        } else if (address == SOUND1 && ((value.code and 0xff) shr 7) != 0) { //Check Sound Channel 1
            memory[address] = value
        } else if (address >= 0xc000 && address <= 0xde00) { //Check Ram Echo
            memory[address] = (value.code and 0xff).toChar()
            memory[address + 0x2000] = (value.code and 0xff).toChar()
        } else if (cgb && address == 0xff4f) {
            if ((value.code and 0x1) != currentCgbVramBank) {
                loadVRamBank(value.code and 0x1)
            }
        } else if (cgb && address == 0xff70) {
            if ((value.code and 0x7) != currentCgbWorkRamBank) {
                loadWorkRamBank(value.code and 0x7)
            }
        } else if (address >= 0xe000 && address <= 0xfe00) { //Check Ram Echo
            memory[address] = (value.code and 0xff).toChar()
            memory[address - 0x2000] = (value.code and 0xff).toChar()
        } else if (address >= 0xa000 && address <= 0xbfff && ramOn && currentRamBank < 4) {
            memory[address] = (value.code and 0xff).toChar()
        } else if (address >= 0xa000 && address <= 0xbfff && ramOn && currentRamBank >= 4) {
        } else if (address > ROM_LIMIT) {
            memory[address] = (value.code and 0xff).toChar()
        }
    }

    private fun setMemoryMBC1(address: Int, value: Char) {
        if (address < ROM_LIMIT) { //main.kotlin.Memory Bank Controller
            if (address < 0x2000) { //RAM ENABLE
                ramOn = (value.code and 15) == 10
            } else if (address < 0x4000) { //main.kotlin.ROM Bank Number
                if (currentRomBank != (value.code and 31)) {
                    currentRomBank = (value.code and 31)
                    if (currentRomBank == 0 || currentRomBank == 1) loadRomBank(1)
                    else if (romBank.size < 0x20) loadRomBank(currentRomBank % romBank.size)
                }
            } else if (address < 0x6000) { //RAM
                if (memoryModel == 0 && ramBank != null) {
                    currentRomBank = (value.code and 3) shl 5
                    saveRamBank(currentRamBank)
                    currentRamBank = 0
                    loadRamBank(currentRamBank)
                } else if (ramBank != null) {
                    currentRamBank = (value.code and 3)
                    saveRamBank(currentRamBank)
                }
            } else { //main.kotlin.ROM/RAM Select
                memoryModel = if ((value.code and 0x1) == 1) 1 else 0
            }
        } else {
            setMemoryMBC0(address, value)
        }
    }

    private fun setMemoryMBC2(address: Int, value: Char) {
        if (address < ROM_LIMIT) { //main.kotlin.Memory Bank Controller
            if (address < 0x2000) { //RAM ENABLE
                ramOn = (value.code and 15) == 10
            } else if (address < 0x4000) { //main.kotlin.ROM Bank Number
                if (currentRomBank != (value.code and 31)) {
                    currentRomBank = (value.code and 31)
                    if (currentRomBank == 0 || currentRomBank == 1) loadRomBank(1)
                    else loadRomBank(currentRomBank)
                }
            }
        } else {
            //else if(address)
            setMemoryMBC0(address, value)
        }
    }

    private fun setMemoryMBC3(address: Int, value: Char) {
        if (address < ROM_LIMIT) { //main.kotlin.Memory Bank Controller
            if (address < 0x2000) { //RAM ENABLE
                ramOn = (value.code and 15) == 10
                if (!ramOn) {
                    try {
                        saveRam()
                    } catch (e: FileNotFoundException) {
                        throw RuntimeException(e)
                    }
                }
            } else if (address < 0x4000) { //main.kotlin.ROM Bank Number
                if (currentRomBank != (value.code and 127)) {
                    currentRomBank = (value.code and 127)
                    if (currentRomBank == 0 || currentRomBank == 1) loadRomBank(1)
                    else loadRomBank(currentRomBank)
                }
            } else if (address < 0x6000) { //RAM
                if ((value.code and 0xff) < 4) {
                    if (memoryModel == 0 && ramBank != null) {
                        currentRomBank = (value.code and 3) shl 5
                        saveRamBank(currentRamBank)
                        currentRamBank = 0
                        loadRamBank(currentRamBank)
                    } else if (ramBank != null) {
                        currentRamBank = (value.code and 3)
                        saveRamBank(currentRamBank)
                    }
                } else {
                    if (value.code == 1 && latchReg == 0) {
                    } else {
                    }
                }
            } else { //Latch Clock Data
            }
        } else {
            setMemoryMBC0(address, value)
        }
    }

    private fun setMemoryMBC5(address: Int, value: Char) {
        if (address < ROM_LIMIT) { //main.kotlin.Memory Bank Controller
            if (address < 0x2000) { //RAM ENABLE
                ramOn = (value.code and 15) == 10
            } else if (address < 0x3000) { //8 least significant bits of main.kotlin.ROM bank number
                currentRomBank = currentRomBank and value.code
                loadRomBank(currentRomBank)
            } else if (address < 0x4000) { //9 bit of main.kotlin.ROM bank number
                currentRomBank = currentRomBank and ((value.code and 0x1) shl 8)
                loadRomBank(currentRomBank)
            } else if (address < 0x6000) {
                currentRamBank = (value.code and 3)
                saveRamBank(currentRamBank)
            }
        } else {
            setMemoryMBC0(address, value)
        }
    }


    //Getting from main.kotlin.Memory
    fun getMemory(address: Int): Char {
        if (address == 0xff00) return displayFrame!!.getJoypad((memory[address].code and 0xff).toChar())
            .toChar()
        else if (address >= 0x8000 && address <= 0x9fff) {
            if (!lcdOn || ppuMode == 3) return 0xff.toChar()
        } else if (address >= 0xfe00 && address <= 0xfe9f) {
            if (ppuMode == 2 || ppuMode == 3) return 0xff.toChar()
        }
        return (memory[address].code and 0xff).toChar()
    }

    fun getMemoryPriv(address: Int): Char {
        return (memory[address].code and 0xff).toChar()
    }

    //Save RAM Bank
    private fun saveRamBank(bankNumber: Int) {
        System.arraycopy(memory, 0xa000, ramBank!![bankNumber], 0, ramBank!![bankNumber]!!.size)
    }

    //Load RAM Bank
    private fun loadRamBank(bankNumber: Int) {
        System.arraycopy(ramBank!![bankNumber], 0, memory, 0xa000, ramBank!![bankNumber]!!.size)
    }

    //Load main.kotlin.ROM Bank
    private fun loadRomBank(bankNumber: Int) {
        System.arraycopy(romBank[bankNumber], 0, memory, 0x4000, 0x4000)
    }

    private fun loadVRamBank(bankNumber: Int) {
        System.arraycopy(memory, 0x8000, cgbVramBank[currentCgbVramBank], 0, 0x2000)
        System.arraycopy(cgbVramBank[bankNumber], 0, memory, 0x8000, 0x2000)

        currentCgbVramBank = bankNumber
    }

    private fun loadWorkRamBank(bankNumber: Int) {
        if (bankNumber == 0) loadWorkRamBank(1)
        else {
            System.arraycopy(memory, 0xc000, cgbWorkRamBank[currentCgbWorkRamBank], 0, 0x1000)
            System.arraycopy(cgbWorkRamBank[bankNumber], 0, memory, 0xd000, 0x1000)

            currentCgbWorkRamBank = bankNumber
        }
    }


    //Store Big main.kotlin.ROM's
    fun storeCartridge(cartridge: ByteArray) {
        for (j in romBank.indices) {
            var i = 0
            while (i < 0x4000 && (cartridge.size - (0x4000 * j)) > 0) {
                romBank[j]!![i] = Char(cartridge[i + j * 0x4000].toUShort())
                i++
            }
        }
    }

    //Do DMA Transfers to OAM
    private fun doDMA(value: Char) {
        val address = (value.code and 0xff) * 0x100
        for (i in 0..0x9f) {
            writePriv(0xfe00 + i, getMemory(address + i))
        }
    }

    //Debug
    fun dumpMemory() {
        print("0 ")
        for (i in 0..0xffff) {
            if (i % 16 == 0 && i != 0) {
                println(" ")
                print(Integer.toHexString(i) + " ")
                print(Integer.toHexString(getMemory(i).code and 0xff) + " ")
            } else print(Integer.toHexString(getMemory(i).code and 0xff) + " ")
        }
    }

    //Initialize RAM Bank
    fun initRamBank(size: Int) {
        when (size) {
            0 -> ramBank = null
            1 -> ramBank = Array<CharArray?>(1) { CharArray(0x800) }
            2 -> ramBank = Array<CharArray?>(1) { CharArray(0x2000) }
            3 -> ramBank = Array<CharArray?>(4) { CharArray(0x2000) }
            4 -> ramBank = Array<CharArray?>(16) { CharArray(0x2000) }
            else -> println("Invalid value!")
        }
    }

    //Initialize main.kotlin.ROM Bank
    fun initRomBank(size: Int) {
        when (size) {
            1 -> romBank = Array<CharArray?>(4) { CharArray(0x4000) }
            2 -> romBank = Array<CharArray?>(8) { CharArray(0x4000) }
            3 -> romBank = Array<CharArray?>(16) { CharArray(0x4000) }
            4 -> romBank = Array<CharArray?>(32) { CharArray(0x4000) }
            5 -> romBank = Array<CharArray?>(64) { CharArray(0x4000) }
            6 -> romBank = Array<CharArray?>(128) { CharArray(0x4000) }
            0x52 -> romBank = Array<CharArray?>(72) { CharArray(0x4000) }
            0x53 -> romBank = Array<CharArray?>(80) { CharArray(0x4000) }
            0x54 -> romBank = Array<CharArray?>(96) { CharArray(0x4000) }
        }
    }

    //Initialize main.kotlin.Memory Status
    private fun init() {
        writePriv(0xff00, 0xcf.toChar())
        writePriv(0xff0f, 0xe0.toChar())
        writePriv(0xff10, 0x80.toChar())
        writePriv(0xff11, 0xbf.toChar())
        writePriv(0xff12, 0xf3.toChar())
        writePriv(0xff14, 0xbf.toChar())
        writePriv(0xff16, 0x3f.toChar())
        writePriv(0xff19, 0xbf.toChar())
        writePriv(0xff1a, 0x7f.toChar())
        writePriv(0xff1b, 0xff.toChar())
        writePriv(0xff1c, 0x9f.toChar())
        writePriv(0xff1e, 0xbf.toChar())
        writePriv(0xff20, 0xff.toChar())
        writePriv(0xff23, 0xbf.toChar())
        writePriv(0xff24, 0x77.toChar())
        writePriv(0xff25, 0xf3.toChar())
        writePriv(0xff26, 0xf1.toChar())
        writePriv(0xff40, 0x91.toChar())
        writePriv(0xff41, 0x80.toChar())
        writePriv(0xff47, 0xfc.toChar())
        writePriv(0xff48, 0xff.toChar())
        writePriv(0xff49, 0xff.toChar())
        writePriv(0xff4d, 0xff.toChar())
        writePriv(0xff44, 0x0.toChar())
    }

    //Utils
    //Set Bit n of main.kotlin.Memory Address
    fun setBit(address: Int, bit: Int) {
        setMemory(address, (memory[address].code or (1 shl bit)).toChar())
    }

    //Reset Bit n of main.kotlin.Memory Address
    fun resetBit(address: Int, bit: Int) {
        setMemory(address, (memory[address].code and ((1 shl bit).inv())).toChar())
    }

    //Test Bit n of main.kotlin.Memory Address
    fun testBit(address: Int, bit: Int): Boolean {
        return ((memory[address].code and 0xff) and (1 shl bit)) shr bit != 0
    }

    //Reset main.kotlin.Memory State to Default
    fun reset() {
        resetMemory()
        init()
    }

    fun storeWordInSP(stackPointer: Int, programCounter: Int) {
        var stackPointer = stackPointer
        setMemory(--stackPointer, ((programCounter and 0xff00) shr 8).toChar())
        setMemory(--stackPointer, (programCounter and 0xff).toChar())

        cpu!!.increaseStackPointer(-2)
    }

    @Throws(FileNotFoundException::class)
    private fun saveRam() {
        val save = ByteArray(ramBank!![currentRamBank]!!.size)
        for (i in ramBank!![currentRamBank]!!.indices) save[i] =
            ramBank!![currentRamBank]!![i].code.toByte()
        val file = File(gameFileName + ".sav")
        try {
            FileOutputStream(file).use { fileOutputStream ->
                fileOutputStream.write(save)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    private fun loadRam() {
        val save: ByteArray?
        val saveFile = File(gameFileName + ".sav")
        if (!saveFile.exists()) return
        save = Files.readAllBytes(saveFile.toPath())
        for (i in 0..0x1fff) memory[0xa000 + i] = Char(save[i].toUShort())
    }

    //Constructor
    @Throws(IOException::class)
    fun Memory(cpu: CPU) {
        this.cpu = cpu
        resetMemory()
        init()
    }
}