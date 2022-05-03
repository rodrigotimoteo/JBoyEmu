import java.util.Arrays;

public class CPU {

    private final boolean DEBUGMODE = true;

    char[] registers = new char[8]; //AF, BC, DE and HL can be 16 bits if paired together
    boolean zeroFlag;
    boolean subtractFlag;
    boolean halfCarryFlag;
    boolean carryFlag;
    char operationCode;
    char programCounter = 0x0100;
    char stackPointer = 0xFFFE;

    int counter = 0;

    boolean continueFlag = true;
    boolean interruptMasterEnable = false;

    String romName = "./src/tetris.gb";

    Memory memory;
    DisplayFrame display;
    PPU ppu;

    //Resets

    private void clearRegisters() {
        Arrays.fill(registers, (char) 0);
    }

    //Geters

    public String getRomName() {
        return romName;
    }

    //Setters (for registers mainly)

    public void setRegister(int index, char value) {
        registers[index] = value;
    }

    public void setRegisters(int index1, int index2, char value1, char value2) {
        registers[index1] = value1;
        registers[index2] = value2;
    }

    //Debug

    private void dumpRegisters() {
        for(int i = 0; i < 8; i++)
            System.out.print(i + ":" + Integer.toHexString((registers[i]) & 0xff) + " ");
    }

    private void dumpFlags() {
        int zeroFlagINT;
        int subtractFlagINT;
        int halfCarryFlagINT;
        int carryFlagINT;

        if(zeroFlag)
            zeroFlagINT = 1;
        else
            zeroFlagINT = 0;

        if(subtractFlag)
            subtractFlagINT = 1;
        else
            subtractFlagINT = 0;

        if(halfCarryFlag)
            halfCarryFlagINT = 1;
        else
            halfCarryFlagINT = 0;

        if(carryFlag)
            carryFlagINT = 1;
        else
            carryFlagINT = 0;

        System.out.print(" Flags Z:" + zeroFlagINT + " N:" + subtractFlagINT + " H:" + halfCarryFlagINT + " C:" + carryFlagINT + "  ");
    }

    //Constructor

    public CPU() {
        clearRegisters();
        memory = new Memory(this);
        display = new DisplayFrame(memory);
        ppu = new PPU(this, memory, display);

        init();
    }

    private void init() {
        registers[0] = 0x1;
        registers[2] = 0xD;
        registers[4] = 0xD8;
        registers[5] = 0xB0;
        registers[6] = 0x1;
        registers[7] = 0x4D;

        zeroFlag = true;
        subtractFlag = false;
        halfCarryFlag = true;
        carryFlag = true;
    }

    private void computeFRegister() {
        registers[5] = 0;
        if(zeroFlag) registers[5] += 128;
        if(subtractFlag) registers[5] += 64;
        if(halfCarryFlag) registers[5] += 32;
        if(carryFlag) registers[5] += 16;
    }

    public void cycle() throws InterruptedException {
        int tempCounter = counter;
        char memValue = memory.getMemory(0xFF44);
        fetchOperationCodes();
        decodeOperationCodes();
        if(DEBUGMODE) {
            //dumpRegisters();
            //dumpFlags();
            //Thread.sleep(1000);
        }
        ppu.draw(counter - tempCounter, memValue);
    }

    private void fetchOperationCodes() {
        operationCode = (char) (memory.getCartridgeMemory(programCounter) & 0xff);
    }

    private void decodeOperationCodes() {
        int tempProgramCounter, carry;

        if(DEBUGMODE) {
            System.out.print(Integer.toHexString(programCounter) + "  ");
            System.out.print(Integer.toHexString(operationCode) + "  " + counter + "  ");
        }

        switch(operationCode) {
            case 0x00: //NOP IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("NOP");

                programCounter += 1;
                break;

            case 0x01: //LD BC,u16 IMPLEMENTED AND WORKING
                counter += 3;
                if(DEBUGMODE) System.out.println("LD BC, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff) + Integer.toHexString(memory.getCartridgeMemory(programCounter + 2) & 0xff));

                registers[1] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[2] = (char) memory.getCartridgeMemory(programCounter + 2);
                programCounter += 3;
                break;

            case 0x02: //LD (BC),A
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (BC), A");

                registers[1] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[2] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x03: //INC BC
                counter++;
                if(DEBUGMODE) System.out.println("INC BC");

                if(registers[2] == 255) {
                    registers[2] = 0;
                    registers[1]++;
                } else
                    registers[2]++;
                programCounter += 1;
                break;

            case 0x04: //INC B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("INC B");

                zeroFlag = (registers[1] & 0xff) == 0x00;
                halfCarryFlag = (registers[1] >> 4 & 0xf) != ((registers[1] + 1) >> 4 & 0xf);
                registers[1]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x05: //DEC B   IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("DEC C");

                zeroFlag = registers[1] - 1 == 0;
                halfCarryFlag = (registers[1] >> 4 & 0xf) != ((registers[1] - 1) >> 4 & 0xf);
                registers[1]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x06: //LD B,u8    IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD B, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                registers[1] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x07: //RLCA
                counter += 1;
                if(DEBUGMODE) System.out.println("RLCA");

                subtractFlag = false;
                halfCarryFlag = false;
                carry = (registers[4] & 0xff) >> 7;
                carryFlag = carry != 0;
                registers[0] = (char) (registers[0] << 1);
                zeroFlag = registers[0] == 0;
                computeFRegister();

                programCounter++;
                break;

            case 0x08: //LD (u16),SP
                counter += 3;
                if(DEBUGMODE) System.out.println("LD " + Integer.toHexString((memory.getCartridgeMemory(programCounter + 2) + (memory.getCartridgeMemory(programCounter + 1) << 4)) & 0xff) + ", SP");

                stackPointer = (char) (memory.getCartridgeMemory(programCounter + 2));
                stackPointer += (char) (memory.getCartridgeMemory(programCounter + 1) << 4);
                programCounter += 3;
                break;

            case 0x09: //ADD HL,BC
                counter++;
                if(DEBUGMODE) System.out.println("ADD HL, BC");

                if(registers[6] + registers[2] > 255) {
                    registers[6] = (char) (registers[6] + registers[2] - 255);
                    registers[7]++;
                } else
                    registers[6] += registers[2];

                if(registers[7] + registers[1] > 255)
                    registers[7] = (char) (registers[7] + registers[1] - 255);
                else
                    registers[7] += registers[1];
                programCounter += 1;
                break;

            case 0x0A: //LD A,(BC)
                counter++;
                if(DEBUGMODE) System.out.println("LD A, (BC)");

                registers[0] = registers[2];
                programCounter += 1;
                break;

            case 0x0B: //DEC BC
                counter++;
                if(DEBUGMODE) System.out.println("DEC BC");

                if(registers[2] == 0) {
                    registers[2] = 255;
                    registers[1]--;
                } else
                    registers[2]--;
                programCounter += 1;
                break;

            case 0x0C: //INC C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("INC C");

                zeroFlag = (registers[2] & 0xff) == 0x00;
                halfCarryFlag = (registers[2] >> 4 & 0xf) != ((registers[2] + 1) >> 4 & 0xf);
                registers[2]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x0D: //DEC C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("DEC C");

                zeroFlag = registers[2] - 1 == 0;
                halfCarryFlag = (registers[2] >> 4 & 0xf) != ((registers[2] - 1) >> 4 & 0xf);
                registers[2]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x0E: //LD C,u8 IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD C, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                registers[2] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x0F: //RRCA
                counter++;
                if(DEBUGMODE) System.out.println("RRCA");

                registers[0] = (char) (registers[0] >> 1);
                subtractFlag = false;
                halfCarryFlag = false;
                programCounter += 1;
                break;


            case 0x10: //STOP WAITING IMPLEMENTATION
                counter += 1;
                if(DEBUGMODE) System.out.println("WAIT");

                continueFlag = false;
                break;

            case 0x11: //LD DE,u16 IMPLEMENTED AND WORKING
                counter += 3;
                if(DEBUGMODE) System.out.println("LD DE, " + Integer.toHexString(((memory.getCartridgeMemory(programCounter + 1) << 8) + memory.getCartridgeMemory(programCounter + 2) & 0xff)));

                registers[3] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[4] = (char) memory.getCartridgeMemory(programCounter + 2);
                programCounter += 3;
                break;

            case 0x12: //LD (DE),A
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (DE), A");

                registers[3] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[4] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x13: //INC DE
                counter++;
                if(DEBUGMODE) System.out.println("INC DE");

                if(registers[4] == 255) {
                    registers[4] = 0;
                    registers[3]++;
                } else
                    registers[4]++;
                programCounter += 1;
                break;

            case 0x14: //INC D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("INC D");

                zeroFlag = (registers[3] & 0xff) == 0x00;
                halfCarryFlag = (registers[3] >> 4 & 0xf) != ((registers[3] + 1) >> 4 & 0xf);
                registers[3]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x15: //DEC D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("DEC D");

                zeroFlag = registers[3] - 1 == 0;
                halfCarryFlag = (registers[3] >> 4 & 0xf) != ((registers[3] - 1) >> 4 & 0xf);
                registers[3]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x16: //LD D,u8 IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD D, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                registers[3] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x17: //RLA NAO ENTENDI A IMPLEMENTACAO
                counter += 1;
                if(DEBUGMODE) System.out.println("RLA");

                break;

            case 0x18: //JR i8
                counter += 2;
                if(DEBUGMODE) System.out.println("JR " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                programCounter += memory.getCartridgeMemory(programCounter + 1);
                break;

            case 0x19: //ADD HL,DE
                counter++;
                if(DEBUGMODE) System.out.println("ADD HL, DE");

                if(registers[6] + registers[4] > 255) {
                    registers[6] = (char) (registers[6] + registers[4] - 255);
                    registers[7]++;
                } else
                    registers[6] += registers[4];

                if(registers[7] + registers[3] > 255)
                    registers[7] = (char) (registers[7] + registers[3] - 255);
                else
                    registers[7] += registers[3];
                programCounter += 1;
                break;

            case 0x1A: //LD A,(DE)
                counter++;
                if(DEBUGMODE) System.out.println("LD A,(DE)");

                registers[0] = registers[4];
                programCounter += 1;
                break;

            case 0x1B: //DEC DE
                counter++;
                if(DEBUGMODE) System.out.println("DEC DE");

                if(registers[4] == 0) {
                    registers[4] = 255;
                    registers[3]--;
                } else
                    registers[4]--;
                programCounter += 1;
                break;

            case 0x1C: //INC E
                counter++;
                if(DEBUGMODE) System.out.println("INC E");

                zeroFlag = (registers[4] & 0xff) == 0x00;
                halfCarryFlag = (registers[4] >> 4 & 0xf) != ((registers[4] + 1) >> 4 & 0xf);
                registers[4]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x1D: //DEC E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("DEC E");

                zeroFlag = registers[4] - 1 == 0;
                halfCarryFlag = (registers[4] >> 4 & 0xf) != ((registers[4] - 1) >> 4 & 0xf);
                registers[4]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x1E: //LD E,u8 IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD E, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                registers[4] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x1F: //RRA
                counter += 1;
                if(DEBUGMODE) System.out.println("RRA");

                break;

            case 0x20: //JR NZ,i8
                counter += 2;
                if(DEBUGMODE) System.out.println("JR NZ, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                if(!zeroFlag)
                    programCounter += (char) (memory.getCartridgeMemory(programCounter + 1));
                programCounter += 2;
                break;

            case 0x21: //LD HL,u16   IMPLEMENTED AND WORKING
                counter += 3;
                if(DEBUGMODE) System.out.println("LD HL, " + Integer.toHexString(((memory.getCartridgeMemory(programCounter + 1) << 8) + memory.getCartridgeMemory(programCounter + 2) & 0xff)));

                registers[7] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[6] = (char) memory.getCartridgeMemory(programCounter + 2);
                programCounter += 3;
                break;

            case 0x22: //LD (HL+),A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD (HL+), A");

                memory.setMemory((int) ((registers[6] & 0xff) * Math.pow(16, 2) + (registers[7] & 0xff)), registers[0]);
                registers[7]++;
                programCounter += 1;
                break;

            case 0x24: //INC H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("INC H");

                zeroFlag = (registers[6] & 0xff) == 0x00 ;
                halfCarryFlag = (registers[6] >> 4 & 0xf) != ((registers[6] + 1) >> 4 & 0xf);
                registers[6]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x25: //DEC E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("DEC E");

                zeroFlag = registers[6] - 1 == 0;
                halfCarryFlag = (registers[6] >> 4 & 0xf) != ((registers[6] - 1) >> 4 & 0xf);
                registers[6]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x26: //LD H,u8 IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD H, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                registers[6] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x2C: //INC L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("INC L");

                zeroFlag = (registers[7] & 0xff) == 0x00;
                halfCarryFlag = (registers[7] >> 4 & 0xf) != ((registers[7] + 1) >> 4 & 0xf);
                registers[7]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x2D: //DEC L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("DEC L");

                zeroFlag = registers[7] - 1 == 0;
                halfCarryFlag = (registers[7] >> 4 & 0xf) != ((registers[7] - 1) >> 4 & 0xf);
                registers[7]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x2E: //LD L,u8 IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD L, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                registers[7] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x2F: //CPL
                counter++;
                if(DEBUGMODE) System.out.println("CPL");

                registers[0] ^= 0xFF;
                programCounter += 1;
                break;

            case 0x32: //LDD (HL),A   IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LDD (HL), A");

                memory.setMemory((int) ((registers[6] & 0xff) * Math.pow(16, 2) + (registers[7] & 0xff)), registers[0]);
                registers[7]--;
                programCounter += 1;
                break;

            case 0x3D: //DEC A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("DEC A");

                zeroFlag = registers[0] - 1 == 0;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] - 1) >> 4 & 0xf);
                registers[0]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x3E: //LD A,u8   IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD A," + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                registers[0] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x40: //LD B,B  IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD B, B");

                programCounter++;
                break;

            case 0x41: //LD B,C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD B, C");

                registers[1] = registers[2];
                programCounter++;
                break;

            case 0x42: //LD B,D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD B, D");

                registers[1] = registers[3];
                programCounter++;
                break;

            case 0x43: //LD B,E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD B, E");

                registers[1] = registers[4];
                programCounter++;
                break;

            case 0x44: //LD B,H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD B, H");

                registers[1] = registers[6];
                programCounter++;
                break;

            case 0x45: //LD B,L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD B, L");

                registers[1] = registers[7];
                programCounter++;
                break;

            case 0x46: //LD B,(HL) IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD B, (HL)");

                registers[1] = registers[7];
                programCounter++;
                break;

            case 0x47: //LD B,A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD B, A");

                registers[1] = registers[0];
                programCounter++;
                break;

            case 0x48: //LD C,B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD C, B");

                registers[2] = registers[1];
                programCounter++;
                break;

            case 0x49: //LD C,C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD C, B");

                programCounter++;
                break;

            case 0x4A: //LD C,D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD C, C");

                registers[2] = registers[3];
                break;

            case 0x4B: //LD C,E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD C, E");

                registers[2] = registers[4];
                break;

            case 0x4C: //LD C,H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD C, H");

                registers[2] = registers[6];
                break;

            case 0x4D: //LD C,L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD C, L");

                registers[2] = registers[7];
                break;

            case 0x4E: //LD C,(HL) IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD C, (HL)");

                registers[2] = registers[7];
                programCounter++;
                break;

            case 0x4F: //LD C,A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD C, A");

                registers[2] = registers[0];
                programCounter++;
                break;

            case 0x50: //LD D,B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD D, B");

                registers[3] = registers[1];
                programCounter++;
                break;

            case 0x51: //LD D,C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD D, C");

                registers[3] = registers[2];
                programCounter++;
                break;

            case 0x52: //LD D,D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD D, D");

                programCounter++;
                break;

            case 0x53: //LD D,E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD D, E");

                registers[3] = registers[4];
                programCounter++;
                break;

            case 0x54: //LD D,H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD D, H");

                registers[3] = registers[6];
                programCounter++;
                break;

            case 0x55: //LD D,L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD D, L");

                registers[3] = registers[7];
                programCounter++;
                break;

            case 0x56: //LD D,(HL) IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD D, (HL)");

                registers[3] = registers[7];
                programCounter++;
                break;

            case 0x57: //LD D,A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD D, A");

                registers[3] = registers[0];
                programCounter++;
                break;

            case 0x58: //LD E,B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD E, B");

                registers[4] = registers[1];
                programCounter++;
                break;

            case 0x59: //LD E,C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD E, C");

                registers[4] = registers[2];
                programCounter++;
                break;

            case 0x5A: //LD E,D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD E, D");

                registers[4] = registers[3];
                programCounter++;
                break;

            case 0x5B: //LD E,E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD E, E");

                programCounter++;
                break;

            case 0x5C: //LD E,H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD E, H");

                registers[4] = registers[6];
                programCounter++;
                break;

            case 0x5D: //LD E,L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD E, L");

                registers[4] = registers[7];
                programCounter++;
                break;

            case 0x5E: //LD E,(HL) IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD E, (HL)");

                registers[4] = registers[7];
                programCounter++;
                break;

            case 0x5F: //LD E,A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD E, A");

                registers[4] = registers[0];
                programCounter++;
                break;

            case 0x60: //LD H,B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD H, B");

                registers[6] = registers[1];
                programCounter++;
                break;

            case 0x61: //LD H,C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD H, C");

                registers[6] = registers[2];
                programCounter++;
                break;

            case 0x62: //LD H,D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD H, D");

                registers[6] = registers[3];
                programCounter++;
                break;

            case 0x63: //LD H,E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD H, E");

                registers[6] = registers[4];
                programCounter++;
                break;

            case 0x64: //LD H,H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD H, H");

                programCounter++;
                break;

            case 0x65: //LD H,L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD H, L");

                registers[6] = registers[7];
                programCounter++;
                break;

            case 0x66: //LD H,(HL) IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD H, (HL)");

                registers[6] = registers[7];
                programCounter++;
                break;

            case 0x67: //LD H,A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD H, A");

                registers[6] = registers[0];
                programCounter++;
                break;

            case 0x68: //LD L,B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD L, B");

                registers[7] = registers[1];
                programCounter++;
                break;

            case 0x69: //LD L,C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD L, C");

                registers[7] = registers[2];
                programCounter++;
                break;

            case 0x6A: //LD L,D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD L, D");

                registers[7] = registers[3];
                programCounter++;
                break;

            case 0x6B: //LD L,E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD L, E");

                registers[7] = registers[4];
                programCounter++;
                break;

            case 0x6C: //LD L,H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD L, H");

                registers[7] = registers[6];
                programCounter++;
                break;

            case 0x6D: //LD L,L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD L, L");

                programCounter++;
                break;

            case 0x6E: //LD L,(HL) IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD L, (HL)");

                programCounter++;
                break;

            case 0x6F: //LD L,A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD L, A");

                registers[7] = registers[0];
                programCounter++;
                break;

            case 0x70: //LD (HL),B
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (HL), B");

                registers[6] = 0x00;
                registers[7] = registers[1];
                programCounter++;
                break;

            case 0x71: //LD (HL),C
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (HL), C");

                registers[6] = 0x00;
                registers[7] = registers[2];
                programCounter++;
                break;

            case 0x72: //LD (HL),D
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (HL), D");

                registers[6] = 0x00;
                registers[7] = registers[3];
                programCounter++;
                break;

            case 0x73: //LD (HL),E
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (HL), E");

                registers[6] = 0x00;
                registers[7] = registers[4];
                programCounter++;
                break;

            case 0x74: //LD (HL),H
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (HL), H");

                registers[7] = registers[6];
                registers[6] = 0x00;
                programCounter++;
                break;

            case 0x75: //LD (HL),L
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (HL), L");

                registers[6] = 0x00;
                programCounter++;
                break;

            case 0x76: //HALT
                System.exit(0);
                break;

            case 0x77: //LD (HL),A
                counter += 2;
                if(DEBUGMODE) System.out.println("LD (HL), A");

                registers[6] = 0x00;
                registers[7] = registers[0];
                programCounter++;
                break;

            case 0x78: //LD A,B  IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD A, B");

                registers[0] = registers[1];
                programCounter++;
                break;

            case 0x79: //LD A,C  IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD A, C");

                registers[0] = registers[2];
                programCounter++;
                break;

            case 0x7A: //LD A,D  IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD A, D");

                registers[0] = registers[3];
                programCounter++;
                break;

            case 0x7B: //LD A,E  IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD A, E");

                registers[0] = registers[4];
                programCounter++;
                break;

            case 0x7C: //LD A,H  IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD A, H");

                registers[0] = registers[6];
                programCounter++;
                break;

            case 0x7D: //LD A,L  IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD A, L");

                registers[0] = registers[7];
                programCounter++;
                break;

            case 0x7E: //LD A,(HL)  IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD A, (HL)");

                registers[0] = registers[7];
                programCounter++;
                break;

            case 0x7F: //LD A,A  IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("LD A, A");

                programCounter++;
                break;

            case 0x80: //ADD A,B NOT SURE
                counter++;
                if(DEBUGMODE) System.out.println("ADD A, B");

                zeroFlag = (registers[0] & 0xff + registers[1] & 0xff) == 0x00;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] + registers[1]) >> 4 & 0xf);
                if(registers[0] + registers[1] > 255) registers[0] = (char) (registers[0] + registers[1] - 255);
                else registers[0] += registers[1];
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x81: //ADD A,C
                counter++;
                if(DEBUGMODE) System.out.println("ADD A, C");

                zeroFlag = (registers[0] & 0xff + registers[2] & 0xff) == 0x00;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] + registers[2]) >> 4 & 0xf);
                if(registers[0] + registers[2] > 255) registers[0] = (char) (registers[0] + registers[2] - 255);
                else registers[0] += registers[2];
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x82: //ADD A,D
                counter++;
                if(DEBUGMODE) System.out.println("ADD A, D");

                zeroFlag = (registers[0] & 0xff + registers[3] & 0xff) == 0x00;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] + registers[3]) >> 4 & 0xf);
                if(registers[0] + registers[3] > 255) registers[0] = (char) (registers[0] + registers[3] - 255);
                else registers[0] += registers[3];
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x83: //ADD A,E
                counter++;
                if(DEBUGMODE) System.out.println("ADD A, D");

                zeroFlag = (registers[0] & 0xff + registers[4] & 0xff) == 0x00;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] + registers[4]) >> 4 & 0xf);
                if(registers[0] + registers[4] > 255) registers[0] = (char) (registers[0] + registers[4] - 255);
                else registers[0] += registers[4];
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x84: //ADD A, H
                counter++;
                if(DEBUGMODE) System.out.println("ADD A, H");

                zeroFlag = (registers[0] & 0xff + registers[6] & 0xff) == 0x00;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] + registers[6]) >> 4 & 0xf);
                if(registers[0] + registers[6] > 255) registers[0] = (char) (registers[0] + registers[6] - 255);
                else registers[0] += registers[6];
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x85: //ADD A,L
                counter++;
                if(DEBUGMODE) System.out.println("ADD A, L");

                zeroFlag = (registers[0] & 0xff + registers[7] & 0xff) == 0x00;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] + registers[7]) >> 4 & 0xf);
                if(registers[0] + registers[7] > 255) registers[0] = (char) (registers[0] + registers[7] - 255);
                else registers[0] += registers[7];
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x86: //ADD A,(HL)
                counter++;
                if(DEBUGMODE) System.out.println("ADD A, (HL)");

                //LACKS IMPLEMENTATION

                break;

            case 0x87: //ADD A,A
                counter++;
                if(DEBUGMODE) System.out.println("ADD A, A");

                zeroFlag = (registers[0] & 0xff + registers[0] & 0xff) == 0x00;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] + registers[0]) >> 4 & 0xf);
                if(registers[0] + registers[0] > 255) registers[0] = (char) (registers[0] + registers[0] - 255);
                else registers[0] += registers[0];
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0xA0: //AND A,B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("AND A, B");

                registers[0] &= registers[1];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = true;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xA1: //AND A,C
                counter++;
                if(DEBUGMODE) System.out.println("AND A, C");

                registers[0] &= registers[2];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = true;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xA2: //AND A,D
                counter++;
                if(DEBUGMODE) System.out.println("AND A, D");

                registers[0] &= registers[3];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = true;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xA3: //AND A,E
                counter++;
                if(DEBUGMODE) System.out.println("AND A, E");

                registers[0] &= registers[4];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = true;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xA4: //AND A,H
                counter++;
                if(DEBUGMODE) System.out.println("AND A, H");

                registers[0] &= registers[6];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = true;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xA5: //AND A,L
                counter++;
                if(DEBUGMODE) System.out.println("AND A, L");

                registers[0] &= registers[7];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = true;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xA6: //AND A,(HL)
                counter++;
                if(DEBUGMODE) System.out.println("AND A, (HL)");

                programCounter++;
                break;

            case 0xA7: //AND A,A
                counter++;
                if(DEBUGMODE) System.out.println("AND A, A");

                registers[0] &= registers[0];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = true;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xA8: //XOR A,B
                counter++;
                if(DEBUGMODE) System.out.println("XOR A, B");

                registers[0] ^= registers[1];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0xA9: //XOR A,C
                counter++;
                if(DEBUGMODE) System.out.println("XOR A, C");

                registers[0] ^= registers[2];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0xAA: //XOR A,D
                counter++;
                if(DEBUGMODE) System.out.println("XOR A, D");

                registers[0] ^= registers[3];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0xAB: //XOR A,E
                counter++;
                if(DEBUGMODE) System.out.println("XOR A, E");

                registers[0] ^= registers[4];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0xAC: //XOR A,H
                counter++;
                if(DEBUGMODE) System.out.println("XOR A, H");

                registers[0] ^= registers[5];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0xAD: //XOR A,L
                counter++;
                if(DEBUGMODE) System.out.println("XOR A, L");

                registers[0] ^= registers[6];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xAE: //XOR A,(HL) POR FAZER
                counter++;
                if(DEBUGMODE) System.out.println("XOR A, (HL)");

                //LACKS IMPLEMENTATION

                break;

            case 0xAF: //XOR A,A    IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("XOR A, A");

                registers[0] ^= registers[0];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xB0: //OR A,B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("OR A, B");

                registers[0] |= registers[1];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xB1: //OR A,C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("OR A, C");

                registers[0] |= registers[2];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xB2: //OR A,D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("OR A, D");

                registers[0] |= registers[3];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xB3: //OR A,E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("OR A, E");

                registers[0] |= registers[4];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xB4: //OR A,H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("OR A, H");

                registers[0] |= registers[6];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xB5: //OR A,L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("OR A, L");

                registers[0] |= registers[7];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xB6: //OR A,(HL)
                counter += 2;
                if(DEBUGMODE) System.out.println("OR A, B");

                //LACKS IMPLEMENTATION

                break;

            case 0xB7: //OR A,A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("OR A, A");

                registers[0] |= registers[0];
                zeroFlag = registers[0] == 0;
                subtractFlag = false;
                halfCarryFlag = false;
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xB8: //CP A,B IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("CP A, B");

                zeroFlag = (registers[0] - registers[1]) == 0;
                subtractFlag = true;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] - registers[1]) >> 4 & 0xf);
                carryFlag = registers[0] > registers[1];
                computeFRegister();
                programCounter++;
                break;

            case 0xB9: //CP A,C IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("CP A, C");

                zeroFlag = (registers[0] - registers[2]) == 0;
                subtractFlag = true;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] - registers[2]) >> 4 & 0xf);
                carryFlag = registers[0] > registers[2];
                computeFRegister();
                programCounter++;
                break;

            case 0xBA: //CP A,D IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("CP A, D");

                zeroFlag = (registers[0] - registers[3]) == 0;
                subtractFlag = true;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] - registers[3]) >> 4 & 0xf);
                carryFlag = registers[0] > registers[3];
                computeFRegister();
                programCounter++;
                break;

            case 0xBB: //CP A,E IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("CP A, E");

                zeroFlag = (registers[0] - registers[4]) == 0;
                subtractFlag = true;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] - registers[4]) >> 4 & 0xf);
                carryFlag = registers[0] > registers[4];
                computeFRegister();
                programCounter++;
                break;

            case 0xBC: //CP A,H IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("CP A, H");

                zeroFlag = (registers[0] - registers[6]) == 0;
                subtractFlag = true;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] - registers[6]) >> 4 & 0xf);
                carryFlag = registers[0] > registers[6];
                computeFRegister();
                programCounter++;
                break;

            case 0xBD: //CP A,L IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("CP A, L");

                zeroFlag = (registers[0] - registers[7]) == 0;
                subtractFlag = true;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] - registers[7]) >> 4 & 0xf);
                carryFlag = registers[0] > registers[7];
                computeFRegister();
                programCounter++;
                break;

            case 0xBE: //CP A,(HL)
                counter++;
                if(DEBUGMODE) System.out.println("CP A, (HL)");

                //LACKS IMPLEMENTATION

                break;

            case 0xBF: //CP A,A IMPLEMENTED AND WORKING
                counter++;
                if(DEBUGMODE) System.out.println("CP A, A");

                zeroFlag = true;
                subtractFlag = true;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != (0);
                carryFlag = false;
                computeFRegister();
                programCounter++;
                break;

            case 0xC0: //RET NZ
                counter += 2;
                if(DEBUGMODE) System.out.println("RET NZ");

                //if(!zeroFlag)
                break;

            case 0xC3: //JP u16   IMPLEMENTED AND WORKING
                counter += 3;
                if(DEBUGMODE) System.out.println("JP " + Integer.toHexString(((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) << 8)) & 0xffff));

                programCounter = (char) ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) << 8));
                break;

            case 0xC4: //CALL NZ, nn
                counter += 3;
                if(DEBUGMODE) System.out.println("CALL NZ, " + ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) & 0xff) * 256));

                if(!zeroFlag) {
                    tempProgramCounter = programCounter + 3;

                    programCounter = (char) ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) & 0xff) * 256);
                    memory.setMemory(stackPointer - 1, (char) (((tempProgramCounter) & 0xff00) / 256));
                    memory.setMemory(stackPointer - 2, (char) ((tempProgramCounter) & 0xff));
                    stackPointer -= 2;
                } else {
                    programCounter += 3;
                }
                break;

            case 0xC6: //ADD A,#
                counter += 2;
                if(DEBUGMODE) System.out.println("ADD A, " + (memory.getCartridgeMemory(programCounter + 1) & 0xff));

                //LACKS IMPLEMENTATION

                break;

            case 0xC8: //RET Z
                counter += 2;
                if(DEBUGMODE) System.out.println("RET Z");

                //if(zeroFlag)
                break;

            case 0xC9: //RET
                counter += 2;
                if(DEBUGMODE) System.out.println("RET");

                break;

            case 0xCB:
                int carry1, bit;
                counter += 1;
                if(DEBUGMODE) System.out.println("CB PREFIX");

                programCounter++;
                operationCode = (char) (memory.getCartridgeMemory(programCounter) & 0xff);

                switch(operationCode) {
                    case 0x00: //RLC B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RLC B");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[1] & 0xff) >> 7;
                        carryFlag = carry != 0;
                        registers[1] = (char) (registers[1] << 1);
                        zeroFlag = registers[1] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x01: //RLC C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RLC C");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[2] & 0xff) >> 7;
                        carryFlag = carry != 0;
                        registers[2] = (char) (registers[2] << 1);
                        zeroFlag = registers[2] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x02: //RLC D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RLC D");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[3] & 0xff) >> 7;
                        carryFlag = carry != 0;
                        registers[3] = (char) (registers[3] << 1);
                        zeroFlag = registers[3] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x03: //RLC E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RLC E");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[4] & 0xff) >> 7;
                        carryFlag = carry != 0;
                        registers[4] = (char) (registers[4] << 1);
                        zeroFlag = registers[4] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x04: //RLC H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RLC H");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[6] & 0xff) >> 7;
                        carryFlag = carry != 0;
                        registers[6] = (char) (registers[6] << 1);
                        zeroFlag = registers[6] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x05: //RLC L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RLC L");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[7] & 0xff) >> 7;
                        carryFlag = carry != 0;
                        registers[7] = (char) (registers[7] << 1);
                        zeroFlag = registers[7] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x06: //RLC HL
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RLC (HL)");

                        //LACKS IMPLEMENTATION

                        break;

                    case 0x07: //RLC A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RLC A");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[0] & 0xff) >> 7;
                        carryFlag = carry != 0;
                        registers[0] = (char) (registers[0] << 1);
                        zeroFlag = registers[0] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x08: //RRC B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RRC B");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[1] & 0x01);
                        carryFlag = carry != 0;
                        registers[1] = (char) (registers[1] >> 1);
                        zeroFlag = registers[1] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x09: //RRC C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RRC C");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[2] & 0x01);
                        carryFlag = carry != 0;
                        registers[2] = (char) (registers[2] >> 1);
                        zeroFlag = registers[2] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x0A: //RRC D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RRC D");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[3] & 0x01);
                        carryFlag = carry != 0;
                        registers[3] = (char) (registers[3] >> 1);
                        zeroFlag = registers[3] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x0B: //RRC E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RRC E");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[4] & 0x01);
                        carryFlag = carry != 0;
                        registers[4] = (char) (registers[4] >> 1);
                        zeroFlag = registers[4] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x0C: //RRC H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RRC H");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[6] & 0x01);
                        carryFlag = carry != 0;
                        registers[6] = (char) (registers[6] >> 1);
                        zeroFlag = registers[6] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x0D: //RRC L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RRC L");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[7] & 0x01);
                        carryFlag = carry != 0;
                        registers[7] = (char) (registers[7] >> 1);
                        zeroFlag = registers[7] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x0E: //RRC (HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RRC (HL)");
                        break;

                    case 0x0F: //RRC A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RRC A");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[0] & 0x01);
                        carryFlag = carry != 0;
                        registers[0] = (char) (registers[0] >> 1);
                        zeroFlag = registers[0] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x10: //RL B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RL B");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[1] & 0xff) >> 7;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[1] = (char) (registers[1] << 1);
                        registers[1] += carry1;
                        zeroFlag = registers[1] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x11: //RL C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RL C");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[2] & 0xff) >> 7;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[2] = (char) (registers[2] << 1);
                        registers[2] += carry1;
                        zeroFlag = registers[2] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x12: //RL D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RL D");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[3] & 0xff) >> 7;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[3] = (char) (registers[3] << 1);
                        registers[3] += carry1;
                        zeroFlag = registers[3] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x13: //RL E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RL E");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[4] & 0xff) >> 7;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[4] = (char) (registers[4] << 1);
                        registers[4] += carry1;
                        zeroFlag = registers[4] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x14: //RL H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RL H");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[6] & 0xff) >> 7;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[6] = (char) (registers[6] << 1);
                        registers[6] += carry1;
                        zeroFlag = registers[6] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x15: //RL L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RL L");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[7] & 0xff) >> 7;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[7] = (char) (registers[7] << 1);
                        registers[7] += carry1;
                        zeroFlag = registers[7] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x16: //RL (HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RL (HL)");

                        //LACKS IMPLEMENTATION

                        break;

                    case 0x17: //RL A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RL A");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[0] & 0xff) >> 7;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[0] = (char) (registers[0] << 1);
                        registers[0] += carry1;
                        zeroFlag = registers[0] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x18: //RR B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RR B");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = registers[1] & 0x01;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[1] = (char) (registers[1] >> 1);
                        registers[1] += carry1 << 7;
                        zeroFlag = registers[1] == 0;
                        computeFRegister();

                        programCounter++;

                        break;

                    case 0x19: //RR C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RR C");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = registers[2] & 0x01;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[2] = (char) (registers[2] >> 1);
                        registers[2] += carry1 << 7;
                        zeroFlag = registers[2] == 0;
                        computeFRegister();

                        programCounter++;

                        break;

                    case 0x1A: //RR D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RR D");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = registers[3] & 0x01;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[3] = (char) (registers[3] >> 1);
                        registers[3] += carry1 << 7;
                        zeroFlag = registers[3] == 0;
                        computeFRegister();

                        programCounter++;

                        break;

                    case 0x1B: //RR E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RR E");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = registers[4] & 0x01;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[4] = (char) (registers[4] >> 1);
                        registers[4] += carry1 << 7;
                        zeroFlag = registers[4] == 0;
                        computeFRegister();

                        programCounter++;

                        break;

                    case 0x1C: //RR H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RR H");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = registers[6] & 0x01;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[6] = (char) (registers[6] >> 1);
                        registers[6] += carry1 << 7;
                        zeroFlag = registers[6] == 0;
                        computeFRegister();

                        programCounter++;

                        break;

                    case 0x1D: //RR L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RR L");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = registers[7] & 0x01;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[7] = (char) (registers[7] >> 1);
                        registers[7] += carry1 << 7;
                        zeroFlag = registers[7] == 0;
                        computeFRegister();

                        programCounter++;

                        break;

                    case 0x1E: //RR (HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RR (HL)");

                        //LACKS IMPLEMENTATION

                        break;

                    case 0x1F: //RR A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RR A");

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = registers[0] & 0x01;
                        carry1 = carryFlag ? 1 : 0;
                        carryFlag = carry != 0;
                        registers[0] = (char) (registers[0] >> 1);
                        registers[0] += carry1 << 7;
                        zeroFlag = registers[0] == 0;
                        computeFRegister();

                        programCounter++;

                        break;

                    case 0x20: //SLA B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SLA B");

                        zeroFlag = registers[1] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x21: //SLA C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SLA C");

                        zeroFlag = registers[2] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x22: //SLA D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SLA D");

                        zeroFlag = registers[3] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x23: //SLA E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SLA E");

                        zeroFlag = registers[4] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x24: //SLA H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SLA H");

                        zeroFlag = registers[6] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x25: //SLA L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SLA L");

                        zeroFlag = registers[7] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x26: //SLA (HL)
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SLA (HL)");

                        //LACKS IMPLEMENTATION

                        break;

                    case 0x27: //SLA A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SLA A");

                        zeroFlag = registers[0] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x28: //SRA B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRA B");

                        zeroFlag = registers[1] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x29: //SRA C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRA C");

                        zeroFlag = registers[2] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x2A: //SRA D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRA D");

                        zeroFlag = registers[3] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x2B: //SRA E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRA E");

                        zeroFlag = registers[4] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x2C: //SRA H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRA H");

                        zeroFlag = registers[6] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x2D: //SRA L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRA L");

                        zeroFlag = registers[7] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x2E: //SRA (HL)
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRA (HL)");

                        //LACKS IMPLEMENTATION
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x2F: //SRA A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRA A");

                        zeroFlag = registers[0] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x30: //SWAP B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SWAP B");

                        zeroFlag = registers[1] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;

                        computeFRegister();

                        break;

                    case 0x31: //SWAP C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SWAP C");

                        zeroFlag = registers[2] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;

                        computeFRegister();

                        break;

                    case 0x32: //SWAP D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SWAP D");

                        zeroFlag = registers[3] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;

                        computeFRegister();

                        break;

                    case 0x33: //SWAP E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SWAP E");

                        zeroFlag = registers[4] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;

                        computeFRegister();

                        break;

                    case 0x34: //SWAP H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SWAP H");

                        zeroFlag = registers[6] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;

                        computeFRegister();

                        break;

                    case 0x35: //SWAP L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SWAP L");

                        zeroFlag = registers[7] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;

                        computeFRegister();

                        break;

                    case 0x36: //SWAP (HL)
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SWAP (HL)");

                        //LACKS IMPLEMENTATION

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;

                        computeFRegister();

                        break;

                    case 0x37: //SWAP A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SWAP A");

                        zeroFlag = registers[0] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;

                        computeFRegister();

                        break;

                    case 0x38: //SRL B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRL B");

                        zeroFlag = registers[1] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x39: //SRL C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRL C");

                        zeroFlag = registers[2] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x3A: //SRL D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRL D");

                        zeroFlag = registers[3] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x3B: //SRL E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRL E");

                        zeroFlag = registers[4] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x3C: //SRL H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRL H");

                        zeroFlag = registers[6] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x3D: //SRL L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRL L");

                        zeroFlag = registers[7] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x3E: //SRL (HL)
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRL B");

                        //LACKS IMPLEMENTATION

                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x3F: //SRL A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SRL A");

                        zeroFlag = registers[0] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;

                        computeFRegister();

                        break;

                    case 0x40: //BIT 0,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 0, B");

                        bit = registers[1] & 0x01;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x41: //BIT 0,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 0, C");

                        bit = registers[2] & 0x01;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x42: //BIT 0,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 0, D");

                        bit = registers[3] & 0x01;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x43: //BIT 0,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 0, E");

                        bit = registers[4] & 0x01;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x44: //BIT 0,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 0, H");

                        bit = registers[6] & 0x01;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x45: //BIT 0,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 0, L");

                        bit = registers[7] & 0x01;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x46: //BIT 0,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("BIT 0, (HL)");

                        bit = registers[7] & 0x01; //NOT SURE
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        carryFlag = false;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x47: //BIT 0,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 0, A");

                        bit = registers[0] & 0x01;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x48: //BIT 1,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 1, B");

                        bit = registers[1] & 0x02 >> 1;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x49: //BIT 1,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 1, C");

                        bit = registers[2] & 0x02 >> 1;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x4A: //BIT 1,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 1, D");

                        bit = registers[3] & 0x02 >> 1;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x4B: //BIT 1,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 1, E");

                        bit = registers[4] & 0x02 >> 1;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x4C: //BIT 1,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 1, H");

                        bit = registers[6] & 0x02 >> 1;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x4D: //BIT 1,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 1, L");

                        bit = registers[7] & 0x02 >> 1;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x4E: //BIT 1,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("BIT 1, (HL)");

                        bit = registers[7] & 0x02 >> 1; //NOT SURE
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x4F: //BIT 1,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 1, A");

                        bit = registers[0] & 0x02 >> 1;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x50: //BIT 2,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 2, B");

                        bit = registers[1] & 0x04 >> 2;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x51: //BIT 2,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 2, C");

                        bit = registers[2] & 0x04 >> 2;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x52: //BIT 2,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 2, D");

                        bit = registers[3] & 0x04 >> 2;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x53: //BIT 2,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 2, E");

                        bit = registers[4] & 0x04 >> 2;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x54: //BIT 2,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 2, H");

                        bit = registers[6] & 0x04 >> 2;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x55: //BIT 2,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 2, L");

                        bit = registers[7] & 0x04 >> 2;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x56: //BIT 2,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("BIT 2, (HL)");

                        bit = registers[7] & 0x04 >> 2; //NOT SURE
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x57: //BIT 2,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 2, A");

                        bit = registers[0] & 0x04 >> 2;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x58: //BIT 3,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 3, B");

                        bit = registers[1] & 0x08 >> 3;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x59: //BIT 3,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 3, C");

                        bit = registers[2] & 0x08 >> 3;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x5A: //BIT 3,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 3, D");

                        bit = registers[3] & 0x08 >> 3;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x5B: //BIT 3,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 3, E");

                        bit = registers[4] & 0x08 >> 3;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x5C: //BIT 3,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 3, H");

                        bit = registers[6] & 0x08 >> 3;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x5D: //BIT 3,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 3, L");

                        bit = registers[7] & 0x08 >> 3;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x5E: //BIT 3,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("BIT 3, (HL)");

                        bit = registers[7] & 0x08 >> 3; //NOT SURE
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x5F: //BIT 3,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 3, A");

                        bit = registers[0] & 0x08 >> 3;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x60: //BIT 4,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 4, B");

                        bit = registers[1] & 0x10 >> 4;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x61: //BIT 4,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 4, C");

                        bit = registers[2] & 0x10 >> 4;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x62: //BIT 4,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 4, D");

                        bit = registers[3] & 0x10 >> 4;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x63: //BIT 4,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 4, E");

                        bit = registers[4] & 0x10 >> 4;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x64: //BIT 4,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 4, H");

                        bit = registers[6] & 0x10 >> 4;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x65: //BIT 4,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 4, L");

                        bit = registers[7] & 0x10 >> 4;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x66: //BIT 4,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("BIT 4, (HL)");

                        bit = registers[7] & 0x10 >> 4; //NOT SURE
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x67: //BIT 4,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 4, A");

                        bit = registers[0] & 0x10 >> 4;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x68: //BIT 5,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 5, B");

                        bit = registers[1] & 0x20 >> 5;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x69: //BIT 5,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 5, C");

                        bit = registers[2] & 0x20 >> 5;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x6A: //BIT 5,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 5, D");

                        bit = registers[3] & 0x20 >> 5;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x6B: //BIT 5,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 5, E");

                        bit = registers[4] & 0x20 >> 5;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x6C: //BIT 5,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 5, H");

                        bit = registers[6] & 0x20 >> 5;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x6D: //BIT 5,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 5, L");

                        bit = registers[7] & 0x20 >> 5;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x6E: //BIT 5,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("BIT 5, (HL)");

                        bit = registers[7] & 0x20 >> 5; //NOT SURE
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x6F: //BIT 5,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 5, A");

                        bit = registers[0] & 0x20 >> 5;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x70: //BIT 6,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 6, B");

                        bit = registers[1] & 0x40 >> 6;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x71: //BIT 6,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 6, C");

                        bit = registers[2] & 0x40 >> 6;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x72: //BIT 6,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 6, D");

                        bit = registers[3] & 0x40 >> 6;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x73: //BIT 6,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 6, E");

                        bit = registers[4] & 0x40 >> 6;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x74: //BIT 6,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 6, H");

                        bit = registers[6] & 0x40 >> 6;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x75: //BIT 6,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 6, L");

                        bit = registers[7] & 0x40 >> 6;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x76: //BIT 6,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("BIT 6, (HL)");

                        bit = registers[7] & 0x40 >> 6; //NOT SURE
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x77: //BIT 6,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 6, A");

                        bit = registers[0] & 0x40 >> 6;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x78: //BIT 7,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 7, B");

                        bit = registers[1] & 0x80 >> 7;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x79: //BIT 7,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 7, C");

                        bit = registers[2] & 0x80 >> 7;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x7A: //BIT 7,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 7, D");

                        bit = registers[3] & 0x80 >> 7;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x7B: //BIT 7,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 7, E");

                        bit = registers[4] & 0x80 >> 7;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x7C: //BIT 7,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 7, H");

                        bit = registers[6] & 0x80 >> 7;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x7D: //BIT 7,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 7, L");

                        bit = registers[7] & 0x80 >> 7;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x7E: //BIT 7, (HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("BIT 7, (HL)");

                        bit = registers[1] & 0x80 >> 7; //NOT SURE
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x7F: //BIT 7,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("BIT 7, A");

                        bit = registers[0] & 0x80 >> 7;
                        zeroFlag = bit == 0;
                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x80: //RES 0,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 0, B");

                        if((registers[1] & 0x01) == 1) registers[1]--;

                        programCounter++;
                        break;

                    case 0x81: //RES 0,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 0, C");

                        if((registers[2] & 0x01) == 1) registers[2]--;

                        programCounter++;
                        break;

                    case 0x82: //RES 0,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 0, D");

                        if((registers[3] & 0x01) == 1) registers[3]--;

                        programCounter++;
                        break;

                    case 0x83: //RES 0,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 0, E");

                        if((registers[4] & 0x01) == 1) registers[4]--;

                        programCounter++;
                        break;

                    case 0x84: //RES 0,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 0, H");

                        if((registers[6] & 0x01) == 1) registers[6]--;

                        programCounter++;
                        break;

                    case 0x85: //RES 0,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 0, L");

                        if((registers[7] & 0x01) == 1) registers[7]--;

                        programCounter++;
                        break;

                    case 0x86: //RES 0,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RES 0, (HL)");

                        if((registers[7] & 0x01) == 1) registers[7]--; //NOT SURE

                        programCounter++;
                        break;

                    case 0x87: //RES 0,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 0, A");

                        if((registers[0] & 0x01) == 1) registers[0]--;

                        programCounter++;
                        break;

                    case 0x88: //RES 1,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 1, B");

                        if((registers[1] & 0x02 >> 1) == 1) registers[1] -= 2;

                        programCounter++;
                        break;

                    case 0x89: //RES 1,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 1, C");

                        if((registers[2] & 0x02 >> 1) == 1) registers[2] -= 2;

                        programCounter++;
                        break;

                    case 0x8A: //RES 1,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 1, D");

                        if((registers[3] & 0x02 >> 1) == 1) registers[3] -= 2;

                        programCounter++;
                        break;

                    case 0x8B: //RES 1,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 1, E");

                        if((registers[4] & 0x02 >> 1) == 1) registers[4] -= 2;

                        programCounter++;
                        break;

                    case 0x8C: //RES 1,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 1, H");

                        if((registers[6] & 0x02 >> 1) == 1) registers[6] -= 2;

                        programCounter++;
                        break;

                    case 0x8D: //RES 1,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 1, L");

                        if((registers[7] & 0x02 >> 1) == 1) registers[7] -= 2;

                        programCounter++;
                        break;

                    case 0x8E: //RES 1,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RES 1, (HL)");

                        if((registers[7] & 0x02 >> 1) == 1) registers[7] -= 2; //NOT SURE

                        programCounter++;
                        break;

                    case 0x8F: //RES 1,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 1, A");

                        if((registers[0] & 0x02 >> 1) == 1) registers[0] -= 2;

                        programCounter++;
                        break;

                    case 0x90: //RES 2,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 2, B");

                        if((registers[1] & 0x04 >> 2) == 1) registers[1] -= 4;

                        programCounter++;
                        break;

                    case 0x91: //RES 2,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 2, C");

                        if((registers[2] & 0x04 >> 2) == 1) registers[2] -= 4;

                        programCounter++;
                        break;

                    case 0x92: //RES 2,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 2, D");

                        if((registers[3] & 0x04 >> 2) == 1) registers[3] -= 4;

                        programCounter++;
                        break;

                    case 0x93: //RES 2,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 2, E");

                        if((registers[4] & 0x04 >> 2) == 1) registers[4] -= 4;

                        programCounter++;
                        break;

                    case 0x94: //RES 2,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 2, H");

                        if((registers[6] & 0x04 >> 2) == 1) registers[6] -= 4;

                        programCounter++;
                        break;

                    case 0x95: //RES 2,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 2, L");

                        if((registers[7] & 0x04 >> 2) == 1) registers[7] -= 4;

                        programCounter++;
                        break;

                    case 0x96: //RES 2,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RES 2, (HL)");

                        if((registers[7] & 0x04 >> 2) == 1) registers[7] -= 4; //NOT SURE

                        programCounter++;
                        break;

                    case 0x97: //RES 2,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 2, A");

                        if((registers[0] & 0x04 >> 2) == 1) registers[0] -= 4;

                        programCounter++;
                        break;

                    case 0x98: //RES 3,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 3, B");

                        if((registers[1] & 0x08 >> 3) == 1) registers[1] -= 8;

                        programCounter++;
                        break;

                    case 0x99: //RES 3,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 3, C");

                        if((registers[2] & 0x08 >> 3) == 1) registers[2] -= 8;

                        programCounter++;
                        break;

                    case 0x9A: //RES 3,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 3, D");

                        if((registers[3] & 0x08 >> 3) == 1) registers[3] -= 8;

                        programCounter++;
                        break;

                    case 0x9B: //RES 3,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 3, E");

                        if((registers[4] & 0x08 >> 3) == 1) registers[4] -= 8;

                        programCounter++;
                        break;

                    case 0x9C: //RES 3,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 3, H");

                        if((registers[6] & 0x08 >> 3) == 1) registers[6] -= 8;

                        programCounter++;
                        break;

                    case 0x9D: //RES 3,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 3, L");

                        if((registers[7] & 0x08 >> 3) == 1) registers[7] -= 8;

                        programCounter++;
                        break;

                    case 0x9E: //RES 3,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RES 3, (HL)");

                        if((registers[7] & 0x08 >> 3) == 1) registers[7] -= 8; //NOT SURE

                        programCounter++;
                        break;

                    case 0x9F: //RES 3,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 3, A");

                        if((registers[0] & 0x08 >> 3) == 1) registers[0] -= 8;

                        programCounter++;
                        break;

                    case 0xA0: //RES 4,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 4, B");

                        if((registers[1] & 0x10 >> 4) == 1) registers[1] -= 16;

                        programCounter++;
                        break;

                    case 0xA1: //RES 4,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 4, C");

                        if((registers[2] & 0x10 >> 4) == 1) registers[2] -= 16;

                        programCounter++;
                        break;

                    case 0xA2: //RES 4,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 4, D");

                        if((registers[3] & 0x10 >> 4) == 1) registers[3] -= 16;

                        programCounter++;
                        break;

                    case 0xA3: //RES 4,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 4, E");

                        if((registers[4] & 0x10 >> 4) == 1) registers[4] -= 16;

                        programCounter++;
                        break;

                    case 0xA4: //RES 4,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 4, H");

                        if((registers[6] & 0x10 >> 4) == 1) registers[6] -= 16;

                        programCounter++;
                        break;

                    case 0xA5: //RES 4,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 4, L");

                        if((registers[7] & 0x10 >> 4) == 1) registers[7] -= 16;

                        programCounter++;
                        break;

                    case 0xA6: //RES 4,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RES 4, (HL)");

                        if((registers[7] & 0x10 >> 4) == 1) registers[7] -= 16; //NOT SURE

                        programCounter++;
                        break;

                    case 0xA7: //RES 4,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 4, A");

                        if((registers[0] & 0x10 >> 4) == 1) registers[0] -= 16;

                        programCounter++;
                        break;

                    case 0xA8: //RES 5,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 5, B");

                        if((registers[1] & 0x20 >> 5) == 1) registers[1] -= 32;

                        programCounter++;
                        break;

                    case 0xA9: //RES 5,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 5, C");

                        if((registers[2] & 0x20 >> 5) == 1) registers[2] -= 32;

                        programCounter++;
                        break;

                    case 0xAA: //RES 5,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 5, D");

                        if((registers[3] & 0x20 >> 5) == 1) registers[3] -= 32;

                        programCounter++;
                        break;

                    case 0xAB: //RES 5,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 5, E");

                        if((registers[4] & 0x20 >> 5) == 1) registers[4] -= 32;

                        programCounter++;
                        break;

                    case 0xAC: //RES 5,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 5, H");

                        if((registers[6] & 0x20 >> 5) == 1) registers[6] -= 32;

                        programCounter++;
                        break;

                    case 0xAD: //RES 5,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 5, L");

                        if((registers[7] & 0x20 >> 5) == 1) registers[7] -= 32;

                        programCounter++;
                        break;

                    case 0xAE: //RES 5,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RES 5, (HL)");

                        if((registers[7] & 0x20 >> 5) == 1) registers[7] -= 32; //NOT SURE

                        programCounter++;
                        break;

                    case 0xAF: //RES 5,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 5, A");

                        if((registers[0] & 0x20 >> 5) == 1) registers[0] -= 32;

                        programCounter++;
                        break;

                    case 0xB0: //RES 6,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 6, B");

                        if((registers[1] & 0x40 >> 6) == 1) registers[1] -= 64;

                        programCounter++;
                        break;

                    case 0xB1: //RES 6,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 6, C");

                        if((registers[2] & 0x40 >> 6) == 1) registers[2] -= 64;

                        programCounter++;
                        break;

                    case 0xB2: //RES 6,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 6, D");

                        if((registers[3] & 0x40 >> 6) == 1) registers[3] -= 64;

                        programCounter++;
                        break;

                    case 0xB3: //RES 6,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 6, E");

                        if((registers[4] & 0x40 >> 6) == 1) registers[4] -= 64;

                        programCounter++;
                        break;

                    case 0xB4: //RES 6,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 6, H");

                        if((registers[6] & 0x40 >> 6) == 1) registers[6] -= 64;

                        programCounter++;
                        break;

                    case 0xB5: //RES 6,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 6, L");

                        if((registers[7] & 0x40 >> 6) == 1) registers[7] -= 64;

                        programCounter++;
                        break;

                    case 0xB6: //RES 6,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RES 6, (HL)");

                        if((registers[7] & 0x40 >> 6) == 1) registers[7] -= 64; //NOT SURE

                        programCounter++;
                        break;

                    case 0xB7: //RES 6,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 6, A");

                        if((registers[0] & 0x40 >> 6) == 1) registers[0] -= 64;

                        programCounter++;
                        break;

                    case 0xB8: //RES 7,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 7, B");

                        if((registers[1] & 0x80 >> 7) == 1) registers[1] -= 128;

                        programCounter++;
                        break;

                    case 0xB9: //RES 7,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 7, C");

                        if((registers[2] & 0x80 >> 7) == 1) registers[2] -= 128;

                        programCounter++;
                        break;

                    case 0xBA: //RES 7,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 7, D");

                        if((registers[3] & 0x80 >> 7) == 1) registers[3] -= 128;

                        programCounter++;
                        break;

                    case 0xBB: //RES 7,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 7, E");

                        if((registers[4] & 0x80 >> 7) == 1) registers[4] -= 128;

                        programCounter++;
                        break;

                    case 0xBC: //RES 7,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 7, H");

                        if((registers[6] & 0x80 >> 7) == 1) registers[6] -= 128;

                        programCounter++;
                        break;

                    case 0xBD: //RES 7,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 7, L");

                        if((registers[7] & 0x80 >> 7) == 1) registers[7] -= 128;

                        programCounter++;
                        break;

                    case 0xBE: //RES 7,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("RES 7, (HL)");

                        if((registers[7] & 0x80 >> 7) == 1) registers[7] -= 128; //NOT SURE

                        programCounter++;
                        break;

                    case 0xBF: //RES 7,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("RES 7, A");

                        if((registers[0] & 0x80 >> 7) == 1) registers[0] -= 128;

                        programCounter++;
                        break;

                    case 0xC0: //SET 0,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 0, B");

                        if((registers[1] & 0x01) == 0) registers[1] += 1;

                        programCounter++;
                        break;

                    case 0xC1: //SET 0,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 0, C");

                        if((registers[2] & 0x01) == 0) registers[2] += 1;

                        programCounter++;
                        break;

                    case 0xC2: //SET 0,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 0, D");

                        if((registers[3] & 0x01) == 0) registers[3] += 1;

                        programCounter++;
                        break;

                    case 0xC3: //SET 0,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 0, E");

                        if((registers[4] & 0x01) == 0) registers[4] += 1;

                        programCounter++;
                        break;

                    case 0xC4: //SET 0,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 0, H");

                        if((registers[6] & 0x01) == 0) registers[6] += 1;

                        programCounter++;
                        break;

                    case 0xC5: //SET 0,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 0, L");

                        if((registers[7] & 0x01) == 0) registers[7] += 1;

                        programCounter++;
                        break;

                    case 0xC6: //SET 0,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("SET 0, (HL)");

                        if((registers[7] & 0x01) == 0) registers[7] += 1; //NOT SURE

                        programCounter++;
                        break;

                    case 0xC7: //SET 0,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 0, A");

                        if((registers[0] & 0x01) == 0) registers[0] += 1;

                        programCounter++;
                        break;

                    case 0xC8: //SET 1,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 1, B");

                        if((registers[1] & 0x02 >> 1) == 0) registers[1] += 2;

                        programCounter++;
                        break;

                    case 0xC9: //SET 1,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 1, C");

                        if((registers[2] & 0x02 >> 1) == 0) registers[2] += 2;

                        programCounter++;
                        break;

                    case 0xCA: //SET 1,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 1, D");

                        if((registers[3] & 0x02 >> 1) == 0) registers[3] += 2;

                        programCounter++;
                        break;

                    case 0xCB: //SET 1,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 1, E");

                        if((registers[4] & 0x02 >> 1) == 0) registers[4] += 2;

                        programCounter++;
                        break;

                    case 0xCC: //SET 1,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 1, H");

                        if((registers[6] & 0x02 >> 1) == 0) registers[6] += 2;

                        programCounter++;
                        break;

                    case 0xCD: //SET 1,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 1, L");

                        if((registers[7] & 0x02 >> 1) == 0) registers[7] += 2;

                        programCounter++;
                        break;

                    case 0xCE: //SET 1,(HL)
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 1, (HL)");

                        if((registers[7] & 0x02 >> 1) == 0) registers[7] += 2; //NOT SURE

                        programCounter++;
                        break;

                    case 0xCF: //SET 1,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 1, A");

                        if((registers[0] & 0x02 >> 1) == 0) registers[0] += 2;

                        programCounter++;
                        break;

                    case 0xD0: //SET 2,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 2, B");

                        if((registers[1] & 0x04 >> 2) == 0) registers[1] += 4;

                        programCounter++;
                        break;

                    case 0xD1: //SET 2,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 2, C");

                        if((registers[2] & 0x04 >> 2) == 0) registers[2] += 4;

                        programCounter++;
                        break;

                    case 0xD2: //SET 2,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 2, D");

                        if((registers[3] & 0x04 >> 2) == 0) registers[3] += 4;

                        programCounter++;
                        break;

                    case 0xD3: //SET 2,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 2, E");

                        if((registers[4] & 0x04 >> 2) == 0) registers[4] += 4;

                        programCounter++;
                        break;

                    case 0xD4: //SET 2,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 2, H");

                        if((registers[6] & 0x04 >> 2) == 0) registers[6] += 4;

                        programCounter++;
                        break;

                    case 0xD5: //SET 2,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 2, L");

                        if((registers[7] & 0x04 >> 2) == 0) registers[7] += 4;

                        programCounter++;
                        break;

                    case 0xD6: //SET 2,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("SET 2, (HL)");

                        if((registers[7] & 0x04 >> 2) == 0) registers[7] += 4; //NOT SURE

                        programCounter++;
                        break;

                    case 0xD7: //SET 2,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 2, A");

                        if((registers[0] & 0x04 >> 2) == 0) registers[0] += 4;

                        programCounter++;
                        break;

                    case 0xD8: //SET 3,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 3, B");

                        if((registers[1] & 0x08 >> 3) == 0) registers[1] += 8;

                        programCounter++;
                        break;

                    case 0xD9: //SET 3,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 3, C");

                        if((registers[2] & 0x08 >> 3) == 0) registers[2] += 8;

                        programCounter++;
                        break;

                    case 0xDA: //SET 3,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 3, D");

                        if((registers[3] & 0x08 >> 3) == 0) registers[3] += 8;

                        programCounter++;
                        break;

                    case 0xDB: //SET 3,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 3, E");

                        if((registers[4] & 0x08 >> 3) == 0) registers[4] += 8;

                        programCounter++;
                        break;

                    case 0xDC: //SET 3,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 3, H");

                        if((registers[6] & 0x08 >> 3) == 0) registers[6] += 8;

                        programCounter++;
                        break;

                    case 0xDD: //SET 3,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 3, L");

                        if((registers[7] & 0x08 >> 3) == 0) registers[7] += 8;

                        programCounter++;
                        break;

                    case 0xDE: //SET 3,(HL)
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 3, (HL)");

                        if((registers[7] & 0x08 >> 3) == 0) registers[7] += 8; //NOT SURE

                        programCounter++;
                        break;

                    case 0xDF: //SET 3,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 3, A");

                        if((registers[0] & 0x08 >> 3) == 0) registers[0] += 8;

                        programCounter++;
                        break;

                    case 0xE0: //SET 4,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 4, B");

                        if((registers[1] & 0x10 >> 4) == 0) registers[1] += 16;

                        programCounter++;
                        break;

                    case 0xE1: //SET 4,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 4, C");

                        if((registers[2] & 0x10 >> 4) == 0) registers[2] += 16;

                        programCounter++;
                        break;

                    case 0xE2: //SET 4,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 4, D");

                        if((registers[3] & 0x10 >> 4) == 0) registers[3] += 16;

                        programCounter++;
                        break;

                    case 0xE3: //SET 4,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 4, E");

                        if((registers[4] & 0x10 >> 4) == 0) registers[4] += 16;

                        programCounter++;
                        break;

                    case 0xE4: //SET 4,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 4, H");

                        if((registers[6] & 0x10 >> 4) == 0) registers[6] += 16;

                        programCounter++;
                        break;

                    case 0xE5: //SET 4,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 4, L");

                        if((registers[7] & 0x10 >> 4) == 0) registers[7] += 16;

                        programCounter++;
                        break;

                    case 0xE6: //SET 4,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("SET 4, (HL)");

                        if((registers[7] & 0x10 >> 4) == 0) registers[7] += 16; //NOT SURE

                        programCounter++;
                        break;

                    case 0xE7: //SET 4,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 4, A");

                        if((registers[0] & 0x10 >> 4) == 0) registers[0] += 16;

                        programCounter++;
                        break;

                    case 0xE8: //SET 5,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 5, B");

                        if((registers[1] & 0x20 >> 5) == 0) registers[1] += 32;

                        programCounter++;
                        break;

                    case 0xE9: //SET 5,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 5, C");

                        if((registers[2] & 0x20 >> 5) == 0) registers[2] += 32;

                        programCounter++;
                        break;

                    case 0xEA: //SET 5,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 5, D");

                        if((registers[3] & 0x20 >> 5) == 0) registers[3] += 32;

                        programCounter++;
                        break;

                    case 0xEB: //SET 5,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 5, E");

                        if((registers[4] & 0x20 >> 5) == 0) registers[4] += 32;

                        programCounter++;
                        break;

                    case 0xEC: //SET 5,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 5, H");

                        if((registers[6] & 0x20 >> 5) == 0) registers[6] += 32;

                        programCounter++;
                        break;

                    case 0xED: //SET 5,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 5, L");

                        if((registers[7] & 0x20 >> 5) == 0) registers[7] += 32;

                        programCounter++;
                        break;

                    case 0xEE: //SET 5,(HL)
                        counter += 3;
                        if(DEBUGMODE) System.out.println("SET 5, (HL)");

                        if((registers[7] & 0x20 >> 5) == 0) registers[7] += 32; //NOT SURE

                        programCounter++;
                        break;

                    case 0xEF: //SET 5,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 5, A");

                        if((registers[0] & 0x20 >> 5) == 0) registers[0] += 32;

                        programCounter++;
                        break;

                    case 0xF0: //SET 6,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 6, B");

                        if((registers[1] & 0x40 >> 6) == 0) registers[1] += 64;

                        programCounter++;
                        break;

                    case 0xF1: //SET 6,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 6, C");

                        if((registers[2] & 0x40 >> 6) == 0) registers[2] += 64;

                        programCounter++;
                        break;

                    case 0xF2: //SET 6,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 6, D");

                        if((registers[3] & 0x40 >> 6) == 0) registers[3] += 64;

                        programCounter++;
                        break;

                    case 0xF3: //SET 6,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 6, E");

                        if((registers[4] & 0x40 >> 6) == 0) registers[4] += 64;

                        programCounter++;
                        break;

                    case 0xF4: //SET 6,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 6, H");

                        if((registers[6] & 0x40 >> 6) == 0) registers[6] += 64;

                        programCounter++;
                        break;

                    case 0xF5: //SET 6,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 6, L");

                        if((registers[7] & 0x40 >> 6) == 0) registers[7] += 64;

                        programCounter++;
                        break;

                    case 0xF6: //SET 6,(HL)
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 6, (HL)");

                        if((registers[7] & 0x40 >> 6) == 0) registers[7] += 64; //NOT SURE

                        programCounter++;
                        break;

                    case 0xF7: //SET 6,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 6, A");

                        if((registers[0] & 0x40 >> 6) == 0) registers[0] += 64;

                        programCounter++;
                        break;

                    case 0xF8: //SET 7,B
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 7, B");

                        if((registers[1] & 0x80 >> 7) == 0) registers[1] += 128;

                        programCounter++;
                        break;

                    case 0xF9: //SET 7,C
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 7, C");

                        if((registers[2] & 0x80 >> 7) == 0) registers[2] += 128;

                        programCounter++;
                        break;

                    case 0xFA: //SET 7,D
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 7, D");

                        if((registers[3] & 0x80 >> 7) == 0) registers[3] += 128;

                        programCounter++;
                        break;

                    case 0xFB: //SET 7,E
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 7, E");

                        if((registers[4] & 0x80 >> 7) == 0) registers[4] += 128;

                        programCounter++;
                        break;

                    case 0xFC: //SET 7,H
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 7, H");

                        if((registers[6] & 0x80 >> 7) == 0) registers[6] += 128;

                        programCounter++;
                        break;

                    case 0xFD: //SET 7,L
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 7, L");

                        if((registers[7] & 0x80 >> 7) == 0) registers[7] += 128;

                        programCounter++;
                        break;

                    case 0xFE: //SET 7,(HL)
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 7, (HL)");

                        if((registers[7] & 0x80 >> 7) == 0) registers[7] += 128; //NOT SURE

                        programCounter++;
                        break;

                    case 0xFF: //SET 7,A
                        counter += 1;
                        if(DEBUGMODE) System.out.println("SET 7, A");

                        if((registers[0] & 0x80 >> 7) == 0) registers[0] += 128;

                        programCounter++;
                        break;

                }

            case 0xCC: //CALL Z,nn
                counter += 3;
                if(DEBUGMODE) System.out.println("CALL Z, " + ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) & 0xff) * 256));

                if(zeroFlag) {
                    tempProgramCounter = programCounter + 3;

                    programCounter = (char) ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) & 0xff) * 256);
                    memory.setMemory(stackPointer - 1, (char) (((tempProgramCounter) & 0xff00) / 256));
                    memory.setMemory(stackPointer - 2, (char) ((tempProgramCounter) & 0xff));
                    stackPointer -= 2;
                } else {
                    programCounter += 3;
                }
                break;

            case 0xCD: //CALL u16
                counter += 3;
                if(DEBUGMODE) System.out.println("CALL " + (memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) & 0xff) * 256);
                tempProgramCounter = programCounter + 3;

                programCounter = (char) ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) & 0xff) * 256);
                memory.setMemory(stackPointer - 1, (char) (((tempProgramCounter) & 0xff00) / 256));
                memory.setMemory(stackPointer - 2, (char) ((tempProgramCounter) & 0xff));
                stackPointer -= 2;
                break;

            case 0xD4: //CALL NC,nn
                counter += 3;

                if(!carryFlag) {
                    tempProgramCounter = programCounter + 3;

                    programCounter = (char) ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) & 0xff) * 256);
                    memory.setMemory(stackPointer - 1, (char) (((tempProgramCounter) & 0xff00) / 256));
                    memory.setMemory(stackPointer - 2, (char) ((tempProgramCounter) & 0xff));
                    stackPointer -= 2;
                } else {
                    programCounter += 3;
                }
                break;

            case 0xDC: //CALL C,nn
                counter += 3;

                if(carryFlag) {
                    tempProgramCounter = programCounter + 3;

                    programCounter = (char) ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) & 0xff) * 256);
                    memory.setMemory(stackPointer - 1, (char) (((tempProgramCounter) & 0xff00) / 256));
                    memory.setMemory(stackPointer - 2, (char) ((tempProgramCounter) & 0xff));
                    stackPointer -= 2;
                } else {
                    programCounter += 3;
                }
                break;

            case 0xE0: //LD (FF00+u8),A    IMPLEMENTED AND WORKING I THINK
                counter += 3;
                if(DEBUGMODE) System.out.println("LD (0xFF00 + " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1)) + "), A");

                memory.setMemory(0xFF00 + memory.getCartridgeMemory(programCounter + 1), registers[0]);
                programCounter += 2;
                break;

            case 0xE1: //POP nn
                counter += 3;
                if(DEBUGMODE) System.out.println("POP");


                break;

            case 0xF0: //LD A,(FF00+u8)    IMPLEMENTED AND WORKING I THINK
                counter += 3;
                if(DEBUGMODE) System.out.println("LD A, (0xFF00 + " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff) + ")");

                registers[0] = memory.getMemory(0xFF00 + memory.getCartridgeMemory(programCounter + 1) & 0xff);
                programCounter += 2;
                break;

            case 0xF3: //DI
                counter++;
                if(DEBUGMODE) System.out.println("DI");

                interruptMasterEnable = true;
                programCounter++;
                break;

            case 0xFB: //EI
                counter++;
                if(DEBUGMODE) System.out.println("EI");

                interruptMasterEnable = false;
                programCounter++;
                break;

            case 0xFE: //CP A,u8   IMPLEMENTED AND WORKING I THINK
                counter += 2;
                if(DEBUGMODE) System.out.println("CP A, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff));

                zeroFlag = registers[0] == memory.getCartridgeMemory(programCounter + 1);
                subtractFlag = true;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != (memory.getCartridgeMemory(programCounter + 1) >> 4 & 0xf);
                carryFlag = registers[0] < memory.getCartridgeMemory(programCounter + 1);
                computeFRegister();
                //Thread.sleep(1);
                programCounter += 2;
                break;

            default:
                System.out.println("No OPCode or Lacks Implementation");
                System.exit(0);
        }
    }
}
