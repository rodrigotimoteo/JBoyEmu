package com.github.rodrigotimoteo.kboyemu.kotlin;

import java.io.*;
import java.nio.file.Files;

public class ROM {
    private static String romTitle;

    private static final int TITLE_START = 0x0134;
    private static final int TITLE_END = 0x0142;
    private static final int CONSOLE_REGISTER = 0x0143;
    private static final int CARTRIDGE_TYPE_REGISTER = 0x0147;
    private static final int ROM_SIZE_REGISTER = 0x0148;
    private static final int RAM_SIZE_REGISTER = 0x0149;
    //private static final int REGION_REGISTER = 0x014a;
    //private static final int LICENSE_REGISTER = 0x014b;

    private static final int ROM_BANK_0 = 0;
    private static final int ROM_BANK_1 = 0x4000;

    public static byte[] romContent;

    public static String getRomTitle() {
        return romTitle;
    }

    public static void loadProgram(File rom, Memory memory) {
        try {
            romContent = Files.readAllBytes(rom.toPath());

            if ((romContent[CONSOLE_REGISTER] & 0xff) == 0x80) {
                System.err.println("Error - Emulator does not support Gameboy Color");
            } //COLOR GB INDICATOR

            StringBuilder stringB = new StringBuilder();
            for (int i = TITLE_START, counter = 0; i <= TITLE_END; i++, counter++) {
                if (romContent[i] != 0)
                    stringB.append((char) romContent[i]);
                else break;
            }
            romTitle = stringB.toString();

            int cartridgeType = romContent[CARTRIDGE_TYPE_REGISTER];

            for (int i = ROM_BANK_0; i < ROM_BANK_0 + 0x4000; i++) {
                memory.writePriv(i, (char) romContent[i]);
            }

            int romSizeInt = romContent[ROM_SIZE_REGISTER];
            int ramSizeInt = romContent[RAM_SIZE_REGISTER];

            memory.setCartridgeType(cartridgeType);
            System.out.println(cartridgeType);

            switch (cartridgeType) {
                case 0 -> {
                    for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                        memory.writePriv(i, (char) romContent[i]);
                    }
                }
                case 1, 17 -> { //main.kotlin.ROM+MBC1 / main.kotlin.ROM+MBC3
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                }
                case 2, 18 -> { //main.kotlin.ROM+MBC1+RAM / main.kotlin.ROM+MBC3+RAM
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                    memory.initRamBank(ramSizeInt);
                }
                case 3, 19 -> { //main.kotlin.ROM+MBC1+RAM+BATT / main.kotlin.ROM+MBC3+RAM+BATT
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                    memory.initRamBank(ramSizeInt);
                    memory.setHasBattery(true);
                }
                case 5 -> { //main.kotlin.ROM+MBC2
                    if(romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                    memory.setLittleRam(true);
                }
                case 6 -> { //main.kotlin.ROM+MBC2+BATT
                    if(romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                    memory.setLittleRam(true);
                    memory.setHasBattery(true);
                }
                case 8 -> { //main.kotlin.ROM+RAM
                    for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                        memory.writePriv(i, (char) romContent[i]);
                    }
                    memory.initRamBank(ramSizeInt);
                }
                case 9 -> { //ROM_RAM_BATTERY
                    for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                        memory.writePriv(i, (char) romContent[i]);
                    }
                    memory.initRamBank(ramSizeInt);
                    memory.setHasBattery(true);
                }
                case 15 -> { //main.kotlin.ROM+MBC3+TIMER+BATT
                    if(romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                    memory.setHasTimer(true);
                    memory.setHasBattery(true);
                }
                case 16 -> { //main.kotlin.ROM+MBC3+TIMER+RAM+BATT
                    if(romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = ROM_BANK_1; i < ROM_BANK_1 + 0x4000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                    memory.initRamBank(ramSizeInt);
                    memory.setHasTimer(true);
                    memory.setHasBattery(true);
                }
            }

            //romContent[REGION_REGISTER];

//            switch (romContent[LICENSE_REGISTER] & 0xff) {
//                case 0x33 -> {}
//                    //CHECK 0144/0145 for code
//                case 0x79 -> {}
//            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not read main.kotlin.ROM");
        }
    }
}
