package com.github.rodrigotimoteo.kboyemucore.cpu

import com.github.rodrigotimoteo.kboyemucore.DisplayFrame
import com.github.rodrigotimoteo.kboyemucore.memory.Memory
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU
import java.io.IOException
import java.io.PrintStream
import java.util.Arrays

class CPU {
    private val TIMER_INTERRUPT = 2

    var registers: CharArray = CharArray(8) //AF, BC, DE and HL can be 16 bits if paired together
    private var zeroFlag = false
    private var subtractFlag = false
    private var halfCarryFlag = false
    private var carryFlag = false

    private var isHalted = false
    private var isStopped = false
    private var timerEnabled = false
    private var handleOverflow = false

    private var operationCode = 0.toChar()
    private var programCounter = 0x100.toChar()
    private var stackPointer = 0xFFFE.toChar()

    private var cgb = false

    private var counter = 0
    private var haltCounter = 0
    private var divClockCounter = 0
    private var totalDiv = 0
    private var timerClockCounter = 0
    private var interruptCounter = 0
    private var timerFrequency = 256

    private val lastTimed = 0

    private var interruptMasterEnable = false
    private var setChangeTo = false
    private var changeInterrupt = false
    private var haltBug = false

    private val debugText = true
    var debug: PrintStream? = null

    var memory: Memory? = null
    var ppu: PPU? = null
    var displayFrame: DisplayFrame? = null


    //Resets
    //Clears all register filling them with 0's
    private fun clearRegisters() {
        Arrays.fill(registers, 0.toChar())
    }


    //Getters
    //Return the main.kotlin.CPU Cycle counter
    fun getCounter(): Int {
        return counter
    }

    //Return Register[index]
    fun getRegister(index: Int): Char {
        return (registers[index].code and 0xff).toChar()
    }

    //Return the Program Counter
    fun getProgramCounter(): Char {
        return programCounter
    }

    //Return the Stack Pointer address
    fun getStackPointer(): Char {
        return stackPointer
    }

    //Return the Status of the Zero Flag
    fun getZeroFlag(): Boolean {
        return zeroFlag
    }

    //Return the Status of Subtract Flag
    fun getSubtractFlag(): Boolean {
        return subtractFlag
    }

    //Return the Status of the Half Carry Flag
    fun getHalfCarryFlag(): Boolean {
        return halfCarryFlag
    }

    //Return the Status of the Carry Flag
    fun getCarryFlag(): Boolean {
        return carryFlag
    }

    //Return the Halted Status
    fun getIsHalted(): Boolean {
        return isHalted
    }

    //Return the Stopped Status
    fun getIsStopped(): Boolean {
        return isStopped
    }

    //Return the main.kotlin.PPU
    fun getPPU(): PPU {
        return ppu!!
    }

    fun getLastTimed(): Int {
        return lastTimed
    }


    //Setters (for registers mainly)
    //Sets Register[index] with a Value
    fun setRegister(index: Int, value: Char) {
        registers[index] = value
    }

    fun setCgbMode() {
        cgb = true

        ppu!!.setCgbMode()
    }


    //Increments a value to the main.kotlin.CPU Cycle Counter
    fun increaseCounter(amount: Int) {
        counter += amount
    }

    //Increments a value to the Program Counter
    fun increaseProgramCounter(amount: Int) {
        programCounter += amount.toChar().code
    }

    //Sets the Program Counter to a Value
    fun setProgramCounter(amount: Char) {
        programCounter = amount
    }

    //Increments a value to the Stack Pointer
    fun increaseStackPointer(amount: Int) {
        stackPointer += amount.toChar().code
    }

    //Sets the Stack Pointer to a Value
    fun setStackPointer(amount: Char) {
        stackPointer = amount
    }

    //Sets the Zero Flag to a State
    fun setZeroFlag(state: Boolean) {
        zeroFlag = state
    }

    //Sets Subtract Flag to a State
    fun setSubtractFlag(state: Boolean) {
        subtractFlag = state
    }

    //Sets the Half Carry Flag to a State
    fun setHalfCarryFlag(state: Boolean) {
        halfCarryFlag = state
    }

    //Sets the Carry Flag to a State
    fun setCarryFlag(state: Boolean) {
        carryFlag = state
    }

    //Set the isHalted Flag
    fun setIsHalted(state: Boolean) {
        isHalted = state
    }

    //Set the isStopped Flag
    fun setIsStopped(state: Boolean) {
        isStopped = state
    }

    //Set the signal to change the IME flag
    fun setChangeInterrupt(state: Boolean) {
        changeInterrupt = state
    }

    //Used to enable interrupt after a cycle
    fun setInterruptCounter(value: Int) {
        interruptCounter = value
    }

    //Set the state to change the IME flag to
    fun setChangeTo(state: Boolean) {
        setChangeTo = state
    }

    //Sets the state of the timer enabled/disabled
    fun setTimerEnabled(state: Boolean) {
        timerEnabled = state
    }

    fun setTimerFrequency(frequency: Int) {
        timerFrequency = frequency
    }

    fun setDivClockCounter(value: Int) {
        divClockCounter = value
    }

    fun setHaltCounter(value: Int) {
        haltCounter = value
    }


    //Constructor
    @Throws(IOException::class)
    fun CPU() {
        clearRegisters()
        memory = Memory(this)
        ppu = PPU(memory, this)
        displayFrame = DisplayFrame(memory, ppu, this)
        memory!!.setDisplayFrame(displayFrame)

        //        if(debugText) {
//            debug = new PrintStream("A.txt");
//            PrintStream console = System.out;
//            System.setOut(debug);
//        }
        CPUInstructions.setCpu(this)
        CPUInstructions.setMem(memory)

        //memory.dumpMemory();
        init()
    }

    private fun init() {
        registers[0] = 0x01.toChar()
        registers[2] = 0x13.toChar()
        registers[4] = 0xD8.toChar()
        registers[5] = 0xB0.toChar()
        registers[6] = 0x01.toChar()
        registers[7] = 0x4D.toChar()

        zeroFlag = true
        subtractFlag = false
        halfCarryFlag = true
        carryFlag = true
    }

    fun reset() {
        memory!!.reset()
        ppu!!.reset()
        programCounter = 0x100.toChar()
        stackPointer = 0xfffe.toChar()
        counter = 0
        init()
    }

    fun computeFlags() {
        zeroFlag = (registers[5].code and 0x80) != 0
        subtractFlag = (registers[5].code and 0x40) != 0
        halfCarryFlag = (registers[5].code and 0x20) != 0
        carryFlag = (registers[5].code and 0x10) != 0
    }

    fun computeFRegister() {
        registers[5] = 0.toChar()
        if (zeroFlag) registers[5] = (registers[5].code or 0x80).toChar()
        if (subtractFlag) registers[5] = (registers[5].code or 0x40).toChar()
        if (halfCarryFlag) registers[5] = (registers[5].code or 0x20).toChar()
        if (carryFlag) registers[5] = (registers[5].code or 0x10).toChar()
    }

    fun saveState(): Array<Any?> {
        val state = arrayOfNulls<Any>(3)
        //        state[0] = saveStateCPU();
        return state
    }


    //    private main.kotlin.CPU saveStateCPU() {
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
    //    public void loadState(main.kotlin.CPU cpu, main.kotlin.Memory memory, main.kotlin.PPU ppu) {
    //        loadState(cpu);
    //        memory.loadState(memory);
    //        ppu.loadState(ppu);
    //    }
    private fun loadState(cpu: CPU) {
        this.registers = cpu.registers.copyOf(registers.size)
        this.zeroFlag = cpu.zeroFlag
        this.subtractFlag = cpu.subtractFlag
        this.halfCarryFlag = cpu.halfCarryFlag
        this.carryFlag = cpu.carryFlag

        this.isHalted = cpu.isHalted
        this.isStopped = cpu.isStopped
        this.timerEnabled = cpu.timerEnabled
        this.handleOverflow = cpu.handleOverflow

        this.operationCode = cpu.operationCode
        this.programCounter = cpu.programCounter
        this.stackPointer = cpu.stackPointer

        this.counter = cpu.counter
        this.haltCounter = cpu.haltCounter
        this.divClockCounter = cpu.divClockCounter
        this.timerClockCounter = cpu.timerClockCounter
        this.interruptCounter = cpu.interruptCounter
        this.timerFrequency = cpu.timerFrequency

        this.interruptMasterEnable = cpu.interruptMasterEnable
        this.setChangeTo = cpu.setChangeTo
        this.changeInterrupt = cpu.changeInterrupt
        this.haltBug = cpu.haltBug
    }

    @Throws(InterruptedException::class)
    fun cycle() {
//        if(counter >= 0x10000) {
//            memory.dumpMemory();
//            System.exit(0);
//        }

//        main.kotlin.CPUInstructions.show();
//        main.kotlin.CPUInstructions.dumpRegisters();
//        if(counter >= 0x10000)
//            System.exit(0);

        if (!getIsStopped()) {
            if (!getIsHalted()) {
                fetchOperationCodes()
                decodeOperationCodes()
                if (changeInterrupt && interruptCounter < counter) {
                    interruptMasterEnable = setChangeTo
                    changeInterrupt = false
                }
            } else {
                handleCPUTimers()
            }

            handleInterrupts()
        }
    }

    fun setInterrupt(interrupt: Int) {
        when (interrupt) {
            0 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 0)
            1 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 1)
            2 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 2)
            3 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 3)
            4 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 4)
        }
    }

    private fun handleInterrupts() {
        if (interruptMasterEnable) {
            val interrupt = (memory!!.getMemory(ReservedAddresses.IF.memoryAddress).code and memory!!.getMemory(ReservedAddresses.IE.memoryAddress).code).toChar()
            if (interrupt.code > 0) {
                if (isHalted) setIsHalted(false)

                interruptMasterEnable = false
                memory!!.storeWordInSP(stackPointer.code, programCounter.code)

                val vBlank = interrupt.code and 0x1
                if (vBlank == 1) {
                    setProgramCounter(0x40.toChar())
                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 0)
                    return
                }

                val LCDCStatus = (interrupt.code and 0x2) shr 1
                if (LCDCStatus == 1) {
                    setProgramCounter(0x48.toChar())
                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 1)
                    return
                }

                val timerOverflow = (interrupt.code and 0x4) shr 2
                if (timerOverflow == 1) {
                    setProgramCounter(0x50.toChar())
                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 2)
                    return
                }

                val serialTransfer = (interrupt.code and 0x8) shr 3
                if (serialTransfer == 1) {
                    setProgramCounter(0x58.toChar())
                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 3)
                    return
                }

                val hiLo = (interrupt.code and 0x10) shr 4
                if (hiLo == 1) {
                    if (isStopped) {
                        isStopped = false
                    }
                    setProgramCounter(0x60.toChar())
                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 4)
                }
            }
        } else if (isHalted) {
            if ((memory!!.getMemory(ReservedAddresses.IF.memoryAddress).code and memory!!.getMemory(ReservedAddresses.IE.memoryAddress).code and 0x1f) > 0) {
                isHalted = false
                if (haltCounter == counter) haltBug = true
            }
        }
    }

    fun resetClocks() {
        val divUsed = timerFrequency == 256

        if (divUsed && timerEnabled) {
            if (memory!!.testBit(ReservedAddresses.DIV.memoryAddress, 1)) {
                memory!!.setMemory(
                    ReservedAddresses.TIMA.memoryAddress,
                    (memory!!.getMemory(ReservedAddresses.TIMA.memoryAddress).code + 1).toChar()
                )
            }
        } else if (timerEnabled) {
            if (totalDiv == 0) memory!!.setMemory(
                ReservedAddresses.TIMA.memoryAddress,
                (memory!!.getMemory(ReservedAddresses.TIMA.memoryAddress).code + 1).toChar()
            )
        }

        divClockCounter = 0
        totalDiv = 0
        timerClockCounter = 0
    }

    fun handleCPUTimers() {
        increaseCounter(1)

        handleDividerTimer()
        handleTimer()
//        System.out.println(divClockCounter + " " + timerClockCounter);
    }

    private fun handleDividerTimer() {
        divClockCounter++
        totalDiv++
        while (divClockCounter >= 64) {
            divClockCounter -= 64
            var div_counter = memory!!.getMemory(ReservedAddresses.DIV.memoryAddress).code
            div_counter = (div_counter + 1) and 0xff
            memory!!.writePriv(ReservedAddresses.DIV.memoryAddress, div_counter.toChar())
        }
        if (totalDiv >= timerFrequency) totalDiv = 0
    }

    private fun handleTimer() {
        CPUInstructions.readTAC()
        if (handleOverflow) {
            memory!!.setMemory(ReservedAddresses.TIMA.memoryAddress, memory!!.getMemory(
                ReservedAddresses.TMA.memoryAddress))
            setInterrupt(TIMER_INTERRUPT)
            handleOverflow = false
        }
        if (timerEnabled) {
            timerClockCounter++
            while (timerClockCounter >= timerFrequency) {
                timerClockCounter -= timerFrequency
                if (memory!!.getMemory(ReservedAddresses.TIMA.memoryAddress).code == 0xff) {
                    handleOverflow = true
                } else {
                    memory!!.setMemory(
                        ReservedAddresses.TIMA.memoryAddress,
                        (((memory!!.getMemory(ReservedAddresses.TIMA.memoryAddress).code and 0xff) + 1) and 0xff).toChar()
                    )
                }
            }
        }
    }

    private fun fetchOperationCodes() {
        if (haltBug) {
            operationCode = memory!!.getMemory((programCounter--).code)
            haltBug = false
        } else operationCode = memory!!.getMemory(programCounter.code)

        handleCPUTimers()
    }

    private fun decodeOperationCodes() {
//        System.out.println(operationCode);

//        System.out.println(counter);

//        if(counter >= 164200) {
//            memory.dumpMemory();
//            System.exit(0);
//        }

//            main.kotlin.CPUInstructions.dumpRegisters();
//            main.kotlin.CPUInstructions.show();
//        }
        /**/ */
//        if(counter >= 366125) {
//            System.exit(1);
//        }
//        if(debugText) System.setOut(debug);

        when (operationCode) {
            0x00 ->  //NOP
                CPUInstructions.nop()

            0x01 ->  //LD BC,u16
                CPUInstructions.ld16bit(0)

            0x02 ->  //LD (BC),A
                CPUInstructions.ldTwoRegisters(0)

            0x03 ->  //INC BC
                CPUInstructions.incR(0)

            0x04 ->  //INC B
                CPUInstructions.inc(1)

            0x05 ->  //DEC B
                CPUInstructions.dec(1)

            0x06 ->  //LD B,u8
                CPUInstructions.ld(1, 9)

            0x07 ->  //RLCA
                CPUInstructions.rlca()

            0x08 ->  //LD (u16),SP
                CPUInstructions.LDnnSP()

            0x09 ->  //ADD HL,BC
                CPUInstructions.addHL(0)

            0x0A ->  //LD A,(BC)
                CPUInstructions.ldTwoRegistersIntoA(0)

            0x0B ->  //DEC BC
                CPUInstructions.decR(0)

            0x0C ->  //INC C
                CPUInstructions.inc(2)

            0x0D ->  //DEC C
                CPUInstructions.dec(2)

            0x0E ->  //LD C,u8
                CPUInstructions.ld(2, 9)

            0x0F ->  //RRCA
                CPUInstructions.rrca()

            0x10 ->  //STOP
                CPUInstructions.stop()

            0x11 ->  //LD DE,u16
                CPUInstructions.ld16bit(1)

            0x12 ->  //LD (DE),A
                CPUInstructions.ldTwoRegisters(1)

            0x13 ->  //INC DE
                CPUInstructions.incR(1)

            0x14 ->  //INC D
                CPUInstructions.inc(3)

            0x15 ->  //DEC D
                CPUInstructions.dec(3)

            0x16 ->  //LD D,u8
                CPUInstructions.ld(3, 9)

            0x17 ->  //RLA
                CPUInstructions.rla()

            0x18 ->  //JR i8
                CPUInstructions.jr()

            0x19 ->  //ADD HL,DE
                CPUInstructions.addHL(1)

            0x1A ->  //LD A,(DE)
                CPUInstructions.ldTwoRegistersIntoA(1)

            0x1B ->  //DEC DE
                CPUInstructions.decR(1)

            0x1C ->  //INC E
                CPUInstructions.inc(4)

            0x1D ->  //DEC E
                CPUInstructions.dec(4)

            0x1E ->  //LD E,u8
                CPUInstructions.ld(4, 9)

            0x1F ->  //RRA
                CPUInstructions.rra()

            0x20 ->  //JR NZ,i8
                CPUInstructions.jrCond(0)

            0x21 ->  //LD HL,u16
                CPUInstructions.ld16bit(2)

            0x22 ->  //LDI (HL),A
                CPUInstructions.ldi(1)

            0x23 ->  //INC HL
                CPUInstructions.incR(2)

            0x24 ->  //INC H
                CPUInstructions.inc(6)

            0x25 ->  //DEC H
                CPUInstructions.dec(6)

            0x26 ->  //LD H,u8
                CPUInstructions.ld(6, 9)

            0x27 ->  //DAA
                CPUInstructions.daa()

            0x28 ->  //JR Z,u8
                CPUInstructions.jrCond(1)

            0x29 ->  //ADD HL, HL
                CPUInstructions.addHL(2)

            0x2A ->  //LDI A,(HL)
                CPUInstructions.ldi(0)

            0x2B ->  //DEC HL
                CPUInstructions.decR(2)

            0x2C ->  //INC L
                CPUInstructions.inc(7)

            0x2D ->  //DEC L
                CPUInstructions.dec(7)

            0x2E ->  //LD L,u8
                CPUInstructions.ld(7, 9)

            0x2F ->  //CPL
                CPUInstructions.cpl()

            0x30 ->  //JR NC,u8
                CPUInstructions.jrCond(2)

            0x31 ->  //LD SP,u16
                CPUInstructions.ld16bit(3)

            0x32 ->  //LDD (HL),A
                CPUInstructions.ldd(1)

            0x33 ->  //INC SP
                CPUInstructions.incR(3)

            0x34 ->  //INC (HL)
                CPUInstructions.inc(8)

            0x35 ->  //INC (HL)
                CPUInstructions.dec(8)

            0x36 ->  //LD (HL), n
                CPUInstructions.ld(8, 9)

            0x37 ->  //SCF
                CPUInstructions.scf()

            0x38 ->  //JR C,u8
                CPUInstructions.jrCond(3)

            0x39 ->  //ADD HL,SP
                CPUInstructions.addHL(3)

            0x3A ->  //LDD A,(HL)
                CPUInstructions.ldd(0)

            0x3B ->  //DEC SP
                CPUInstructions.decR(3)

            0x3C ->  //INC A
                CPUInstructions.inc(0)

            0x3D ->  //DEC A
                CPUInstructions.dec(0)

            0x3E ->  //LD A,u8
                CPUInstructions.ldTwoRegistersIntoA(3)

            0x3F ->  //CCF
                CPUInstructions.ccf()

            0x40 ->  //LD B,B
                CPUInstructions.ld(1, 1)

            0x41 ->  //LD B,C
                CPUInstructions.ld(1, 2)

            0x42 ->  //LD B,D
                CPUInstructions.ld(1, 3)

            0x43 ->  //LD B,E
                CPUInstructions.ld(1, 4)

            0x44 ->  //LD B,H
                CPUInstructions.ld(1, 6)

            0x45 ->  //LD B,L
                CPUInstructions.ld(1, 7)

            0x46 ->  //LD B,(HL)
                CPUInstructions.ld(1, 8)

            0x47 ->  //LD B,A
                CPUInstructions.ld(1, 0)

            0x48 ->  //LD C,B
                CPUInstructions.ld(2, 1)

            0x49 ->  //LD C,C
                CPUInstructions.ld(2, 2)

            0x4A ->  //LD C,D
                CPUInstructions.ld(2, 3)

            0x4B ->  //LD C,E
                CPUInstructions.ld(2, 4)

            0x4C ->  //LD C,H
                CPUInstructions.ld(2, 6)

            0x4D ->  //LD C,L
                CPUInstructions.ld(2, 7)

            0x4E ->  //LD C,(HL)
                CPUInstructions.ld(2, 8)

            0x4F ->  //LD C,A
                CPUInstructions.ld(2, 0)

            0x50 ->  //LD D,B
                CPUInstructions.ld(3, 1)

            0x51 ->  //LD D,C
                CPUInstructions.ld(3, 2)

            0x52 ->  //LD D,D
                CPUInstructions.ld(3, 3)

            0x53 ->  //LD D,E
                CPUInstructions.ld(3, 4)

            0x54 ->  //LD D,H
                CPUInstructions.ld(3, 6)

            0x55 ->  //LD D,L
                CPUInstructions.ld(3, 7)

            0x56 ->  //LD D,(HL)
                CPUInstructions.ld(3, 8)

            0x57 ->  //LD D,A
                CPUInstructions.ld(3, 0)

            0x58 ->  //LD E,B
                CPUInstructions.ld(4, 1)

            0x59 ->  //LD E,C
                CPUInstructions.ld(4, 2)

            0x5A ->  //LD E,D
                CPUInstructions.ld(4, 3)

            0x5B ->  //LD E,E
                CPUInstructions.ld(4, 4)

            0x5C ->  //LD E,H
                CPUInstructions.ld(4, 6)

            0x5D ->  //LD E,L
                CPUInstructions.ld(4, 7)

            0x5E ->  //LD E,(HL)
                CPUInstructions.ld(4, 8)

            0x5F ->  //LD E,A
                CPUInstructions.ld(4, 0)

            0x60 ->  //LD H,B
                CPUInstructions.ld(6, 1)

            0x61 ->  //LD H,C
                CPUInstructions.ld(6, 2)

            0x62 ->  //LD H,D
                CPUInstructions.ld(6, 3)

            0x63 ->  //LD H,E
                CPUInstructions.ld(6, 4)

            0x64 ->  //LD H,H
                CPUInstructions.ld(6, 6)

            0x65 ->  //LD H,L
                CPUInstructions.ld(6, 7)

            0x66 ->  //LD H,(HL)
                CPUInstructions.ld(6, 8)

            0x67 ->  //LD H,A
                CPUInstructions.ld(6, 0)

            0x68 ->  //LD L,B
                CPUInstructions.ld(7, 1)

            0x69 ->  //LD L,C
                CPUInstructions.ld(7, 2)

            0x6A ->  //LD L,D
                CPUInstructions.ld(7, 3)

            0x6B ->  //LD L,E
                CPUInstructions.ld(7, 4)

            0x6C ->  //LD L,H
                CPUInstructions.ld(7, 6)

            0x6D ->  //LD L,L
                CPUInstructions.ld(7, 7)

            0x6E ->  //LD L,(HL)
                CPUInstructions.ld(7, 8)

            0x6F ->  //LD L,A
                CPUInstructions.ld(7, 0)

            0x70 ->  //LD (HL),B
                CPUInstructions.ld(8, 1)

            0x71 ->  //LD (HL),C
                CPUInstructions.ld(8, 2)

            0x72 ->  //LD (HL),D
                CPUInstructions.ld(8, 3)

            0x73 ->  //LD (HL),E
                CPUInstructions.ld(8, 4)

            0x74 ->  //LD (HL),H
                CPUInstructions.ld(8, 6)

            0x75 ->  //LD (HL),L
                CPUInstructions.ld(8, 7)

            0x76 ->  //HALT
                CPUInstructions.halt()

            0x77 ->  //LD (HL),A
                CPUInstructions.ld(8, 0)

            0x78 ->  //LD A,B
                CPUInstructions.ld(0, 1)

            0x79 ->  //LD A,C
                CPUInstructions.ld(0, 2)

            0x7A ->  //LD A,D
                CPUInstructions.ld(0, 3)

            0x7B ->  //LD A,E
                CPUInstructions.ld(0, 4)

            0x7C ->  //LD A,H
                CPUInstructions.ld(0, 6)

            0x7D ->  //LD A,L
                CPUInstructions.ld(0, 7)

            0x7E ->  //LD A,(HL)
                CPUInstructions.ld(0, 8)

            0x7F ->  //LD A,A
                CPUInstructions.ld(0, 0)

            0x80 ->  //ADD A,B
                CPUInstructions.add(1)

            0x81 ->  //ADD A,C
                CPUInstructions.add(2)

            0x82 ->  //ADD A,D
                CPUInstructions.add(3)

            0x83 ->  //ADD A,E
                CPUInstructions.add(4)

            0x84 ->  //ADD A, H
                CPUInstructions.add(6)

            0x85 ->  //ADD A,L
                CPUInstructions.add(7)

            0x86 ->  //ADD A,(HL)
                CPUInstructions.add(8)

            0x87 ->  //ADD A,A
                CPUInstructions.add(0)

            0x88 ->  //ADC A,B
                CPUInstructions.adc(1)

            0x89 ->  //ADC A,C
                CPUInstructions.adc(2)

            0x8A ->  //ADC A,D
                CPUInstructions.adc(3)

            0x8B ->  //ADC A,E
                CPUInstructions.adc(4)

            0x8C ->  //ADC A,H
                CPUInstructions.adc(6)

            0x8D ->  //ADC A,L
                CPUInstructions.adc(7)

            0x8E ->  //ADC A,(HL)
                CPUInstructions.adc(8)

            0x8F ->  //ADC A,A
                CPUInstructions.adc(0)

            0x90 ->  //SUB A,B
                CPUInstructions.sub(1)

            0x91 ->  //SUB A,C
                CPUInstructions.sub(2)

            0x92 ->  //SUB A,D
                CPUInstructions.sub(3)

            0x93 ->  //SUB A,E
                CPUInstructions.sub(4)

            0x94 ->  //SUB A,H
                CPUInstructions.sub(6)

            0x95 ->  //SUB A,L
                CPUInstructions.sub(7)

            0x96 ->  //SUB A, (HL)
                CPUInstructions.sub(8)

            0x97 ->  //SUB A,A
                CPUInstructions.sub(0)

            0x98 ->  //SBC A,B
                CPUInstructions.sbc(1)

            0x99 ->  //SBC A,C
                CPUInstructions.sbc(2)

            0x9A ->  //SBC A,D
                CPUInstructions.sbc(3)

            0x9B ->  //SBC A,E
                CPUInstructions.sbc(4)

            0x9C ->  //SBC A,H
                CPUInstructions.sbc(6)

            0x9D ->  //SBC A,L
                CPUInstructions.sbc(7)

            0x9E ->  //SBC A, (HL)
                CPUInstructions.sbc(8)

            0x9F ->  //SBC A,A
                CPUInstructions.sbc(0)

            0xA0 ->  //AND A,B
                CPUInstructions.and(1)

            0xA1 ->  //AND A,C
                CPUInstructions.and(2)

            0xA2 ->  //AND A,D
                CPUInstructions.and(3)

            0xA3 ->  //AND A,E
                CPUInstructions.and(4)

            0xA4 ->  //AND A,H
                CPUInstructions.and(6)

            0xA5 ->  //AND A,L
                CPUInstructions.and(7)

            0xA6 ->  //AND A,(HL)
                CPUInstructions.and(8)

            0xA7 ->  //AND A,A
                CPUInstructions.and(0)

            0xA8 ->  //XOR A,B
                CPUInstructions.xor(1)

            0xA9 ->  //XOR A,C
                CPUInstructions.xor(2)

            0xAA ->  //XOR A,D
                CPUInstructions.xor(3)

            0xAB ->  //XOR A,E
                CPUInstructions.xor(4)

            0xAC ->  //XOR A,H
                CPUInstructions.xor(6)

            0xAD ->  //XOR A,L
                CPUInstructions.xor(7)

            0xAE ->  //XOR A,(HL)
                CPUInstructions.xor(8)

            0xAF ->  //XOR A,A
                CPUInstructions.xor(0)

            0xB0 ->  //OR A,B
                CPUInstructions.or(1)

            0xB1 ->  //OR A,C
                CPUInstructions.or(2)

            0xB2 ->  //OR A,D
                CPUInstructions.or(3)

            0xB3 ->  //OR A,E
                CPUInstructions.or(4)

            0xB4 ->  //OR A,H
                CPUInstructions.or(6)

            0xB5 ->  //OR A,L
                CPUInstructions.or(7)

            0xB6 ->  //OR A,(HL)
                CPUInstructions.or(8)

            0xB7 ->  //OR A,A
                CPUInstructions.or(0)

            0xB8 ->  //CP A,B
                CPUInstructions.cp(1)

            0xB9 ->  //CP A,C
                CPUInstructions.cp(2)

            0xBA ->  //CP A,D
                CPUInstructions.cp(3)

            0xBB ->  //CP A,E
                CPUInstructions.cp(4)

            0xBC ->  //CP A,H
                CPUInstructions.cp(6)

            0xBD ->  //CP A,L
                CPUInstructions.cp(7)

            0xBE ->  //CP A,(HL)
                CPUInstructions.cp(8)

            0xBF ->  //CP A,A
                CPUInstructions.cp(0)

            0xC0 ->  //RET NZ
                CPUInstructions.retCond(0)

            0xC1 ->  //POP BC
                CPUInstructions.pop(1)

            0xC2 ->  //JP NZ,u16
                CPUInstructions.jpCond(0)

            0xC3 ->  //JP u16
                CPUInstructions.jp()

            0xC4 ->  //CALL NZ, nn
                CPUInstructions.callCond(0)

            0xC5 ->  //PUSH BC
                CPUInstructions.push(1)

            0xC6 ->  //ADD A,#
                CPUInstructions.add(9)

            0xC7 ->  //RST 00H
                CPUInstructions.rst(0)

            0xC8 ->  //RET Z
                CPUInstructions.retCond(1)

            0xC9 ->  //RET
                CPUInstructions.ret()

            0xCA ->  //JP Z,u16
                CPUInstructions.jpCond(1)

            0xCB -> {
                CPUInstructions.cb()
                operationCode = (memory!!.getMemory(programCounter.code).code and 0xff).toChar()
                when (operationCode) {
                    0x00 ->  //RLC B
                        CPUInstructions.rlc(1)

                    0x01 ->  //RLC C
                        CPUInstructions.rlc(2)

                    0x02 ->  //RLC D
                        CPUInstructions.rlc(3)

                    0x03 ->  //RLC E
                        CPUInstructions.rlc(4)

                    0x04 ->  //RLC H
                        CPUInstructions.rlc(6)

                    0x05 ->  //RLC L
                        CPUInstructions.rlc(7)

                    0x06 ->  //RLC HL
                        CPUInstructions.rlc(8)

                    0x07 ->  //RLC A
                        CPUInstructions.rlc(0)

                    0x08 ->  //RRC B
                        CPUInstructions.rrc(1)

                    0x09 ->  //RRC C
                        CPUInstructions.rrc(2)

                    0x0A ->  //RRC D
                        CPUInstructions.rrc(3)

                    0x0B ->  //RRC E
                        CPUInstructions.rrc(4)

                    0x0C ->  //RRC H
                        CPUInstructions.rrc(6)

                    0x0D ->  //RRC L
                        CPUInstructions.rrc(7)

                    0x0E ->  //RRC (HL)
                        CPUInstructions.rrc(8)

                    0x0F ->  //RRC A
                        CPUInstructions.rrc(0)

                    0x10 ->  //RL B
                        CPUInstructions.rl(1)

                    0x11 ->  //RL C
                        CPUInstructions.rl(2)

                    0x12 ->  //RL D
                        CPUInstructions.rl(3)

                    0x13 ->  //RL E
                        CPUInstructions.rl(4)

                    0x14 ->  //RL H
                        CPUInstructions.rl(6)

                    0x15 ->  //RL L
                        CPUInstructions.rl(7)

                    0x16 ->  //RL (HL)
                        CPUInstructions.rl(8)

                    0x17 ->  //RL A
                        CPUInstructions.rl(0)

                    0x18 ->  //RR B
                        CPUInstructions.rr(1)

                    0x19 ->  //RR C
                        CPUInstructions.rr(2)

                    0x1A ->  //RR D
                        CPUInstructions.rr(3)

                    0x1B ->  //RR E
                        CPUInstructions.rr(4)

                    0x1C ->  //RR H
                        CPUInstructions.rr(6)

                    0x1D ->  //RR L
                        CPUInstructions.rr(7)

                    0x1E ->  //RR (HL)
                        CPUInstructions.rr(8)

                    0x1F ->  //RR A
                        CPUInstructions.rr(0)

                    0x20 ->  //SLA B
                        CPUInstructions.sla(1)

                    0x21 ->  //SLA C
                        CPUInstructions.sla(2)

                    0x22 ->  //SLA D
                        CPUInstructions.sla(3)

                    0x23 ->  //SLA E
                        CPUInstructions.sla(4)

                    0x24 ->  //SLA H
                        CPUInstructions.sla(6)

                    0x25 ->  //SLA L
                        CPUInstructions.sla(7)

                    0x26 ->  //SLA (HL)
                        CPUInstructions.sla(8)

                    0x27 ->  //SLA A
                        CPUInstructions.sla(0)

                    0x28 ->  //SRA B
                        CPUInstructions.sra(1)

                    0x29 ->  //SRA C
                        CPUInstructions.sra(2)

                    0x2A ->  //SRA D
                        CPUInstructions.sra(3)

                    0x2B ->  //SRA E
                        CPUInstructions.sra(4)

                    0x2C ->  //SRA H
                        CPUInstructions.sra(6)

                    0x2D ->  //SRA L
                        CPUInstructions.sra(7)

                    0x2E ->  //SRA (HL)
                        CPUInstructions.sra(8)

                    0x2F ->  //SRA A
                        CPUInstructions.sra(0)

                    0x30 ->  //SWAP B
                        CPUInstructions.swap(1)

                    0x31 ->  //SWAP C
                        CPUInstructions.swap(2)

                    0x32 ->  //SWAP D
                        CPUInstructions.swap(3)

                    0x33 ->  //SWAP E
                        CPUInstructions.swap(4)

                    0x34 ->  //SWAP H
                        CPUInstructions.swap(6)

                    0x35 ->  //SWAP L
                        CPUInstructions.swap(7)

                    0x36 ->  //SWAP (HL)
                        CPUInstructions.swap(8)

                    0x37 ->  //SWAP A
                        CPUInstructions.swap(0)

                    0x38 ->  //SRL B
                        CPUInstructions.srl(1)

                    0x39 ->  //SRL C
                        CPUInstructions.srl(2)

                    0x3A ->  //SRL D
                        CPUInstructions.srl(3)

                    0x3B ->  //SRL E
                        CPUInstructions.srl(4)

                    0x3C ->  //SRL H
                        CPUInstructions.srl(6)

                    0x3D ->  //SRL L
                        CPUInstructions.srl(7)

                    0x3E ->  //SRL (HL)
                        CPUInstructions.srl(8)

                    0x3F ->  //SRL A
                        CPUInstructions.srl(0)

                    0x40 ->  //BIT 0,B
                        CPUInstructions.bit(0, 1)

                    0x41 ->  //BIT 0,C
                        CPUInstructions.bit(0, 2)

                    0x42 ->  //BIT 0,D
                        CPUInstructions.bit(0, 3)

                    0x43 ->  //BIT 0,E
                        CPUInstructions.bit(0, 4)

                    0x44 ->  //BIT 0,H
                        CPUInstructions.bit(0, 6)

                    0x45 ->  //BIT 0,L
                        CPUInstructions.bit(0, 7)

                    0x46 ->  //BIT 0,(HL)
                        CPUInstructions.bit(0, 8)

                    0x47 ->  //BIT 0,A
                        CPUInstructions.bit(0, 0)

                    0x48 ->  //BIT 1,B
                        CPUInstructions.bit(1, 1)

                    0x49 ->  //BIT 1,C
                        CPUInstructions.bit(1, 2)

                    0x4A ->  //BIT 1,D
                        CPUInstructions.bit(1, 3)

                    0x4B ->  //BIT 1,E
                        CPUInstructions.bit(1, 4)

                    0x4C ->  //BIT 1,H
                        CPUInstructions.bit(1, 6)

                    0x4D ->  //BIT 1,L
                        CPUInstructions.bit(1, 7)

                    0x4E ->  //BIT 1,(HL)
                        CPUInstructions.bit(1, 8)

                    0x4F ->  //BIT 1,A
                        CPUInstructions.bit(1, 0)

                    0x50 ->  //BIT 2,B
                        CPUInstructions.bit(2, 1)

                    0x51 ->  //BIT 2,C
                        CPUInstructions.bit(2, 2)

                    0x52 ->  //BIT 2,D
                        CPUInstructions.bit(2, 3)

                    0x53 ->  //BIT 2,E
                        CPUInstructions.bit(2, 4)

                    0x54 ->  //BIT 2,H
                        CPUInstructions.bit(2, 6)

                    0x55 ->  //BIT 2,L
                        CPUInstructions.bit(2, 7)

                    0x56 ->  //BIT 2,(HL)
                        CPUInstructions.bit(2, 8)

                    0x57 ->  //BIT 2,A
                        CPUInstructions.bit(2, 0)

                    0x58 ->  //BIT 3,B
                        CPUInstructions.bit(3, 1)

                    0x59 ->  //BIT 3,C
                        CPUInstructions.bit(3, 2)

                    0x5A ->  //BIT 3,D
                        CPUInstructions.bit(3, 3)

                    0x5B ->  //BIT 3,E
                        CPUInstructions.bit(3, 4)

                    0x5C ->  //BIT 3,H
                        CPUInstructions.bit(3, 6)

                    0x5D ->  //BIT 3,L
                        CPUInstructions.bit(3, 7)

                    0x5E ->  //BIT 3,(HL)
                        CPUInstructions.bit(3, 8)

                    0x5F ->  //BIT 3,A
                        CPUInstructions.bit(3, 0)

                    0x60 ->  //BIT 4,B
                        CPUInstructions.bit(4, 1)

                    0x61 ->  //BIT 4,C
                        CPUInstructions.bit(4, 2)

                    0x62 ->  //BIT 4,D
                        CPUInstructions.bit(4, 3)

                    0x63 ->  //BIT 4,E
                        CPUInstructions.bit(4, 4)

                    0x64 ->  //BIT 4,H
                        CPUInstructions.bit(4, 6)

                    0x65 ->  //BIT 4,L
                        CPUInstructions.bit(4, 7)

                    0x66 ->  //BIT 4,(HL)
                        CPUInstructions.bit(4, 8)

                    0x67 ->  //BIT 4,A
                        CPUInstructions.bit(4, 0)

                    0x68 ->  //BIT 5,B
                        CPUInstructions.bit(5, 1)

                    0x69 ->  //BIT 5,C
                        CPUInstructions.bit(5, 2)

                    0x6A ->  //BIT 5,D
                        CPUInstructions.bit(5, 3)

                    0x6B ->  //BIT 5,E
                        CPUInstructions.bit(5, 4)

                    0x6C ->  //BIT 5,H
                        CPUInstructions.bit(5, 6)

                    0x6D ->  //BIT 5,L
                        CPUInstructions.bit(5, 7)

                    0x6E ->  //BIT 5,(HL)
                        CPUInstructions.bit(5, 8)

                    0x6F ->  //BIT 5,A
                        CPUInstructions.bit(5, 0)

                    0x70 ->  //BIT 6,B
                        CPUInstructions.bit(6, 1)

                    0x71 ->  //BIT 6,C
                        CPUInstructions.bit(6, 2)

                    0x72 ->  //BIT 6,D
                        CPUInstructions.bit(6, 3)

                    0x73 ->  //BIT 6,E
                        CPUInstructions.bit(6, 4)

                    0x74 ->  //BIT 6,H
                        CPUInstructions.bit(6, 6)

                    0x75 ->  //BIT 6,L
                        CPUInstructions.bit(6, 7)

                    0x76 ->  //BIT 6,(HL)
                        CPUInstructions.bit(6, 8)

                    0x77 ->  //BIT 6,A
                        CPUInstructions.bit(6, 0)

                    0x78 ->  //BIT 7,B
                        CPUInstructions.bit(7, 1)

                    0x79 ->  //BIT 7,C
                        CPUInstructions.bit(7, 2)

                    0x7A ->  //BIT 7,D
                        CPUInstructions.bit(7, 3)

                    0x7B ->  //BIT 7,E
                        CPUInstructions.bit(7, 4)

                    0x7C ->  //BIT 7,H
                        CPUInstructions.bit(7, 6)

                    0x7D ->  //BIT 7,L
                        CPUInstructions.bit(7, 7)

                    0x7E ->  //BIT 7, (HL)
                        CPUInstructions.bit(7, 8)

                    0x7F ->  //BIT 7,A
                        CPUInstructions.bit(7, 0)

                    0x80 ->  //RES 0,B
                        CPUInstructions.res(0, 1)

                    0x81 ->  //RES 0,C
                        CPUInstructions.res(0, 2)

                    0x82 ->  //RES 0,D
                        CPUInstructions.res(0, 3)

                    0x83 ->  //RES 0,E
                        CPUInstructions.res(0, 4)

                    0x84 ->  //RES 0,H
                        CPUInstructions.res(0, 6)

                    0x85 ->  //RES 0,L
                        CPUInstructions.res(0, 7)

                    0x86 ->  //RES 0,(HL)
                        CPUInstructions.res(0, 8)

                    0x87 ->  //RES 0,A
                        CPUInstructions.res(0, 0)

                    0x88 ->  //RES 1,B
                        CPUInstructions.res(1, 1)

                    0x89 ->  //RES 1,C
                        CPUInstructions.res(1, 2)

                    0x8A ->  //RES 1,D
                        CPUInstructions.res(1, 3)

                    0x8B ->  //RES 1,E
                        CPUInstructions.res(1, 4)

                    0x8C ->  //RES 1,H
                        CPUInstructions.res(1, 6)

                    0x8D ->  //RES 1,L
                        CPUInstructions.res(1, 7)

                    0x8E ->  //RES 1,(HL)
                        CPUInstructions.res(1, 8)

                    0x8F ->  //RES 1,A
                        CPUInstructions.res(1, 0)

                    0x90 ->  //RES 2,B
                        CPUInstructions.res(2, 1)

                    0x91 ->  //RES 2,C
                        CPUInstructions.res(2, 2)

                    0x92 ->  //RES 2,D
                        CPUInstructions.res(2, 3)

                    0x93 ->  //RES 2,E
                        CPUInstructions.res(2, 4)

                    0x94 ->  //RES 2,H
                        CPUInstructions.res(2, 6)

                    0x95 ->  //RES 2,L
                        CPUInstructions.res(2, 7)

                    0x96 ->  //RES 2,(HL)
                        CPUInstructions.res(2, 8)

                    0x97 ->  //RES 2,A
                        CPUInstructions.res(2, 0)

                    0x98 ->  //RES 3,B
                        CPUInstructions.res(3, 1)

                    0x99 ->  //RES 3,C
                        CPUInstructions.res(3, 2)

                    0x9A ->  //RES 3,D
                        CPUInstructions.res(3, 3)

                    0x9B ->  //RES 3,E
                        CPUInstructions.res(3, 4)

                    0x9C ->  //RES 3,H
                        CPUInstructions.res(3, 6)

                    0x9D ->  //RES 3,L
                        CPUInstructions.res(3, 7)

                    0x9E ->  //RES 3,(HL)
                        CPUInstructions.res(3, 8)

                    0x9F ->  //RES 3,A
                        CPUInstructions.res(3, 0)

                    0xA0 ->  //RES 4,B
                        CPUInstructions.res(4, 1)

                    0xA1 ->  //RES 4,C
                        CPUInstructions.res(4, 2)

                    0xA2 ->  //RES 4,D
                        CPUInstructions.res(4, 3)

                    0xA3 ->  //RES 4,E
                        CPUInstructions.res(4, 4)

                    0xA4 ->  //RES 4,H
                        CPUInstructions.res(4, 6)

                    0xA5 ->  //RES 4,L
                        CPUInstructions.res(4, 7)

                    0xA6 ->  //RES 4,(HL)
                        CPUInstructions.res(4, 8)

                    0xA7 ->  //RES 4,A
                        CPUInstructions.res(4, 0)

                    0xA8 ->  //RES 5,B
                        CPUInstructions.res(5, 1)

                    0xA9 ->  //RES 5,C
                        CPUInstructions.res(5, 2)

                    0xAA ->  //RES 5,D
                        CPUInstructions.res(5, 3)

                    0xAB ->  //RES 5,E
                        CPUInstructions.res(5, 4)

                    0xAC ->  //RES 5,H
                        CPUInstructions.res(5, 6)

                    0xAD ->  //RES 5,L
                        CPUInstructions.res(5, 7)

                    0xAE ->  //RES 5,(HL)
                        CPUInstructions.res(5, 8)

                    0xAF ->  //RES 5,A
                        CPUInstructions.res(5, 0)

                    0xB0 ->  //RES 6,B
                        CPUInstructions.res(6, 1)

                    0xB1 ->  //RES 6,C
                        CPUInstructions.res(6, 2)

                    0xB2 ->  //RES 6,D
                        CPUInstructions.res(6, 3)

                    0xB3 ->  //RES 6,E
                        CPUInstructions.res(6, 4)

                    0xB4 ->  //RES 6,H
                        CPUInstructions.res(6, 6)

                    0xB5 ->  //RES 6,L
                        CPUInstructions.res(6, 7)

                    0xB6 ->  //RES 6,(HL)
                        CPUInstructions.res(6, 8)

                    0xB7 ->  //RES 6,A
                        CPUInstructions.res(6, 0)

                    0xB8 ->  //RES 7,B
                        CPUInstructions.res(7, 1)

                    0xB9 ->  //RES 7,C
                        CPUInstructions.res(7, 2)

                    0xBA ->  //RES 7,D
                        CPUInstructions.res(7, 3)

                    0xBB ->  //RES 7,E
                        CPUInstructions.res(7, 4)

                    0xBC ->  //RES 7,H
                        CPUInstructions.res(7, 6)

                    0xBD ->  //RES 7,L
                        CPUInstructions.res(7, 7)

                    0xBE ->  //RES 7,(HL)
                        CPUInstructions.res(7, 8)

                    0xBF ->  //RES 7,A
                        CPUInstructions.res(7, 0)

                    0xC0 ->  //SET 0,B
                        CPUInstructions.set(0, 1)

                    0xC1 ->  //SET 0,C
                        CPUInstructions.set(0, 2)

                    0xC2 ->  //SET 0,D
                        CPUInstructions.set(0, 3)

                    0xC3 ->  //SET 0,E
                        CPUInstructions.set(0, 4)

                    0xC4 ->  //SET 0,H
                        CPUInstructions.set(0, 6)

                    0xC5 ->  //SET 0,L
                        CPUInstructions.set(0, 7)

                    0xC6 ->  //SET 0,(HL)
                        CPUInstructions.set(0, 8)

                    0xC7 ->  //SET 0,A
                        CPUInstructions.set(0, 0)

                    0xC8 ->  //SET 1,B
                        CPUInstructions.set(1, 1)

                    0xC9 ->  //SET 1,C
                        CPUInstructions.set(1, 2)

                    0xCA ->  //SET 1,D
                        CPUInstructions.set(1, 3)

                    0xCB ->  //SET 1,E
                        CPUInstructions.set(1, 4)

                    0xCC ->  //SET 1,H
                        CPUInstructions.set(1, 6)

                    0xCD ->  //SET 1,L
                        CPUInstructions.set(1, 7)

                    0xCE ->  //SET 1,(HL)
                        CPUInstructions.set(1, 8)

                    0xCF ->  //SET 1,A
                        CPUInstructions.set(1, 0)

                    0xD0 ->  //SET 2,B
                        CPUInstructions.set(2, 1)

                    0xD1 ->  //SET 2,C
                        CPUInstructions.set(2, 2)

                    0xD2 ->  //SET 2,D
                        CPUInstructions.set(2, 3)

                    0xD3 ->  //SET 2,E
                        CPUInstructions.set(2, 4)

                    0xD4 ->  //SET 2,H
                        CPUInstructions.set(2, 6)

                    0xD5 ->  //SET 2,L
                        CPUInstructions.set(2, 7)

                    0xD6 ->  //SET 2,(HL)
                        CPUInstructions.set(2, 8)

                    0xD7 ->  //SET 2,A
                        CPUInstructions.set(2, 0)

                    0xD8 ->  //SET 3,B
                        CPUInstructions.set(3, 1)

                    0xD9 ->  //SET 3,C
                        CPUInstructions.set(3, 2)

                    0xDA ->  //SET 3,D
                        CPUInstructions.set(3, 3)

                    0xDB ->  //SET 3,E
                        CPUInstructions.set(3, 4)

                    0xDC ->  //SET 3,H
                        CPUInstructions.set(3, 6)

                    0xDD ->  //SET 3,L
                        CPUInstructions.set(3, 7)

                    0xDE ->  //SET 3,(HL)
                        CPUInstructions.set(3, 8)

                    0xDF ->  //SET 3,A
                        CPUInstructions.set(3, 0)

                    0xE0 ->  //SET 4,B
                        CPUInstructions.set(4, 1)

                    0xE1 ->  //SET 4,C
                        CPUInstructions.set(4, 2)

                    0xE2 ->  //SET 4,D
                        CPUInstructions.set(4, 3)

                    0xE3 ->  //SET 4,E
                        CPUInstructions.set(4, 4)

                    0xE4 ->  //SET 4,H
                        CPUInstructions.set(4, 6)

                    0xE5 ->  //SET 4,L
                        CPUInstructions.set(4, 7)

                    0xE6 ->  //SET 4,(HL)
                        CPUInstructions.set(4, 8)

                    0xE7 ->  //SET 4,A
                        CPUInstructions.set(4, 0)

                    0xE8 ->  //SET 5,B
                        CPUInstructions.set(5, 1)

                    0xE9 ->  //SET 5,C
                        CPUInstructions.set(5, 2)

                    0xEA ->  //SET 5,D
                        CPUInstructions.set(5, 3)

                    0xEB ->  //SET 5,E
                        CPUInstructions.set(5, 4)

                    0xEC ->  //SET 5,H
                        CPUInstructions.set(5, 6)

                    0xED ->  //SET 5,L
                        CPUInstructions.set(5, 7)

                    0xEE ->  //SET 5,(HL)
                        CPUInstructions.set(5, 8)

                    0xEF ->  //SET 5,A
                        CPUInstructions.set(5, 0)

                    0xF0 ->  //SET 6,B
                        CPUInstructions.set(6, 1)

                    0xF1 ->  //SET 6,C
                        CPUInstructions.set(6, 2)

                    0xF2 ->  //SET 6,D
                        CPUInstructions.set(6, 3)

                    0xF3 ->  //SET 6,E
                        CPUInstructions.set(6, 4)

                    0xF4 ->  //SET 6,H
                        CPUInstructions.set(6, 6)

                    0xF5 ->  //SET 6,L
                        CPUInstructions.set(6, 7)

                    0xF6 ->  //SET 6,(HL)
                        CPUInstructions.set(6, 8)

                    0xF7 ->  //SET 6,A
                        CPUInstructions.set(6, 0)

                    0xF8 ->  //SET 7,B
                        CPUInstructions.set(7, 1)

                    0xF9 ->  //SET 7,C
                        CPUInstructions.set(7, 2)

                    0xFA ->  //SET 7,D
                        CPUInstructions.set(7, 3)

                    0xFB ->  //SET 7,E
                        CPUInstructions.set(7, 4)

                    0xFC ->  //SET 7,H
                        CPUInstructions.set(7, 6)

                    0xFD ->  //SET 7,L
                        CPUInstructions.set(7, 7)

                    0xFE ->  //SET 7,(HL)
                        CPUInstructions.set(7, 8)

                    0xFF ->  //SET 7,A
                        CPUInstructions.set(7, 0)
                }
            }

            0xCC ->  //CALL Z,nn
                CPUInstructions.callCond(1)

            0xCD ->  //CALL u16
                CPUInstructions.call()

            0xCE ->  //ADC A,#
                CPUInstructions.adc(9)

            0xCF ->  //RST 08H
                CPUInstructions.rst(1)

            0xD0 ->  //RET NC
                CPUInstructions.retCond(2)

            0xD1 ->  //POP DE
                CPUInstructions.pop(2)

            0xD2 ->  //JP NC,u16
                CPUInstructions.jpCond(2)

            0xD4 ->  //CALL NC,nn
                CPUInstructions.callCond(2)

            0xD5 ->  //PUSH DE
                CPUInstructions.push(2)

            0xD6 ->  //SUB A, #
                CPUInstructions.sub(9)

            0xD7 ->  //RST 10H
                CPUInstructions.rst(2)

            0xD8 ->  //RET C
                CPUInstructions.retCond(3)

            0xD9 ->  //RETI
                CPUInstructions.reti()

            0xDA ->  //JP C,u16
                CPUInstructions.jpCond(3)

            0xDC ->  //CALL C,nn
                CPUInstructions.callCond(3)

            0xDE ->  //SBC A,#
                CPUInstructions.sbc(9)

            0xDF ->  //RST 18H
                CPUInstructions.rst(3)

            0xE0 ->  //LD (FF00+u8),A
                CPUInstructions.ldh(0)

            0xE1 ->  //POP nn
                CPUInstructions.pop(3)

            0xE2 ->  //LD (C), A
                CPUInstructions.ldAC(1)

            0xE5 ->  //PUSH HL
                CPUInstructions.push(3)

            0xE6 ->  //AND #
                CPUInstructions.and(9)

            0xE7 ->  //RST 20H
                CPUInstructions.rst(4)

            0xE8 ->  //ADD SP,n
                CPUInstructions.addSP()

            0xE9 ->  //JP (HL)
                CPUInstructions.jpHL()

            0xEA ->  //LD (nn),A
                CPUInstructions.ldTwoRegisters(2)

            0xEE ->  //XOR #
                CPUInstructions.xor(9)

            0xEF ->  //RST 28H
                CPUInstructions.rst(5)

            0xF0 ->  //LD A,(FF00+u8)
                CPUInstructions.ldh(1)

            0xF1 ->  //POP AF
                CPUInstructions.pop(0)

            0xF2 ->  //LD A,(C)
                CPUInstructions.ldAC(0)

            0xF3 ->  //DI
                CPUInstructions.di()

            0xF5 ->  //PUSH AF
                CPUInstructions.push(0)

            0xF6 ->  //OR #
                CPUInstructions.or(9)

            0xF7 ->  //RST 30H
                CPUInstructions.rst(6)

            0xF8 ->  //LDHL SP,n
                CPUInstructions.LDHL()

            0xF9 ->  //LD SP,HL
                CPUInstructions.ldSPHL()

            0xFA ->  //LD A,(nn)
                CPUInstructions.ldTwoRegistersIntoA(2)

            0xFB ->  //EI
                CPUInstructions.ei()

            0xFE ->  //CP A,u8
                CPUInstructions.cp(9)

            0xFF ->  //RST 38H
                CPUInstructions.rst(7)

            else -> {
                println("No OPCode or Lacks Implementation")
                System.exit(0)
            }
        }
    }
}