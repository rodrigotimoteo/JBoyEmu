package com.github.rodrigotimoteo.kboyemucore.memory.rom

import com.github.rodrigotimoteo.kboyemucore.memory.Memory
import java.io.File
import java.io.IOException
import java.nio.file.Files

class ROM {
    var romTitle: String? = null

    val TITLE_START: Int = 0x0134
    val TITLE_END: Int = 0x0142
    val CONSOLE_REGISTER: Int = 0x0143
    val CARTRIDGE_TYPE_REGISTER: Int = 0x0147
    val ROM_SIZE_REGISTER: Int = 0x0148
    val RAM_SIZE_REGISTER: Int = 0x0149


    //private static final int REGION_REGISTER = 0x014a;
    //private static final int LICENSE_REGISTER = 0x014b;
    val ROM_BANK_0: Int = 0
    val ROM_BANK_1: Int = 0x4000

    lateinit var romContent: ByteArray

    fun getRomTitle(): String? {
        return romTitle
    }

    fun loadProgram(rom: File, memory: Memory) {
        try {
            romContent = Files.readAllBytes(rom.toPath())

            if ((romContent[CONSOLE_REGISTER].toInt() and 0xff) == 0x80) {
                System.err.println("Error - Emulator does not support Gameboy Color")
            } //COLOR GB INDICATOR


            val stringB = StringBuilder()
            run {
                var i = TITLE_START
                var counter = 0
                while (i <= TITLE_END) {
                    if (romContent[i].toInt() != 0) stringB.append(Char(romContent[i].toUShort()))
                    else break
                    i++
                    counter++
                }
            }
            romTitle = stringB.toString()

            val cartridgeType = romContent[CARTRIDGE_TYPE_REGISTER].toInt()

            for (i in ROM_BANK_0..<ROM_BANK_0 + 0x4000) {
                memory.writePriv(i, Char(romContent[i].toUShort()))
            }

            val romSizeInt = romContent[ROM_SIZE_REGISTER].toInt()
            val ramSizeInt = romContent[RAM_SIZE_REGISTER].toInt()

            memory.setCartridgeType(cartridgeType)
            println(cartridgeType)

            when (cartridgeType) {
                0 -> {
                    for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                        memory.writePriv(i, Char(romContent[i].toUShort()))
                    }
                }

                1, 17 -> { //main.kotlin.ROM+MBC1 / main.kotlin.ROM+MBC3
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt)
                        memory.storeCartridge(romContent)
                    } else {
                        for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                            memory.writePriv(i, Char(romContent[i].toUShort()))
                        }
                    }
                }

                2, 18 -> { //main.kotlin.ROM+MBC1+RAM / main.kotlin.ROM+MBC3+RAM
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt)
                        memory.storeCartridge(romContent)
                    } else {
                        for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                            memory.writePriv(i, Char(romContent[i].toUShort()))
                        }
                    }
                    memory.initRamBank(ramSizeInt)
                }

                3, 19 -> { //main.kotlin.ROM+MBC1+RAM+BATT / main.kotlin.ROM+MBC3+RAM+BATT
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt)
                        memory.storeCartridge(romContent)
                    } else {
                        for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                            memory.writePriv(i, Char(romContent[i].toUShort()))
                        }
                    }
                    memory.initRamBank(ramSizeInt)
                    memory.setHasBattery(true)
                }

                5 -> { //main.kotlin.ROM+MBC2
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt)
                        memory.storeCartridge(romContent)
                    } else {
                        for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                            memory.writePriv(i, Char(romContent[i].toUShort()))
                        }
                    }
                    memory.setLittleRam(true)
                }

                6 -> { //main.kotlin.ROM+MBC2+BATT
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt)
                        memory.storeCartridge(romContent)
                    } else {
                        for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                            memory.writePriv(i, Char(romContent[i].toUShort()))
                        }
                    }
                    memory.setLittleRam(true)
                    memory.setHasBattery(true)
                }

                8 -> { //main.kotlin.ROM+RAM
                    for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                        memory.writePriv(i, Char(romContent[i].toUShort()))
                    }
                    memory.initRamBank(ramSizeInt)
                }

                9 -> { //ROM_RAM_BATTERY
                    for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                        memory.writePriv(i, Char(romContent[i].toUShort()))
                    }
                    memory.initRamBank(ramSizeInt)
                    memory.setHasBattery(true)
                }

                15 -> { //main.kotlin.ROM+MBC3+TIMER+BATT
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt)
                        memory.storeCartridge(romContent)
                    } else {
                        for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                            memory.writePriv(i, Char(romContent[i].toUShort()))
                        }
                    }
                    memory.setHasTimer(true)
                    memory.setHasBattery(true)
                }

                16 -> { //main.kotlin.ROM+MBC3+TIMER+RAM+BATT
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt)
                        memory.storeCartridge(romContent)
                    } else {
                        for (i in ROM_BANK_1..<ROM_BANK_1 + 0x4000) {
                            memory.writePriv(i, Char(romContent[i].toUShort()))
                        }
                    }
                    memory.initRamBank(ramSizeInt)
                    memory.setHasTimer(true)
                    memory.setHasBattery(true)
                }
            }

            //romContent[REGION_REGISTER];

//            switch (romContent[LICENSE_REGISTER] & 0xff) {
//                case 0x33 -> {}
//                    //CHECK 0144/0145 for code
//                case 0x79 -> {}
//            }
        } catch (e: IOException) {
            e.printStackTrace()
            System.err.println("Could not read main.kotlin.ROM")
        }
    }
}