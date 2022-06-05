import java.io.*;
import java.nio.file.Files;

public class ROM {

    private static String romTitle;
    private static int romSize = 2; //Starts at 2 Banks by Default because it is the minimum value
    private static int ramSize;
    private static int region;
    private static int licenseeCode;
    private static int cartridgeType;
    public static byte[] romContent;

    public static String getRomTitle() {
        return romTitle;
    }

    public int getCartridgeType() {
        return cartridgeType;
    }

    public int getRomSize() {
        return romSize;
    }

    public int getRamSize() {
        return ramSize;
    }

    public static void loadProgram(File rom, Memory memory) {
        try {
            romContent = Files.readAllBytes(rom.toPath());

            if(romContent[0x0143] == 80) {
                System.err.println("Error - Emulator does not support Gameboy Color");
                System.exit(1);
            } //COLOR GB INDICATOR

            StringBuilder stringB = new StringBuilder();
            for(int i = 0x0134, counter = 0; i <= 0x0142; i++, counter++) {
                if(romContent[i] != 0)
                    stringB.append((char)romContent[i]);
                else break;
            }
            romTitle = stringB.toString();

            cartridgeType = romContent[0x0147];

            for(int i = 0; i < 0x7fff; i++) {
                memory.writePriv(i, (char) romContent[i]);
            }

            switch (cartridgeType) {
                case 0 -> { //ROM ONLY
                    for (int i = 0; i < 0x2000; i++) {
                        memory.writePriv(0xA000 + i, (char) 0xff);
                    }
                }
                case 1 -> { //ROM+MBC1

                }
                case 8 -> { //ROM_RAM

                }
                case 9 -> { //ROM_RAM_BATTERY
                }
            }
            memory.setCartridgeType(cartridgeType);

            int romSizeInt = romContent[0x0148];

            for(int i = 0; i < romSizeInt; i++) {
                romSize *= 2;
            }

            int ramSizeInt = romContent[0x0149];

            switch (ramSizeInt) {
                case 0, 1 -> ramSize = 1;
                case 3 -> ramSize = 4;
                case 4 -> ramSize = 16;
            }

            region = romContent[0x014A];

            switch(romContent[0x014B]) {
                case 33:

                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not read ROM");
        }
    }
}
