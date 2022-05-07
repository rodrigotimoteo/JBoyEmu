import java.util.Arrays;

public class CPU {

    char[] registers = new char[8]; //AF, BC, DE and HL can be 16 bits if paired together
    private boolean zeroFlag;
    private boolean subtractFlag;
    private boolean halfCarryFlag;
    private boolean carryFlag;
    private char operationCode;
    private char programCounter = 0x0100;
    private char stackPointer = 0xFFFE;

    int counter = 0;

    private boolean continueFlag = true;
    private boolean interruptMasterEnable = false;

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

    public int getCounter() {
        return counter;
    }

    public char getRegister(int index) {
        return registers[index];
    }

    public char getProgramCounter() {
        return programCounter;
    }

    public char getOperationCode() {
        return operationCode;
    }

    public char getStackPointer() {
        return stackPointer;
    }

    public boolean getZeroFlag() {
        return zeroFlag;
    }

    public boolean getSubtractFlag() {
        return subtractFlag;
    }

    public boolean getHalfCarryFlag() {
        return halfCarryFlag;
    }

    public boolean getCarryFlag() {
        return carryFlag;
    }

    //Setters (for registers mainly)

    public void setRegister(int index, char value) {
        registers[index] = value;
    }

    public void setRegisters(int index1, int index2, char value1, char value2) {
        registers[index1] = value1;
        registers[index2] = value2;
    }

    public void increaseCounter(int amount) {
        counter += amount;
    }

    public void increaseProgramCounter(int amount) {
        programCounter += amount;
    }

    public void setProgramCounter(char amount) {
        programCounter = amount;
    }

    public void increaseStackPointer(int amount) {
        stackPointer += amount;
    }

    public void setStackPointer(char amount) {
        stackPointer = amount;
    }

    public void setZeroFlag(boolean state) {
        zeroFlag = state;
    }

    public void setSubtractFlag(boolean state) {
        subtractFlag = state;
    }

    public void setHalfCarryFlag(boolean state) {
        halfCarryFlag = state;
    }

    public void setCarryFlag(boolean state) {
        carryFlag = state;
    }

    public void setInterruptMasterEnable(boolean state) {
        interruptMasterEnable = state;
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
        CPUInstructions.setCpu(this);
        CPUInstructions.setMem(memory);

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

    public void computeFRegister() {
        registers[5] = 0;
        if(zeroFlag) registers[5] += 128;
        if(subtractFlag) registers[5] += 64;
        if(halfCarryFlag) registers[5] += 32;
        if(carryFlag) registers[5] += 16;
    }

    public void cycle() throws InterruptedException {
        fetchOperationCodes();
        decodeOperationCodes();
        //if(DEBUGMODE) {
            //dumpRegisters();
            //dumpFlags();
            //Thread.sleep(1000);
        //}
        //ppu.cycle();
    }

    public PPU getPPU() {
        return ppu;
    }

    private void fetchOperationCodes() {
        operationCode = (char) (memory.getCartridgeMemory(programCounter) & 0xff);
    }

    private void decodeOperationCodes() {
        int tempProgramCounter, carry;

        CPUInstructions.show();

        switch(operationCode) {
            case 0x00: //NOP IMPLEMENTED AND WORKING
                CPUInstructions.nop();
                break;

            case 0x01: //LD BC,u16 IMPLEMENTED AND WORKING
                counter += 3;
                //if(DEBUGMODE) System.out.println("LD BC, " + Integer.toHexString(memory.getCartridgeMemory(programCounter + 1) & 0xff) + Integer.toHexString(memory.getCartridgeMemory(programCounter + 2) & 0xff));

                registers[1] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[2] = (char) memory.getCartridgeMemory(programCounter + 2);
                programCounter += 3;
                break;

            case 0x02: //LD (BC),A
                CPUInstructions.ldTwoRegisters(1, 0);
                break;

            case 0x03: //INC BC
                CPUInstructions.incR(0);
                break;

            case 0x04: //INC B IMPLEMENTED AND WORKING
                CPUInstructions.inc(1);
                break;

            case 0x05: //DEC B   IMPLEMENTED AND WORKING
                CPUInstructions.dec(1);
                break;

            case 0x06: //LD B,u8    IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 9);
                break;

            case 0x07: //RLCA
                CPUInstructions.rlca();
                break;

            case 0x08: //LD (u16),SP
                counter += 3;
                //if(DEBUGMODE) System.out.println("LD " + Integer.toHexString((memory.getCartridgeMemory(programCounter + 2) + (memory.getCartridgeMemory(programCounter + 1) << 4)) & 0xff) + ", SP");

                stackPointer = (char) (memory.getCartridgeMemory(programCounter + 2));
                stackPointer += (char) (memory.getCartridgeMemory(programCounter + 1) << 4);
                programCounter += 3;
                break;

            case 0x09: //ADD HL,BC
                counter++;
                //if(DEBUGMODE) System.out.println("ADD HL, BC");

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
                CPUInstructions.ldTwoRegisters(0, 0);
                break;

            case 0x0B: //DEC BC
                CPUInstructions.decR(0);
                break;

            case 0x0C: //INC C IMPLEMENTED AND WORKING
                CPUInstructions.inc(2);
                break;

            case 0x0D: //DEC C IMPLEMENTED AND WORKING
                CPUInstructions.dec(2);
                break;

            case 0x0E: //LD C,u8 IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 9);
                break;

            case 0x0F: //RRCA
                CPUInstructions.rrca();
                break;

            case 0x10: //STOP WAITING IMPLEMENTATION
                CPUInstructions.stop();
                break;

            case 0x11: //LD DE,u16 IMPLEMENTED AND WORKING
                counter += 3;
                //if(DEBUGMODE) System.out.println("LD DE, " + Integer.toHexString(((memory.getCartridgeMemory(programCounter + 1) << 8) + memory.getCartridgeMemory(programCounter + 2) & 0xff)));

                registers[3] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[4] = (char) memory.getCartridgeMemory(programCounter + 2);
                programCounter += 3;
                break;

            case 0x12: //LD (DE),A
                CPUInstructions.ldTwoRegisters(1, 1);
                break;

            case 0x13: //INC DE
                CPUInstructions.incR(1);
                break;

            case 0x14: //INC D IMPLEMENTED AND WORKING
                CPUInstructions.inc(3);
                break;

            case 0x15: //DEC D IMPLEMENTED AND WORKING
                CPUInstructions.dec(3);
                break;

            case 0x16: //LD D,u8 IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 9);
                break;

            case 0x17: //RLA NAO ENTENDI A IMPLEMENTACAO
                CPUInstructions.rla();
                break;

            case 0x18: //JR i8
                CPUInstructions.jr(0, 4);
                break;

            case 0x19: //ADD HL,DE
                counter++;
                //if(DEBUGMODE) System.out.println("ADD HL, DE");

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
                CPUInstructions.ldTwoRegisters(0, 1);
                break;

            case 0x1B: //DEC DE
                CPUInstructions.decR(1);
                break;

            case 0x1C: //INC E
                CPUInstructions.inc(4);
                break;

            case 0x1D: //DEC E IMPLEMENTED AND WORKING
                CPUInstructions.dec(4);
                break;

            case 0x1E: //LD E,u8 IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 9);
                break;

            case 0x1F: //RRA
                CPUInstructions.rra();
                break;

            case 0x20: //JR NZ,i8
                CPUInstructions.jr(1, 0);
                break;

            case 0x21: //LD HL,u16   IMPLEMENTED AND WORKING
                counter += 3;
                //if(DEBUGMODE) System.out.println("LD HL, " + Integer.toHexString(((memory.getCartridgeMemory(programCounter + 1) << 8) + memory.getCartridgeMemory(programCounter + 2) & 0xff)));

                registers[7] = (char) memory.getCartridgeMemory(programCounter + 1);
                registers[6] = (char) memory.getCartridgeMemory(programCounter + 2);
                programCounter += 3;
                break;

            case 0x22: //LDI (HL),A IMPLEMENTED AND WORKING
                CPUInstructions.ldi(1);
                break;

            case 0x23: //INC HL
                CPUInstructions.incR(2);
                break;

            case 0x24: //INC H IMPLEMENTED AND WORKING
                CPUInstructions.inc(6);
                break;

            case 0x25: //DEC H IMPLEMENTED AND WORKING
                CPUInstructions.dec(6);
                break;

            case 0x26: //LD H,u8 IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 9);
                break;

            case 0x28: //JR Z,u8
                CPUInstructions.jr(1, 1);
                break;

            case 0x2A: //LDI A,(HL)
                CPUInstructions.ldi(0);
                break;

            case 0x2B: //DEC HL
                CPUInstructions.decR(2);
                break;

            case 0x2C: //INC L IMPLEMENTED AND WORKING
                CPUInstructions.inc(7);
                break;

            case 0x2D: //DEC L IMPLEMENTED AND WORKING
                CPUInstructions.dec(7);
                break;

            case 0x2E: //LD L,u8 IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 9);
                break;

            case 0x2F: //CPL
                CPUInstructions.cpl();
                break;

            case 0x30: //JR NC,u8
                CPUInstructions.jr(1, 2);
                break;

            case 0x32: //LDD (HL),A   IMPLEMENTED AND WORKING
                CPUInstructions.ldd(1);
                break;

            case 0x33: //INC SP
                CPUInstructions.incR(3);
                break;

            case 0x34: //INC (HL)
                CPUInstructions.inc(8);
                break;

            case 0x35: //INC (HL)
                CPUInstructions.dec(8);
                break;

            case 0x36: //LD (HL), n
                CPUInstructions.ld(8, 9);
                break;

            case 0x37: //SCF
                CPUInstructions.scf();
                break;

            case 0x38: //JR C,u8
                CPUInstructions.jr(1, 3);
                break;

            case 0x3A: //LDD A,(HL)
                CPUInstructions.ldd(0);
                break;

            case 0x3B: //DEC SP
                CPUInstructions.decR(3);
                break;

            case 0x3C: //INC A
                CPUInstructions.inc(0);
                break;

            case 0x3D: //DEC A IMPLEMENTED AND WORKING
                CPUInstructions.dec(0);
                break;

            case 0x3E: //LD A,u8   IMPLEMENTED AND WORKING
                CPUInstructions.ldTwoRegisters(0, 3);
                break;

            case 0x3F: //CCF
                CPUInstructions.ccf();

            case 0x40: //LD B,B  IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 1);
                break;

            case 0x41: //LD B,C IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 2);
                break;

            case 0x42: //LD B,D IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 3);
                break;

            case 0x43: //LD B,E IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 4);
                break;

            case 0x44: //LD B,H IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 6);
                break;

            case 0x45: //LD B,L IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 7);
                break;

            case 0x46: //LD B,(HL) IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 8);
                break;

            case 0x47: //LD B,A IMPLEMENTED AND WORKING
                CPUInstructions.ld(1, 0);
                break;

            case 0x48: //LD C,B IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 1);
                break;

            case 0x49: //LD C,C IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 2);
                break;

            case 0x4A: //LD C,D IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 3);
                break;

            case 0x4B: //LD C,E IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 4);
                break;

            case 0x4C: //LD C,H IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 6);
                break;

            case 0x4D: //LD C,L IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 7);
                break;

            case 0x4E: //LD C,(HL) IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 8);
                break;

            case 0x4F: //LD C,A IMPLEMENTED AND WORKING
                CPUInstructions.ld(2, 0);
                break;

            case 0x50: //LD D,B IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 1);
                break;

            case 0x51: //LD D,C IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 2);
                break;

            case 0x52: //LD D,D IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 3);
                break;

            case 0x53: //LD D,E IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 4);
                break;

            case 0x54: //LD D,H IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 6);
                break;

            case 0x55: //LD D,L IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 7);
                break;

            case 0x56: //LD D,(HL) IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 8);
                break;

            case 0x57: //LD D,A IMPLEMENTED AND WORKING
                CPUInstructions.ld(3, 0);
                break;

            case 0x58: //LD E,B IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 1);
                break;

            case 0x59: //LD E,C IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 2);
                break;

            case 0x5A: //LD E,D IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 3);
                break;

            case 0x5B: //LD E,E IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 4);
                break;

            case 0x5C: //LD E,H IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 6);
                break;

            case 0x5D: //LD E,L IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 7);
                break;

            case 0x5E: //LD E,(HL) IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 8);
                break;

            case 0x5F: //LD E,A IMPLEMENTED AND WORKING
                CPUInstructions.ld(4, 0);
                break;

            case 0x60: //LD H,B IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 1);
                break;

            case 0x61: //LD H,C IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 2);
                break;

            case 0x62: //LD H,D IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 3);
                break;

            case 0x63: //LD H,E IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 4);
                break;

            case 0x64: //LD H,H IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 6);
                break;

            case 0x65: //LD H,L IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 7);
                break;

            case 0x66: //LD H,(HL) IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 8);
                break;

            case 0x67: //LD H,A IMPLEMENTED AND WORKING
                CPUInstructions.ld(6, 0);
                break;

            case 0x68: //LD L,B IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 1);
                break;

            case 0x69: //LD L,C IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 2);
                break;

            case 0x6A: //LD L,D IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 3);
                break;

            case 0x6B: //LD L,E IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 4);
                break;

            case 0x6C: //LD L,H IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 6);
                break;

            case 0x6D: //LD L,L IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 7);
                break;

            case 0x6E: //LD L,(HL) IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 8);
                break;

            case 0x6F: //LD L,A IMPLEMENTED AND WORKING
                CPUInstructions.ld(7, 0);
                break;

            case 0x70: //LD (HL),B
                CPUInstructions.ld(8, 1);
                break;

            case 0x71: //LD (HL),C
                CPUInstructions.ld(8, 2);
                break;

            case 0x72: //LD (HL),D
                CPUInstructions.ld(8, 3);
                break;

            case 0x73: //LD (HL),E
                CPUInstructions.ld(8, 4);
                break;

            case 0x74: //LD (HL),H
                CPUInstructions.ld(8, 6);
                break;

            case 0x75: //LD (HL),L
                CPUInstructions.ld(8, 7);
                break;

            case 0x76: //HALT
                System.out.println("Oi");
                System.exit(0);
                break;

            case 0x77: //LD (HL),A
                CPUInstructions.ld(8, 0);
                break;

            case 0x78: //LD A,B  IMPLEMENTED AND WORKING
                CPUInstructions.ld(0, 1);
                break;

            case 0x79: //LD A,C  IMPLEMENTED AND WORKING
                CPUInstructions.ld(0, 2);
                break;

            case 0x7A: //LD A,D  IMPLEMENTED AND WORKING
                CPUInstructions.ld(0, 3);
                break;

            case 0x7B: //LD A,E  IMPLEMENTED AND WORKING
                CPUInstructions.ld(0, 4);
                break;

            case 0x7C: //LD A,H  IMPLEMENTED AND WORKING
                CPUInstructions.ld(0, 6);
                break;

            case 0x7D: //LD A,L  IMPLEMENTED AND WORKING
                CPUInstructions.ld(0, 7);
                break;

            case 0x7E: //LD A,(HL)  IMPLEMENTED AND WORKING
                CPUInstructions.ld(0, 8);
                break;

            case 0x7F: //LD A,A  IMPLEMENTED AND WORKING
                CPUInstructions.ld(0, 0);
                break;

            case 0x80: //ADD A,B NOT SURE
                CPUInstructions.add(1);
                break;

            case 0x81: //ADD A,C
                CPUInstructions.add(2);
                break;

            case 0x82: //ADD A,D
                CPUInstructions.add(3);
                break;

            case 0x83: //ADD A,E
                CPUInstructions.add(4);
                break;

            case 0x84: //ADD A, H
                CPUInstructions.add(6);
                break;

            case 0x85: //ADD A,L
                CPUInstructions.add(7);
                break;

            case 0x86: //ADD A,(HL)
                CPUInstructions.add(8);
                break;

            case 0x87: //ADD A,A
                CPUInstructions.add(0);
                break;

            case 0x88: //ADC A,B
                CPUInstructions.adc(1);
                break;

            case 0x89: //ADC A,C
                CPUInstructions.adc(2);
                break;

            case 0x8A: //ADC A,D
                CPUInstructions.adc(3);
                break;

            case 0x8B: //ADC A,E
                CPUInstructions.adc(4);
                break;

            case 0x8C: //ADC A,H
                CPUInstructions.adc(6);
                break;

            case 0x8D: //ADC A,L
                CPUInstructions.adc(7);
                break;

            case 0x8E: //ADC A,(HL)
                CPUInstructions.adc(8);
                break;

            case 0x8F: //ADC A,A
                CPUInstructions.adc(0);
                break;

            case 0xA0: //AND A,B
                CPUInstructions.and(1);
                break;

            case 0xA1: //AND A,C
                CPUInstructions.and(2);
                break;

            case 0xA2: //AND A,D
                CPUInstructions.and(3);
                break;

            case 0xA3: //AND A,E
                CPUInstructions.and(4);
                break;

            case 0xA4: //AND A,H
                CPUInstructions.and(6);
                break;

            case 0xA5: //AND A,L
                CPUInstructions.and(7);
                break;

            case 0xA6: //AND A,(HL)
                CPUInstructions.and(8);
                break;

            case 0xA7: //AND A,A
                CPUInstructions.and(0);
                break;

            case 0xA8: //XOR A,B
                CPUInstructions.xor(1);
                break;

            case 0xA9: //XOR A,C
                CPUInstructions.xor(2);
                break;

            case 0xAA: //XOR A,D
                CPUInstructions.xor(3);
                break;

            case 0xAB: //XOR A,E
                CPUInstructions.xor(4);
                break;

            case 0xAC: //XOR A,H
                CPUInstructions.xor(6);
                break;

            case 0xAD: //XOR A,L
                CPUInstructions.xor(7);
                break;

            case 0xAE: //XOR A,(HL)
                CPUInstructions.xor(8);
                break;

            case 0xAF: //XOR A,A
                CPUInstructions.xor(0);
                break;

            case 0xB0: //OR A,B
                CPUInstructions.or(1);
                break;

            case 0xB1: //OR A,C
                CPUInstructions.or(2);
                break;

            case 0xB2: //OR A,D
                CPUInstructions.or(3);
                break;

            case 0xB3: //OR A,E
                CPUInstructions.or(4);
                break;

            case 0xB4: //OR A,H
                CPUInstructions.or(6);
                break;

            case 0xB5: //OR A,L
                CPUInstructions.or(7);
                break;

            case 0xB6: //OR A,(HL)
                CPUInstructions.or(8);
                break;

            case 0xB7: //OR A,A
                CPUInstructions.or(0);
                break;

            case 0xB8: //CP A,B
                CPUInstructions.cp(1);
                break;

            case 0xB9: //CP A,C
                CPUInstructions.cp(2);
                break;

            case 0xBA: //CP A,D
                CPUInstructions.cp(3);
                break;

            case 0xBB: //CP A,E
                CPUInstructions.cp(4);
                break;

            case 0xBC: //CP A,H
                CPUInstructions.cp(6);
                break;

            case 0xBD: //CP A,L
                CPUInstructions.cp(7);
                break;

            case 0xBE: //CP A,(HL)
                CPUInstructions.cp(8);
                break;

            case 0xBF: //CP A,A
                CPUInstructions.cp(0);
                break;

            case 0xC0: //RET NZ
                counter += 2;
                //if(DEBUGMODE) System.out.println("RET NZ");

                //if(!zeroFlag)
                break;

            case 0xC2: //JP NZ,u16
                CPUInstructions.jpCond(0);
                break;

            case 0xC3: //JP u16   IMPLEMENTED AND WORKING
                CPUInstructions.jp();
                break;

            case 0xC4: //CALL NZ, nn
                CPUInstructions.callCond(0);
                break;

            case 0xC6: //ADD A,#
                CPUInstructions.add(9);
                break;

            case 0xC8: //RET Z
                counter += 2;
                //if(DEBUGMODE) System.out.println("RET Z");

                //if(zeroFlag)
                break;

            case 0xC9: //RET
                counter += 2;
                //if(DEBUGMODE) System.out.println("RET");

                break;

            case 0xCA: //JP Z,u16
                CPUInstructions.jpCond(1);
                break;

            case 0xCB:
                int carry1, bit;

                CPUInstructions.cb();

                programCounter++;
                operationCode = (char) (memory.getCartridgeMemory(programCounter) & 0xff);

                switch(operationCode) {
                    case 0x00: //RLC B
                        CPUInstructions.rlc(1);
                        break;

                    case 0x01: //RLC C
                        CPUInstructions.rlc(2);
                        break;

                    case 0x02: //RLC D
                        CPUInstructions.rlc(3);
                        break;

                    case 0x03: //RLC E
                        CPUInstructions.rlc(4);
                        break;

                    case 0x04: //RLC H
                        CPUInstructions.rlc(6);
                        break;

                    case 0x05: //RLC L
                        CPUInstructions.rlc(7);
                        break;

                    case 0x06: //RLC HL
                        CPUInstructions.rlc(8);
                        break;

                    case 0x07: //RLC A
                        CPUInstructions.rlc(0);
                        break;

                    case 0x08: //RRC B
                        CPUInstructions.rrc(1);
                        break;

                    case 0x09: //RRC C
                        CPUInstructions.rrc(2);
                        break;

                    case 0x0A: //RRC D
                        CPUInstructions.rrc(3);
                        break;

                    case 0x0B: //RRC E
                        CPUInstructions.rrc(4);
                        break;

                    case 0x0C: //RRC H
                        CPUInstructions.rrc(6);
                        break;

                    case 0x0D: //RRC L
                        CPUInstructions.rrc(7);
                        break;

                    case 0x0E: //RRC (HL)
                        CPUInstructions.rrc(8);
                        break;

                    case 0x0F: //RRC A
                        CPUInstructions.rrc(0);
                        break;

                    case 0x10: //RL B
                        CPUInstructions.rl(1);
                        break;

                    case 0x11: //RL C
                        CPUInstructions.rl(2);
                        break;

                    case 0x12: //RL D
                        CPUInstructions.rl(3);
                        break;

                    case 0x13: //RL E
                        CPUInstructions.rl(4);
                        break;

                    case 0x14: //RL H
                        CPUInstructions.rl(6);
                        break;

                    case 0x15: //RL L
                        CPUInstructions.rl(7);
                        break;

                    case 0x16: //RL (HL)
                        CPUInstructions.rl(8);
                        break;

                    case 0x17: //RL A
                        CPUInstructions.rl(0);
                        break;

                    case 0x18: //RR B
                        CPUInstructions.rr(1);
                        break;

                    case 0x19: //RR C
                        CPUInstructions.rr(2);
                        break;

                    case 0x1A: //RR D
                        CPUInstructions.rr(3);
                        break;

                    case 0x1B: //RR E
                        CPUInstructions.rr(4);
                        break;

                    case 0x1C: //RR H
                        CPUInstructions.rr(6);
                        break;

                    case 0x1D: //RR L
                        CPUInstructions.rr(7);
                        break;

                    case 0x1E: //RR (HL)
                        CPUInstructions.rr(8);
                        break;

                    case 0x1F: //RR A
                        CPUInstructions.rr(0);
                        break;

                    case 0x20: //SLA B
                        CPUInstructions.sla(1);
                        break;

                    case 0x21: //SLA C
                        CPUInstructions.sla(2);
                        break;

                    case 0x22: //SLA D
                        CPUInstructions.sla(3);
                        break;

                    case 0x23: //SLA E
                        CPUInstructions.sla(4);
                        break;

                    case 0x24: //SLA H
                        CPUInstructions.sla(6);
                        break;

                    case 0x25: //SLA L
                        CPUInstructions.sla(7);
                        break;

                    case 0x26: //SLA (HL)
                        CPUInstructions.sla(8);
                        break;

                    case 0x27: //SLA A
                        CPUInstructions.sla(0);
                        break;

                    case 0x28: //SRA B
                        CPUInstructions.sra(1);
                        break;

                    case 0x29: //SRA C
                        CPUInstructions.sra(2);
                        break;

                    case 0x2A: //SRA D
                        CPUInstructions.sra(3);
                        break;

                    case 0x2B: //SRA E
                        CPUInstructions.sra(4);
                        break;

                    case 0x2C: //SRA H
                        CPUInstructions.sra(6);
                        break;

                    case 0x2D: //SRA L
                        CPUInstructions.sra(7);
                        break;

                    case 0x2E: //SRA (HL)
                        CPUInstructions.sra(8);
                        break;

                    case 0x2F: //SRA A
                        CPUInstructions.sra(0);
                        break;

                    case 0x30: //SWAP B
                        CPUInstructions.swap(1);
                        break;

                    case 0x31: //SWAP C
                        CPUInstructions.swap(2);
                        break;

                    case 0x32: //SWAP D
                        CPUInstructions.swap(3);
                        break;

                    case 0x33: //SWAP E
                        CPUInstructions.swap(4);
                        break;

                    case 0x34: //SWAP H
                        CPUInstructions.swap(6);
                        break;

                    case 0x35: //SWAP L
                        CPUInstructions.swap(7);
                        break;

                    case 0x36: //SWAP (HL)
                        CPUInstructions.swap(8);
                        break;

                    case 0x37: //SWAP A
                        CPUInstructions.swap(0);
                        break;

                    case 0x38: //SRL B
                        CPUInstructions.srl(1);
                        break;

                    case 0x39: //SRL C
                        CPUInstructions.srl(2);
                        break;

                    case 0x3A: //SRL D
                        CPUInstructions.srl(3);
                        break;

                    case 0x3B: //SRL E
                        CPUInstructions.srl(4);
                        break;

                    case 0x3C: //SRL H
                        CPUInstructions.srl(6);
                        break;

                    case 0x3D: //SRL L
                        CPUInstructions.srl(7);
                        break;

                    case 0x3E: //SRL (HL)
                        CPUInstructions.srl(8);
                        break;

                    case 0x3F: //SRL A
                        CPUInstructions.srl(0);
                        break;

                    case 0x40: //BIT 0,B
                        CPUInstructions.bit(0, 1);
                        break;

                    case 0x41: //BIT 0,C
                        CPUInstructions.bit(0, 2);
                        break;

                    case 0x42: //BIT 0,D
                        CPUInstructions.bit(0, 3);
                        break;

                    case 0x43: //BIT 0,E
                        CPUInstructions.bit(0, 4);
                        break;

                    case 0x44: //BIT 0,H
                        CPUInstructions.bit(0, 6);
                        break;

                    case 0x45: //BIT 0,L
                        CPUInstructions.bit(0, 7);
                        break;

                    case 0x46: //BIT 0,(HL)
                        CPUInstructions.bit(0, 8);
                        break;

                    case 0x47: //BIT 0,A
                        CPUInstructions.bit(0, 0);
                        break;

                    case 0x48: //BIT 1,B
                        CPUInstructions.bit(1, 1);
                        break;

                    case 0x49: //BIT 1,C
                        CPUInstructions.bit(1, 2);
                        break;

                    case 0x4A: //BIT 1,D
                        CPUInstructions.bit(1, 3);
                        break;

                    case 0x4B: //BIT 1,E
                        CPUInstructions.bit(1, 4);
                        break;

                    case 0x4C: //BIT 1,H
                        CPUInstructions.bit(1, 6);
                        break;

                    case 0x4D: //BIT 1,L
                        CPUInstructions.bit(1, 7);
                        break;

                    case 0x4E: //BIT 1,(HL)
                        CPUInstructions.bit(1, 8);
                        break;

                    case 0x4F: //BIT 1,A
                        CPUInstructions.bit(1, 0);
                        break;

                    case 0x50: //BIT 2,B
                        CPUInstructions.bit(2, 1);
                        break;

                    case 0x51: //BIT 2,C
                        CPUInstructions.bit(2, 2);
                        break;

                    case 0x52: //BIT 2,D
                        CPUInstructions.bit(2, 3);
                        break;

                    case 0x53: //BIT 2,E
                        CPUInstructions.bit(2, 4);
                        break;

                    case 0x54: //BIT 2,H
                        CPUInstructions.bit(2, 6);
                        break;

                    case 0x55: //BIT 2,L
                        CPUInstructions.bit(2, 7);
                        break;

                    case 0x56: //BIT 2,(HL)
                        CPUInstructions.bit(2, 8);
                        break;

                    case 0x57: //BIT 2,A
                        CPUInstructions.bit(2, 0);
                        break;

                    case 0x58: //BIT 3,B
                        CPUInstructions.bit(3, 1);
                        break;

                    case 0x59: //BIT 3,C
                        CPUInstructions.bit(3, 2);
                        break;

                    case 0x5A: //BIT 3,D
                        CPUInstructions.bit(3, 3);
                        break;

                    case 0x5B: //BIT 3,E
                        CPUInstructions.bit(3, 4);
                        break;

                    case 0x5C: //BIT 3,H
                        CPUInstructions.bit(3, 6);
                        break;

                    case 0x5D: //BIT 3,L
                        CPUInstructions.bit(3, 7);
                        break;

                    case 0x5E: //BIT 3,(HL)
                        CPUInstructions.bit(3, 8);
                        break;

                    case 0x5F: //BIT 3,A
                        CPUInstructions.bit(3, 0);
                        break;

                    case 0x60: //BIT 4,B
                        CPUInstructions.bit(4, 1);
                        break;

                    case 0x61: //BIT 4,C
                        CPUInstructions.bit(4, 2);
                        break;

                    case 0x62: //BIT 4,D
                        CPUInstructions.bit(4, 3);
                        break;

                    case 0x63: //BIT 4,E
                        CPUInstructions.bit(4, 4);
                        break;

                    case 0x64: //BIT 4,H
                        CPUInstructions.bit(4, 6);
                        break;

                    case 0x65: //BIT 4,L
                        CPUInstructions.bit(4, 7);
                        break;

                    case 0x66: //BIT 4,(HL)
                        CPUInstructions.bit(4, 8);
                        break;

                    case 0x67: //BIT 4,A
                        CPUInstructions.bit(4, 0);
                        break;

                    case 0x68: //BIT 5,B
                        CPUInstructions.bit(5, 1);
                        break;

                    case 0x69: //BIT 5,C
                        CPUInstructions.bit(5, 2);
                        break;

                    case 0x6A: //BIT 5,D
                        CPUInstructions.bit(5, 3);
                        break;

                    case 0x6B: //BIT 5,E
                        CPUInstructions.bit(5, 4);
                        break;

                    case 0x6C: //BIT 5,H
                        CPUInstructions.bit(5, 6);
                        break;

                    case 0x6D: //BIT 5,L
                        CPUInstructions.bit(5, 7);
                        break;

                    case 0x6E: //BIT 5,(HL)
                        CPUInstructions.bit(5, 8);
                        break;

                    case 0x6F: //BIT 5,A
                        CPUInstructions.bit(5, 0);
                        break;

                    case 0x70: //BIT 6,B
                        CPUInstructions.bit(6, 1);
                        break;

                    case 0x71: //BIT 6,C
                        CPUInstructions.bit(6, 2);
                        break;

                    case 0x72: //BIT 6,D
                        CPUInstructions.bit(6, 3);
                        break;

                    case 0x73: //BIT 6,E
                        CPUInstructions.bit(6, 4);
                        break;

                    case 0x74: //BIT 6,H
                        CPUInstructions.bit(6, 6);
                        break;

                    case 0x75: //BIT 6,L
                        CPUInstructions.bit(6, 7);
                        break;

                    case 0x76: //BIT 6,(HL)
                        CPUInstructions.bit(6, 8);
                        break;

                    case 0x77: //BIT 6,A
                        CPUInstructions.bit(6, 0);
                        break;

                    case 0x78: //BIT 7,B
                        CPUInstructions.bit(7, 1);
                        break;

                    case 0x79: //BIT 7,C
                        CPUInstructions.bit(7, 2);
                        break;

                    case 0x7A: //BIT 7,D
                        CPUInstructions.bit(7, 3);
                        break;

                    case 0x7B: //BIT 7,E
                        CPUInstructions.bit(7, 4);
                        break;

                    case 0x7C: //BIT 7,H
                        CPUInstructions.bit(7, 6);
                        break;

                    case 0x7D: //BIT 7,L
                        CPUInstructions.bit(7, 7);
                        break;

                    case 0x7E: //BIT 7, (HL)
                        CPUInstructions.bit(7, 8);
                        break;

                    case 0x7F: //BIT 7,A
                        CPUInstructions.bit(7, 0);
                        break;

                    case 0x80: //RES 0,B
                        CPUInstructions.res(0,1);
                        break;

                    case 0x81: //RES 0,C
                        CPUInstructions.res(0,2);
                        break;

                    case 0x82: //RES 0,D
                        CPUInstructions.res(0,3);
                        break;

                    case 0x83: //RES 0,E
                        CPUInstructions.res(0,4);
                        break;

                    case 0x84: //RES 0,H
                        CPUInstructions.res(0,6);
                        break;

                    case 0x85: //RES 0,L
                        CPUInstructions.res(0,7);
                        break;

                    case 0x86: //RES 0,(HL)
                        CPUInstructions.res(0,8);
                        break;

                    case 0x87: //RES 0,A
                        CPUInstructions.res(0,0);
                        break;

                    case 0x88: //RES 1,B
                        CPUInstructions.res(1,1);
                        break;

                    case 0x89: //RES 1,C
                        CPUInstructions.res(1,2);
                        break;

                    case 0x8A: //RES 1,D
                        CPUInstructions.res(1,3);
                        break;

                    case 0x8B: //RES 1,E
                        CPUInstructions.res(1,4);
                        break;

                    case 0x8C: //RES 1,H
                        CPUInstructions.res(1,6);
                        break;

                    case 0x8D: //RES 1,L
                        CPUInstructions.res(1,7);
                        break;

                    case 0x8E: //RES 1,(HL)
                        CPUInstructions.res(1,8);
                        break;

                    case 0x8F: //RES 1,A
                        CPUInstructions.res(1,0);
                        break;

                    case 0x90: //RES 2,B
                        CPUInstructions.res(2,1);
                        break;

                    case 0x91: //RES 2,C
                        CPUInstructions.res(2,2);
                        break;

                    case 0x92: //RES 2,D
                        CPUInstructions.res(2,3);
                        break;

                    case 0x93: //RES 2,E
                        CPUInstructions.res(2,4);
                        break;

                    case 0x94: //RES 2,H
                        CPUInstructions.res(2,6);
                        break;

                    case 0x95: //RES 2,L
                        CPUInstructions.res(2,7);
                        break;

                    case 0x96: //RES 2,(HL)
                        CPUInstructions.res(2,8);
                        break;

                    case 0x97: //RES 2,A
                        CPUInstructions.res(2,0);
                        break;

                    case 0x98: //RES 3,B
                        CPUInstructions.res(3,1);
                        break;

                    case 0x99: //RES 3,C
                        CPUInstructions.res(3,2);
                        break;

                    case 0x9A: //RES 3,D
                        CPUInstructions.res(3,3);
                        break;

                    case 0x9B: //RES 3,E
                        CPUInstructions.res(3,4);
                        break;

                    case 0x9C: //RES 3,H
                        CPUInstructions.res(3,6);
                        break;

                    case 0x9D: //RES 3,L
                        CPUInstructions.res(3,7);
                        break;

                    case 0x9E: //RES 3,(HL)
                        CPUInstructions.res(3,8);
                        break;

                    case 0x9F: //RES 3,A
                        CPUInstructions.res(3,0);
                        break;

                    case 0xA0: //RES 4,B
                        CPUInstructions.res(4,1);
                        break;

                    case 0xA1: //RES 4,C
                        CPUInstructions.res(4,2);
                        break;

                    case 0xA2: //RES 4,D
                        CPUInstructions.res(4,3);
                        break;

                    case 0xA3: //RES 4,E
                        CPUInstructions.res(4,4);
                        break;

                    case 0xA4: //RES 4,H
                        CPUInstructions.res(4,6);
                        break;

                    case 0xA5: //RES 4,L
                        CPUInstructions.res(4,7);
                        break;

                    case 0xA6: //RES 4,(HL)
                        CPUInstructions.res(4,8);
                        break;

                    case 0xA7: //RES 4,A
                        CPUInstructions.res(4,0);
                        break;

                    case 0xA8: //RES 5,B
                        CPUInstructions.res(5,1);
                        break;

                    case 0xA9: //RES 5,C
                        CPUInstructions.res(5,2);
                        break;

                    case 0xAA: //RES 5,D
                        CPUInstructions.res(5,3);
                        break;

                    case 0xAB: //RES 5,E
                        CPUInstructions.res(5,4);
                        break;

                    case 0xAC: //RES 5,H
                        CPUInstructions.res(5,6);
                        break;

                    case 0xAD: //RES 5,L
                        CPUInstructions.res(5,7);
                        break;

                    case 0xAE: //RES 5,(HL)
                        CPUInstructions.res(5,8);
                        break;

                    case 0xAF: //RES 5,A
                        CPUInstructions.res(5,0);
                        break;

                    case 0xB0: //RES 6,B
                        CPUInstructions.res(6,1);
                        break;

                    case 0xB1: //RES 6,C
                        CPUInstructions.res(6,2);
                        break;

                    case 0xB2: //RES 6,D
                        CPUInstructions.res(6,3);
                        break;

                    case 0xB3: //RES 6,E
                        CPUInstructions.res(6,4);
                        break;

                    case 0xB4: //RES 6,H
                        CPUInstructions.res(6,6);
                        break;

                    case 0xB5: //RES 6,L
                        CPUInstructions.res(6,7);
                        break;

                    case 0xB6: //RES 6,(HL)
                        CPUInstructions.res(6,8);
                        break;

                    case 0xB7: //RES 6,A
                        CPUInstructions.res(6,0);
                        break;

                    case 0xB8: //RES 7,B
                        CPUInstructions.res(7,1);
                        break;

                    case 0xB9: //RES 7,C
                        CPUInstructions.res(7,2);
                        break;

                    case 0xBA: //RES 7,D
                        CPUInstructions.res(7,3);
                        break;

                    case 0xBB: //RES 7,E
                        CPUInstructions.res(7,4);
                        break;

                    case 0xBC: //RES 7,H
                        CPUInstructions.res(7,6);
                        break;

                    case 0xBD: //RES 7,L
                        CPUInstructions.res(7,7);
                        break;

                    case 0xBE: //RES 7,(HL)
                        CPUInstructions.res(7,8);
                        break;

                    case 0xBF: //RES 7,A
                        CPUInstructions.res(7,0);
                        break;

                    case 0xC0: //SET 0,B
                        CPUInstructions.set(0,1);
                        break;

                    case 0xC1: //SET 0,C
                        CPUInstructions.set(0,2);
                        break;

                    case 0xC2: //SET 0,D
                        CPUInstructions.set(0,3);
                        break;

                    case 0xC3: //SET 0,E
                        CPUInstructions.set(0,4);
                        break;

                    case 0xC4: //SET 0,H
                        CPUInstructions.set(0,6);
                        break;

                    case 0xC5: //SET 0,L
                        CPUInstructions.set(0,7);
                        break;

                    case 0xC6: //SET 0,(HL)
                        CPUInstructions.set(0,8);
                        break;

                    case 0xC7: //SET 0,A
                        CPUInstructions.set(0,0);
                        break;

                    case 0xC8: //SET 1,B
                        CPUInstructions.set(1,1);
                        break;

                    case 0xC9: //SET 1,C
                        CPUInstructions.set(1,2);
                        break;

                    case 0xCA: //SET 1,D
                        CPUInstructions.set(1,3);
                        break;

                    case 0xCB: //SET 1,E
                        CPUInstructions.set(1,4);
                        break;

                    case 0xCC: //SET 1,H
                        CPUInstructions.set(1,6);
                        break;

                    case 0xCD: //SET 1,L
                        CPUInstructions.set(1,7);
                        break;

                    case 0xCE: //SET 1,(HL)
                        CPUInstructions.set(1,8);
                        break;

                    case 0xCF: //SET 1,A
                        CPUInstructions.set(1,0);
                        break;

                    case 0xD0: //SET 2,B
                        CPUInstructions.set(2,1);
                        break;

                    case 0xD1: //SET 2,C
                        CPUInstructions.set(2,2);
                        break;

                    case 0xD2: //SET 2,D
                        CPUInstructions.set(2,3);
                        break;

                    case 0xD3: //SET 2,E
                        CPUInstructions.set(2,4);
                        break;

                    case 0xD4: //SET 2,H
                        CPUInstructions.set(2,6);
                        break;

                    case 0xD5: //SET 2,L
                        CPUInstructions.set(2,7);
                        break;

                    case 0xD6: //SET 2,(HL)
                        CPUInstructions.set(2,8);
                        break;

                    case 0xD7: //SET 2,A
                        CPUInstructions.set(2,0);
                        break;

                    case 0xD8: //SET 3,B
                        CPUInstructions.set(3,1);
                        break;

                    case 0xD9: //SET 3,C
                        CPUInstructions.set(3,2);
                        break;

                    case 0xDA: //SET 3,D
                        CPUInstructions.set(3,3);
                        break;

                    case 0xDB: //SET 3,E
                        CPUInstructions.set(3,4);
                        break;

                    case 0xDC: //SET 3,H
                        CPUInstructions.set(3,6);
                        break;

                    case 0xDD: //SET 3,L
                        CPUInstructions.set(3,7);
                        break;

                    case 0xDE: //SET 3,(HL)
                        CPUInstructions.set(3,8);
                        break;

                    case 0xDF: //SET 3,A
                        CPUInstructions.set(3,0);
                        break;

                    case 0xE0: //SET 4,B
                        CPUInstructions.set(4,1);
                        break;

                    case 0xE1: //SET 4,C
                        CPUInstructions.set(4,2);
                        break;

                    case 0xE2: //SET 4,D
                        CPUInstructions.set(4,3);
                        break;

                    case 0xE3: //SET 4,E
                        CPUInstructions.set(4,4);
                        break;

                    case 0xE4: //SET 4,H
                        CPUInstructions.set(4,6);
                        break;

                    case 0xE5: //SET 4,L
                        CPUInstructions.set(4,7);
                        break;

                    case 0xE6: //SET 4,(HL)
                        CPUInstructions.set(4,8);
                        break;

                    case 0xE7: //SET 4,A
                        CPUInstructions.set(4,0);
                        break;

                    case 0xE8: //SET 5,B
                        CPUInstructions.set(5,1);
                        break;

                    case 0xE9: //SET 5,C
                        CPUInstructions.set(5,2);
                        break;

                    case 0xEA: //SET 5,D
                        CPUInstructions.set(5,3);
                        break;

                    case 0xEB: //SET 5,E
                        CPUInstructions.set(5,4);
                        break;

                    case 0xEC: //SET 5,H
                        CPUInstructions.set(5,6);
                        break;

                    case 0xED: //SET 5,L
                        CPUInstructions.set(5,7);
                        break;

                    case 0xEE: //SET 5,(HL)
                        CPUInstructions.set(5,8);
                        break;

                    case 0xEF: //SET 5,A
                        CPUInstructions.set(5,0);
                        break;

                    case 0xF0: //SET 6,B
                        CPUInstructions.set(6,1);
                        break;

                    case 0xF1: //SET 6,C
                        CPUInstructions.set(6,2);
                        break;

                    case 0xF2: //SET 6,D
                        CPUInstructions.set(6,3);
                        break;

                    case 0xF3: //SET 6,E
                        CPUInstructions.set(6,4);
                        break;

                    case 0xF4: //SET 6,H
                        CPUInstructions.set(6,6);
                        break;

                    case 0xF5: //SET 6,L
                        CPUInstructions.set(6,7);
                        break;

                    case 0xF6: //SET 6,(HL)
                        CPUInstructions.set(6,8);
                        break;

                    case 0xF7: //SET 6,A
                        CPUInstructions.set(6,0);
                        break;

                    case 0xF8: //SET 7,B
                        CPUInstructions.set(7,1);
                        break;

                    case 0xF9: //SET 7,C
                        CPUInstructions.set(7,2);
                        break;

                    case 0xFA: //SET 7,D
                        CPUInstructions.set(7,3);
                        break;

                    case 0xFB: //SET 7,E
                        CPUInstructions.set(7,4);
                        break;

                    case 0xFC: //SET 7,H
                        CPUInstructions.set(7,6);
                        break;

                    case 0xFD: //SET 7,L
                        CPUInstructions.set(7,7);
                        break;

                    case 0xFE: //SET 7,(HL)
                        CPUInstructions.set(7,8);
                        break;

                    case 0xFF: //SET 7,A
                        CPUInstructions.set(7,0);
                        break;

                }

                break;

            case 0xCC: //CALL Z,nn
                CPUInstructions.callCond(1);
                break;

            case 0xCD: //CALL u16
                CPUInstructions.call();
                break;

            case 0xCE: //ADC A,#
                CPUInstructions.adc(9);
                break;

            case 0xD2: //JP NC,u16
                CPUInstructions.jpCond(2);
                break;

            case 0xD4: //CALL NC,nn
                CPUInstructions.callCond(2);
                break;

            case 0xDA: //JP C,u16
                CPUInstructions.jpCond(3);
                break;

            case 0xDC: //CALL C,nn
                CPUInstructions.callCond(3);
                break;

            case 0xE0: //LD (FF00+u8),A    IMPLEMENTED AND WORKING I THINK
                CPUInstructions.ldh(0);
                break;

            case 0xE1: //POP nn
                counter += 3;
                //if(DEBUGMODE) System.out.println("POP");


                break;

            case 0xE2: //LD (C), A
                CPUInstructions.ldAC(1);
                break;

            case 0xE6: //AND #
                CPUInstructions.and(9);
                break;

            case 0xE8: //ADD SP,n
                CPUInstructions.addSP();
                break;

            case 0xE9: //JP (HL)
                CPUInstructions.jpHL();
                break;

            case 0xEA: //LD (nn),A
                CPUInstructions.ldTwoRegisters(1, 2);
                break;

            case 0xEE: //XOR #
                CPUInstructions.xor(9);
                break;

            case 0xF0: //LD A,(FF00+u8)    IMPLEMENTED AND WORKING I THINK
                CPUInstructions.ldh(1);
                break;

            case 0xF2: //LD A,(C)
                CPUInstructions.ldAC(0);
                break;

            case 0xF3: //DI
                CPUInstructions.di();
                break;

            case 0xF6: //OR #
                CPUInstructions.or(9);
                break;

            case 0xFA: //LD A,(nn)
                CPUInstructions.ldTwoRegisters(0, 2);
                break;

            case 0xFB: //EI
                CPUInstructions.ei();
                break;

            case 0xFE: //CP A,u8   IMPLEMENTED AND WORKING I THINK
                CPUInstructions.cp(9);
                break;

            default:
                System.out.println("No OPCode or Lacks Implementation");
                System.exit(0);
        }
    }
}
