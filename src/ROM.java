import java.io.*;
import java.nio.file.Files;

public class ROM {

    private static String romTitle;
    private static int region;
    private static int licenseeCode;
    public static byte[] romContent;
    public static byte[] bootRomContent;

    public static String getRomTitle() {
        return romTitle;
    }

    public static void loadProgram(File rom, Memory memory) {
        try {
            romContent = Files.readAllBytes(rom.toPath());

            if (romContent[0x0143] == 80) {
                System.err.println("Error - Emulator does not support Gameboy Color");
                System.exit(1);
            } //COLOR GB INDICATOR

            StringBuilder stringB = new StringBuilder();
            for (int i = 0x0134, counter = 0; i <= 0x0142; i++, counter++) {
                if (romContent[i] != 0)
                    stringB.append((char) romContent[i]);
                else break;
            }
            romTitle = stringB.toString();

            int cartridgeType = romContent[0x0147];

            for (int i = 0; i < 0x4000; i++) {
                memory.writePriv(i, (char) romContent[i]);
            }

            int romSizeInt = romContent[0x0148];
            int ramSizeInt = romContent[0x0149];

            memory.setCartridgeType(cartridgeType);


            switch (cartridgeType) {
                case 0 -> {
                    for (int i = 0x4000; i < 0x8000; i++) {
                        memory.writePriv(i, (char) romContent[i]);
                    }
                }
                case 1 -> { //ROM+MBC1
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = 0x4000; i < 0x8000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                }
                case 2 -> { //ROM+MBC1+RAM
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = 0x4000; i < 0x8000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                    memory.initRamBank(ramSizeInt);
                }
                case 3 -> { //ROM+MBC1+RAM+BATT
                    if (romSizeInt != 0) {
                        memory.initRomBank(romSizeInt);
                        memory.storeCartridge(romContent);
                    } else {
                        for (int i = 0x4000; i < 0x8000; i++) {
                            memory.writePriv(i, (char) romContent[i]);
                        }
                    }
                    memory.initRamBank(ramSizeInt);
                    memory.setHasBattery(true);
                }
                case 5 -> { //ROM+MBC2

                }
                case 6 -> { //ROM+MBC2+BATT
                    memory.setHasBattery(true);
                }
                case 8 -> { //ROM+RAM
                    for (int i = 0x4000; i < 0x8000; i++) {
                        memory.writePriv(i, (char) romContent[i]);
                    }
                    memory.initRamBank(ramSizeInt);
                }
                case 9 -> { //ROM_RAM_BATTERY
                    for (int i = 0x4000; i < 0x8000; i++) {
                        memory.writePriv(i, (char) romContent[i]);
                    }
                    memory.initRamBank(ramSizeInt);
                    memory.setHasBattery(true);
                }
            }

            region = romContent[0x014A];

            switch (romContent[0x014B]) {
                case 33:

                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not read ROM");
        }
    }
}
