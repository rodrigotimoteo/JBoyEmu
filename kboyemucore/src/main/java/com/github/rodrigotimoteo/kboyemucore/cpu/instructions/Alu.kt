package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.RegisterNames

/**
 * Class responsible for handling all things that deal with arithmetic operations
 *
 * @param cpu used for cpu communication
 * @param bus used for communication with other parts in this case accessing memory
 *
 * @author rodrigotimoteo
 **/
@Suppress("TooManyFunctions")
class Alu(
    private val cpu: CPU,
    private val bus: Bus
) {
    /**
     * Checks whether the zero flag should be set or reset
     *
     * @param value to check
     * @return status of zero flag
     */
    private fun checkZero(value: Int): Boolean = (value and 0xFF) == 0x00

    /**
     * Checks if the half carry flag will be set or reset based on the values used in the additions.
     *
     * @param value1 first value used
     * @param value2 second value used
     * @param carry this is either 1 if carry flag was set or 0 otherwise
     * @return status of half carry flag
     */
    private fun checkHalfCarryAdd(
        value1: Int,
        value2: Int,
        carry: Int
    ): Boolean = (((value1 and 0x0F) + (value2 and 0xF) + carry) and 0x10) == 0x10


    /**
     * Checks if the carry flag will be set or reset based on the values used in additions.
     *
     * @param value to check
     * @return status of carry flag
     */
    private fun checkCarryAdd(value: Int): Boolean = value > 0xFF

    /**
     * Checks if the half carry flag will be set or reset based on the values used in the subtractions.
     *
     * @param value1 first value used
     * @param value2 second value used
     * @param carry this is either 1 if a carry flag was set or 0 otherwise
     * @return status of half carry flag
     */
    private fun checkHalfCarrySub(
        value1: Int,
        value2: Int,
        carry: Int
    ): Boolean = ((value1 and 0x0F) - (value2 and 0x0F) - carry) < 0

    /**
     * Checks if the carry flag will be set or reset based on the values used in subtractions.
     *
     * @param value to check
     * @return status of carry flag
     */
    private fun checkCarrySub(value: Int): Boolean = value < 0

    /**
     * Performs the add operation from any given value stored in a register to register A
     *
     * @param register used to retrieve the value to add
     */
    fun add(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val finalValue = valueInGivenRegister + valueInRegisterA

        val halfCarry = checkHalfCarryAdd(valueInGivenRegister, valueInRegisterA, 0)
        val carry = checkCarryAdd(finalValue)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = halfCarry, carry = carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Performs the add operation from any given value stored in a register to register A
     *
     * @param memoryAddress used to retrieve the value to add
     * @param useHL if should retrieve from HL register
     */
    fun addSpecial(memoryAddress: Int, useHL: Boolean) {
        cpu.timers.tick()

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val valueInAddress = bus.getValue(memoryAddress).toInt()
        val finalValue = valueInAddress + valueInRegisterA

        val halfCarry = checkHalfCarryAdd(valueInAddress, valueInRegisterA, 0)
        val carry = checkCarryAdd(finalValue)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = halfCarry, carry = carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        if (useHL) {
            cpu.cpuRegisters.incrementProgramCounter(1)
        } else {
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Performs the operations of adding any given value (in this case only the ones contained inside registers) to A
     * also adding the carry flag status (1 if true 0 otherwise)
     *
     * @param register used to retrieve the register to add to register A's value
     */
    fun adc(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()

        val carryAsValue = if (cpu.cpuRegisters.flags.getCarryFlag()) 1 else 0
        val finalValue = valueInGivenRegister + valueInRegisterA + carryAsValue

        val halfCarry = checkHalfCarryAdd(valueInGivenRegister, valueInRegisterA, carryAsValue)
        val carry = checkCarryAdd(finalValue)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = halfCarry, carry = carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Performs the operations of adding any given value (in this case, the value is contained in memory) to A also
     * adding the carry flag status (1 if true 0 otherwise). This can be using register address (1 mCycles) or given by
     * the memory directly (2 mCycles)
     *
     * @param memoryAddress used to retrieve the value to add to register A's value
     * @param useHL      if HL is being used or not
     */
    fun adcSpecial(memoryAddress: Int, useHL: Boolean) {
        cpu.timers.tick()

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val valueInAddress = bus.getValue(memoryAddress).toInt()

        val carryAsValue = if (cpu.cpuRegisters.flags.getCarryFlag()) 1 else 0
        val halfCarry = checkHalfCarryAdd(valueInAddress, valueInRegisterA, carryAsValue)
        val finalValue = valueInAddress + valueInRegisterA + carryAsValue

        val carry = checkCarryAdd(finalValue)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = halfCarry, carry = carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        if (useHL) {
            cpu.cpuRegisters.incrementProgramCounter(1)
        } else {
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Subtracts the given value inside the register from register A's value
     *
     * @param register which register to use
     */
    fun sub(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()

        val halfCarry = checkHalfCarrySub(valueInRegisterA, valueInGivenRegister, 0)
        val finalValue = (valueInRegisterA - valueInGivenRegister)

        val carry = checkCarrySub(finalValue)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = true, half = halfCarry, carry = carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Subtracts the given value from the value inside register A's value, this value can be gathered from the HL
     * register combo if useHL is true if not it will be gathered from the given memory address
     *
     * @param memoryAddress which memory address to use
     * @param useHL if HL is being used or not
     */
    fun subSpecial(memoryAddress: Int, useHL: Boolean) {
        cpu.timers.tick()

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val valueInAddress = bus.getValue(memoryAddress).toInt()

        val halfCarry = checkHalfCarrySub(valueInRegisterA, valueInAddress, 0)
        val finalValue = (valueInRegisterA - valueInAddress)

        val carry = checkCarrySub(finalValue)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = true, half = halfCarry, carry = carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        if (useHL) {
            cpu.cpuRegisters.incrementProgramCounter(1)
        } else {
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Subtracts the given register and the carry flag from register A (SBC instruction).
     *
     * Updates flags: Z (zero), N (subtract), H (half-carry), C (carry).
     *
     * @param register The register to subtract from A.
     */
    fun sbc(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()

        val carryAsValue = if (cpu.cpuRegisters.flags.getCarryFlag()) 1 else 0
        val halfCarry = checkHalfCarrySub(valueInRegisterA, valueInGivenRegister, carryAsValue)
        val finalValue = (valueInRegisterA - valueInGivenRegister - carryAsValue)

        val carry = checkCarrySub(finalValue)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = true, half = halfCarry, carry = carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Subtracts the value at the given memory address and the carry flag from register A.
     *
     * Used for SBC (A, HL) or SBC (A, nn) depending on `useHL`.
     * Updates flags: Z, N, H, C.
     *
     * @param memoryAddress The memory address to read the value from.
     * @param useHL If true, increments PC by 1 (HL indirect); otherwise, by 2 (immediate address).
     */
    fun sbcSpecial(memoryAddress: Int, useHL: Boolean) {
        cpu.timers.tick()

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val valueInAddress = bus.getValue(memoryAddress).toInt()

        val carryAsValue = if (cpu.cpuRegisters.flags.getCarryFlag()) 1 else 0
        val halfCarry = checkHalfCarrySub(valueInRegisterA, valueInAddress, carryAsValue)
        val finalValue = (valueInRegisterA - valueInAddress - carryAsValue)

        val carry = checkCarrySub(finalValue)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = true, half = halfCarry, carry = carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        if (useHL) {
            cpu.cpuRegisters.incrementProgramCounter(1)
        } else {
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Performs bitwise AND between register A and the given register.
     *
     * Stores the result in A and updates flags: Z (zero), N (reset), H (set), C (reset).
     *
     * @param register The register to AND with A.
     */
    fun and(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val finalValue = valueInRegisterA and valueInGivenRegister

        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = true, carry = false)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Performs bitwise AND between register A and the value at the given memory address.
     *
     * Used for AND (A, HL) or AND (A, nn) based on `useHL`.
     * Updates flags: Z (zero), N (reset), H (set), C (reset).
     *
     * @param memoryAddress The address to read the value from.
     * @param useHL If true, increments PC by 1 (HL); otherwise by 2 (immediate address).
     */
    fun andSpecial(memoryAddress: Int, useHL: Boolean) {
        cpu.timers.tick()

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val valueInAddress = bus.getValue(memoryAddress).toInt()
        val finalValue = valueInRegisterA and valueInAddress

        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = true, carry = false)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        if (useHL) {
            cpu.cpuRegisters.incrementProgramCounter(1)
        } else {
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Performs bitwise OR between register A and the given register.
     *
     * Stores the result in A and updates flags: Z (zero), N (reset), H (reset), C (reset).
     *
     * @param register The register to OR with A.
     */
    fun or(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val finalValue = valueInRegisterA or valueInGivenRegister

        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = false, carry = false)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Performs bitwise OR between register A and the value at the given memory address.
     *
     * Used for OR (A, HL) or OR (A, nn) depending on `useHL`.
     * Updates flags: Z (zero), N (reset), H (reset), C (reset).
     *
     * @param memoryAddress The memory address to read the value from.
     * @param useHL If true, increments PC by 1 (HL); otherwise by 2 (immediate address).
     */
    fun orSpecial(memoryAddress: Int, useHL: Boolean) {
        cpu.timers.tick()

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val valueInAddress = bus.getValue(memoryAddress).toInt()
        val finalValue = valueInRegisterA or valueInAddress

        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = false, carry = false)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        if (useHL) {
            cpu.cpuRegisters.incrementProgramCounter(1)
        } else {
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Performs bitwise XOR between register A and the given register.
     *
     * Stores the result in A and updates flags: Z (zero), N (reset), H (reset), C (reset).
     *
     * @param register The register to XOR with A.
     */
    fun xor(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val finalValue = valueInRegisterA xor valueInGivenRegister

        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = false, carry = false)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Performs bitwise XOR between register A and the value at the given memory address.
     *
     * Used for XOR (A, HL) or XOR (A, nn) depending on `useHL`.
     * Updates flags: Z (zero), N (reset), H (reset), C (reset).
     *
     * @param memoryAddress The memory address to read the value from.
     * @param useHL If true, increments PC by 1 (HL); otherwise by 2 (immediate address).
     */
    fun xorSpecial(memoryAddress: Int, useHL: Boolean) {
        cpu.timers.tick()

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val valueInAddress = bus.getValue(memoryAddress).toInt()
        val finalValue = valueInRegisterA xor valueInAddress

        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = false, carry = false)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())

        if (useHL) {
            cpu.cpuRegisters.incrementProgramCounter(1)
        } else {
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Compares register A with the given register by performing A - register (without storing result).
     *
     * Updates flags: Z (zero), N (subtract), H (half-carry), C (carry).
     *
     * @param register The register to compare with A.
     */
    fun cp(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val finalValue = valueInRegisterA - valueInGivenRegister

        val zero = checkZero(finalValue)
        val halfCarry = checkHalfCarrySub(valueInRegisterA, valueInGivenRegister, 0)
        val carry = checkCarrySub(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = true, half = halfCarry, carry = carry)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Compares register A with the value at the given memory address by performing (value - A) without storing the
     * result.
     *
     * Used for CP (A, HL) or CP (A, nn) depending on `useHL`.
     * Updates flags: Z (zero), N (subtract), H (half-carry), C (carry).
     *
     * @param memoryAddress The memory address to read the value from.
     * @param useHL If true, increments PC by 1 (HL); otherwise by 2 (immediate address).
     */
    fun cpSpecial(memoryAddress: Int, useHL: Boolean) {
        cpu.timers.tick()

        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val valueInAddress = bus.getValue(memoryAddress).toInt()
        val finalValue = valueInRegisterA - valueInAddress

        val zero = checkZero(finalValue)
        val halfCarry = checkHalfCarrySub(valueInRegisterA, valueInAddress, 0)
        val carry = checkCarrySub(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = true, half = halfCarry, carry = carry)
        if (useHL) {
            cpu.cpuRegisters.incrementProgramCounter(1)
        } else {
            cpu.cpuRegisters.incrementProgramCounter(2)
        }
    }

    /**
     * Increments the value of the specified register by 1.
     *
     * Updates flags: Z (zero), N (reset), H (half-carry). Carry flag is not affected.
     *
     * @param register The register to increment.
     */
    fun inc(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val finalValue = (valueInGivenRegister + 1) and 0xFF

        val halfCarry = checkHalfCarryAdd(valueInGivenRegister, 1, 0)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = halfCarry, carry = null)
        cpu.cpuRegisters.setRegister(register, finalValue.toUByte())
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Increments the value at the specified memory address by 1.
     *
     * Updates flags: Z (zero), N (reset), H (half-carry). Carry flag is not affected.
     *
     * @param memoryAddress The memory address to increment.
     */
    fun incSpecial(memoryAddress: Int) {
        cpu.timers.tick()

        val valueInAddress = bus.getValue(memoryAddress).toInt()
        val finalValue = (valueInAddress + 1) and 0xFF

        val halfCarry = checkHalfCarryAdd(valueInAddress, 1, 0)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = false, half = halfCarry, carry = null)
        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.timers.tick()
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Decrements the value of the specified register by 1.
     *
     * Updates flags: Z (zero), N (reset), H (half-carry). Carry flag is not affected.
     *
     * @param register The register to decrement.
     */
    fun dec(register: RegisterNames) {
        val valueInGivenRegister = cpu.cpuRegisters.getRegister(register).value.toInt()
        val finalValue = valueInGivenRegister - 1

        val halfCarry = checkHalfCarrySub(valueInGivenRegister, 1, 0)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = true, half = halfCarry, carry = null)
        cpu.cpuRegisters.setRegister(register, finalValue.toUByte())
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Decrements the value at the specified memory address by 1.
     *
     * Updates flags: Z (zero), N (reset), H (half-carry). Carry flag is not affected.
     *
     * @param memoryAddress The memory address to decrement.
     */
    fun decSpecial(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val valueInAddress = bus.getValue(memoryAddress).toInt()
        val finalValue = valueInAddress - 1

        val halfCarry = checkHalfCarrySub(valueInAddress, 1, 0)
        val zero = checkZero(finalValue)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = true, half = halfCarry, carry = null)
        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Adds the value of the given 16-bit register pair to HL.
     *
     * Updates flags: N (reset), H (half-carry), C (carry). Zero flag is not affected.
     *
     * @param type maps which two word register to add to HL
     */
    fun addHL(type: Int) {
        cpu.timers.tick()

        val givenRegisterPair = when(type) {
            0 -> cpu.cpuRegisters.getBC()
            1 -> cpu.cpuRegisters.getDE()
            2 -> cpu.cpuRegisters.getHL()
            else -> return
        }

        val valueInHL = cpu.cpuRegisters.getHL()

        val halfCarry = ((valueInHL and 0x0FFF) + (givenRegisterPair and 0x0FFF) and 0x1000) == 0x1000
        val carry = (valueInHL and 0xFFFF) + (givenRegisterPair and 0xFFFF) > 0xFFFF
        val finalValue = ((valueInHL and 0xFFFF) + (givenRegisterPair and 0xFFFF)) and 0xFFFF

        cpu.cpuRegisters.flags.setFlags(zero = null, subtract = false, half = halfCarry, carry)
        cpu.cpuRegisters.setHL(finalValue)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Adds the Stack Pointer (SP) value to HL.
     *
     * Updates flags: N (reset), H (half-carry), C (carry). Zero flag is not affected.
     */
    fun addHLSP() {
        cpu.timers.tick()

        val valueInHL = cpu.cpuRegisters.getHL()
        val stackPointer = cpu.cpuRegisters.getStackPointer()

        val halfCarry = ((valueInHL and 0x0FFF) + (stackPointer and 0x0FFF) and 0x1000) == 0x1000
        val carry = (valueInHL and 0xFFFF) + (stackPointer and 0xFFFF) > 0xFFFF
        val finalValue = ((valueInHL and 0xFFFF) + (stackPointer and 0xFFFF)) and 0xFFFF

        cpu.cpuRegisters.flags.setFlags(zero = null, subtract = false, half = halfCarry, carry)
        cpu.cpuRegisters.setHL(finalValue)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Adds a signed 8-bit immediate value to the Stack Pointer (SP).
     *
     * Updates flags: Z (reset), N (reset), H (half-carry), C (carry).
     *
     * @param memoryAddress The memory address of the signed 8-bit immediate value to add.
     */
    fun addSP(memoryAddress: Int) {
        repeat(3) { cpu.timers.tick() }

        val stackPointer = cpu.cpuRegisters.getStackPointer()
        val valueInAddress = bus.getValue(memoryAddress)
        val valueSigned = valueInAddress.toByte().toInt()
        val finalValue = (stackPointer + valueSigned) and 0xFFFF

        val halfCarry = checkHalfCarryAdd(valueInAddress.toInt(), stackPointer, 0)
        val carry =
            (((stackPointer and 0xFF) + (valueInAddress.toInt() and 0xFF)) and 0x100) == 0x100

        cpu.cpuRegisters.flags.setFlags(zero = false, subtract = false, half = halfCarry, carry)
        cpu.cpuRegisters.setStackPointer(finalValue)
        cpu.cpuRegisters.incrementProgramCounter(2)
    }

    /**
     * Increments the value of the specified 16-bit register pair by 1.
     *
     * Does not affect any flags.
     *
     * @param type maps which two word register should be decremented
     */
    fun incR(type: Int) {
        cpu.timers.tick()

        when (type) {
            0 -> {
                val finalValue = (cpu.cpuRegisters.getBC() + 1) and 0xFFFF
                cpu.cpuRegisters.setBC(finalValue)
            }
            1 -> {
                val finalValue = (cpu.cpuRegisters.getDE() + 1) and 0xFFFF
                cpu.cpuRegisters.setDE(finalValue)
            }
            2 -> {
                val finalValue = (cpu.cpuRegisters.getHL() + 1) and 0xFFFF
                cpu.cpuRegisters.setHL(finalValue)
            }
            else -> return
        }

        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Decrements the value of the specified 16-bit register pair by 1.
     *
     * Does not affect any flags.
     *
     * @param type maps which two word register should be decremented
     */
    fun decR(type: Int) {
        repeat(2) { cpu.timers.tick() }

        when (type) {
            0 -> {
                val finalValue = (cpu.cpuRegisters.getBC() - 1) and 0xFFFF
                cpu.cpuRegisters.setBC(finalValue)
            }
            1 -> {
                val finalValue = (cpu.cpuRegisters.getDE() - 1) and 0xFFFF
                cpu.cpuRegisters.setDE(finalValue)
            }
            2 -> {
                val finalValue = (cpu.cpuRegisters.getHL() - 1) and 0xFFFF
                cpu.cpuRegisters.setHL(finalValue)
            }
            else -> return
        }
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Increments the Stack Pointer (SP) by 1.
     *
     * Does not affect any flags.
     */
    fun incSP() {
        cpu.timers.tick()
        cpu.cpuRegisters.incrementStackPointer(1)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Decrements the Stack Pointer (SP) by 1.
     *
     * Does not affect any flags.
     */
    fun decSP() {
        cpu.timers.tick()
        cpu.cpuRegisters.incrementStackPointer(-1)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Performs the Decimal Adjust Accumulator (DAA) operation on register A.
     *
     * Adjusts register A after addition or subtraction to form correct BCD representation.
     * Updates flags: Z (zero), H (reset), C (carry). N (subtract) flag remains unchanged.
     */
    fun daa() {
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()

        var offset = 0
        var carry = false

        if ((!cpu.cpuRegisters.flags.getSubtractFlag() && (valueInRegisterA and 0x0F) > 0x09) ||
            cpu.cpuRegisters.flags.getHalfCarryFlag()
        ) {
            offset = offset or 0x06
        }
        if ((!cpu.cpuRegisters.flags.getSubtractFlag() && valueInRegisterA > 0x99) ||
            cpu.cpuRegisters.flags.getCarryFlag()
        ) {
            offset = offset or 0x60
            carry = true
        }

        val finalValue = if (cpu.cpuRegisters.flags.getSubtractFlag()) {
            valueInRegisterA - offset
        } else {
            valueInRegisterA + offset
        }
        val zero = checkZero(finalValue and 0xFF)

        cpu.cpuRegisters.flags.setFlags(zero = zero, subtract = null, half = false, carry)
        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())
        cpu.cpuRegisters.incrementProgramCounter(1)
    }

    /**
     * Complements (bitwise NOT) the value in register A.
     *
     * Sets flags: N (subtract) and H (half-carry) to true. Z (zero) and C (carry) flags are unaffected.
     */
    fun cpl() {
        val valueInRegisterA = cpu.cpuRegisters.getRegister(RegisterNames.A).value.toInt()
        val finalValue = (valueInRegisterA.inv() and 0xFF)

        cpu.cpuRegisters.setRegister(RegisterNames.A, finalValue.toUByte())
        cpu.cpuRegisters.flags.setFlags(zero = null, subtract = true, half = true, carry = null)
        cpu.cpuRegisters.incrementProgramCounter(1)
    }
}
