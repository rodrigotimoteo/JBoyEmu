package com.github.rodrigotimoteo.kboyemucore.cpu

import com.github.rodrigotimoteo.kboyemucore.DisplayFrame
import com.github.rodrigotimoteo.kboyemucore.memory.Memory
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU
import java.io.IOException
import java.io.PrintStream
import java.util.Arrays

class OldCPU {
//    private val TIMER_INTERRUPT = 2
//
//    var registers: CharArray = CharArray(8) //AF, BC, DE and HL can be 16 bits if paired together
//    private var zeroFlag = false
//    private var subtractFlag = false
//    private var halfCarryFlag = false
//    private var carryFlag = false
//
//    private var isHalted = false
//    private var isStopped = false
//    private var timerEnabled = false
//    private var handleOverflow = false
//
//    private var operationCode = 0.toChar()
//    private var programCounter = 0x100.toChar()
//    private var stackPointer = 0xFFFE.toChar()
//
//    private var cgb = false
//
//    private var counter = 0
//    private var haltCounter = 0
//    private var divClockCounter = 0
//    private var totalDiv = 0
//    private var timerClockCounter = 0
//    private var interruptCounter = 0
//    private var timerFrequency = 256
//
//    private val lastTimed = 0
//
//    private var interruptMasterEnable = false
//    private var setChangeTo = false
//    private var changeInterrupt = false
//    private var haltBug = false
//
//    private val debugText = true
//    var debug: PrintStream? = null
//
//    var memory: Memory? = null
//    var ppu: PPU? = null
//    var displayFrame: DisplayFrame? = null
//
//
//    //Resets
//    //Clears all register filling them with 0's
//    private fun clearRegisters() {
//        Arrays.fill(registers, 0.toChar())
//    }
//
//
//    //Getters
//    //Return the main.kotlin.CPU Cycle counter
//    fun getCounter(): Int {
//        return counter
//    }
//
//    //Return Register[index]
//    fun getRegister(index: Int): Char {
//        return (registers[index].code and 0xff).toChar()
//    }
//
//    //Return the Program Counter
//    fun getProgramCounter(): Char {
//        return programCounter
//    }
//
//    //Return the Stack Pointer address
//    fun getStackPointer(): Char {
//        return stackPointer
//    }
//
//    //Return the Status of the Zero Flag
//    fun getZeroFlag(): Boolean {
//        return zeroFlag
//    }
//
//    //Return the Status of Subtract Flag
//    fun getSubtractFlag(): Boolean {
//        return subtractFlag
//    }
//
//    //Return the Status of the Half Carry Flag
//    fun getHalfCarryFlag(): Boolean {
//        return halfCarryFlag
//    }
//
//    //Return the Status of the Carry Flag
//    fun getCarryFlag(): Boolean {
//        return carryFlag
//    }
//
//    //Return the Halted Status
//    fun getIsHalted(): Boolean {
//        return isHalted
//    }
//
//    //Return the Stopped Status
//    fun getIsStopped(): Boolean {
//        return isStopped
//    }
//
//    //Return the main.kotlin.PPU
//    fun getPPU(): PPU {
//        return ppu!!
//    }
//
//    fun getLastTimed(): Int {
//        return lastTimed
//    }
//
//
//    //Setters (for registers mainly)
//    //Sets Register[index] with a Value
//    fun setRegister(index: Int, value: Char) {
//        registers[index] = value
//    }
//
//    fun setCgbMode() {
//        cgb = true
//
//        ppu!!.setCgbMode()
//    }
//
//
//    //Increments a value to the main.kotlin.CPU Cycle Counter
//    fun increaseCounter(amount: Int) {
//        counter += amount
//    }
//
//    //Increments a value to the Program Counter
//    fun increaseProgramCounter(amount: Int) {
//        programCounter += amount.toChar().code
//    }
//
//    //Sets the Program Counter to a Value
//    fun setProgramCounter(amount: Char) {
//        programCounter = amount
//    }
//
//    //Increments a value to the Stack Pointer
//    fun increaseStackPointer(amount: Int) {
//        stackPointer += amount.toChar().code
//    }
//
//    //Sets the Stack Pointer to a Value
//    fun setStackPointer(amount: Char) {
//        stackPointer = amount
//    }
//
//    //Sets the Zero Flag to a State
//    fun setZeroFlag(state: Boolean) {
//        zeroFlag = state
//    }
//
//    //Sets Subtract Flag to a State
//    fun setSubtractFlag(state: Boolean) {
//        subtractFlag = state
//    }
//
//    //Sets the Half Carry Flag to a State
//    fun setHalfCarryFlag(state: Boolean) {
//        halfCarryFlag = state
//    }
//
//    //Sets the Carry Flag to a State
//    fun setCarryFlag(state: Boolean) {
//        carryFlag = state
//    }
//
//    //Set the isHalted Flag
//    fun setIsHalted(state: Boolean) {
//        isHalted = state
//    }
//
//    //Set the isStopped Flag
//    fun setIsStopped(state: Boolean) {
//        isStopped = state
//    }
//
//    //Set the signal to change the IME flag
//    fun setChangeInterrupt(state: Boolean) {
//        changeInterrupt = state
//    }
//
//    //Used to enable interrupt after a cycle
//    fun setInterruptCounter(value: Int) {
//        interruptCounter = value
//    }
//
//    //Set the state to change the IME flag to
//    fun setChangeTo(state: Boolean) {
//        setChangeTo = state
//    }
//
//    //Sets the state of the timer enabled/disabled
//    fun setTimerEnabled(state: Boolean) {
//        timerEnabled = state
//    }
//
//    fun setTimerFrequency(frequency: Int) {
//        timerFrequency = frequency
//    }
//
//    fun setDivClockCounter(value: Int) {
//        divClockCounter = value
//    }
//
//    fun setHaltCounter(value: Int) {
//        haltCounter = value
//    }
//
//
//    //Constructor
//    @Throws(IOException::class)
//    fun CPU() {
//        clearRegisters()
//        memory = Memory(this)
//        ppu = PPU(memory, this)
//        displayFrame = DisplayFrame(memory, ppu, this)
//        memory!!.setDisplayFrame(displayFrame)
//
//        //        if(debugText) {
////            debug = new PrintStream("A.txt");
////            PrintStream console = System.out;
////            System.setOut(debug);
////        }
//        CPUInstructions.setCpu(this)
//        CPUInstructions.setMem(memory)
//
//        //memory.dumpMemory();
//        init()
//    }
//
//    private fun init() {
//        registers[0] = 0x01.toChar()
//        registers[2] = 0x13.toChar()
//        registers[4] = 0xD8.toChar()
//        registers[5] = 0xB0.toChar()
//        registers[6] = 0x01.toChar()
//        registers[7] = 0x4D.toChar()
//
//        zeroFlag = true
//        subtractFlag = false
//        halfCarryFlag = true
//        carryFlag = true
//    }
//
//    fun reset() {
//        memory!!.reset()
//        ppu!!.reset()
//        programCounter = 0x100.toChar()
//        stackPointer = 0xfffe.toChar()
//        counter = 0
//        init()
//    }
//
//    fun computeFlags() {
//        zeroFlag = (registers[5].code and 0x80) != 0
//        subtractFlag = (registers[5].code and 0x40) != 0
//        halfCarryFlag = (registers[5].code and 0x20) != 0
//        carryFlag = (registers[5].code and 0x10) != 0
//    }
//
//    fun computeFRegister() {
//        registers[5] = 0.toChar()
//        if (zeroFlag) registers[5] = (registers[5].code or 0x80).toChar()
//        if (subtractFlag) registers[5] = (registers[5].code or 0x40).toChar()
//        if (halfCarryFlag) registers[5] = (registers[5].code or 0x20).toChar()
//        if (carryFlag) registers[5] = (registers[5].code or 0x10).toChar()
//    }
//
//    fun saveState(): Array<Any?> {
//        val state = arrayOfNulls<Any>(3)
//        //        state[0] = saveStateCPU();
//        return state
//    }
//
//
//    //    private main.kotlin.CPU saveStateCPU() {
//    //saveStates[state].registers = Arrays.copyOf(registers, registers.length);
//    //        saveStates[state].zeroFlag = zeroFlag;
//    //        saveStates[state].subtractFlag = subtractFlag;
//    //        saveStates[state].halfCarryFlag = halfCarryFlag;
//    //        saveStates[state].carryFlag = carryFlag;
//    //
//    //        saveStates[state].isHalted = isHalted;
//    //        saveStates[state].isStopped = isStopped;
//    //        saveStates[state].timerEnabled = timerEnabled;
//    //        saveStates[state].handleOverflow = handleOverflow;
//    //
//    //        saveStates[state].operationCode = operationCode;
//    //        saveStates[state].programCounter = programCounter;
//    //        saveStates[state].stackPointer = stackPointer;
//    //
//    //        saveStates[state].counter = counter;
//    //        saveStates[state].haltCounter = haltCounter;
//    //        saveStates[state].divClockCounter = divClockCounter;
//    //        saveStates[state].timerClockCounter = timerClockCounter;
//    //        saveStates[state].interruptCounter = interruptCounter;
//    //        saveStates[state].timerFrequency = timerFrequency;
//    //
//    //        saveStates[state].interruptMasterEnable = interruptMasterEnable;
//    //        saveStates[state].setChangeTo = setChangeTo;
//    //        saveStates[state].changeInterrupt = changeInterrupt;
//    //        saveStates[state].haltBug = haltBug;
//    //    }
//    //
//    //    public void loadState(main.kotlin.CPU cpu, main.kotlin.Memory memory, main.kotlin.PPU ppu) {
//    //        loadState(cpu);
//    //        memory.loadState(memory);
//    //        ppu.loadState(ppu);
//    //    }
//    private fun loadState(cpu: OldCPU) {
//        this.registers = cpu.registers.copyOf(registers.size)
//        this.zeroFlag = cpu.zeroFlag
//        this.subtractFlag = cpu.subtractFlag
//        this.halfCarryFlag = cpu.halfCarryFlag
//        this.carryFlag = cpu.carryFlag
//
//        this.isHalted = cpu.isHalted
//        this.isStopped = cpu.isStopped
//        this.timerEnabled = cpu.timerEnabled
//        this.handleOverflow = cpu.handleOverflow
//
//        this.operationCode = cpu.operationCode
//        this.programCounter = cpu.programCounter
//        this.stackPointer = cpu.stackPointer
//
//        this.counter = cpu.counter
//        this.haltCounter = cpu.haltCounter
//        this.divClockCounter = cpu.divClockCounter
//        this.timerClockCounter = cpu.timerClockCounter
//        this.interruptCounter = cpu.interruptCounter
//        this.timerFrequency = cpu.timerFrequency
//
//        this.interruptMasterEnable = cpu.interruptMasterEnable
//        this.setChangeTo = cpu.setChangeTo
//        this.changeInterrupt = cpu.changeInterrupt
//        this.haltBug = cpu.haltBug
//    }
//
//    @Throws(InterruptedException::class)
//    fun cycle() {
////        if(counter >= 0x10000) {
////            memory.dumpMemory();
////            System.exit(0);
////        }
//
////        main.kotlin.CPUInstructions.show();
////        main.kotlin.CPUInstructions.dumpRegisters();
////        if(counter >= 0x10000)
////            System.exit(0);
//
//        if (!getIsStopped()) {
//            if (!getIsHalted()) {
//                fetchOperationCodes()
//                decodeOperationCodes()
//                if (changeInterrupt && interruptCounter < counter) {
//                    interruptMasterEnable = setChangeTo
//                    changeInterrupt = false
//                }
//            } else {
//                handleCPUTimers()
//            }
//
//            handleInterrupts()
//        }
//    }
//
//    fun setInterrupt(interrupt: Int) {
//        when (interrupt) {
//            0 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 0)
//            1 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 1)
//            2 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 2)
//            3 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 3)
//            4 -> memory!!.setBit(ReservedAddresses.IF.memoryAddress, 4)
//        }
//    }
//
//    private fun handleInterrupts() {
//        if (interruptMasterEnable) {
//            val interrupt = (memory!!.getMemory(ReservedAddresses.IF.memoryAddress).code and memory!!.getMemory(ReservedAddresses.IE.memoryAddress).code).toChar()
//            if (interrupt.code > 0) {
//                if (isHalted) setIsHalted(false)
//
//                interruptMasterEnable = false
//                memory!!.storeWordInSP(stackPointer.code, programCounter.code)
//
//                val vBlank = interrupt.code and 0x1
//                if (vBlank == 1) {
//                    setProgramCounter(0x40.toChar())
//                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 0)
//                    return
//                }
//
//                val LCDCStatus = (interrupt.code and 0x2) shr 1
//                if (LCDCStatus == 1) {
//                    setProgramCounter(0x48.toChar())
//                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 1)
//                    return
//                }
//
//                val timerOverflow = (interrupt.code and 0x4) shr 2
//                if (timerOverflow == 1) {
//                    setProgramCounter(0x50.toChar())
//                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 2)
//                    return
//                }
//
//                val serialTransfer = (interrupt.code and 0x8) shr 3
//                if (serialTransfer == 1) {
//                    setProgramCounter(0x58.toChar())
//                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 3)
//                    return
//                }
//
//                val hiLo = (interrupt.code and 0x10) shr 4
//                if (hiLo == 1) {
//                    if (isStopped) {
//                        isStopped = false
//                    }
//                    setProgramCounter(0x60.toChar())
//                    memory!!.resetBit(ReservedAddresses.IF.memoryAddress, 4)
//                }
//            }
//        } else if (isHalted) {
//            if ((memory!!.getMemory(ReservedAddresses.IF.memoryAddress).code and memory!!.getMemory(ReservedAddresses.IE.memoryAddress).code and 0x1f) > 0) {
//                isHalted = false
//                if (haltCounter == counter) haltBug = true
//            }
//        }
//    }
//
//    fun resetClocks() {
//        val divUsed = timerFrequency == 256
//
//        if (divUsed && timerEnabled) {
//            if (memory!!.testBit(ReservedAddresses.DIV.memoryAddress, 1)) {
//                memory!!.setMemory(
//                    ReservedAddresses.TIMA.memoryAddress,
//                    (memory!!.getMemory(ReservedAddresses.TIMA.memoryAddress).code + 1).toChar()
//                )
//            }
//        } else if (timerEnabled) {
//            if (totalDiv == 0) memory!!.setMemory(
//                ReservedAddresses.TIMA.memoryAddress,
//                (memory!!.getMemory(ReservedAddresses.TIMA.memoryAddress).code + 1).toChar()
//            )
//        }
//
//        divClockCounter = 0
//        totalDiv = 0
//        timerClockCounter = 0
//    }
//
//    fun handleCPUTimers() {
//        increaseCounter(1)
//
//        handleDividerTimer()
//        handleTimer()
////        System.out.println(divClockCounter + " " + timerClockCounter);
//    }
//
//    private fun handleDividerTimer() {
//        divClockCounter++
//        totalDiv++
//        while (divClockCounter >= 64) {
//            divClockCounter -= 64
//            var div_counter = memory!!.getMemory(ReservedAddresses.DIV.memoryAddress).code
//            div_counter = (div_counter + 1) and 0xff
//            memory!!.writePriv(ReservedAddresses.DIV.memoryAddress, div_counter.toChar())
//        }
//        if (totalDiv >= timerFrequency) totalDiv = 0
//    }
//
//    private fun handleTimer() {
//        CPUInstructions.readTAC()
//        if (handleOverflow) {
//            memory!!.setMemory(ReservedAddresses.TIMA.memoryAddress, memory!!.getMemory(
//                ReservedAddresses.TMA.memoryAddress))
//            setInterrupt(TIMER_INTERRUPT)
//            handleOverflow = false
//        }
//        if (timerEnabled) {
//            timerClockCounter++
//            while (timerClockCounter >= timerFrequency) {
//                timerClockCounter -= timerFrequency
//                if (memory!!.getMemory(ReservedAddresses.TIMA.memoryAddress).code == 0xff) {
//                    handleOverflow = true
//                } else {
//                    memory!!.setMemory(
//                        ReservedAddresses.TIMA.memoryAddress,
//                        (((memory!!.getMemory(ReservedAddresses.TIMA.memoryAddress).code and 0xff) + 1) and 0xff).toChar()
//                    )
//                }
//            }
//        }
//    }
}
