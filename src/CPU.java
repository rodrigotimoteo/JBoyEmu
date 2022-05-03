import java.util.Arrays;

public class CPU {

    private final boolean DEBUGMODE = false;

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
        fetchOperationCodes();
        decodeOperationCodes();
        if(DEBUGMODE) {
            dumpRegisters();
            dumpFlags();
            Thread.sleep(1000);
        }
    }

    private void fetchOperationCodes() {
        operationCode = (char) (memory.getCartridgeMemory(programCounter) & 0xff);
    }

    private void decodeOperationCodes() {
        int tempProgramCounter, carry;

        if(DEBUGMODE) {
            System.out.print(Integer.toHexString(programCounter) + "  ");
            System.out.println(Integer.toHexString(operationCode) + "  " + counter);
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
                if(DEBUGMODE) System.out.println("LD B, " + memory.getCartridgeMemory(programCounter + 1));

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
                if(DEBUGMODE) System.out.println("LD " + ((memory.getCartridgeMemory(programCounter + 2) + (memory.getCartridgeMemory(programCounter + 1) << 4)) + ", SP"));

                stackPointer = (char) (memory.getCartridgeMemory(programCounter + 2));
                stackPointer += (char) (memory.getCartridgeMemory(programCounter + 1) << 4);
                programCounter += 3;
                break;

            case 0x09: //ADD HL,BC
                counter++;

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
                System.out.println("INC C");

                zeroFlag = (registers[2] & 0xff) == 0x00;
                halfCarryFlag = (registers[2] >> 4 & 0xf) != ((registers[2] + 1) >> 4 & 0xf);
                registers[2]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x0D: //DEC C IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = registers[2] - 1 == 0;
                halfCarryFlag = (registers[2] >> 4 & 0xf) != ((registers[2] - 1) >> 4 & 0xf);
                registers[2]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x0E: //LD C,u8 IMPLEMENTED AND WORKING
                counter += 2;
                System.out.println("LD C, " + memory.getCartridgeMemory(programCounter + 1));

                registers[2] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x0F: //RRCA
                counter++;

                registers[0] = (char) (registers[0] >> 1);
                subtractFlag = false;
                halfCarryFlag = false;
                programCounter += 1;
                break;


            case 0x10: //STOP WAITING IMPLEMENTATION
                continueFlag = false;
                break;

            case 0x11: //LD DE,u16 IMPLEMENTED AND WORKING
                counter += 3;

                registers[3] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[4] = (char) memory.getCartridgeMemory(programCounter + 2);
                programCounter += 3;
                break;

            case 0x12: //LD (DE),A
                counter += 2;

                registers[3] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[4] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x13: //INC DE
                counter++;

                if(registers[4] == 255) {
                    registers[4] = 0;
                    registers[3]++;
                } else
                    registers[4]++;
                programCounter += 1;
                break;

            case 0x14: //INC D IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = (registers[3] & 0xff) == 0x00;
                halfCarryFlag = (registers[3] >> 4 & 0xf) != ((registers[3] + 1) >> 4 & 0xf);
                registers[3]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x15: //DEC D IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = registers[3] - 1 == 0;
                halfCarryFlag = (registers[3] >> 4 & 0xf) != ((registers[3] - 1) >> 4 & 0xf);
                registers[3]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x16: //LD D,u8 IMPLEMENTED AND WORKING
                counter += 2;

                registers[3] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x17: //RLA NAO ENTENDI A IMPLEMENTACAO


            case 0x18: //JR i8
                counter += 2;

                programCounter += memory.getCartridgeMemory(programCounter + 1);
                break;

            case 0x19: //ADD HL,DE
                counter++;

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

                registers[0] = registers[4];
                programCounter += 1;
                break;

            case 0x1B: //DEC DE
                counter++;

                if(registers[4] == 0) {
                    registers[4] = 255;
                    registers[3]--;
                } else
                    registers[4]--;
                programCounter += 1;
                break;

            case 0x1C: //INC E
                counter++;

                zeroFlag = (registers[4] & 0xff) == 0x00;
                halfCarryFlag = (registers[4] >> 4 & 0xf) != ((registers[4] + 1) >> 4 & 0xf);
                registers[4]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x1D: //DEC E IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = registers[4] - 1 == 0;
                halfCarryFlag = (registers[4] >> 4 & 0xf) != ((registers[4] - 1) >> 4 & 0xf);
                registers[4]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x1E: //LD E,u8 IMPLEMENTED AND WORKING
                counter += 2;

                registers[4] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x1F: //RRA NAO ENTENDO A IMPLEMENTACAO
                break;

            case 0x20: //JR NZ,i8
                counter += 2;

                if(!zeroFlag)
                    programCounter += (char) (memory.getCartridgeMemory(programCounter + 1));
                programCounter += 2;
                break;

            case 0x21: //LD HL,u16   IMPLEMENTED AND WORKING
                counter += 3;

                registers[7] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[6] = (char) memory.getCartridgeMemory(programCounter + 2);
                programCounter += 3;
                break;

            case 0x22: //LD (HL+),A IMPLEMENTED AND WORKING
                counter++;

                memory.setMemory((int) ((registers[6] & 0xff) * Math.pow(16, 2) + (registers[7] & 0xff)), registers[0]);
                registers[7]++;
                programCounter += 1;
                break;

            case 0x24: //INC H IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = (registers[6] & 0xff) == 0x00 ;
                halfCarryFlag = (registers[6] >> 4 & 0xf) != ((registers[6] + 1) >> 4 & 0xf);
                registers[6]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x25: //DEC E IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = registers[6] - 1 == 0;
                halfCarryFlag = (registers[6] >> 4 & 0xf) != ((registers[6] - 1) >> 4 & 0xf);
                registers[6]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x26: //LD H,u8 IMPLEMENTED AND WORKING
                counter += 2;

                registers[6] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x2C: //INC L IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = (registers[7] & 0xff) == 0x00;
                halfCarryFlag = (registers[7] >> 4 & 0xf) != ((registers[7] + 1) >> 4 & 0xf);
                registers[7]++;
                subtractFlag = false;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x2D: //DEC L IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = registers[7] - 1 == 0;
                halfCarryFlag = (registers[7] >> 4 & 0xf) != ((registers[7] - 1) >> 4 & 0xf);
                registers[7]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x2E: //LD L,u8 IMPLEMENTED AND WORKING
                counter += 2;

                registers[7] = (char) memory.getCartridgeMemory(programCounter + 1);
                programCounter += 2;
                break;

            case 0x2F: //CPL
                counter++;

                registers[0] ^= 0xFF;
                programCounter += 1;
                break;

            case 0x32: //LDD (HL),A   IMPLEMENTED AND WORKING
                counter++;

                memory.setMemory((int) ((registers[6] & 0xff) * Math.pow(16, 2) + (registers[7] & 0xff)), registers[0]);
                registers[7]--;
                programCounter += 1;
                break;

            case 0x3D: //DEC A IMPLEMENTED AND WORKING
                counter++;

                zeroFlag = registers[0] - 1 == 0;
                halfCarryFlag = (registers[0] >> 4 & 0xf) != ((registers[0] - 1) >> 4 & 0xf);
                registers[0]--;
                subtractFlag = true;
                computeFRegister();
                programCounter += 1;
                break;

            case 0x3E: //LD A,u8   IMPLEMENTED AND WORKING
                counter += 2;
                if(DEBUGMODE) System.out.println("LD A," + memory.getCartridgeMemory(programCounter + 1));

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

                programCounter = (char) ((memory.getCartridgeMemory(programCounter + 1) & 0xff) + (memory.getCartridgeMemory(programCounter + 2) << 8));
                break;

            case 0xC4: //CALL NZ, nn
                counter += 3;

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

                programCounter++;
                operationCode = (char) (memory.getCartridgeMemory(programCounter) & 0xff);

                switch(operationCode) {
                    case 0x00: //RLC B
                        counter += 1;

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
                        break;

                    case 0x07: //RLC A
                        counter += 1;

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

                        subtractFlag = false;
                        halfCarryFlag = false;
                        carry = (registers[7] & 0x01);
                        carryFlag = carry != 0;
                        registers[7] = (char) (registers[7] >> 1);
                        zeroFlag = registers[7] == 0;
                        computeFRegister();

                        programCounter++;
                        break;

                    case 0x0E:
                        break;

                    case 0x0F: //RRC A
                        counter += 1;

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
                        break;

                    case 0x17: //RL A
                        counter += 1;

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

                    case 0x18:
                        break;

                    case 0x19:
                        break;

                    case 0x1A:
                        break;

                    case 0x1B:
                        break;

                    case 0x1C:
                        break;

                    case 0x1D:
                        break;

                    case 0x1E:
                        break;

                    case 0x1F:
                        break;

                    case 0x20:
                        break;

                    case 0x21:
                        break;

                    case 0x22:
                        break;

                    case 0x23:
                        break;

                    case 0x24:
                        break;

                    case 0x25:
                        break;

                    case 0x26:
                        break;

                    case 0x27:
                        break;

                    case 0x28:
                        break;

                    case 0x29:
                        break;

                    case 0x2A:
                        break;

                    case 0x2B:
                        break;

                    case 0x2C:
                        break;

                    case 0x2D:
                        break;

                    case 0x2E:
                        break;

                    case 0x2F:
                        break;

                    case 0x30:
                        break;

                    case 0x31:
                        break;

                    case 0x32:
                        break;

                    case 0x33:
                        break;

                    case 0x34:
                        break;

                    case 0x35:
                        break;

                    case 0x36:
                        break;

                    case 0x37:
                        break;

                    case 0x38:
                        break;

                    case 0x39:
                        break;

                    case 0x3A:
                        break;

                    case 0x3B:
                        break;

                    case 0x3C:
                        break;

                    case 0x3D:
                        break;

                    case 0x3E:
                        break;

                    case 0x3F:
                        break;

                    case 0x40:
                        break;

                    case 0x41:
                        break;

                    case 0x42:
                        break;

                    case 0x43:
                        break;

                    case 0x44:
                        break;

                    case 0x45:
                        break;

                    case 0x46:
                        break;

                    case 0x47: //RL A
                        counter += 1;

                        //bit = (registers[0] & ( 1 << k )) >> k;

                        subtractFlag = false;
                        halfCarryFlag = true;
                        computeFRegister();

                        programCounter += 1;
                        break;

                    case 0x48:
                        break;

                    case 0x49:
                        break;

                    case 0x4A:
                        break;

                    case 0x4B:
                        break;

                    case 0x4C:
                        break;

                    case 0x4D:
                        break;

                    case 0x4E:
                        break;

                    case 0x4F:
                        break;

                    case 0x50:
                        break;

                    case 0x51:
                        break;

                    case 0x52:
                        break;

                    case 0x53:
                        break;

                    case 0x54:
                        break;

                    case 0x55:
                        break;

                    case 0x56:
                        break;

                    case 0x57:
                        break;

                    case 0x58:
                        break;

                    case 0x59:
                        break;

                    case 0x5A:
                        break;

                    case 0x5B:
                        break;

                    case 0x5C:
                        break;

                    case 0x5D:
                        break;

                    case 0x5E:
                        break;

                    case 0x5F:
                        break;

                    case 0x60:
                        break;

                    case 0x61:
                        break;

                    case 0x62:
                        break;

                    case 0x63:
                        break;

                    case 0x64:
                        break;

                    case 0x65:
                        break;

                    case 0x66:
                        break;

                    case 0x67:
                        break;

                    case 0x68:
                        break;

                    case 0x69:
                        break;

                    case 0x6A:
                        break;

                    case 0x6B:
                        break;

                    case 0x6C:
                        break;

                    case 0x6D:
                        break;

                    case 0x6E:
                        break;

                    case 0x6F:
                        break;

                    case 0x70:
                        break;

                    case 0x71:
                        break;

                    case 0x72:
                        break;

                    case 0x73:
                        break;

                    case 0x74:
                        break;

                    case 0x75:
                        break;

                    case 0x76:
                        break;

                    case 0x77:
                        break;

                    case 0x78:
                        break;

                    case 0x79:
                        break;

                    case 0x7A:
                        break;

                    case 0x7B:
                        break;

                    case 0x7C:
                        break;

                    case 0x7D:
                        break;

                    case 0x7E:
                        break;

                    case 0x7F:
                        break;

                    case 0x80:
                        break;

                    case 0x81:
                        break;

                    case 0x82:
                        break;

                    case 0x83:
                        break;

                    case 0x84:
                        break;

                    case 0x85:
                        break;

                    case 0x86:
                        break;

                    case 0x87:
                        break;

                    case 0x88:
                        break;

                    case 0x89:
                        break;

                    case 0x8A:
                        break;

                    case 0x8B:
                        break;

                    case 0x8C:
                        break;

                    case 0x8D:
                        break;

                    case 0x8E:
                        break;

                    case 0x8F:
                        break;

                    case 0x90:
                        break;

                    case 0x91:
                        break;

                    case 0x92:
                        break;

                    case 0x93:
                        break;

                    case 0x94:
                        break;

                    case 0x95:
                        break;

                    case 0x96:
                        break;

                    case 0x97:
                        break;

                    case 0x98:
                        break;

                    case 0x99:
                        break;

                    case 0x9A:
                        break;

                    case 0x9B:
                        break;

                    case 0x9C:
                        break;

                    case 0x9D:
                        break;

                    case 0x9E:
                        break;

                    case 0x9F:
                        break;

                    case 0xA0:
                        break;

                    case 0xA1:
                        break;

                    case 0xA2:
                        break;

                    case 0xA3:
                        break;

                    case 0xA4:
                        break;

                    case 0xA5:
                        break;

                    case 0xA6:
                        break;

                    case 0xA7:
                        break;

                    case 0xA8:
                        break;

                    case 0xA9:
                        break;

                    case 0xAA:
                        break;

                    case 0xAB:
                        break;

                    case 0xAC:
                        break;

                    case 0xAD:
                        break;

                    case 0xAE:
                        break;

                    case 0xAF:
                        break;

                    case 0xB0:
                        break;

                    case 0xB1:
                        break;

                    case 0xB2:
                        break;

                    case 0xB3:
                        break;

                    case 0xB4:
                        break;

                    case 0xB5:
                        break;

                    case 0xB6:
                        break;

                    case 0xB7:
                        break;

                    case 0xB8:
                        break;

                    case 0xB9:
                        break;

                    case 0xBA:
                        break;

                    case 0xBB:
                        break;

                    case 0xBC:
                        break;

                    case 0xBD:
                        break;

                    case 0xBE:
                        break;

                    case 0xBF:
                        break;

                    case 0xC0: //SWAP B
                        counter += 2;
                        if(DEBUGMODE) System.out.println("SWAP B");

                        //LACKS IMPLEMENTATION
                        zeroFlag = registers[1] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;
                        computeFRegister();

                        break;

                    case 0xC1: //SWAP C
                        counter += 2;
                        if(DEBUGMODE) System.out.println("SWAP C");

                        //LACKS IMPLEMENTATION
                        zeroFlag = registers[2] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;
                        computeFRegister();
                        break;

                    case 0xC2: //SWAP D
                        counter += 2;
                        if(DEBUGMODE) System.out.println("SWAP D");

                        //LACKS IMPLEMENTATION
                        zeroFlag = registers[3] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;
                        computeFRegister();
                        break;

                    case 0xC3: //SWAP E
                        counter += 2;
                        if(DEBUGMODE) System.out.println("SWAP E");

                        //LACKS IMPLEMENTATION
                        zeroFlag = registers[4] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;
                        computeFRegister();
                        break;

                    case 0xC4: //SWAP H
                        counter += 2;
                        if(DEBUGMODE) System.out.println("SWAP H");

                        //LACKS IMPLEMENTATION
                        zeroFlag = registers[6] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;
                        computeFRegister();
                        break;

                    case 0xC5: //SWAP L
                        counter += 2;
                        if(DEBUGMODE) System.out.println("SWAP L");

                        //LACKS IMPLEMENTATION
                        zeroFlag = registers[7] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;
                        computeFRegister();
                        break;

                    case 0xC6: //SWAP (HL)
                        counter += 4;
                        if(DEBUGMODE) System.out.println("SWAP C");

                        byte temp = (byte) registers[7];
                        registers[7] = registers[6];
                        registers[6] = (char) temp;
                        zeroFlag = registers[1] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;
                        computeFRegister();
                        break;

                    case 0xC7: //SWAP A
                        counter += 2;
                        if(DEBUGMODE) System.out.println("SWAP A");

                        //LACKS IMPLEMENTATION
                        zeroFlag = registers[0] == 0;
                        subtractFlag = false;
                        halfCarryFlag = false;
                        carryFlag = false;
                        computeFRegister();
                        break;

                    case 0xC8:
                        break;

                    case 0xC9:
                        break;

                    case 0xCA:
                        break;

                    case 0xCB:
                        break;

                    case 0xCC:
                        break;

                    case 0xCD:
                        break;

                    case 0xCE:
                        break;

                    case 0xCF:
                        break;

                    case 0xD0:
                        break;

                    case 0xD1:
                        break;

                    case 0xD2:
                        break;

                    case 0xD3:
                        break;

                    case 0xD4:
                        break;

                    case 0xD5:
                        break;

                    case 0xD6:
                        break;

                    case 0xD7:
                        break;

                    case 0xD8:
                        break;

                    case 0xD9:
                        break;

                    case 0xDA:
                        break;

                    case 0xDB:
                        break;

                    case 0xDC:
                        break;

                    case 0xDD:
                        break;

                    case 0xDE:
                        break;

                    case 0xDF:
                        break;

                    case 0xE0:
                        break;

                    case 0xE1:
                        break;

                    case 0xE2:
                        break;

                    case 0xE3:
                        break;

                    case 0xE4:
                        break;

                    case 0xE5:
                        break;

                    case 0xE6:
                        break;

                    case 0xE7:
                        break;

                    case 0xE8:
                        break;

                    case 0xE9:
                        break;

                    case 0xEA:
                        break;

                    case 0xEB:
                        break;

                    case 0xEC:
                        break;

                    case 0xED:
                        break;

                    case 0xEE:
                        break;

                    case 0xEF:
                        break;

                    case 0xF0:
                        break;

                    case 0xF1:
                        break;

                    case 0xF2:
                        break;

                    case 0xF3:
                        break;

                    case 0xF4:
                        break;

                    case 0xF5:
                        break;

                    case 0xF6:
                        break;

                    case 0xF7:
                        break;

                    case 0xF8:
                        break;

                    case 0xF9:
                        break;

                    case 0xFA:
                        break;

                    case 0xFB:
                        break;

                    case 0xFC:
                        break;

                    case 0xFD:
                        break;

                    case 0xFE:
                        break;

                    case 0xFF:
                        break;

                }

            case 0xCC: //CALL Z,nn
                counter += 3;

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

                memory.setMemory(0xFF00 + memory.getCartridgeMemory(programCounter + 1), registers[0]);
                programCounter += 2;
                break;

            case 0xE1: //POP nn
                counter += 3;



            case 0xF0: //LD A,(FF00+u8)    IMPLEMENTED AND WORKING I THINK
                counter += 3;

                registers[0] = memory.getMemory(0xFF00 + memory.getCartridgeMemory(programCounter + 1) & 0xff);
                programCounter += 2;
                break;

            case 0xF3: //DI
                counter++;

                interruptMasterEnable = true;
                programCounter++;
                break;

            case 0xFB: //EI
                counter++;

                interruptMasterEnable = false;
                programCounter++;
                break;

            case 0xFE: //CP A,u8   IMPLEMENTED AND WORKING I THINK
                counter += 2;

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
