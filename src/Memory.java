import java.util.Arrays;

public class Memory {

    private final char[] memory = new char[0x10000];
    private char[][] romBank;   //Only used when MBC's are needed
    private char[][] ramBank;

    private boolean ramOn = false;
    private boolean hasBattery;

    private int memoryModel = 0; //ROM = 0 RAM = 1
    private int currentRomBank;
    private int currentRamBank;

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
    }

    public void setHasBattery(boolean state) {
        hasBattery = state;
    }

    public void setDisplayFrame(DisplayFrame displayFrame) {
        this.displayFrame = displayFrame;
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
        if(address == 0xff44 || address == 0xff04) {
            memory[address] = 0; //Check timer and currentLine
        }
        else if(address == 0xff26) { //Check sound enable/disable
            if(value != 0) memory[address] = 0xff;
            else memory[address] = 0;
        }
        else if(address == 0xff46) {
            doDMA(value);
        }
        else if(address == 0xff14 && ((value & 0xff) >> 7) != 0) { //Check Sound Channel 1
            memory[address] = value;
        }
        else if(address >= 0xc000 && address <= 0xde00) { //Check Ram Echo
            memory[address] = (char) (value & 0xff);
            memory[address + 0x2000] = (char) (value & 0xff);
        }
        else if(address >= 0xe000 && address <= 0xfe00) { //Check Ram Echo
            memory[address] = (char) (value & 0xff);
            memory[address - 0x2000] = (char) (value & 0xff);
        }
        else if(address == 0xff00) {
            if(value == 0x10) setMemory(address, (char) (0xc0 + (memory[0xff00] & 0xf) + 0x10));
            else if(value == 0x20) setMemory(address, (char) (0xc0 + (memory[0xff00] & 0xf) + 0x20));
            else if(value == 0x30) setMemory(address, (char) (0xc0 + (memory[0xff00] & 0xf)));
        }
        else if(address >= 0xa000 && address <= 0xbfff && ramOn) {
            memory[address] = (char) (value & 0xff);
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
                    loadRomBank(currentRomBank);
                }
            }
            else if(address < 0x6000) { //RAM
                currentRamBank = (value & 0b0000_0011);

            }
            else { //ROM/RAM Select
                memoryModel = (value & 0x1) == 1 ? 1 : 0;
            }
        } else if(ramOn) {
            if(memoryModel == 0) {

            } else {

            }
        }
    }

    private void setMemoryMBC3(int address, char value) {
        if(address < ROM_LIMIT) { //Memory Bank Controller
            if(address < 0x2000) { //RAM ENABLE
                ramOn = (value & 0b0000_1111) == 0b0000_1010;
            }
            else if(address < 0x4000) { //ROM Bank Number
                if(currentRomBank != (value & 0b0001_1111)) {
                    currentRomBank = (value & 0b0001_1111);
                    loadRomBank(currentRomBank);
                }
            }
            else if(address < 0x6000) { //RAM
                currentRamBank = (value & 0b0000_0011);

            }
            else { //ROM/RAM Select
                memoryModel = (value & 0x1) == 1 ? 1 : 0;
            }
        } else if(ramOn) {
            if(memoryModel == 0) {

            } else {

            }
        }
    }

    private void setMemoryMBC5(int address, char value) {
        if(address < ROM_LIMIT) { //Memory Bank Controller
            if(address < 0x2000) { //RAM ENABLE
                ramOn = (value & 0b0000_1111) == 0b0000_1010;
            }
            else if(address < 0x4000) { //ROM Bank Number
                if(currentRomBank != (value & 0b0001_1111)) {
                    currentRomBank = (value & 0b0001_1111);
                    loadRomBank(currentRomBank);
                }
            }
            else if(address < 0x6000) { //RAM
                currentRamBank = (value & 0b0000_0011);

            }
            else { //ROM/RAM Select
                memoryModel = (value & 0x1) == 1 ? 1 : 0;
            }
        } else if(ramOn) {
            if(memoryModel == 0) {

            } else {

            }
        }
    }

    //Getting from Memory

    public char getMemory(int address) {
        if(address == 0xff00) return (char) displayFrame.getJoypad((char) (memory[address] & 0xff));
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
            setMemory(0xfe00 + i, getMemory(address + i));
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
        writePriv(0xff44, (char) 0x90);
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

    //Constructor
    public Memory(CPU cpu) {
        this.cpu = cpu;
        resetMemory();
        init();
    }


}
