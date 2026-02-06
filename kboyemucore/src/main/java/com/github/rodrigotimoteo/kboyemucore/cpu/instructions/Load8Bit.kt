package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.RegisterNames

/**
 * Class responsible for handling all things that deal with 8 bit load operations in CPU instruction
 * set
 *
 * @param cpu used for cpu communication
 * @param bus used for communication with other parts in this case accessing memory
 *
 * @author rodrigotimoteo
 **/
class Load8Bit(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * This operation assigns the value stored in register A to the memory at the location of a 16-bit
     * Register (combination of register)
     *
     * @param memoryAddress which memory address to use
     */
    fun ldTwoRegisters(memoryAddress: Int) {
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value

        cpu.timers.tick()
        bus.setValue(memoryAddress, valueInRegisterA)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * This operation assigns the value stored in register A to the memory at the location given by
     * the word next to the program counter in memory
     */
    fun ldNN() {
        val memoryAddress = bus.calculateNN()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value

        cpu.timers.tick()
        bus.setValue(memoryAddress, valueInRegisterA)
        cpu.cpuRegisters.incrementProgramCounter(3)
    }

    /**
     * Puts the value n (retrieved from the memory address of a 16-bit register value) into A
     *
     * @param memoryAddress which memory address to use
     */
    fun ldTwoRegistersIntoA(memoryAddress: Int) {
        val valueAtAddress = bus.getValue(memoryAddress)

        cpu.timers.tick()
        cpu.cpuRegisters.setRegister(RegisterNames.A, valueAtAddress)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Puts the value nn (retrieved by getting the two values next to the program counter and
     * retrieving the value at the given address) into A
     */
    fun ldNNIntoA() {
        val memoryAddress = bus.calculateNN()
        val valueAtAddress = bus.getValue(memoryAddress)

        cpu.timers.tick()
        cpu.cpuRegisters.setRegister(RegisterNames.A, valueAtAddress)
        cpu.cpuRegisters.incrementProgramCounter(3)
    }

    /**
     * Stores the value given by the immediate word next to the program counter into the given register
     *
     * @param register where to store immediate word
     */
    fun ldNRegister(register: RegisterNames) {
        val programCounter = cpu.cpuRegisters.getProgramCounter()
        val immediateWord = bus.getValue(programCounter + 1)

        cpu.timers.tick()
        cpu.cpuRegisters.setRegister(register, immediateWord)
        cpu.cpuRegisters.incrementProgramCounter(2)
    }

    /**
     * Stores the value given by the immediate word next to the program counter into the memory
     * address given by the HL register aggregation
     */
    fun ldNHL() {
        repeat(2) { cpu.timers.tick() }

        val programCounter = cpu.cpuRegisters.getProgramCounter()
        val valueInHL = cpu.cpuRegisters.getHL()
        val value = bus.getValue(programCounter + 1)

        bus.setValue(valueInHL, value)
        cpu.cpuRegisters.incrementProgramCounter(2)
    }

    /**
     * Loads the value in one register into another
     *
     * @param registerIn to receive a new value
     * @param registerOut input register
     */
    fun ld(registerIn: RegisterNames, registerOut: RegisterNames) {
        val value = cpu.cpuRegisters.getRegister(registerOut).value

        cpu.cpuRegisters.setRegister(registerIn, value)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Assigns a value contained in the memory address given by the HL register to a given register
     *
     * @param register to receive new value
     */
    fun ldHLtoRegister(register: RegisterNames) {
        val valueInHL = cpu.cpuRegisters.getHL()

        cpu.timers.tick()
        cpu.cpuRegisters.setRegister(register, bus.getValue(valueInHL))
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Assigns a value contained in a register to the memory address given by the HL address
     *
     * @param register to be used as input value
     */
    fun ldRtoHL(register: RegisterNames) {
        val value = cpu.cpuRegisters.getRegister(register).value
        val valueInHL = cpu.cpuRegisters.getHL()

        cpu.timers.tick()
        bus.setValue(valueInHL, value)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Assigns a value to A to given word (starting from address 0xFF00 added the value of register C)
     * or vice versa
     *
     * @param aIntoC defines whether the value should be assigned to the A register false or otherwise true
     */
    fun ldAC(aIntoC: Boolean) {
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value
        val valueInRegisterC = cpu.cpuRegisters.getRegister(RegisterNames.C).value.toInt()
        val memoryAddress = 0xFF00 + valueInRegisterC

        if (aIntoC) {
            bus.setValue(memoryAddress, valueInRegisterA)
        } else {
            cpu.cpuRegisters.setRegister(RegisterNames.A, bus.getValue(memoryAddress))
        }

        cpu.timers.tick()
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Loads the value from A into the address given by HL or vice versa as well as lowering HL value by one
     *
     * @param aIntoC sets whether register A should be used as input or has the receiver
     */
    fun ldd(aIntoC: Boolean) {
        val valueInHL = cpu.cpuRegisters.getHL()

        if (aIntoC) ldTwoRegisters(valueInHL)
        else ldTwoRegistersIntoA(valueInHL)

        val finalValueInHL = (valueInHL - 1) and 0xFFFF
        cpu.cpuRegisters.setHL(finalValueInHL)
    }

    /**
     * Loads the value from A into the address given by HL or vice versa as well as incrementing HL
     * value by one
     *
     * @param aIntoC sets whether register A should be used as input true or as the receiver false
     */
    fun ldi(aIntoC: Boolean) {
        val valueInHL = cpu.cpuRegisters.getHL()

        if (aIntoC) ldTwoRegisters(valueInHL)
        else ldTwoRegistersIntoA(valueInHL)

        val finalValueInHL = (valueInHL + 1) and 0xFFFF
        cpu.cpuRegisters.setHL(finalValueInHL)
    }

    /**
     * Puts register A's value into memory address 0xFF00 plus the immediate word after the program
     * counter or vice-versa
     *
     * @param isInput sets whether register A should be used as input true or as the receiver false
     */
    fun ldh(isInput: Boolean) {
        repeat(2) { cpu.timers.tick() }

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value
        val programCounter = cpu.cpuRegisters.getProgramCounter()
        val valueN = bus.getValue(programCounter + 1).toInt()
        val memoryAddress = 0xFF00 + valueN

        if (isInput) {
            bus.setValue(memoryAddress, valueInRegisterA)
        } else {
            cpu.cpuRegisters.setRegister(RegisterNames.A, bus.getValue(memoryAddress))
        }

        cpu.cpuRegisters.incrementProgramCounter(2)
    }
}
