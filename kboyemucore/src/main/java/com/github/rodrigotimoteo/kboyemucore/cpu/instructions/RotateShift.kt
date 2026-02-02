package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.RegisterNames

/**
 * Class responsible for handling all rotate and shift operations
 *
 * @param cpu used for cpu communication
 * @param bus used for communication with other parts in this case accessing memory
 *
 * @author rodrigotimoteo
 **/
@Suppress("MagicNumber")
class RotateShift(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * Rotates A to the left and the 8th bit is used to set/reset the carry flag
     */
    fun rlca() {
        val valueInRegisterA = cpu.CPURegisters.getRegister(RegisterNames.A).value.toInt()

        val carry = (valueInRegisterA and 0xFF) == 0xFF
        val finalValue = ((valueInRegisterA shl 1) and 0xFF) or ((valueInRegisterA and 0xFF) shr 7)

        cpu.CPURegisters.flags.setFlags(zero = false, subtract = false, half = false, carry = carry)

        cpu.CPURegisters.setRegister(RegisterNames.A, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotates A to the left through the carry flag
     */
    fun rla() {
        val valueInRegisterA = cpu.CPURegisters.getRegister(RegisterNames.A).value.toInt()

        val carry = ((valueInRegisterA and 0xFF) == 0xFF)
        val finalValue = ((valueInRegisterA shl 1) and 0xFF) or
                if (cpu.CPURegisters.flags.getCarryFlag()) 1 else 0

        cpu.CPURegisters.flags.setFlags(zero = false, subtract = false, half = false, carry = carry)

        cpu.CPURegisters.setRegister(RegisterNames.A, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotate A to the right and the 0 bit is used to set or reset the carry flag
     */
    fun rrca() {
        val valueInRegisterA = cpu.CPURegisters.getRegister(RegisterNames.A).value.toInt()

        val carry = (valueInRegisterA and 0x01) == 0x01
        val finalValue = ((valueInRegisterA shr 1) and 0xFF) or ((valueInRegisterA and 0x01) shl 7)

        cpu.CPURegisters.flags.setFlags(zero = false, subtract = false, half = false, carry = carry)

        cpu.CPURegisters.setRegister(RegisterNames.A, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotates A to the right through the carry flag
     */
    fun rra() {
        val valueInRegisterA = cpu.CPURegisters.getRegister(RegisterNames.A).value.toInt()

        val carry = (valueInRegisterA and 0x01) == 0x01
        val finalValue = (((valueInRegisterA shr 1) and 0xFF)
                or ((if (cpu.CPURegisters.flags.getCarryFlag()) 1 else 0) shl 7)) and 0xFF

        cpu.CPURegisters.flags.setFlags(zero = false, subtract = false, half = false, carry = carry)

        cpu.CPURegisters.setRegister(RegisterNames.A, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotates word n to left, and old 7 bit is used to set or reset the carry flag
     *
     * @param register which register to rotate left
     */
    fun rlc(register: RegisterNames) {
        val valueInRegister = cpu.CPURegisters.getRegister(register).value.toInt()

        val carry = (valueInRegister and 0xFF) == 0xFF
        val finalValue =
            (((valueInRegister shl 1) and 0xFF) or ((valueInRegister and 0xFF) shr 7)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        cpu.CPURegisters.setRegister(register, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }


    /**
     * Rotates given word to the left and uses old 7 bit to set or reset the carry flag
     *
     * @param memoryAddress HL value
     */
    fun rlcHL(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val givenValue = bus.getValue(memoryAddress).toInt()
        val carry = (givenValue and 0xFF) == 0xFF
        val finalValue = (((givenValue shl 1) and 0xFF) or ((givenValue and 0xFF) shr 7)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotate the given register left through the carry left
     *
     * @param register which register to rotate left
     */
    fun rl(register: RegisterNames) {
        val valueInRegister = cpu.CPURegisters.getRegister(register).value.toInt()
        val carry = (valueInRegister and 0xFF) == 0xFF
        val finalValue = (((valueInRegister shl 1) and 0xFF)
                or (if (cpu.CPURegisters.flags.getCarryFlag()) 1 else 0)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        cpu.CPURegisters.setRegister(register, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotates given word to the left through the carry flag
     *
     * @param memoryAddress HL value
     */
    fun rlHL(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val givenValue = bus.getValue(memoryAddress).toInt()
        val carry = (givenValue and 0xFF) == 0xFF
        val finalValue = (((givenValue shl 1) and 0xFF)
                or (if (cpu.CPURegisters.flags.getCarryFlag()) 1 else 0)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotates word n to right, and old 0 bit is used to set or reset the carry
     * flag
     *
     * @param register which register to rotate right
     */
    fun rrc(register: RegisterNames) {
        val valueInRegister = cpu.CPURegisters.getRegister(register).value.toInt()
        val carry = (valueInRegister and 0x01) == 0x01
        val finalValue =
            (((valueInRegister shr 1) and 0xff) or ((valueInRegister and 0x01) shl 7)) and 0xff

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        cpu.CPURegisters.setRegister(register, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotates given word to the right and uses old 7 bit to set or reset the
     * carry flag
     *
     * @param memoryAddress HL value
     */
    fun rrcHL(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val givenValue = bus.getValue(memoryAddress).toInt()
        val carry = (givenValue and 0x01) == 0x01
        val finalValue = (((givenValue shr 1) and 0xFF) or ((givenValue and 0x01) shl 7)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotates word n to right through the carry flag
     *
     * @param register which register to rotate right
     */
    fun rr(register: RegisterNames) {
        val valueInRegister = cpu.CPURegisters.getRegister(register).value.toInt()
        val carry = (valueInRegister and 0x01) == 0x01
        val finalValue = (((valueInRegister shr 1) and 0xFF)
                or ((if (cpu.CPURegisters.flags.getCarryFlag()) 1 else 0) shl 7)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        cpu.CPURegisters.setRegister(register, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Rotates given word to the right through the carry flag
     *
     * @param memoryAddress HL value
     */
    fun rrHL(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val givenValue = bus.getValue(memoryAddress).toInt()
        val carry = (givenValue and 0x01) == 0x01
        val finalValue = (((givenValue shr 1) and 0xFF)
                or ((if (cpu.CPURegisters.flags.getCarryFlag()) 1 else 0) shl 7)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Shifts the register's value left into the carry.
     *
     * @param register which register to shift left
     */
    fun sla(register: RegisterNames) {
        val valueInRegister = cpu.CPURegisters.getRegister(register).value.toInt()
        val carry = (valueInRegister and 0x80) == 0x80
        val finalValue = (valueInRegister shl 1) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        cpu.CPURegisters.setRegister(register, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Shifts word n to left through the carry flag
     *
     * @param memoryAddress HL value
     */
    fun slaHL(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val givenValue = bus.getValue(memoryAddress).toInt()
        val carry = (givenValue and 0x80) == 0x80
        val finalValue = (givenValue shl 1) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Swaps the lower bits with the higher bits of a given register
     *
     * @param register which register to swap
     */
    fun swap(register: RegisterNames) {
        val valueInRegister = cpu.CPURegisters.getRegister(register).value.toInt()
        val finalValue =
            (((valueInRegister and 0xF0) shr 4) or ((valueInRegister and 0x0F) shl 4)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = false
        )

        cpu.CPURegisters.setRegister(register, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Swaps the lower bits with the higher bits of memory stored at the address
     * given by the HL value
     *
     * @param memoryAddress HL value
     */
    fun swapHL(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val givenValue = bus.getValue(memoryAddress).toInt()
        val finalValue = (((givenValue and 0x0F) shl 4) or ((givenValue and 0xF0) shr 4)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = false
        )

        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Shifts the value contained inside the given register to the right into
     * the carry. Uses the carry value
     *
     * @param register which register to shift
     */
    fun sra(register: RegisterNames) {
        val valueInRegister = cpu.CPURegisters.getRegister(register).value.toInt()
        val carry = (valueInRegister and 0x01) != 0
        val finalValue = ((valueInRegister shr 1) or (valueInRegister and 0xFF)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        cpu.CPURegisters.setRegister(register, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Shifts right the value of the memory contained inside the address given
     * by the 16bit register HL. Uses the carry value
     *
     * @param memoryAddress HL value
     */
    fun sraHL(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val givenValue = bus.getValue(memoryAddress).toInt()
        val carry = (givenValue and 0x01) != 0
        val finalValue = ((givenValue shr 1) or (givenValue and 0xFF)) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Shifts the value contained inside the given register to the right into
     * the carry. Doesn't use the carry value
     *
     * @param register which register to shift
     */
    fun srl(register: RegisterNames) {
        val valueInRegister = cpu.CPURegisters.getRegister(register).value.toInt()
        val carry = (valueInRegister and 0x01) == 0x01
        val finalValue = (valueInRegister shr 1) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        cpu.CPURegisters.setRegister(register, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }

    /**
     * Shifts right a word contained in the memory address of 16bit register HL into
     * the carry. Doesn't use the carry value
     *
     * @param memoryAddress HL value
     */
    fun srlHL(memoryAddress: Int) {
        repeat(2) { cpu.timers.tick() }

        val givenValue = bus.getValue(memoryAddress).toInt()
        val carry = (givenValue and 0x01) == 0x01
        val finalValue = (givenValue shr 1) and 0xFF

        cpu.CPURegisters.flags.setFlags(
            zero = finalValue == 0x00,
            subtract = false,
            half = false,
            carry = carry
        )

        bus.setValue(memoryAddress, finalValue.toUByte())
        cpu.CPURegisters.incrementProgramCounter(1)
    }
}
