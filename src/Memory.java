import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class Memory {

    private String gameFileName;

    private final char[] memory = new char[0x10000];
    private char[][] romBank;   //Only used when MBC's are needed
    private char[][] ramBank;

    private char[][] cgbWorkRamBank;
    private char[][] cgbVramBank;

    private final int DIV = 0xff04;
    private final int TIMA = 0xff05;
    private final int TAC = 0xff07;
    private final int SOUND1 = 0xff14;

    private boolean ramOn = false;
    private boolean littleRam = false;
    private boolean hasBattery = false;
    private boolean hasTimer = false;
    private boolean lcdOn = false;
    private boolean cgb;

    private int memoryModel = 0; //ROM = 0 RAM = 1
    private int ppuMode = 0;
    private int latchReg = 0;
    private int currentRomBank;
    private int currentRamBank;

    private int currentCgbVramBank = 0;
    private int currentCgbWorkRamBank = 1;

    private final int ROM_LIMIT = 0x8000;

    private CPU cpu;
    DisplayFrame displayFrame;

    private int cartridgeType;

    //Resets

    private void resetMemory() {
        Arrays.fill(memory, (char) 0 );
    }

    //Setters

    public void setCartridgeType(int cartridgeType) {
        this.cartridgeType = cartridgeType;

        try {
            loadRam();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setHasBattery(boolean state) {
        hasBattery = state;
    }

    public void setLittleRam(boolean state) {
        littleRam = state;
    }

    public void setHasTimer(boolean state) {
        hasTimer = state;
    }

    public void setDisplayFrame(DisplayFrame displayFrame) {
        this.displayFrame = displayFrame;
    }

    public void setLcdOn(boolean state) {
        lcdOn = state;
    }

    public void setPpuMode(int value) {
        ppuMode = value;
    }

    public void setGameFileName(String name) {
        gameFileName = name;
    }

    public void setCgbMode() {
        cgbVramBank = new char[2][0x2000];
        cgbWorkRamBank = new char[7][0x1000];

        cpu.setCgbMode();

        cgb = true;
    }

    //Writing to Memory

    public void writePriv(int address, char value) {
        memory[address] = (char) (value & 0xff);
    }

    public void setMemory(int address, char value) {
        setMemoryMBC0(address, value);
        switch(cartridgeType) {
            case 0, 8, 9 -> setMemoryMBC0(address, value);
            case 1, 2, 3 -> setMemoryMBC1(address, value);
            case 5, 6 -> setMemoryMBC2(address, value);
            case 0xf, 0x10, 0x11, 0x12, 0x13 -> setMemoryMBC3(address, value);
            case 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e -> setMemoryMBC5(address, value);
            default -> setMemoryMBC0(address, value);
        }
    }

    private void setMemoryMBC0(int address, char value) {
        if(address == 0xff44) {} //Check currentLine
        else if(address == DIV) {  //Check timer
            cpu.resetClocks();
            memory[address] = 0;
        }
        else if(address == TAC) {
            int oldTac = memory[address] & 0x3;
            if(oldTac == 0 && (value & 0x03) == 1 && (value & 0x4) != 0) setMemory(TIMA, (char) (memory[TIMA] + 1));

            memory[address] = (char) (value & 0xff);
        }
        else if(address == 0xff26) { //Check sound enable/disable
            if(value != 0) memory[address] = 0xff;
            else memory[address] = 0;
        }
        else if(address == 0x0143 && !cgb) {
            setCgbMode();
        }
        else if(address == 0xff46) {
            doDMA(value);
        }
        else if(address == 0xff0f) {
            memory[address] = (char) ((0xe0 | value) & 0xff);
        }
        else if(address == SOUND1 && ((value & 0xff) >> 7) != 0) { //Check Sound Channel 1
            memory[address] = value;
        }
        else if(address >= 0xc000 && address <= 0xde00) { //Check Ram Echo
            memory[address] = (char) (value & 0xff);
            memory[address + 0x2000] = (char) (value & 0xff);
        }
        else if(cgb && address == 0xff4f) {
            if((value & 0x1) != currentCgbVramBank) {
                loadVRamBank(value & 0x1);
            }
        }
        else if(cgb && address == 0xff70) {
            if((value & 0x7) != currentCgbWorkRamBank) {
                loadWorkRamBank(value & 0x7);
            }
        }
//        else if(address >= 0x8000 && address <= 0x9fff) {
//            if(!lcdOn || ppuMode == 3) return;
//        }
//        else if(address >= 0xfe00 && address <= 0xfe9f) {
//            if(ppuMode == 2 || ppuMode == 3) return;
//        }
        else if(address >= 0xe000 && address <= 0xfe00) { //Check Ram Echo
            memory[address] = (char) (value & 0xff);
            memory[address - 0x2000] = (char) (value & 0xff);
        }
        else if(address >= 0xa000 && address <= 0xbfff && ramOn && currentRamBank < 4) {
            memory[address] = (char) (value & 0xff);
        }
        else if(address >= 0xa000 && address <= 0xbfff && ramOn && currentRamBank >= 4) {

        }
        else if(address > ROM_LIMIT) {
            memory[address] = (char) (value & 0xff);
        }
    }

    private void setMemoryMBC1(int address, char value) {
        if(address < ROM_LIMIT) { //Memory Bank Controller
            if(address < 0x2000) { //RAM ENABLE
                ramOn = (value & 0b0000_1111) == 0b0000_1010;
            }
            else if(address < 0x4000) { //ROM Bank Number
                if(currentRomBank != (value & 0b0001_1111)) {
                    currentRomBank = (value & 0b0001_1111);
                    if(currentRomBank == 0 || currentRomBank == 1)
                        loadRomBank(1);
                    else if(romBank.length < 0x20)
                        loadRomBank(currentRomBank % romBank.length);
                }
            }
            else if(address < 0x6000) { //RAM
                if(memoryModel == 0 && ramBank != null) {
                    currentRomBank = (value & 0b0000_0011) << 5;
                    saveRamBank(currentRamBank);
                    currentRamBank = 0;
                    loadRamBank(currentRamBank);
                }
                else if(ramBank != null) {
                    currentRamBank = (value & 0b0000_0011);
                    saveRamBank(currentRamBank);
                }
            }
            else { //ROM/RAM Select
                memoryModel = (value & 0x1) == 1 ? 1 : 0;
            }
        } else {
            setMemoryMBC0(address, value);
        }
    }

    private void setMemoryMBC2(int address, char value) {
        if(address < ROM_LIMIT) { //Memory Bank Controller
            if(address < 0x2000) { //RAM ENABLE
                ramOn = (value & 0b0000_1111) == 0b0000_1010;
            }
            else if(address < 0x4000) { //ROM Bank Number
                if(currentRomBank != (value & 0b0001_1111)) {
                    currentRomBank = (value & 0b0001_1111);
                    if(currentRomBank == 0 || currentRomBank == 1)
                        loadRomBank(1);
                    else
                        loadRomBank(currentRomBank);
                }
            }
        } else {
            //else if(address)
            setMemoryMBC0(address, value);
        }
    }

    private void setMemoryMBC3(int address, char value) {
        if(address < ROM_LIMIT) { //Memory Bank Controller
            if(address < 0x2000) { //RAM ENABLE
                ramOn = (value & 0b1111) == 0b1010;
                if(!ramOn) {
                    try {
                        saveRam();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else if(address < 0x4000) { //ROM Bank Number
                if(currentRomBank != (value & 0b01111111)) {
                    currentRomBank = (value & 0b01111111);
                    if(currentRomBank == 0 || currentRomBank == 1)
                        loadRomBank(1);
                    else
                        loadRomBank(currentRomBank);
                }
            }
            else if(address < 0x6000) { //RAM
                if((value & 0xff) < 4) {
                    if(memoryModel == 0 && ramBank != null) {
                        currentRomBank = (value & 0b0000_0011) << 5;
                        saveRamBank(currentRamBank);
                        currentRamBank = 0;
                        loadRamBank(currentRamBank);
                    }
                    else if(ramBank != null) {
                        currentRamBank = (value & 0b0000_0011);
                        saveRamBank(currentRamBank);
                    }
                }
                else {
                    if(value == 1 && latchReg == 0) {

                    } else {

                    }
                }
            }
            else { //Latch Clock Data

            }
        } else {
            setMemoryMBC0(address, value);
        }
    }

    private void setMemoryMBC5(int address, char value) {
        if(address < ROM_LIMIT) { //Memory Bank Controller
            if(address < 0x2000) { //RAM ENABLE
                ramOn = (value & 0b00001111) == 0b00001010;
            }
            else if(address < 0x3000) { //8 least significant bits of ROM bank number
                currentRomBank &= value;
                loadRomBank(currentRomBank);
            }
            else if(address < 0x4000) { //9 bit of ROM bank number
                currentRomBank &= (value & 0x1) << 8;
                loadRomBank(currentRomBank);
            }
            else if(address < 0x6000) {
                currentRamBank = (value & 0b00000011);
                saveRamBank(currentRamBank);
            }
        } else {
            setMemoryMBC0(address, value);
        }
    }

    //Getting from Memory

    public char getMemory(int address) {
        if(address == 0xff00) return (char) displayFrame.getJoypad((char) (memory[address] & 0xff));
        else if(address >= 0x8000 && address <= 0x9fff) {
            if(!lcdOn || ppuMode == 3) return 0xff;
        }
        else if(address >= 0xfe00 && address <= 0xfe9f) {
            if(ppuMode == 2 || ppuMode == 3) return 0xff;
        }
        return (char) (memory[address] & 0xff);
    }

    public char getMemoryPriv(int address) {
        return (char) (memory[address] & 0xff);
    }

    //Save RAM Bank
    private void saveRamBank(int bankNumber) {
        System.arraycopy(memory, 0xa000, ramBank[bankNumber], 0, ramBank[bankNumber].length);
    }

    //Load RAM Bank
    private void loadRamBank(int bankNumber) {
        System.arraycopy(ramBank[bankNumber], 0, memory, 0xa000, ramBank[bankNumber].length);
    }

    //Load ROM Bank
    private void loadRomBank(int bankNumber) {
        System.arraycopy(romBank[bankNumber], 0, memory, 0x4000, 0x4000);
    }

    private void loadVRamBank(int bankNumber) {
        System.arraycopy(memory, 0x8000, cgbVramBank[currentCgbVramBank], 0, 0x2000);
        System.arraycopy(cgbVramBank[bankNumber], 0, memory, 0x8000, 0x2000);

        currentCgbVramBank = bankNumber;
    }

    private void loadWorkRamBank(int bankNumber) {
        if(bankNumber == 0) loadWorkRamBank(1);
        else {
            System.arraycopy(memory, 0xc000, cgbWorkRamBank[currentCgbWorkRamBank], 0, 0x1000);
            System.arraycopy(cgbWorkRamBank[bankNumber], 0, memory, 0xd000, 0x1000);

            currentCgbWorkRamBank = bankNumber;
        }
    }


    //Store Big ROM's
    public void storeCartridge(byte[] cartridge) {
        for(int j = 0; j < romBank.length; j++) {
            for(int i = 0; i < 0x4000 && (cartridge.length - (0x4000 * j)) > 0; i++) {
                romBank[j][i] = (char) cartridge[i + j * 0x4000];
            }
        }
    }

    //Do DMA Transfers to OAM
    private void doDMA(char value) {
        int address = (value & 0xff) * 0x100;
        for(int i = 0; i < 0xa0; i++) {
            writePriv(0xfe00 + i, getMemory(address + i));
        }
    }

    //Debug
    public void dumpMemory() {
        System.out.print("0 ");
        for(int i = 0; i < 0x10000; i++) {
            if(i % 16 == 0 && i != 0) { System.out.println(" "); System.out.print(Integer.toHexString(i ) + " "); System.out.print(Integer.toHexString(getMemory(i) & 0xff) + " ");  }
            else System.out.print(Integer.toHexString(getMemory(i) & 0xff) + " ");
        }
    }

    //Initialize RAM Bank
    public void initRamBank(int size) {
        switch(size) {
            case 0 -> ramBank = null;
            case 1 -> ramBank = new char[1][0x800];
            case 2 -> ramBank = new char[1][0x2000];
            case 3 -> ramBank = new char[4][0x2000];
            case 4 -> ramBank = new char[16][0x2000];
            default -> System.out.println("Invalid value!");
        }
    }

    //Initialize ROM Bank
    public void initRomBank(int size) {
        switch(size) {
            case 1 -> romBank = new char[4][0x4000];
            case 2 -> romBank = new char[8][0x4000];
            case 3 -> romBank = new char[16][0x4000];
            case 4 -> romBank = new char[32][0x4000];
            case 5 -> romBank = new char[64][0x4000];
            case 6 -> romBank = new char[128][0x4000];
            case 0x52 -> romBank = new char[72][0x4000];
            case 0x53 -> romBank = new char[80][0x4000];
            case 0x54 -> romBank = new char[96][0x4000];
        }
    }

    //Initialize Memory Status
    private void init() {
        writePriv(0xff00, (char) 0xcf);
        writePriv(0xff0f, (char) 0xe0);
        writePriv(0xff10, (char) 0x80);
        writePriv(0xff11, (char) 0xbf);
        writePriv(0xff12, (char) 0xf3);
        writePriv(0xff14, (char) 0xbf);
        writePriv(0xff16, (char) 0x3f);
        writePriv(0xff19, (char) 0xbf);
        writePriv(0xff1a, (char) 0x7f);
        writePriv(0xff1b, (char) 0xff);
        writePriv(0xff1c, (char) 0x9f);
        writePriv(0xff1e, (char) 0xbf);
        writePriv(0xff20, (char) 0xff);
        writePriv(0xff23, (char) 0xbf);
        writePriv(0xff24, (char) 0x77);
        writePriv(0xff25, (char) 0xf3);
        writePriv(0xff26, (char) 0xf1);
        writePriv(0xff40, (char) 0x91);
        writePriv(0xff41, (char) 0x80);
        writePriv(0xff47, (char) 0xfc);
        writePriv(0xff48, (char) 0xff);
        writePriv(0xff49, (char) 0xff);
        writePriv(0xff4d, (char) 0xff);
        writePriv(0xff44, (char) 0x0);
    }

    //Utils
    //Set Bit n of Memory Address
    public void setBit(int address, int bit) {
        setMemory(address, (char) (memory[address] | (1 << bit)));
    }

    //Reset Bit n of Memory Address
    public void resetBit(int address, int bit) {
        setMemory(address, (char) (memory[address] & (~(1 << bit))));
    }

    //Test Bit n of Memory Address
    public boolean testBit(int address, int bit) {
        return ((memory[address] & 0xff) & (1 << bit)) >> bit != 0;
    }

    //Reset Memory State to Default
    public void reset() {
        resetMemory();
        init();
    }

    public void storeWordInSP(int stackPointer, int programCounter) {
        setMemory(--stackPointer, (char) ((programCounter & 0xff00) >> 8));
        setMemory(--stackPointer, (char) (programCounter & 0xff));

        cpu.increaseStackPointer(-2);
    }

    private void saveRam() throws FileNotFoundException {
        byte[] save = new byte[ramBank[currentRamBank].length];
        for(int i = 0; i < ramBank[currentRamBank].length; i++) save[i] = (byte) ramBank[currentRamBank][i];
        File file = new File(gameFileName + ".sav");
        try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(save);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadRam() throws IOException {
        byte[] save;
        File saveFile = new File(gameFileName + ".sav");
        if(!saveFile.exists()) return;
        save = Files.readAllBytes(saveFile.toPath());
        for(int i = 0; i < 0x2000; i++) memory[0xa000 + i] = (char) save[i];
    }

    //Constructor
    public Memory(CPU cpu) throws IOException {
        this.cpu = cpu;
        resetMemory();
        init();
    }
}
