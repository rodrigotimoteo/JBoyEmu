import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class CPU {
    private final int DIV = 0xff04;
    private final int TIMA = 0xff05;
    private final int TMA = 0xff06;
    private final int IF = 0xff0f;
    private final int IE = 0xffff;

    private final int TIMER_INTERRUPT = 2;

    char[] registers = new char[8]; //AF, BC, DE and HL can be 16 bits if paired together
    private boolean zeroFlag;
    private boolean subtractFlag;
    private boolean halfCarryFlag;
    private boolean carryFlag;

    private boolean isHalted;
    private boolean isStopped;
    private boolean timerEnabled;
    private boolean handleOverflow;

    private char operationCode;
    private char programCounter = 0x100;
    private char stackPointer = 0xFFFE;

    private boolean cgb;

    private int counter = 0;
    private int haltCounter = 0;
    private int divClockCounter = 0;
    private int totalDiv = 0;
    private int timerClockCounter = 0;
    private int interruptCounter = 0;
    private int timerFrequency = 256;

    private int lastTimed = 0;

    private boolean interruptMasterEnable = false;
    private boolean setChangeTo = false;
    private boolean changeInterrupt = false;
    private boolean haltBug = false;

    private final boolean debugText = true;
    PrintStream debug;

    Memory memory;
    PPU ppu;
    DisplayFrame displayFrame;

    //Resets

    //Clears all register filling them with 0's
    private void clearRegisters() {
        Arrays.fill(registers, (char) 0);
    }

    //Getters

    //Return the CPU Cycle counter
    public int getCounter() {
        return counter;
    }

    //Return Register[index]
    public char getRegister(int index) {
        return (char) (registers[index] & 0xff);
    }

    //Return the Program Counter
    public char getProgramCounter() {
        return programCounter;
    }

    //Return the Stack Pointer address
    public char getStackPointer() {
        return stackPointer;
    }

    //Return the Status of the Zero Flag
    public boolean getZeroFlag() {
        return zeroFlag;
    }

    //Return the Status of Subtract Flag
    public boolean getSubtractFlag() {
        return subtractFlag;
    }

    //Return the Status of the Half Carry Flag
    public boolean getHalfCarryFlag() {
        return halfCarryFlag;
    }

    //Return the Status of the Carry Flag
    public boolean getCarryFlag() {
        return carryFlag;
    }

    //Return the Halted Status
    public boolean getIsHalted() {
        return isHalted;
    }

    //Return the Stopped Status
    public boolean getIsStopped() {
        return isStopped;
    }

    //Return the PPU
    public PPU getPPU() {
        return ppu;
    }

    public int getLastTimed() {
        return lastTimed;
    }

    //Setters (for registers mainly)

    //Sets Register[index] with a Value
    public void setRegister(int index, char value) {
        registers[index] = value;
    }

    public void setCgbMode() {
        cgb = true;

        ppu.setCgbMode();
    }


    //Increments a value to the CPU Cycle Counter
    public void increaseCounter(int amount) {
        counter += amount;
    }

    //Increments a value to the Program Counter
    public void increaseProgramCounter(int amount) {
        programCounter += amount;
    }

    //Sets the Program Counter to a Value
    public void setProgramCounter(char amount) {
        programCounter = amount;
    }

    //Increments a value to the Stack Pointer
    public void increaseStackPointer(int amount) {
        stackPointer += amount;
    }

    //Sets the Stack Pointer to a Value
    public void setStackPointer(char amount) {
        stackPointer = amount;
    }

    //Sets the Zero Flag to a State
    public void setZeroFlag(boolean state) {
        zeroFlag = state;
    }

    //Sets Subtract Flag to a State
    public void setSubtractFlag(boolean state) {
        subtractFlag = state;
    }

    //Sets the Half Carry Flag to a State
    public void setHalfCarryFlag(boolean state) {
        halfCarryFlag = state;
    }

    //Sets the Carry Flag to a State
    public void setCarryFlag(boolean state) {
        carryFlag = state;
    }

    //Set the isHalted Flag
    public void setIsHalted(boolean state) {
        isHalted = state;
    }

    //Set the isStopped Flag
    public void setIsStopped(boolean state) {
        isStopped = state;
    }

    //Set the signal to change the IME flag
    public void setChangeInterrupt(boolean state) {
        changeInterrupt = state;
    }

    //Used to enable interrupt after a cycle
    public void setInterruptCounter(int value) {
        interruptCounter = value;
    }

    //Set the state to change the IME flag to
    public void setChangeTo(boolean state) {
        setChangeTo = state;
    }

    //Sets the state of the timer enabled/disabled
    public void setTimerEnabled(boolean state) {
        timerEnabled = state;
    }

    public void setTimerFrequency(int frequency) {
        timerFrequency = frequency;
    }

    public void setDivClockCounter(int value) {
        divClockCounter = value;
    }

    public void setHaltCounter(int value) {
        haltCounter = value;
    }

    //Constructor

    public CPU() throws IOException {
        clearRegisters();
        memory = new Memory(this);
        ppu = new PPU(memory, this);
        displayFrame = new DisplayFrame(memory, ppu, this);
        memory.setDisplayFrame(displayFrame);

//        if(debugText) {
//            debug = new PrintStream("A.txt");
//            PrintStream console = System.out;
//            System.setOut(debug);
//        }

        CPUInstructions.setCpu(this);
        CPUInstructions.setMem(memory);

        //memory.dumpMemory();

        init();
    }

    private void init() {
        registers[0] = 0x01;
        registers[2] = 0x13;
        registers[4] = 0xD8;
        registers[5] = 0xB0;
        registers[6] = 0x01;
        registers[7] = 0x4D;

        zeroFlag = true;
        subtractFlag = false;
        halfCarryFlag = true;
        carryFlag = true;
    }

    public void reset() {
        memory.reset();
        ppu.reset();
        programCounter = 0x100;
        stackPointer = 0xfffe;
        counter = 0;
        init();
    }

    public void computeFlags() {
        zeroFlag = (registers[5] & 0x80) != 0;
        subtractFlag = (registers[5] & 0x40) != 0;
        halfCarryFlag = (registers[5] & 0x20) != 0;
        carryFlag = (registers[5] & 0x10) != 0;
    }

    public void computeFRegister() {
        registers[5] = 0;
        if(zeroFlag) registers[5] = (char) (registers[5] | 0x80);
        if(subtractFlag) registers[5] = (char) (registers[5] | 0x40);
        if(halfCarryFlag) registers[5] = (char) (registers[5] | 0x20);
        if(carryFlag) registers[5] = (char) (registers[5] | 0x10);
    }

    public Object[] saveState() {
        Object[] state = new Object[3];
//        state[0] = saveStateCPU();
        return state;
    }

//    private CPU saveStateCPU() {
//saveStates[state].registers = Arrays.copyOf(registers, registers.length);
//        saveStates[state].zeroFlag = zeroFlag;
//        saveStates[state].subtractFlag = subtractFlag;
//        saveStates[state].halfCarryFlag = halfCarryFlag;
//        saveStates[state].carryFlag = carryFlag;
//
//        saveStates[state].isHalted = isHalted;
//        saveStates[state].isStopped = isStopped;
//        saveStates[state].timerEnabled = timerEnabled;
//        saveStates[state].handleOverflow = handleOverflow;
//
//        saveStates[state].operationCode = operationCode;
//        saveStates[state].programCounter = programCounter;
//        saveStates[state].stackPointer = stackPointer;
//
//        saveStates[state].counter = counter;
//        saveStates[state].haltCounter = haltCounter;
//        saveStates[state].divClockCounter = divClockCounter;
//        saveStates[state].timerClockCounter = timerClockCounter;
//        saveStates[state].interruptCounter = interruptCounter;
//        saveStates[state].timerFrequency = timerFrequency;
//
//        saveStates[state].interruptMasterEnable = interruptMasterEnable;
//        saveStates[state].setChangeTo = setChangeTo;
//        saveStates[state].changeInterrupt = changeInterrupt;
//        saveStates[state].haltBug = haltBug;
//    }
//
//    public void loadState(CPU cpu, Memory memory, PPU ppu) {
//        loadState(cpu);
//        memory.loadState(memory);
//        ppu.loadState(ppu);
//    }

    private void loadState(CPU cpu) {
        this.registers = Arrays.copyOf(cpu.registers, registers.length);
        this.zeroFlag = cpu.zeroFlag;
        this.subtractFlag = cpu.subtractFlag;
        this.halfCarryFlag = cpu.halfCarryFlag;
        this.carryFlag = cpu.carryFlag;

        this.isHalted = cpu.isHalted;
        this.isStopped = cpu.isStopped;
        this.timerEnabled = cpu.timerEnabled;
        this.handleOverflow = cpu.handleOverflow;

        this.operationCode = cpu.operationCode;
        this.programCounter = cpu.programCounter;
        this.stackPointer = cpu.stackPointer;

        this.counter = cpu.counter;
        this.haltCounter = cpu.haltCounter;
        this.divClockCounter = cpu.divClockCounter;
        this.timerClockCounter = cpu.timerClockCounter;
        this.interruptCounter = cpu.interruptCounter;
        this.timerFrequency = cpu.timerFrequency;

        this.interruptMasterEnable = cpu.interruptMasterEnable;
        this.setChangeTo = cpu.setChangeTo;
        this.changeInterrupt = cpu.changeInterrupt;
        this.haltBug = cpu.haltBug;
    }

    public void cycle() throws InterruptedException {
//        if(counter >= 0x10000) {
//            memory.dumpMemory();
//            System.exit(0);
//        }

//        CPUInstructions.show();
//        CPUInstructions.dumpRegisters();
//        if(counter >= 0x10000)
//            System.exit(0);

        if (!getIsStopped()) {
            if(!getIsHalted()) {
                fetchOperationCodes();
                decodeOperationCodes();
                if(changeInterrupt && interruptCounter < counter) {
                    interruptMasterEnable = setChangeTo;
                    changeInterrupt = false;
                }
            } else {
                handleCPUTimers();
            }

            handleInterrupts();
        }
    }

    public void setInterrupt(int interrupt) {
        switch(interrupt) {
            case 0-> memory.setBit(IF, 0);
            case 1-> memory.setBit(IF, 1);
            case 2-> memory.setBit(IF, 2);
            case 3-> memory.setBit(IF, 3);
            case 4-> memory.setBit(IF, 4);
        }
    }

    private void handleInterrupts() {
        if(interruptMasterEnable) {
            char interrupt = (char) (memory.getMemory(IF) & memory.getMemory(IE));
            if(interrupt > 0) {
                if(isHalted) setIsHalted(false);

                interruptMasterEnable = false;
                memory.storeWordInSP(stackPointer, programCounter);

                int vBlank = interrupt & 0x1;
                if(vBlank == 1) {
                    setProgramCounter((char) 0x40);
                    memory.resetBit(IF, 0);
                    return;
                }

                int LCDCStatus = (interrupt & 0x2) >> 1;
                if(LCDCStatus == 1) {
                    setProgramCounter((char) 0x48);
                    memory.resetBit(IF, 1);
                    return;
                }

                int timerOverflow = (interrupt & 0x4) >> 2;
                if(timerOverflow == 1) {
                    setProgramCounter((char) 0x50);
                    memory.resetBit(IF, 2);
                    return;
                }

                int serialTransfer = (interrupt & 0x8) >> 3;
                if(serialTransfer == 1) {
                    setProgramCounter((char) 0x58);
                    memory.resetBit(IF, 3);
                    return;
                }

                int hiLo = (interrupt & 0x10) >> 4;
                if(hiLo == 1) {
                    if(isStopped) {
                        isStopped = false;
                    }
                    setProgramCounter((char) 0x60);
                    memory.resetBit(IF, 4);
                }
            }
        }
        else if(isHalted) {
            if ((memory.getMemory(IF) & memory.getMemory(IE) & 0x1f) > 0) {
                isHalted = false;
                if (haltCounter == counter) haltBug = true;
            }
        }
    }

    public void resetClocks() {
        boolean divUsed = timerFrequency == 256;

        if(divUsed && timerEnabled) {
            if(memory.testBit(DIV, 1)) memory.setMemory(TIMA, (char) (memory.getMemory(TIMA) + 1));
        } else if(timerEnabled) {
            if(totalDiv == 0) memory.setMemory(TIMA, (char) (memory.getMemory(TIMA) + 1));
        }

        divClockCounter = 0;
        totalDiv = 0;
        timerClockCounter = 0;
    }

    public void handleCPUTimers() {
        increaseCounter(1);

        handleDividerTimer();
        handleTimer();
//        System.out.println(divClockCounter + " " + timerClockCounter);
    }

    private void handleDividerTimer() {
        divClockCounter++;
        totalDiv++;
        while (divClockCounter >= 64) {
            divClockCounter -= 64;
            int div_counter = memory.getMemory(DIV);
            div_counter = (div_counter + 1) & 0xff;
            memory.writePriv(DIV, (char) div_counter);
        }
        if(totalDiv >= timerFrequency) totalDiv = 0;
    }

    private void handleTimer() {
        CPUInstructions.readTAC();
        if(handleOverflow) {
            memory.setMemory(TIMA, memory.getMemory(TMA));
            setInterrupt(TIMER_INTERRUPT);
            handleOverflow = false;
        }
        if (timerEnabled) {
            timerClockCounter++;
            while(timerClockCounter >= timerFrequency) {
                timerClockCounter -= timerFrequency;
                if (memory.getMemory(TIMA) == 0xff) {
                    handleOverflow = true;
                } else {
                    memory.setMemory(TIMA, (char) (((memory.getMemory(TIMA) & 0xff) + 1) & 0xff));
                }
            }
        }
    }

    private void fetchOperationCodes() {
        if(haltBug) {
            operationCode = memory.getMemory(programCounter--);
            haltBug = false;
        }
        else operationCode = memory.getMemory(programCounter);

        handleCPUTimers();
    }

    private void decodeOperationCodes() {
//        System.out.println(operationCode);

//        System.out.println(counter);

//        if(counter >= 164200) {
//            memory.dumpMemory();
//            System.exit(0);
//        }

//            CPUInstructions.dumpRegisters();
//            CPUInstructions.show();
//        }
////
//        if(counter >= 366125) {
//            System.exit(1);
//        }
//        if(debugText) System.setOut(debug);

        switch (operationCode) {
            case 0x00 -> //NOP
                    CPUInstructions.nop();
            case 0x01 -> //LD BC,u16
                    CPUInstructions.ld16bit(0);
            case 0x02 -> //LD (BC),A
                    CPUInstructions.ldTwoRegisters(0);
            case 0x03 -> //INC BC
                    CPUInstructions.incR(0);
            case 0x04 -> //INC B
                    CPUInstructions.inc(1);
            case 0x05 -> //DEC B
                    CPUInstructions.dec(1);
            case 0x06 -> //LD B,u8
                    CPUInstructions.ld(1, 9);
            case 0x07 -> //RLCA
                    CPUInstructions.rlca();
            case 0x08 -> //LD (u16),SP
                    CPUInstructions.LDnnSP();
            case 0x09 -> //ADD HL,BC
                    CPUInstructions.addHL(0);
            case 0x0A -> //LD A,(BC)
                    CPUInstructions.ldTwoRegistersIntoA(0);
            case 0x0B -> //DEC BC
                    CPUInstructions.decR(0);
            case 0x0C -> //INC C
                    CPUInstructions.inc(2);
            case 0x0D -> //DEC C
                    CPUInstructions.dec(2);
            case 0x0E -> //LD C,u8
                    CPUInstructions.ld(2, 9);
            case 0x0F -> //RRCA
                    CPUInstructions.rrca();
            case 0x10 -> //STOP
                    CPUInstructions.stop();
            case 0x11 -> //LD DE,u16
                    CPUInstructions.ld16bit(1);
            case 0x12 -> //LD (DE),A
                    CPUInstructions.ldTwoRegisters(1);
            case 0x13 -> //INC DE
                    CPUInstructions.incR(1);
            case 0x14 -> //INC D
                    CPUInstructions.inc(3);
            case 0x15 -> //DEC D
                    CPUInstructions.dec(3);
            case 0x16 -> //LD D,u8
                    CPUInstructions.ld(3, 9);
            case 0x17 -> //RLA
                    CPUInstructions.rla();
            case 0x18 -> //JR i8
                    CPUInstructions.jr();
            case 0x19 -> //ADD HL,DE
                    CPUInstructions.addHL(1);
            case 0x1A -> //LD A,(DE)
                    CPUInstructions.ldTwoRegistersIntoA(1);
            case 0x1B -> //DEC DE
                    CPUInstructions.decR(1);
            case 0x1C -> //INC E
                    CPUInstructions.inc(4);
            case 0x1D -> //DEC E
                    CPUInstructions.dec(4);
            case 0x1E -> //LD E,u8
                    CPUInstructions.ld(4, 9);
            case 0x1F -> //RRA
                    CPUInstructions.rra();
            case 0x20 -> //JR NZ,i8
                    CPUInstructions.jrCond(0);
            case 0x21 -> //LD HL,u16
                    CPUInstructions.ld16bit(2);
            case 0x22 -> //LDI (HL),A
                    CPUInstructions.ldi(1);
            case 0x23 -> //INC HL
                    CPUInstructions.incR(2);
            case 0x24 -> //INC H
                    CPUInstructions.inc(6);
            case 0x25 -> //DEC H
                    CPUInstructions.dec(6);
            case 0x26 -> //LD H,u8
                    CPUInstructions.ld(6, 9);
            case 0x27 -> //DAA
                    CPUInstructions.daa();
            case 0x28 -> //JR Z,u8
                    CPUInstructions.jrCond(1);
            case 0x29 -> //ADD HL, HL
                    CPUInstructions.addHL(2);
            case 0x2A -> //LDI A,(HL)
                    CPUInstructions.ldi(0);
            case 0x2B -> //DEC HL
                    CPUInstructions.decR(2);
            case 0x2C -> //INC L
                    CPUInstructions.inc(7);
            case 0x2D -> //DEC L
                    CPUInstructions.dec(7);
            case 0x2E -> //LD L,u8
                    CPUInstructions.ld(7, 9);
            case 0x2F -> //CPL
                    CPUInstructions.cpl();
            case 0x30 -> //JR NC,u8
                    CPUInstructions.jrCond(2);
            case 0x31 -> //LD SP,u16
                    CPUInstructions.ld16bit(3);
            case 0x32 -> //LDD (HL),A
                    CPUInstructions.ldd(1);
            case 0x33 -> //INC SP
                    CPUInstructions.incR(3);
            case 0x34 -> //INC (HL)
                    CPUInstructions.inc(8);
            case 0x35 -> //INC (HL)
                    CPUInstructions.dec(8);
            case 0x36 -> //LD (HL), n
                    CPUInstructions.ld(8, 9);
            case 0x37 -> //SCF
                    CPUInstructions.scf();
            case 0x38 -> //JR C,u8
                    CPUInstructions.jrCond(3);
            case 0x39 -> //ADD HL,SP
                    CPUInstructions.addHL(3);
            case 0x3A -> //LDD A,(HL)
                    CPUInstructions.ldd(0);
            case 0x3B -> //DEC SP
                    CPUInstructions.decR(3);
            case 0x3C -> //INC A
                    CPUInstructions.inc(0);
            case 0x3D -> //DEC A
                    CPUInstructions.dec(0);
            case 0x3E -> //LD A,u8
                    CPUInstructions.ldTwoRegistersIntoA(3);
            case 0x3F -> //CCF
                    CPUInstructions.ccf();
            case 0x40 -> //LD B,B
                    CPUInstructions.ld(1, 1);
            case 0x41 -> //LD B,C
                    CPUInstructions.ld(1, 2);
            case 0x42 -> //LD B,D
                    CPUInstructions.ld(1, 3);
            case 0x43 -> //LD B,E
                    CPUInstructions.ld(1, 4);
            case 0x44 -> //LD B,H
                    CPUInstructions.ld(1, 6);
            case 0x45 -> //LD B,L
                    CPUInstructions.ld(1, 7);
            case 0x46 -> //LD B,(HL)
                    CPUInstructions.ld(1, 8);
            case 0x47 -> //LD B,A
                    CPUInstructions.ld(1, 0);
            case 0x48 -> //LD C,B
                    CPUInstructions.ld(2, 1);
            case 0x49 -> //LD C,C
                    CPUInstructions.ld(2, 2);
            case 0x4A -> //LD C,D
                    CPUInstructions.ld(2, 3);
            case 0x4B -> //LD C,E
                    CPUInstructions.ld(2, 4);
            case 0x4C -> //LD C,H
                    CPUInstructions.ld(2, 6);
            case 0x4D -> //LD C,L
                    CPUInstructions.ld(2, 7);
            case 0x4E -> //LD C,(HL)
                    CPUInstructions.ld(2, 8);
            case 0x4F -> //LD C,A
                    CPUInstructions.ld(2, 0);
            case 0x50 -> //LD D,B
                    CPUInstructions.ld(3, 1);
            case 0x51 -> //LD D,C
                    CPUInstructions.ld(3, 2);
            case 0x52 -> //LD D,D
                    CPUInstructions.ld(3, 3);
            case 0x53 -> //LD D,E
                    CPUInstructions.ld(3, 4);
            case 0x54 -> //LD D,H
                    CPUInstructions.ld(3, 6);
            case 0x55 -> //LD D,L
                    CPUInstructions.ld(3, 7);
            case 0x56 -> //LD D,(HL)
                    CPUInstructions.ld(3, 8);
            case 0x57 -> //LD D,A
                    CPUInstructions.ld(3, 0);
            case 0x58 -> //LD E,B
                    CPUInstructions.ld(4, 1);
            case 0x59 -> //LD E,C
                    CPUInstructions.ld(4, 2);
            case 0x5A -> //LD E,D
                    CPUInstructions.ld(4, 3);
            case 0x5B -> //LD E,E
                    CPUInstructions.ld(4, 4);
            case 0x5C -> //LD E,H
                    CPUInstructions.ld(4, 6);
            case 0x5D -> //LD E,L
                    CPUInstructions.ld(4, 7);
            case 0x5E -> //LD E,(HL)
                    CPUInstructions.ld(4, 8);
            case 0x5F -> //LD E,A
                    CPUInstructions.ld(4, 0);
            case 0x60 -> //LD H,B
                    CPUInstructions.ld(6, 1);
            case 0x61 -> //LD H,C
                    CPUInstructions.ld(6, 2);
            case 0x62 -> //LD H,D
                    CPUInstructions.ld(6, 3);
            case 0x63 -> //LD H,E
                    CPUInstructions.ld(6, 4);
            case 0x64 -> //LD H,H
                    CPUInstructions.ld(6, 6);
            case 0x65 -> //LD H,L
                    CPUInstructions.ld(6, 7);
            case 0x66 -> //LD H,(HL)
                    CPUInstructions.ld(6, 8);
            case 0x67 -> //LD H,A
                    CPUInstructions.ld(6, 0);
            case 0x68 -> //LD L,B
                    CPUInstructions.ld(7, 1);
            case 0x69 -> //LD L,C
                    CPUInstructions.ld(7, 2);
            case 0x6A -> //LD L,D
                    CPUInstructions.ld(7, 3);
            case 0x6B -> //LD L,E
                    CPUInstructions.ld(7, 4);
            case 0x6C -> //LD L,H
                    CPUInstructions.ld(7, 6);
            case 0x6D -> //LD L,L
                    CPUInstructions.ld(7, 7);
            case 0x6E -> //LD L,(HL)
                    CPUInstructions.ld(7, 8);
            case 0x6F -> //LD L,A
                    CPUInstructions.ld(7, 0);
            case 0x70 -> //LD (HL),B
                    CPUInstructions.ld(8, 1);
            case 0x71 -> //LD (HL),C
                    CPUInstructions.ld(8, 2);
            case 0x72 -> //LD (HL),D
                    CPUInstructions.ld(8, 3);
            case 0x73 -> //LD (HL),E
                    CPUInstructions.ld(8, 4);
            case 0x74 -> //LD (HL),H
                    CPUInstructions.ld(8, 6);
            case 0x75 -> //LD (HL),L
                    CPUInstructions.ld(8, 7);
            case 0x76 -> //HALT
                    CPUInstructions.halt();
            case 0x77 -> //LD (HL),A
                    CPUInstructions.ld(8, 0);
            case 0x78 -> //LD A,B
                    CPUInstructions.ld(0, 1);
            case 0x79 -> //LD A,C
                    CPUInstructions.ld(0, 2);
            case 0x7A -> //LD A,D
                    CPUInstructions.ld(0, 3);
            case 0x7B -> //LD A,E
                    CPUInstructions.ld(0, 4);
            case 0x7C -> //LD A,H
                    CPUInstructions.ld(0, 6);
            case 0x7D -> //LD A,L
                    CPUInstructions.ld(0, 7);
            case 0x7E -> //LD A,(HL)
                    CPUInstructions.ld(0, 8);
            case 0x7F -> //LD A,A
                    CPUInstructions.ld(0, 0);
            case 0x80 -> //ADD A,B
                    CPUInstructions.add(1);
            case 0x81 -> //ADD A,C
                    CPUInstructions.add(2);
            case 0x82 -> //ADD A,D
                    CPUInstructions.add(3);
            case 0x83 -> //ADD A,E
                    CPUInstructions.add(4);
            case 0x84 -> //ADD A, H
                    CPUInstructions.add(6);
            case 0x85 -> //ADD A,L
                    CPUInstructions.add(7);
            case 0x86 -> //ADD A,(HL)
                    CPUInstructions.add(8);
            case 0x87 -> //ADD A,A
                    CPUInstructions.add(0);
            case 0x88 -> //ADC A,B
                    CPUInstructions.adc(1);
            case 0x89 -> //ADC A,C
                    CPUInstructions.adc(2);
            case 0x8A -> //ADC A,D
                    CPUInstructions.adc(3);
            case 0x8B -> //ADC A,E
                    CPUInstructions.adc(4);
            case 0x8C -> //ADC A,H
                    CPUInstructions.adc(6);
            case 0x8D -> //ADC A,L
                    CPUInstructions.adc(7);
            case 0x8E -> //ADC A,(HL)
                    CPUInstructions.adc(8);
            case 0x8F -> //ADC A,A
                    CPUInstructions.adc(0);
            case 0x90 -> //SUB A,B
                    CPUInstructions.sub(1);
            case 0x91 -> //SUB A,C
                    CPUInstructions.sub(2);
            case 0x92 -> //SUB A,D
                    CPUInstructions.sub(3);
            case 0x93 -> //SUB A,E
                    CPUInstructions.sub(4);
            case 0x94 -> //SUB A,H
                    CPUInstructions.sub(6);
            case 0x95 -> //SUB A,L
                    CPUInstructions.sub(7);
            case 0x96 -> //SUB A, (HL)
                    CPUInstructions.sub(8);
            case 0x97 -> //SUB A,A
                    CPUInstructions.sub(0);
            case 0x98 -> //SBC A,B
                    CPUInstructions.sbc(1);
            case 0x99 -> //SBC A,C
                    CPUInstructions.sbc(2);
            case 0x9A -> //SBC A,D
                    CPUInstructions.sbc(3);
            case 0x9B -> //SBC A,E
                    CPUInstructions.sbc(4);
            case 0x9C -> //SBC A,H
                    CPUInstructions.sbc(6);
            case 0x9D -> //SBC A,L
                    CPUInstructions.sbc(7);
            case 0x9E -> //SBC A, (HL)
                    CPUInstructions.sbc(8);
            case 0x9F -> //SBC A,A
                    CPUInstructions.sbc(0);
            case 0xA0 -> //AND A,B
                    CPUInstructions.and(1);
            case 0xA1 -> //AND A,C
                    CPUInstructions.and(2);
            case 0xA2 -> //AND A,D
                    CPUInstructions.and(3);
            case 0xA3 -> //AND A,E
                    CPUInstructions.and(4);
            case 0xA4 -> //AND A,H
                    CPUInstructions.and(6);
            case 0xA5 -> //AND A,L
                    CPUInstructions.and(7);
            case 0xA6 -> //AND A,(HL)
                    CPUInstructions.and(8);
            case 0xA7 -> //AND A,A
                    CPUInstructions.and(0);
            case 0xA8 -> //XOR A,B
                    CPUInstructions.xor(1);
            case 0xA9 -> //XOR A,C
                    CPUInstructions.xor(2);
            case 0xAA -> //XOR A,D
                    CPUInstructions.xor(3);
            case 0xAB -> //XOR A,E
                    CPUInstructions.xor(4);
            case 0xAC -> //XOR A,H
                    CPUInstructions.xor(6);
            case 0xAD -> //XOR A,L
                    CPUInstructions.xor(7);
            case 0xAE -> //XOR A,(HL)
                    CPUInstructions.xor(8);
            case 0xAF -> //XOR A,A
                    CPUInstructions.xor(0);
            case 0xB0 -> //OR A,B
                    CPUInstructions.or(1);
            case 0xB1 -> //OR A,C
                    CPUInstructions.or(2);
            case 0xB2 -> //OR A,D
                    CPUInstructions.or(3);
            case 0xB3 -> //OR A,E
                    CPUInstructions.or(4);
            case 0xB4 -> //OR A,H
                    CPUInstructions.or(6);
            case 0xB5 -> //OR A,L
                    CPUInstructions.or(7);
            case 0xB6 -> //OR A,(HL)
                    CPUInstructions.or(8);
            case 0xB7 -> //OR A,A
                    CPUInstructions.or(0);
            case 0xB8 -> //CP A,B
                    CPUInstructions.cp(1);
            case 0xB9 -> //CP A,C
                    CPUInstructions.cp(2);
            case 0xBA -> //CP A,D
                    CPUInstructions.cp(3);
            case 0xBB -> //CP A,E
                    CPUInstructions.cp(4);
            case 0xBC -> //CP A,H
                    CPUInstructions.cp(6);
            case 0xBD -> //CP A,L
                    CPUInstructions.cp(7);
            case 0xBE -> //CP A,(HL)
                    CPUInstructions.cp(8);
            case 0xBF -> //CP A,A
                    CPUInstructions.cp(0);
            case 0xC0 -> //RET NZ
                    CPUInstructions.retCond(0);
            case 0xC1 -> //POP BC
                    CPUInstructions.pop(1);
            case 0xC2 -> //JP NZ,u16
                    CPUInstructions.jpCond(0);
            case 0xC3 -> //JP u16
                    CPUInstructions.jp();
            case 0xC4 -> //CALL NZ, nn
                    CPUInstructions.callCond(0);
            case 0xC5 -> //PUSH BC
                    CPUInstructions.push(1);
            case 0xC6 -> //ADD A,#
                    CPUInstructions.add(9);
            case 0xC7 -> //RST 00H
                    CPUInstructions.rst(0);
            case 0xC8 -> //RET Z
                    CPUInstructions.retCond(1);
            case 0xC9 -> //RET
                    CPUInstructions.ret();
            case 0xCA -> //JP Z,u16
                    CPUInstructions.jpCond(1);
            case 0xCB -> {
                CPUInstructions.cb();
                operationCode = (char) (memory.getMemory(programCounter) & 0xff);
                switch (operationCode) {
                    case 0x00 -> //RLC B
                            CPUInstructions.rlc(1);
                    case 0x01 -> //RLC C
                            CPUInstructions.rlc(2);
                    case 0x02 -> //RLC D
                            CPUInstructions.rlc(3);
                    case 0x03 -> //RLC E
                            CPUInstructions.rlc(4);
                    case 0x04 -> //RLC H
                            CPUInstructions.rlc(6);
                    case 0x05 -> //RLC L
                            CPUInstructions.rlc(7);
                    case 0x06 -> //RLC HL
                            CPUInstructions.rlc(8);
                    case 0x07 -> //RLC A
                            CPUInstructions.rlc(0);
                    case 0x08 -> //RRC B
                            CPUInstructions.rrc(1);
                    case 0x09 -> //RRC C
                            CPUInstructions.rrc(2);
                    case 0x0A -> //RRC D
                            CPUInstructions.rrc(3);
                    case 0x0B -> //RRC E
                            CPUInstructions.rrc(4);
                    case 0x0C -> //RRC H
                            CPUInstructions.rrc(6);
                    case 0x0D -> //RRC L
                            CPUInstructions.rrc(7);
                    case 0x0E -> //RRC (HL)
                            CPUInstructions.rrc(8);
                    case 0x0F -> //RRC A
                            CPUInstructions.rrc(0);
                    case 0x10 -> //RL B
                            CPUInstructions.rl(1);
                    case 0x11 -> //RL C
                            CPUInstructions.rl(2);
                    case 0x12 -> //RL D
                            CPUInstructions.rl(3);
                    case 0x13 -> //RL E
                            CPUInstructions.rl(4);
                    case 0x14 -> //RL H
                            CPUInstructions.rl(6);
                    case 0x15 -> //RL L
                            CPUInstructions.rl(7);
                    case 0x16 -> //RL (HL)
                            CPUInstructions.rl(8);
                    case 0x17 -> //RL A
                            CPUInstructions.rl(0);
                    case 0x18 -> //RR B
                            CPUInstructions.rr(1);
                    case 0x19 -> //RR C
                            CPUInstructions.rr(2);
                    case 0x1A -> //RR D
                            CPUInstructions.rr(3);
                    case 0x1B -> //RR E
                            CPUInstructions.rr(4);
                    case 0x1C -> //RR H
                            CPUInstructions.rr(6);
                    case 0x1D -> //RR L
                            CPUInstructions.rr(7);
                    case 0x1E -> //RR (HL)
                            CPUInstructions.rr(8);
                    case 0x1F -> //RR A
                            CPUInstructions.rr(0);
                    case 0x20 -> //SLA B
                            CPUInstructions.sla(1);
                    case 0x21 -> //SLA C
                            CPUInstructions.sla(2);
                    case 0x22 -> //SLA D
                            CPUInstructions.sla(3);
                    case 0x23 -> //SLA E
                            CPUInstructions.sla(4);
                    case 0x24 -> //SLA H
                            CPUInstructions.sla(6);
                    case 0x25 -> //SLA L
                            CPUInstructions.sla(7);
                    case 0x26 -> //SLA (HL)
                            CPUInstructions.sla(8);
                    case 0x27 -> //SLA A
                            CPUInstructions.sla(0);
                    case 0x28 -> //SRA B
                            CPUInstructions.sra(1);
                    case 0x29 -> //SRA C
                            CPUInstructions.sra(2);
                    case 0x2A -> //SRA D
                            CPUInstructions.sra(3);
                    case 0x2B -> //SRA E
                            CPUInstructions.sra(4);
                    case 0x2C -> //SRA H
                            CPUInstructions.sra(6);
                    case 0x2D -> //SRA L
                            CPUInstructions.sra(7);
                    case 0x2E -> //SRA (HL)
                            CPUInstructions.sra(8);
                    case 0x2F -> //SRA A
                            CPUInstructions.sra(0);
                    case 0x30 -> //SWAP B
                            CPUInstructions.swap(1);
                    case 0x31 -> //SWAP C
                            CPUInstructions.swap(2);
                    case 0x32 -> //SWAP D
                            CPUInstructions.swap(3);
                    case 0x33 -> //SWAP E
                            CPUInstructions.swap(4);
                    case 0x34 -> //SWAP H
                            CPUInstructions.swap(6);
                    case 0x35 -> //SWAP L
                            CPUInstructions.swap(7);
                    case 0x36 -> //SWAP (HL)
                            CPUInstructions.swap(8);
                    case 0x37 -> //SWAP A
                            CPUInstructions.swap(0);
                    case 0x38 -> //SRL B
                            CPUInstructions.srl(1);
                    case 0x39 -> //SRL C
                            CPUInstructions.srl(2);
                    case 0x3A -> //SRL D
                            CPUInstructions.srl(3);
                    case 0x3B -> //SRL E
                            CPUInstructions.srl(4);
                    case 0x3C -> //SRL H
                            CPUInstructions.srl(6);
                    case 0x3D -> //SRL L
                            CPUInstructions.srl(7);
                    case 0x3E -> //SRL (HL)
                            CPUInstructions.srl(8);
                    case 0x3F -> //SRL A
                            CPUInstructions.srl(0);
                    case 0x40 -> //BIT 0,B
                            CPUInstructions.bit(0, 1);
                    case 0x41 -> //BIT 0,C
                            CPUInstructions.bit(0, 2);
                    case 0x42 -> //BIT 0,D
                            CPUInstructions.bit(0, 3);
                    case 0x43 -> //BIT 0,E
                            CPUInstructions.bit(0, 4);
                    case 0x44 -> //BIT 0,H
                            CPUInstructions.bit(0, 6);
                    case 0x45 -> //BIT 0,L
                            CPUInstructions.bit(0, 7);
                    case 0x46 -> //BIT 0,(HL)
                            CPUInstructions.bit(0, 8);
                    case 0x47 -> //BIT 0,A
                            CPUInstructions.bit(0, 0);
                    case 0x48 -> //BIT 1,B
                            CPUInstructions.bit(1, 1);
                    case 0x49 -> //BIT 1,C
                            CPUInstructions.bit(1, 2);
                    case 0x4A -> //BIT 1,D
                            CPUInstructions.bit(1, 3);
                    case 0x4B -> //BIT 1,E
                            CPUInstructions.bit(1, 4);
                    case 0x4C -> //BIT 1,H
                            CPUInstructions.bit(1, 6);
                    case 0x4D -> //BIT 1,L
                            CPUInstructions.bit(1, 7);
                    case 0x4E -> //BIT 1,(HL)
                            CPUInstructions.bit(1, 8);
                    case 0x4F -> //BIT 1,A
                            CPUInstructions.bit(1, 0);
                    case 0x50 -> //BIT 2,B
                            CPUInstructions.bit(2, 1);
                    case 0x51 -> //BIT 2,C
                            CPUInstructions.bit(2, 2);
                    case 0x52 -> //BIT 2,D
                            CPUInstructions.bit(2, 3);
                    case 0x53 -> //BIT 2,E
                            CPUInstructions.bit(2, 4);
                    case 0x54 -> //BIT 2,H
                            CPUInstructions.bit(2, 6);
                    case 0x55 -> //BIT 2,L
                            CPUInstructions.bit(2, 7);
                    case 0x56 -> //BIT 2,(HL)
                            CPUInstructions.bit(2, 8);
                    case 0x57 -> //BIT 2,A
                            CPUInstructions.bit(2, 0);
                    case 0x58 -> //BIT 3,B
                            CPUInstructions.bit(3, 1);
                    case 0x59 -> //BIT 3,C
                            CPUInstructions.bit(3, 2);
                    case 0x5A -> //BIT 3,D
                            CPUInstructions.bit(3, 3);
                    case 0x5B -> //BIT 3,E
                            CPUInstructions.bit(3, 4);
                    case 0x5C -> //BIT 3,H
                            CPUInstructions.bit(3, 6);
                    case 0x5D -> //BIT 3,L
                            CPUInstructions.bit(3, 7);
                    case 0x5E -> //BIT 3,(HL)
                            CPUInstructions.bit(3, 8);
                    case 0x5F -> //BIT 3,A
                            CPUInstructions.bit(3, 0);
                    case 0x60 -> //BIT 4,B
                            CPUInstructions.bit(4, 1);
                    case 0x61 -> //BIT 4,C
                            CPUInstructions.bit(4, 2);
                    case 0x62 -> //BIT 4,D
                            CPUInstructions.bit(4, 3);
                    case 0x63 -> //BIT 4,E
                            CPUInstructions.bit(4, 4);
                    case 0x64 -> //BIT 4,H
                            CPUInstructions.bit(4, 6);
                    case 0x65 -> //BIT 4,L
                            CPUInstructions.bit(4, 7);
                    case 0x66 -> //BIT 4,(HL)
                            CPUInstructions.bit(4, 8);
                    case 0x67 -> //BIT 4,A
                            CPUInstructions.bit(4, 0);
                    case 0x68 -> //BIT 5,B
                            CPUInstructions.bit(5, 1);
                    case 0x69 -> //BIT 5,C
                            CPUInstructions.bit(5, 2);
                    case 0x6A -> //BIT 5,D
                            CPUInstructions.bit(5, 3);
                    case 0x6B -> //BIT 5,E
                            CPUInstructions.bit(5, 4);
                    case 0x6C -> //BIT 5,H
                            CPUInstructions.bit(5, 6);
                    case 0x6D -> //BIT 5,L
                            CPUInstructions.bit(5, 7);
                    case 0x6E -> //BIT 5,(HL)
                            CPUInstructions.bit(5, 8);
                    case 0x6F -> //BIT 5,A
                            CPUInstructions.bit(5, 0);
                    case 0x70 -> //BIT 6,B
                            CPUInstructions.bit(6, 1);
                    case 0x71 -> //BIT 6,C
                            CPUInstructions.bit(6, 2);
                    case 0x72 -> //BIT 6,D
                            CPUInstructions.bit(6, 3);
                    case 0x73 -> //BIT 6,E
                            CPUInstructions.bit(6, 4);
                    case 0x74 -> //BIT 6,H
                            CPUInstructions.bit(6, 6);
                    case 0x75 -> //BIT 6,L
                            CPUInstructions.bit(6, 7);
                    case 0x76 -> //BIT 6,(HL)
                            CPUInstructions.bit(6, 8);
                    case 0x77 -> //BIT 6,A
                            CPUInstructions.bit(6, 0);
                    case 0x78 -> //BIT 7,B
                            CPUInstructions.bit(7, 1);
                    case 0x79 -> //BIT 7,C
                            CPUInstructions.bit(7, 2);
                    case 0x7A -> //BIT 7,D
                            CPUInstructions.bit(7, 3);
                    case 0x7B -> //BIT 7,E
                            CPUInstructions.bit(7, 4);
                    case 0x7C -> //BIT 7,H
                            CPUInstructions.bit(7, 6);
                    case 0x7D -> //BIT 7,L
                            CPUInstructions.bit(7, 7);
                    case 0x7E -> //BIT 7, (HL)
                            CPUInstructions.bit(7, 8);
                    case 0x7F -> //BIT 7,A
                            CPUInstructions.bit(7, 0);
                    case 0x80 -> //RES 0,B
                            CPUInstructions.res(0, 1);
                    case 0x81 -> //RES 0,C
                            CPUInstructions.res(0, 2);
                    case 0x82 -> //RES 0,D
                            CPUInstructions.res(0, 3);
                    case 0x83 -> //RES 0,E
                            CPUInstructions.res(0, 4);
                    case 0x84 -> //RES 0,H
                            CPUInstructions.res(0, 6);
                    case 0x85 -> //RES 0,L
                            CPUInstructions.res(0, 7);
                    case 0x86 -> //RES 0,(HL)
                            CPUInstructions.res(0, 8);
                    case 0x87 -> //RES 0,A
                            CPUInstructions.res(0, 0);
                    case 0x88 -> //RES 1,B
                            CPUInstructions.res(1, 1);
                    case 0x89 -> //RES 1,C
                            CPUInstructions.res(1, 2);
                    case 0x8A -> //RES 1,D
                            CPUInstructions.res(1, 3);
                    case 0x8B -> //RES 1,E
                            CPUInstructions.res(1, 4);
                    case 0x8C -> //RES 1,H
                            CPUInstructions.res(1, 6);
                    case 0x8D -> //RES 1,L
                            CPUInstructions.res(1, 7);
                    case 0x8E -> //RES 1,(HL)
                            CPUInstructions.res(1, 8);
                    case 0x8F -> //RES 1,A
                            CPUInstructions.res(1, 0);
                    case 0x90 -> //RES 2,B
                            CPUInstructions.res(2, 1);
                    case 0x91 -> //RES 2,C
                            CPUInstructions.res(2, 2);
                    case 0x92 -> //RES 2,D
                            CPUInstructions.res(2, 3);
                    case 0x93 -> //RES 2,E
                            CPUInstructions.res(2, 4);
                    case 0x94 -> //RES 2,H
                            CPUInstructions.res(2, 6);
                    case 0x95 -> //RES 2,L
                            CPUInstructions.res(2, 7);
                    case 0x96 -> //RES 2,(HL)
                            CPUInstructions.res(2, 8);
                    case 0x97 -> //RES 2,A
                            CPUInstructions.res(2, 0);
                    case 0x98 -> //RES 3,B
                            CPUInstructions.res(3, 1);
                    case 0x99 -> //RES 3,C
                            CPUInstructions.res(3, 2);
                    case 0x9A -> //RES 3,D
                            CPUInstructions.res(3, 3);
                    case 0x9B -> //RES 3,E
                            CPUInstructions.res(3, 4);
                    case 0x9C -> //RES 3,H
                            CPUInstructions.res(3, 6);
                    case 0x9D -> //RES 3,L
                            CPUInstructions.res(3, 7);
                    case 0x9E -> //RES 3,(HL)
                            CPUInstructions.res(3, 8);
                    case 0x9F -> //RES 3,A
                            CPUInstructions.res(3, 0);
                    case 0xA0 -> //RES 4,B
                            CPUInstructions.res(4, 1);
                    case 0xA1 -> //RES 4,C
                            CPUInstructions.res(4, 2);
                    case 0xA2 -> //RES 4,D
                            CPUInstructions.res(4, 3);
                    case 0xA3 -> //RES 4,E
                            CPUInstructions.res(4, 4);
                    case 0xA4 -> //RES 4,H
                            CPUInstructions.res(4, 6);
                    case 0xA5 -> //RES 4,L
                            CPUInstructions.res(4, 7);
                    case 0xA6 -> //RES 4,(HL)
                            CPUInstructions.res(4, 8);
                    case 0xA7 -> //RES 4,A
                            CPUInstructions.res(4, 0);
                    case 0xA8 -> //RES 5,B
                            CPUInstructions.res(5, 1);
                    case 0xA9 -> //RES 5,C
                            CPUInstructions.res(5, 2);
                    case 0xAA -> //RES 5,D
                            CPUInstructions.res(5, 3);
                    case 0xAB -> //RES 5,E
                            CPUInstructions.res(5, 4);
                    case 0xAC -> //RES 5,H
                            CPUInstructions.res(5, 6);
                    case 0xAD -> //RES 5,L
                            CPUInstructions.res(5, 7);
                    case 0xAE -> //RES 5,(HL)
                            CPUInstructions.res(5, 8);
                    case 0xAF -> //RES 5,A
                            CPUInstructions.res(5, 0);
                    case 0xB0 -> //RES 6,B
                            CPUInstructions.res(6, 1);
                    case 0xB1 -> //RES 6,C
                            CPUInstructions.res(6, 2);
                    case 0xB2 -> //RES 6,D
                            CPUInstructions.res(6, 3);
                    case 0xB3 -> //RES 6,E
                            CPUInstructions.res(6, 4);
                    case 0xB4 -> //RES 6,H
                            CPUInstructions.res(6, 6);
                    case 0xB5 -> //RES 6,L
                            CPUInstructions.res(6, 7);
                    case 0xB6 -> //RES 6,(HL)
                            CPUInstructions.res(6, 8);
                    case 0xB7 -> //RES 6,A
                            CPUInstructions.res(6, 0);
                    case 0xB8 -> //RES 7,B
                            CPUInstructions.res(7, 1);
                    case 0xB9 -> //RES 7,C
                            CPUInstructions.res(7, 2);
                    case 0xBA -> //RES 7,D
                            CPUInstructions.res(7, 3);
                    case 0xBB -> //RES 7,E
                            CPUInstructions.res(7, 4);
                    case 0xBC -> //RES 7,H
                            CPUInstructions.res(7, 6);
                    case 0xBD -> //RES 7,L
                            CPUInstructions.res(7, 7);
                    case 0xBE -> //RES 7,(HL)
                            CPUInstructions.res(7, 8);
                    case 0xBF -> //RES 7,A
                            CPUInstructions.res(7, 0);
                    case 0xC0 -> //SET 0,B
                            CPUInstructions.set(0, 1);
                    case 0xC1 -> //SET 0,C
                            CPUInstructions.set(0, 2);
                    case 0xC2 -> //SET 0,D
                            CPUInstructions.set(0, 3);
                    case 0xC3 -> //SET 0,E
                            CPUInstructions.set(0, 4);
                    case 0xC4 -> //SET 0,H
                            CPUInstructions.set(0, 6);
                    case 0xC5 -> //SET 0,L
                            CPUInstructions.set(0, 7);
                    case 0xC6 -> //SET 0,(HL)
                            CPUInstructions.set(0, 8);
                    case 0xC7 -> //SET 0,A
                            CPUInstructions.set(0, 0);
                    case 0xC8 -> //SET 1,B
                            CPUInstructions.set(1, 1);
                    case 0xC9 -> //SET 1,C
                            CPUInstructions.set(1, 2);
                    case 0xCA -> //SET 1,D
                            CPUInstructions.set(1, 3);
                    case 0xCB -> //SET 1,E
                            CPUInstructions.set(1, 4);
                    case 0xCC -> //SET 1,H
                            CPUInstructions.set(1, 6);
                    case 0xCD -> //SET 1,L
                            CPUInstructions.set(1, 7);
                    case 0xCE -> //SET 1,(HL)
                            CPUInstructions.set(1, 8);
                    case 0xCF -> //SET 1,A
                            CPUInstructions.set(1, 0);
                    case 0xD0 -> //SET 2,B
                            CPUInstructions.set(2, 1);
                    case 0xD1 -> //SET 2,C
                            CPUInstructions.set(2, 2);
                    case 0xD2 -> //SET 2,D
                            CPUInstructions.set(2, 3);
                    case 0xD3 -> //SET 2,E
                            CPUInstructions.set(2, 4);
                    case 0xD4 -> //SET 2,H
                            CPUInstructions.set(2, 6);
                    case 0xD5 -> //SET 2,L
                            CPUInstructions.set(2, 7);
                    case 0xD6 -> //SET 2,(HL)
                            CPUInstructions.set(2, 8);
                    case 0xD7 -> //SET 2,A
                            CPUInstructions.set(2, 0);
                    case 0xD8 -> //SET 3,B
                            CPUInstructions.set(3, 1);
                    case 0xD9 -> //SET 3,C
                            CPUInstructions.set(3, 2);
                    case 0xDA -> //SET 3,D
                            CPUInstructions.set(3, 3);
                    case 0xDB -> //SET 3,E
                            CPUInstructions.set(3, 4);
                    case 0xDC -> //SET 3,H
                            CPUInstructions.set(3, 6);
                    case 0xDD -> //SET 3,L
                            CPUInstructions.set(3, 7);
                    case 0xDE -> //SET 3,(HL)
                            CPUInstructions.set(3, 8);
                    case 0xDF -> //SET 3,A
                            CPUInstructions.set(3, 0);
                    case 0xE0 -> //SET 4,B
                            CPUInstructions.set(4, 1);
                    case 0xE1 -> //SET 4,C
                            CPUInstructions.set(4, 2);
                    case 0xE2 -> //SET 4,D
                            CPUInstructions.set(4, 3);
                    case 0xE3 -> //SET 4,E
                            CPUInstructions.set(4, 4);
                    case 0xE4 -> //SET 4,H
                            CPUInstructions.set(4, 6);
                    case 0xE5 -> //SET 4,L
                            CPUInstructions.set(4, 7);
                    case 0xE6 -> //SET 4,(HL)
                            CPUInstructions.set(4, 8);
                    case 0xE7 -> //SET 4,A
                            CPUInstructions.set(4, 0);
                    case 0xE8 -> //SET 5,B
                            CPUInstructions.set(5, 1);
                    case 0xE9 -> //SET 5,C
                            CPUInstructions.set(5, 2);
                    case 0xEA -> //SET 5,D
                            CPUInstructions.set(5, 3);
                    case 0xEB -> //SET 5,E
                            CPUInstructions.set(5, 4);
                    case 0xEC -> //SET 5,H
                            CPUInstructions.set(5, 6);
                    case 0xED -> //SET 5,L
                            CPUInstructions.set(5, 7);
                    case 0xEE -> //SET 5,(HL)
                            CPUInstructions.set(5, 8);
                    case 0xEF -> //SET 5,A
                            CPUInstructions.set(5, 0);
                    case 0xF0 -> //SET 6,B
                            CPUInstructions.set(6, 1);
                    case 0xF1 -> //SET 6,C
                            CPUInstructions.set(6, 2);
                    case 0xF2 -> //SET 6,D
                            CPUInstructions.set(6, 3);
                    case 0xF3 -> //SET 6,E
                            CPUInstructions.set(6, 4);
                    case 0xF4 -> //SET 6,H
                            CPUInstructions.set(6, 6);
                    case 0xF5 -> //SET 6,L
                            CPUInstructions.set(6, 7);
                    case 0xF6 -> //SET 6,(HL)
                            CPUInstructions.set(6, 8);
                    case 0xF7 -> //SET 6,A
                            CPUInstructions.set(6, 0);
                    case 0xF8 -> //SET 7,B
                            CPUInstructions.set(7, 1);
                    case 0xF9 -> //SET 7,C
                            CPUInstructions.set(7, 2);
                    case 0xFA -> //SET 7,D
                            CPUInstructions.set(7, 3);
                    case 0xFB -> //SET 7,E
                            CPUInstructions.set(7, 4);
                    case 0xFC -> //SET 7,H
                            CPUInstructions.set(7, 6);
                    case 0xFD -> //SET 7,L
                            CPUInstructions.set(7, 7);
                    case 0xFE -> //SET 7,(HL)
                            CPUInstructions.set(7, 8);
                    case 0xFF -> //SET 7,A
                            CPUInstructions.set(7, 0);
                }
            }
            case 0xCC -> //CALL Z,nn
                    CPUInstructions.callCond(1);
            case 0xCD -> //CALL u16
                    CPUInstructions.call();
            case 0xCE -> //ADC A,#
                    CPUInstructions.adc(9);
            case 0xCF -> //RST 08H
                    CPUInstructions.rst(1);
            case 0xD0 -> //RET NC
                    CPUInstructions.retCond(2);
            case 0xD1 -> //POP DE
                    CPUInstructions.pop(2);
            case 0xD2 -> //JP NC,u16
                    CPUInstructions.jpCond(2);
            case 0xD4 -> //CALL NC,nn
                    CPUInstructions.callCond(2);
            case 0xD5 -> //PUSH DE
                    CPUInstructions.push(2);
            case 0xD6 -> //SUB A, #
                    CPUInstructions.sub(9);
            case 0xD7 -> //RST 10H
                    CPUInstructions.rst(2);
            case 0xD8 -> //RET C
                    CPUInstructions.retCond(3);
            case 0xD9 -> //RETI
                    CPUInstructions.reti();
            case 0xDA -> //JP C,u16
                    CPUInstructions.jpCond(3);
            case 0xDC -> //CALL C,nn
                    CPUInstructions.callCond(3);
            case 0xDE -> //SBC A,#
                    CPUInstructions.sbc(9);
            case 0xDF -> //RST 18H
                    CPUInstructions.rst(3);
            case 0xE0 -> //LD (FF00+u8),A
                    CPUInstructions.ldh(0);
            case 0xE1 -> //POP nn
                    CPUInstructions.pop(3);
            case 0xE2 -> //LD (C), A
                    CPUInstructions.ldAC(1);
            case 0xE5 -> //PUSH HL
                    CPUInstructions.push(3);
            case 0xE6 -> //AND #
                    CPUInstructions.and(9);
            case 0xE7 -> //RST 20H
                    CPUInstructions.rst(4);
            case 0xE8 -> //ADD SP,n
                    CPUInstructions.addSP();
            case 0xE9 -> //JP (HL)
                    CPUInstructions.jpHL();
            case 0xEA -> //LD (nn),A
                    CPUInstructions.ldTwoRegisters(2);
            case 0xEE -> //XOR #
                    CPUInstructions.xor(9);
            case 0xEF -> //RST 28H
                    CPUInstructions.rst(5);
            case 0xF0 -> //LD A,(FF00+u8)
                    CPUInstructions.ldh(1);
            case 0xF1 -> //POP AF
                    CPUInstructions.pop(0);
            case 0xF2 -> //LD A,(C)
                    CPUInstructions.ldAC(0);
            case 0xF3 -> //DI
                    CPUInstructions.di();
            case 0xF5 -> //PUSH AF
                    CPUInstructions.push(0);
            case 0xF6 -> //OR #
                    CPUInstructions.or(9);
            case 0xF7 -> //RST 30H
                    CPUInstructions.rst(6);
            case 0xF8 -> //LDHL SP,n
                    CPUInstructions.LDHL();
            case 0xF9 -> //LD SP,HL
                    CPUInstructions.ldSPHL();
            case 0xFA -> //LD A,(nn)
                    CPUInstructions.ldTwoRegistersIntoA(2);
            case 0xFB -> //EI
                    CPUInstructions.ei();
            case 0xFE -> //CP A,u8
                    CPUInstructions.cp(9);
            case 0xFF -> //RST 38H
                    CPUInstructions.rst(7);
            default -> {
                System.out.println("No OPCode or Lacks Implementation");
                System.exit(0);
            }
        }
    }
}
