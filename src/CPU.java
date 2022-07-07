import java.io.FileNotFoundException;
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

    private final boolean debugText = false;
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

    public CPU() throws FileNotFoundException {
        clearRegisters();
        memory = new Memory(this);
        ppu = new PPU(memory, this);
        displayFrame = new DisplayFrame(memory, ppu, this);
        memory.setDisplayFrame(displayFrame);

        if(debugText) {
            debug = new PrintStream("A.txt");
            PrintStream console = System.out;
            System.setOut(debug);
        }

        CPUInstructions.setCpu(this);
        CPUInstructions.setMem(memory);

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
                if(isHalted) {
                    setIsHalted(false);
                    programCounter++;
                }

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
        else if(isHalted)
            if((memory.getMemory(IF) & memory.getMemory(IE) & 0x1f) > 0) {
                isHalted = false;
                if(haltCounter == counter) haltBug = true;
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
        //System.out.println(operationCode);

//        System.out.println(counter);

//        if(counter >= 164200) {
//            memory.dumpMemory();
//            System.exit(0);
//        }

//            CPUInstructions.dumpRegisters();
//            CPUInstructions.show();
//        }
//
//        if(counter >= 366125) {
//            System.exit(1);
//        }
//        if(debugText) System.setOut(debug);

        switch (operationCode) {
            case 0x00 -> CPUInstructions.nop();  //NOP
            case 0x01 -> CPUInstructions.ld16bit(0);  //LD BC,u16
            case 0x02 -> CPUInstructions.ldTwoRegisters(0);  //LD (BC),A
            case 0x03 -> CPUInstructions.incR(0);                  //INC BC
            case 0x04 -> CPUInstructions.inc(1);                   //INC B
            case 0x05 -> CPUInstructions.dec(1);                   //DEC B
            case 0x06 -> CPUInstructions.ld(1, 9);  //LD B,u8
            case 0x07 -> CPUInstructions.rlca();                             //RLCA
            case 0x08 -> CPUInstructions.LDnnSP();                           //LD (u16),SP
            case 0x09 -> CPUInstructions.addHL(0);                 //ADD HL,BC
            case 0x0A -> CPUInstructions.ldTwoRegistersIntoA(0);             //LD A,(BC)
            case 0x0B -> CPUInstructions.decR(0);                  //DEC BC
            case 0x0C -> CPUInstructions.inc(2);                   //INC C
            case 0x0D -> CPUInstructions.dec(2);                   //DEC C
            case 0x0E -> CPUInstructions.ld(2, 9);  //LD C,u8
            case 0x0F -> CPUInstructions.rrca();                             //RRCA
            case 0x10 -> CPUInstructions.stop();                             //STOP
            case 0x11 -> CPUInstructions.ld16bit(1);                  //LD DE,u16
            case 0x12 -> CPUInstructions.ldTwoRegisters(1);                  //LD (DE),A
            case 0x13 -> CPUInstructions.incR(1);                  //INC DE
            case 0x14 -> CPUInstructions.inc(3);                   //INC D
            case 0x15 -> CPUInstructions.dec(3);                   //DEC D
            case 0x16 -> CPUInstructions.ld(3, 9);  //LD D,u8
            case 0x17 -> CPUInstructions.rla();                              //RLA
            case 0x18 -> CPUInstructions.jr();                               //JR i8
            case 0x19 -> CPUInstructions.addHL(1);                 //ADD HL,DE
            case 0x1A -> CPUInstructions.ldTwoRegistersIntoA(1);             //LD A,(DE)
            case 0x1B -> CPUInstructions.decR(1);                  //DEC DE
            case 0x1C -> CPUInstructions.inc(4);                   //INC E
            case 0x1D -> CPUInstructions.dec(4);                   //DEC E
            case 0x1E -> CPUInstructions.ld(4, 9);  //LD E,u8
            case 0x1F -> CPUInstructions.rra();                              //RRA
            case 0x20 -> CPUInstructions.jrCond(0);                   //JR NZ,i8
            case 0x21 -> CPUInstructions.ld16bit(2);                  //LD HL,u16
            case 0x22 -> CPUInstructions.ldi(1);                      //LDI (HL),A
            case 0x23 -> CPUInstructions.incR(2);                  //INC HL
            case 0x24 -> CPUInstructions.inc(6);                   //INC H
            case 0x25 -> CPUInstructions.dec(6);                   //DEC H
            case 0x26 -> CPUInstructions.ld(6, 9);  //LD H,u8
            case 0x27 -> CPUInstructions.daa();                              //DAA
            case 0x28 -> CPUInstructions.jrCond(1);                   //JR Z,u8
            case 0x29 -> CPUInstructions.addHL(2);                //ADD HL, HL
            case 0x2A -> CPUInstructions.ldi(0);                      //LDI A,(HL)
            case 0x2B -> CPUInstructions.decR(2);                  //DEC HL
            case 0x2C -> CPUInstructions.inc(7);                   //INC L
            case 0x2D -> CPUInstructions.dec(7);                   //DEC L
            case 0x2E -> CPUInstructions.ld(7, 9);  //LD L,u8
            case 0x2F -> CPUInstructions.cpl();                              //CPL
            case 0x30 -> CPUInstructions.jrCond(2);                    //JR NC,u8
            case 0x31 -> CPUInstructions.ld16bit(3);                   //LD SP,u16
            case 0x32 -> CPUInstructions.ldd(1);                       //LDD (HL),A
            case 0x33 -> CPUInstructions.incR(3);                  //INC SP
            case 0x34 -> CPUInstructions.inc(8);                   //INC (HL)
            case 0x35 -> CPUInstructions.dec(8);                   //INC (HL)
            case 0x36 -> CPUInstructions.ld(8, 9);  //LD (HL), n
            case 0x37 -> CPUInstructions.scf();                              //SCF
            case 0x38 -> CPUInstructions.jrCond(3);                    //JR C,u8
            case 0x39 -> CPUInstructions.addHL(3);                 //ADD HL,SP
            case 0x3A -> CPUInstructions.ldd(0);                       //LDD A,(HL)
            case 0x3B -> CPUInstructions.decR(3);                  //DEC SP
            case 0x3C -> CPUInstructions.inc(0);                   //INC A
            case 0x3D -> CPUInstructions.dec(0);                   //DEC A
            case 0x3E -> CPUInstructions.ldTwoRegistersIntoA(3);             //LD A,u8
            case 0x3F -> CPUInstructions.ccf();                              //CCF
            case 0x40 -> CPUInstructions.ld(1, 1);  //LD B,B
            case 0x41 -> CPUInstructions.ld(1, 2);  //LD B,C
            case 0x42 -> CPUInstructions.ld(1, 3);  //LD B,D
            case 0x43 -> CPUInstructions.ld(1, 4);  //LD B,E
            case 0x44 -> CPUInstructions.ld(1, 6);  //LD B,H
            case 0x45 -> CPUInstructions.ld(1, 7);  //LD B,L
            case 0x46 -> CPUInstructions.ld(1, 8);  //LD B,(HL)
            case 0x47 -> CPUInstructions.ld(1, 0);  //LD B,A
            case 0x48 -> CPUInstructions.ld(2, 1);  //LD C,B
            case 0x49 -> CPUInstructions.ld(2, 2);  //LD C,C
            case 0x4A -> CPUInstructions.ld(2, 3);  //LD C,D
            case 0x4B -> CPUInstructions.ld(2, 4);  //LD C,E
            case 0x4C -> CPUInstructions.ld(2, 6);  //LD C,H
            case 0x4D -> CPUInstructions.ld(2, 7);  //LD C,L
            case 0x4E -> CPUInstructions.ld(2, 8);  //LD C,(HL)
            case 0x4F -> CPUInstructions.ld(2, 0);  //LD C,A
            case 0x50 -> CPUInstructions.ld(3, 1);  //LD D,B
            case 0x51 -> CPUInstructions.ld(3, 2);  //LD D,C
            case 0x52 -> CPUInstructions.ld(3, 3);  //LD D,D
            case 0x53 -> CPUInstructions.ld(3, 4);  //LD D,E
            case 0x54 -> CPUInstructions.ld(3, 6);  //LD D,H
            case 0x55 -> CPUInstructions.ld(3, 7);  //LD D,L
            case 0x56 -> CPUInstructions.ld(3, 8);  //LD D,(HL)
            case 0x57 -> CPUInstructions.ld(3, 0);  //LD D,A
            case 0x58 -> CPUInstructions.ld(4, 1);  //LD E,B
            case 0x59 -> CPUInstructions.ld(4, 2);  //LD E,C
            case 0x5A -> CPUInstructions.ld(4, 3);  //LD E,D
            case 0x5B -> CPUInstructions.ld(4, 4);  //LD E,E
            case 0x5C -> CPUInstructions.ld(4, 6);  //LD E,H
            case 0x5D -> CPUInstructions.ld(4, 7);  //LD E,L
            case 0x5E -> CPUInstructions.ld(4, 8);  //LD E,(HL)
            case 0x5F -> CPUInstructions.ld(4, 0);  //LD E,A
            case 0x60 -> CPUInstructions.ld(6, 1);  //LD H,B
            case 0x61 -> CPUInstructions.ld(6, 2);  //LD H,C
            case 0x62 -> CPUInstructions.ld(6, 3);  //LD H,D
            case 0x63 -> CPUInstructions.ld(6, 4);  //LD H,E
            case 0x64 -> CPUInstructions.ld(6, 6);  //LD H,H
            case 0x65 -> CPUInstructions.ld(6, 7);  //LD H,L
            case 0x66 -> CPUInstructions.ld(6, 8);  //LD H,(HL)
            case 0x67 -> CPUInstructions.ld(6, 0);  //LD H,A
            case 0x68 -> CPUInstructions.ld(7, 1);  //LD L,B
            case 0x69 -> CPUInstructions.ld(7, 2);  //LD L,C
            case 0x6A -> CPUInstructions.ld(7, 3);  //LD L,D
            case 0x6B -> CPUInstructions.ld(7, 4);  //LD L,E
            case 0x6C -> CPUInstructions.ld(7, 6);  //LD L,H
            case 0x6D -> CPUInstructions.ld(7, 7);  //LD L,L
            case 0x6E -> CPUInstructions.ld(7, 8);  //LD L,(HL)
            case 0x6F -> CPUInstructions.ld(7, 0);  //LD L,A
            case 0x70 -> CPUInstructions.ld(8, 1);  //LD (HL),B
            case 0x71 -> CPUInstructions.ld(8, 2);  //LD (HL),C
            case 0x72 -> CPUInstructions.ld(8, 3);  //LD (HL),D
            case 0x73 -> CPUInstructions.ld(8, 4);  //LD (HL),E
            case 0x74 -> CPUInstructions.ld(8, 6);  //LD (HL),H
            case 0x75 -> CPUInstructions.ld(8, 7);  //LD (HL),L
            case 0x76 -> CPUInstructions.halt();                             //HALT
            case 0x77 -> CPUInstructions.ld(8, 0);  //LD (HL),A
            case 0x78 -> CPUInstructions.ld(0, 1);  //LD A,B
            case 0x79 -> CPUInstructions.ld(0, 2);  //LD A,C
            case 0x7A -> CPUInstructions.ld(0, 3);  //LD A,D
            case 0x7B -> CPUInstructions.ld(0, 4);  //LD A,E
            case 0x7C -> CPUInstructions.ld(0, 6);  //LD A,H
            case 0x7D -> CPUInstructions.ld(0, 7);  //LD A,L
            case 0x7E -> CPUInstructions.ld(0, 8);  //LD A,(HL)
            case 0x7F -> CPUInstructions.ld(0, 0);  //LD A,A
            case 0x80 -> CPUInstructions.add(1);                             //ADD A,B
            case 0x81 -> CPUInstructions.add(2);                             //ADD A,C
            case 0x82 -> CPUInstructions.add(3);                             //ADD A,D
            case 0x83 -> CPUInstructions.add(4);                             //ADD A,E
            case 0x84 -> CPUInstructions.add(6);                             //ADD A,H
            case 0x85 -> CPUInstructions.add(7);                             //ADD A,L
            case 0x86 -> CPUInstructions.add(8);                             //ADD A,(HL)
            case 0x87 -> CPUInstructions.add(0);                             //ADD A,A
            case 0x88 -> CPUInstructions.adc(1);                   //ADC A,B
            case 0x89 -> CPUInstructions.adc(2);                   //ADC A,C
            case 0x8A -> CPUInstructions.adc(3);                   //ADC A,D
            case 0x8B -> CPUInstructions.adc(4);                   //ADC A,E
            case 0x8C -> CPUInstructions.adc(6);                   //ADC A,H
            case 0x8D -> CPUInstructions.adc(7);                   //ADC A,L
            case 0x8E -> CPUInstructions.adc(8);                   //ADC A,(HL)
            case 0x8F -> CPUInstructions.adc(0);                   //ADC A,A
            case 0x90 -> CPUInstructions.sub(1);                   //SUB A,B
            case 0x91 -> CPUInstructions.sub(2);                   //SUB A,C
            case 0x92 -> CPUInstructions.sub(3);                   //SUB A,D
            case 0x93 -> CPUInstructions.sub(4);                   //SUB A,E
            case 0x94 -> CPUInstructions.sub(6);                   //SUB A,H
            case 0x95 -> CPUInstructions.sub(7);                   //SUB A,L
            case 0x96 -> CPUInstructions.sub(8);                   //SUB A, (HL)
            case 0x97 -> CPUInstructions.sub(0);                   //SUB A,A
            case 0x98 -> CPUInstructions.sbc(1);                   //SBC A,B
            case 0x99 -> CPUInstructions.sbc(2);                   //SBC A,C
            case 0x9A -> CPUInstructions.sbc(3);                   //SBC A,D
            case 0x9B -> CPUInstructions.sbc(4);                   //SBC A,E
            case 0x9C -> CPUInstructions.sbc(6);                   //SBC A,H
            case 0x9D -> CPUInstructions.sbc(7);                   //SBC A,L
            case 0x9E -> CPUInstructions.sbc(8);                   //SBC A, (HL)
            case 0x9F -> CPUInstructions.sbc(0);                   //SBC A,A
            case 0xA0 -> CPUInstructions.and(1);                   //AND A,B
            case 0xA1 -> CPUInstructions.and(2);                   //AND A,C
            case 0xA2 -> CPUInstructions.and(3);                   //AND A,D
            case 0xA3 -> CPUInstructions.and(4);                   //AND A,E
            case 0xA4 -> CPUInstructions.and(6);                   //AND A,H
            case 0xA5 -> CPUInstructions.and(7);                   //AND A,L
            case 0xA6 -> CPUInstructions.and(8);                   //AND A,(HL)
            case 0xA7 -> CPUInstructions.and(0);                   //AND A,A
            case 0xA8 -> CPUInstructions.xor(1);                   //XOR A,B
            case 0xA9 -> CPUInstructions.xor(2);                   //XOR A,C
            case 0xAA -> CPUInstructions.xor(3);                   //XOR A,D
            case 0xAB -> CPUInstructions.xor(4);                   //XOR A,E
            case 0xAC -> CPUInstructions.xor(6);                   //XOR A,H
            case 0xAD -> CPUInstructions.xor(7);                   //XOR A,L
            case 0xAE -> CPUInstructions.xor(8);                   //XOR A,(HL)
            case 0xAF -> CPUInstructions.xor(0);                   //XOR A,A
            case 0xB0 -> CPUInstructions.or(1);                    //OR A,B
            case 0xB1 -> CPUInstructions.or(2);                    //OR A,C
            case 0xB2 -> CPUInstructions.or(3);                    //OR A,D
            case 0xB3 -> CPUInstructions.or(4);                    //OR A,E
            case 0xB4 -> CPUInstructions.or(6);                    //OR A,H
            case 0xB5 -> CPUInstructions.or(7);                    //OR A,L
            case 0xB6 -> CPUInstructions.or(8);                    //OR A,(HL)
            case 0xB7 -> CPUInstructions.or(0);                    //OR A,A
            case 0xB8 -> CPUInstructions.cp(1);                    //CP A,B
            case 0xB9 -> CPUInstructions.cp(2);                    //CP A,C
            case 0xBA -> CPUInstructions.cp(3);                    //CP A,D
            case 0xBB -> CPUInstructions.cp(4);                    //CP A,E
            case 0xBC -> CPUInstructions.cp(6);                    //CP A,H
            case 0xBD -> CPUInstructions.cp(7);                    //CP A,L
            case 0xBE -> CPUInstructions.cp(8);                    //CP A,(HL)
            case 0xBF -> CPUInstructions.cp(0);                    //CP A,A
            case 0xC0 -> CPUInstructions.retCond(0);                   //RET NZ
            case 0xC1 -> CPUInstructions.pop(1);                   //POP BC
            case 0xC2 -> CPUInstructions.jpCond(0);                    //JP NZ,u16
            case 0xC3 -> CPUInstructions.jp();                               //JP u16
            case 0xC4 -> CPUInstructions.callCond(0);                 //CALL NZ, nn
            case 0xC5 -> CPUInstructions.push(1);                 //PUSH BC
            case 0xC6 -> CPUInstructions.add(9);                             //ADD A,#
            case 0xC7 -> CPUInstructions.rst(0);                      //RST 00H
            case 0xC8 -> CPUInstructions.retCond(1);                  //RET Z
            case 0xC9 -> CPUInstructions.ret();                              //RET
            case 0xCA -> CPUInstructions.jpCond(1);                   //JP Z,u16
            case 0xCB -> {
                CPUInstructions.cb();
                fetchOperationCodes();
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
            }                                               //CB PREFIX
            case 0xCC -> CPUInstructions.callCond(1);                 //CALL Z,nn
            case 0xCD -> CPUInstructions.call();                             //CALL u16
            case 0xCE -> CPUInstructions.adc(9);                   //ADC A,#
            case 0xCF -> CPUInstructions.rst(1);                       //RST 08H
            case 0xD0 -> CPUInstructions.retCond(2);                   //RET NC
            case 0xD1 -> CPUInstructions.pop(2);                   //POP DE
            case 0xD2 -> CPUInstructions.jpCond(2);                   //JP NC,u16
            case 0xD4 -> CPUInstructions.callCond(2);                 //CALL NC,nn
            case 0xD5 -> CPUInstructions.push(2);                  //PUSH DE
            case 0xD6 -> CPUInstructions.sub(9);                   //SUB A, #
            case 0xD7 -> CPUInstructions.rst(2);                       //RST 10H
            case 0xD8 -> CPUInstructions.retCond(3);                   //RET C
            case 0xD9 -> CPUInstructions.reti();                              //RETI
            case 0xDA -> CPUInstructions.jpCond(3);                    //JP C,u16
            case 0xDC -> CPUInstructions.callCond(3);                  //CALL C,nn
            case 0xDE -> CPUInstructions.sbc(9);                   //SBC A,#
            case 0xDF -> CPUInstructions.rst(3);                       //RST 18H
            case 0xE0 -> CPUInstructions.ldh(0);                       //LD (FF00+u8),A
            case 0xE1 -> CPUInstructions.pop(3);                   //POP nn
            case 0xE2 -> CPUInstructions.ldAC(1);                      //LD (C), A
            case 0xE5 -> CPUInstructions.push(3);                  //PUSH HL
            case 0xE6 -> CPUInstructions.and(9);                   //AND #
            case 0xE7 -> CPUInstructions.rst(4);                       //RST 20H
            case 0xE8 -> CPUInstructions.addSP();                            //ADD SP,n
            case 0xE9 -> CPUInstructions.jpHL();                             //JP (HL)
            case 0xEA -> CPUInstructions.ldTwoRegisters(2);                  //LD (nn),A
            case 0xEE -> CPUInstructions.xor(9);                   //XOR #
            case 0xEF -> CPUInstructions.rst(5);                      //RST 28H
            case 0xF0 -> CPUInstructions.ldh(1);                      //LD A,(FF00+u8)
            case 0xF1 -> CPUInstructions.pop(0);                   //POP AF
            case 0xF2 -> CPUInstructions.ldAC(0);                      //LD A,(C)
            case 0xF3 -> CPUInstructions.di();                               //DI
            case 0xF5 -> CPUInstructions.push(0);                 //PUSH AF
            case 0xF6 -> CPUInstructions.or(9);                    //OR #
            case 0xF7 -> CPUInstructions.rst(6);                      //RST 30H
            case 0xF8 -> CPUInstructions.LDHL();                             //LDHL SP,n
            case 0xF9 -> CPUInstructions.ldSPHL();                           //LD SP,HL
            case 0xFA -> CPUInstructions.ldTwoRegistersIntoA(2);             //LD A,(nn)
            case 0xFB -> CPUInstructions.ei();                               //EI
            case 0xFE -> CPUInstructions.cp(9);                   //CP A,u8
            case 0xFF -> CPUInstructions.rst(7);                      //RST 38H
            default -> {
                System.err.println("No OPCode or Lacks Implementation");
                System.exit(0);
            }
        }
    }
}
