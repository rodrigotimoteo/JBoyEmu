package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.RegisterNames

/**
 * Class responsible for decoding instructions based on program counter and executing them with the
 * given arguments
 *
 * @author rodrigotimoteo
 **/
class Decoder(
    private val cpu: CPU,
    private val bus: Bus
) {

    /**
     * Holds reference for Alu instruction handler
     */
    private val alu: Alu = Alu(cpu, bus)

    /**
     * Holds reference for Cpu Control instruction handler
     */
    private val control: Control = Control(cpu, bus)

    /**
     * Holds reference for Jump instruction handler
     */
    private val jump: Jump = Jump(cpu, bus)

    /**
     * Holds reference for 8-bit Loads instruction handler
     */
    private val load8Bit: Load8Bit = Load8Bit(cpu, bus)

    /**
     * Holds reference for 16-bit Loads instruction handler
     */
    private val load16Bit: Load16Bit = Load16Bit(cpu, bus)

    /**
     * Holds reference for Rotates and Shifts instruction handler
     */
    private val rotateShift: RotateShift = RotateShift(cpu, bus)

    /**
     * Holds reference for Single Bit instruction handler
     */
    private val singleBit: SingleBit = SingleBit(cpu, bus)

    /**
     * Stores whether the next instruction is of the secondary opcode table
     * prefixed by 0xCB
     */
    private var cbInstruction: Boolean = false

    /**
     * Holds all regular operations this makes it easier to access instead of using a giant when
     * statement
     */
    private val regularOperations: Array<() -> Unit> = Array(256) {
        { control.nop() }
    }

    /**
     * Holds all CB operations this makes it easier to access instead of using a giant when statement
     */
    private val cbOperations: Array<() -> Unit> = Array(256) {
        { control.nop() }
    }

    init {
        regularOperations[0x00] = { control.nop() } // NOP
        regularOperations[0x01] = { load16Bit.ld16bit(0) } // LD BC, u16
        regularOperations[0x02] = { load8Bit.ldTwoRegisters(cpu.CPURegisters.getBC()) } // LD (BC), A
        regularOperations[0x03] = { alu.incR(0) } // INC BC
        regularOperations[0x04] = { alu.inc(RegisterNames.B) } // INC B
        regularOperations[0x05] = { alu.dec(RegisterNames.B) } // DEC B
        regularOperations[0x06] = { load8Bit.ldNRegister(RegisterNames.B) } // LD B, u8
        regularOperations[0x07] = { rotateShift.rlca() } // RLCA
        regularOperations[0x08] = { load16Bit.ldNNSP() } // LD (u16), SP
        regularOperations[0x09] = { alu.addHL(0) } // ADD HL, BC
        regularOperations[0x0A] =
            { load8Bit.ldTwoRegistersIntoA(cpu.CPURegisters.getBC()) } // LD A, (BC)
        regularOperations[0x0B] = { alu.decR(0) } // DEC BC
        regularOperations[0x0C] = { alu.inc(RegisterNames.C) } // INC C
        regularOperations[0x0D] = { alu.dec(RegisterNames.C) } // DEC C
        regularOperations[0x0E] = { load8Bit.ldNRegister(RegisterNames.C) } // LD C, u8
        regularOperations[0x0F] = { rotateShift.rrca() } // RRCA
        regularOperations[0x10] = { control.stop() } // STOP
        regularOperations[0x11] = { load16Bit.ld16bit(1) } // LD DE, u16
        regularOperations[0x12] = { load8Bit.ldTwoRegisters(cpu.CPURegisters.getDE()) } // LD (DE), A
        regularOperations[0x13] = { alu.incR(1) } // INC DE
        regularOperations[0x14] = { alu.inc(RegisterNames.D) } // INC D
        regularOperations[0x15] = { alu.dec(RegisterNames.D) } // DEC D
        regularOperations[0x16] = { load8Bit.ldNRegister(RegisterNames.D) } // LD D, u8
        regularOperations[0x17] = { rotateShift.rla() } // RLA
        regularOperations[0x18] = { jump.jr() } // JR i8
        regularOperations[0x19] = { alu.addHL(1) } // ADD HL, DE
        regularOperations[0x1A] =
            { load8Bit.ldTwoRegistersIntoA(cpu.CPURegisters.getDE()) } // LD A, (DE)
        regularOperations[0x1B] = { alu.decR(1) } // DEC DE
        regularOperations[0x1C] = { alu.inc(RegisterNames.E) } // INC E
        regularOperations[0x1D] = { alu.dec(RegisterNames.E) } // DEC E
        regularOperations[0x1E] = { load8Bit.ldNRegister(RegisterNames.E) } // LD E, u8
        regularOperations[0x1F] = { rotateShift.rra() } // RRA
        regularOperations[0x20] = { jump.jrCond(JumpConstants.NZ) } // JR NZ, i8
        regularOperations[0x21] = { load16Bit.ld16bit(2) } // LD HL, u16
        regularOperations[0x22] = { load8Bit.ldi(true) } // LDI (HL), A
        regularOperations[0x23] = { alu.incR(2) } // INC HL
        regularOperations[0x24] = { alu.inc(RegisterNames.H) } // INC H
        regularOperations[0x25] = { alu.dec(RegisterNames.H) } // DEC H
        regularOperations[0x26] = { load8Bit.ldNRegister(RegisterNames.H) } // LD H, u8
        regularOperations[0x27] = { alu.daa() } // DAA
        regularOperations[0x28] = { jump.jrCond(JumpConstants.Z) } // JR Z, u8
        regularOperations[0x28] = { alu.addHL(2) } // ADD HL, HL
        regularOperations[0x2A] = { load8Bit.ldi(false) } // LDI A, (HL)
        regularOperations[0x2B] = { alu.decR(2) } // DEC HL
        regularOperations[0x2C] = { alu.inc(RegisterNames.L) } // INC L
        regularOperations[0x2D] = { alu.dec(RegisterNames.L) } // DEC L
        regularOperations[0x2E] = { load8Bit.ldNRegister(RegisterNames.L) } // LD L, u8
        regularOperations[0x2F] = { alu.cpl() } // CPL
        regularOperations[0x30] = { jump.jrCond(JumpConstants.NC) } // JR NC, u8
        regularOperations[0x31] = { load16Bit.ldSPUU() } // LD SP, u16
        regularOperations[0x32] = { load8Bit.ldd(true) } // LDD (HL), A
        regularOperations[0x33] = { alu.incSP() } // INC SP
        regularOperations[0x34] = { alu.incSpecial(cpu.CPURegisters.getHL()) } // INC (HL)
        regularOperations[0x35] = { alu.decSpecial(cpu.CPURegisters.getHL()) } // DEC (HL)
        regularOperations[0x36] = { load8Bit.ldNHL() } // LD (HL), n
        regularOperations[0x37] = { control.scf() } // SCF
        regularOperations[0x38] = { jump.jrCond(JumpConstants.C) } // JR C, u8
        regularOperations[0x39] = { alu.addHLSP() } // ADD HL, SP
        regularOperations[0x3A] = { load8Bit.ldd(false) } // LDD A, (HL)
        regularOperations[0x3B] = { alu.decSP() } // DEC SP
        regularOperations[0x3C] = { alu.inc(RegisterNames.A) } // INC A
        regularOperations[0x3D] = { alu.dec(RegisterNames.A) } // DEC A
        regularOperations[0x3E] = { load8Bit.ldNRegister(RegisterNames.A) } // LD A, u8
        regularOperations[0x3F] = { control.ccf() } // CCF
        regularOperations[0x40] = { load8Bit.ld(RegisterNames.B, RegisterNames.B) } // LD B, B
        regularOperations[0x41] = { load8Bit.ld(RegisterNames.B, RegisterNames.C) } // LD B, C
        regularOperations[0x42] = { load8Bit.ld(RegisterNames.B, RegisterNames.D) } // LD B, D
        regularOperations[0x43] = { load8Bit.ld(RegisterNames.B, RegisterNames.E) } // LD B, E
        regularOperations[0x44] = { load8Bit.ld(RegisterNames.B, RegisterNames.H) } // LD B, H
        regularOperations[0x45] = { load8Bit.ld(RegisterNames.B, RegisterNames.L) } // LD B, L
        regularOperations[0x46] = { load8Bit.ldHLtoRegister(RegisterNames.B) } // LD B, (HL)
        regularOperations[0x47] = { load8Bit.ld(RegisterNames.B, RegisterNames.A) } // LD C, A
        regularOperations[0x48] = { load8Bit.ld(RegisterNames.C, RegisterNames.B) } // LD C, B
        regularOperations[0x49] = { load8Bit.ld(RegisterNames.C, RegisterNames.C) } // LD C, C
        regularOperations[0x4A] = { load8Bit.ld(RegisterNames.C, RegisterNames.D) } // LD C, D
        regularOperations[0x4B] = { load8Bit.ld(RegisterNames.C, RegisterNames.E) } // LD C, E
        regularOperations[0x4C] = { load8Bit.ld(RegisterNames.C, RegisterNames.H) } // LD C, H
        regularOperations[0x4D] = { load8Bit.ld(RegisterNames.C, RegisterNames.L) } // LD C, L
        regularOperations[0x4E] = { load8Bit.ldHLtoRegister(RegisterNames.C) } // LD C, (HL)
        regularOperations[0x4F] = { load8Bit.ld(RegisterNames.C, RegisterNames.A) } // LD C, A
        regularOperations[0x50] = { load8Bit.ld(RegisterNames.D, RegisterNames.B) } // LD D, B
        regularOperations[0x51] = { load8Bit.ld(RegisterNames.D, RegisterNames.C) } // LD D, C
        regularOperations[0x52] = { load8Bit.ld(RegisterNames.D, RegisterNames.D) } // LD D, D
        regularOperations[0x53] = { load8Bit.ld(RegisterNames.D, RegisterNames.E) } // LD D, E
        regularOperations[0x54] = { load8Bit.ld(RegisterNames.D, RegisterNames.H) } // LD D, H
        regularOperations[0x55] = { load8Bit.ld(RegisterNames.D, RegisterNames.L) } // LD D, L
        regularOperations[0x56] = { load8Bit.ldHLtoRegister(RegisterNames.D) } // LD D, (HL)
        regularOperations[0x57] = { load8Bit.ld(RegisterNames.D, RegisterNames.A) } // LD D, A
        regularOperations[0x58] = { load8Bit.ld(RegisterNames.E, RegisterNames.B) } // LD E, B
        regularOperations[0x59] = { load8Bit.ld(RegisterNames.E, RegisterNames.C) } // LD E, C
        regularOperations[0x5A] = { load8Bit.ld(RegisterNames.E, RegisterNames.D) } // LD E, D
        regularOperations[0x5B] = { load8Bit.ld(RegisterNames.E, RegisterNames.E) } // LD E, E
        regularOperations[0x5C] = { load8Bit.ld(RegisterNames.E, RegisterNames.H) } // LD E, H
        regularOperations[0x5D] = { load8Bit.ld(RegisterNames.E, RegisterNames.L) } // LD E, L
        regularOperations[0x5E] = { load8Bit.ldHLtoRegister(RegisterNames.E) } // LD E, (HL)
        regularOperations[0x5F] = { load8Bit.ld(RegisterNames.E, RegisterNames.A) } // LD E, A
        regularOperations[0x60] = { load8Bit.ld(RegisterNames.H, RegisterNames.B) } // LD H, B
        regularOperations[0x61] = { load8Bit.ld(RegisterNames.H, RegisterNames.C) } // LD H, C
        regularOperations[0x62] = { load8Bit.ld(RegisterNames.H, RegisterNames.D) } // LD H, D
        regularOperations[0x63] = { load8Bit.ld(RegisterNames.H, RegisterNames.E) } // LD H, E
        regularOperations[0x64] = { load8Bit.ld(RegisterNames.H, RegisterNames.H) } // LD H, H
        regularOperations[0x65] = { load8Bit.ld(RegisterNames.H, RegisterNames.L) } // LD H, L
        regularOperations[0x66] = { load8Bit.ldHLtoRegister(RegisterNames.H) } // LD H, (HL)
        regularOperations[0x67] = { load8Bit.ld(RegisterNames.H, RegisterNames.A) } // LD H, A
        regularOperations[0x68] = { load8Bit.ld(RegisterNames.L, RegisterNames.B) } // LD L, B
        regularOperations[0x69] = { load8Bit.ld(RegisterNames.L, RegisterNames.C) } // LD L, C
        regularOperations[0x6A] = { load8Bit.ld(RegisterNames.L, RegisterNames.D) } // LD L, D
        regularOperations[0x6B] = { load8Bit.ld(RegisterNames.L, RegisterNames.E) } // LD L, E
        regularOperations[0x6C] = { load8Bit.ld(RegisterNames.L, RegisterNames.H) } // LD L, H
        regularOperations[0x6D] = { load8Bit.ld(RegisterNames.L, RegisterNames.L) } // LD L, L
        regularOperations[0x6E] = { load8Bit.ldHLtoRegister(RegisterNames.L) } // LD L, (HL)
        regularOperations[0x6F] = { load8Bit.ld(RegisterNames.L, RegisterNames.A) } // LD L, A
        regularOperations[0x70] = { load8Bit.ldRtoHL(RegisterNames.B) } // LD (HL), B
        regularOperations[0x71] = { load8Bit.ldRtoHL(RegisterNames.C) } // LD (HL), C
        regularOperations[0x72] = { load8Bit.ldRtoHL(RegisterNames.D) } // LD (HL), D
        regularOperations[0x73] = { load8Bit.ldRtoHL(RegisterNames.E) } // LD (HL), E
        regularOperations[0x74] = { load8Bit.ldRtoHL(RegisterNames.H) } // LD (HL), H
        regularOperations[0x75] = { load8Bit.ldRtoHL(RegisterNames.L) } // LD (HL), L
        regularOperations[0x76] = { control.halt() } // HALT
        regularOperations[0x77] = { load8Bit.ldRtoHL(RegisterNames.A) } // LD (HL), A
        regularOperations[0x78] = { load8Bit.ld(RegisterNames.A, RegisterNames.B) } // LD A, B
        regularOperations[0x79] = { load8Bit.ld(RegisterNames.A, RegisterNames.C) } // LD A, C
        regularOperations[0x7A] = { load8Bit.ld(RegisterNames.A, RegisterNames.D) } // LD A, D
        regularOperations[0x7B] = { load8Bit.ld(RegisterNames.A, RegisterNames.E) } // LD A, E
        regularOperations[0x7C] = { load8Bit.ld(RegisterNames.A, RegisterNames.H) } // LD A, H
        regularOperations[0x7D] = { load8Bit.ld(RegisterNames.A, RegisterNames.L) } // LD A, L
        regularOperations[0x7E] = { load8Bit.ldHLtoRegister(RegisterNames.L) } // LD A, (HL)
        regularOperations[0x7F] = { load8Bit.ld(RegisterNames.A, RegisterNames.A) } // LD A, A
        regularOperations[0x80] = { alu.add(RegisterNames.B) } // ADD A, B
        regularOperations[0x81] = { alu.add(RegisterNames.C) } // ADD A, C
        regularOperations[0x82] = { alu.add(RegisterNames.D) } // ADD A, D
        regularOperations[0x83] = { alu.add(RegisterNames.E) } // ADD A, E
        regularOperations[0x84] = { alu.add(RegisterNames.H) } // ADD A, H
        regularOperations[0x85] = { alu.add(RegisterNames.L) } // ADD A, L
        regularOperations[0x86] = {
            alu.addSpecial(cpu.CPURegisters.getHL(), true)
        } // ADD A, (HL)
        regularOperations[0x87] = { alu.add(RegisterNames.A) } // ADD A, A
        regularOperations[0x88] = { alu.adc(RegisterNames.B) } // ADC A, B
        regularOperations[0x89] = { alu.adc(RegisterNames.C) } // ADC A, C
        regularOperations[0x8A] = { alu.adc(RegisterNames.D) } // ADC A, D
        regularOperations[0x8B] = { alu.adc(RegisterNames.E) } // ADC A, E
        regularOperations[0x8C] = { alu.adc(RegisterNames.H) } // ADC A, H
        regularOperations[0x8D] = { alu.adc(RegisterNames.L) } // ADC A, L
        regularOperations[0x8E] = {
            alu.adcSpecial(cpu.CPURegisters.getHL(), true)
        } // ADC A, (HL)
        regularOperations[0x8F] = { alu.adc(RegisterNames.A) } // ADC A, A
        regularOperations[0x90] = { alu.sub(RegisterNames.B) } // SUB A, B
        regularOperations[0x91] = { alu.sub(RegisterNames.C) } // SUB A, C
        regularOperations[0x92] = { alu.sub(RegisterNames.D) } // SUB A, D
        regularOperations[0x93] = { alu.sub(RegisterNames.E) } // SUB A, E
        regularOperations[0x94] = { alu.sub(RegisterNames.H) } // SUB A, H
        regularOperations[0x95] = { alu.sub(RegisterNames.L) } // SUB A, L
        regularOperations[0x96] = {
            alu.subSpecial(cpu.CPURegisters.getHL(), true)
        } // SUB A, (HL)
        regularOperations[0x97] = { alu.sub(RegisterNames.A) } // SUB A, A
        regularOperations[0x98] = { alu.sbc(RegisterNames.B) } // SBC A, B
        regularOperations[0x99] = { alu.sbc(RegisterNames.C) } // SBC A, C
        regularOperations[0x9A] = { alu.sbc(RegisterNames.D) } // SBC A, D
        regularOperations[0x9B] = { alu.sbc(RegisterNames.E) } // SBC A, E
        regularOperations[0x9C] = { alu.sbc(RegisterNames.H) } // SBC A, H
        regularOperations[0x9D] = { alu.sbc(RegisterNames.L) } // SBC A, L
        regularOperations[0x9E] = {
            alu.sbcSpecial(cpu.CPURegisters.getHL(), true)
        } // SBC A, (HL)
        regularOperations[0x9F] = { alu.sbc(RegisterNames.A) } // SBC A, A
        regularOperations[0xA0] = { alu.and(RegisterNames.B) } // AND A, B
        regularOperations[0xA1] = { alu.and(RegisterNames.C) } // AND A, C
        regularOperations[0xA2] = { alu.and(RegisterNames.D) } // AND A, D
        regularOperations[0xA3] = { alu.and(RegisterNames.E) } // AND A, E
        regularOperations[0xA4] = { alu.and(RegisterNames.H) } // AND A, H
        regularOperations[0xA5] = { alu.and(RegisterNames.L) } // AND A, L
        regularOperations[0xA6] = {
            alu.andSpecial(cpu.CPURegisters.getHL(), true)
        } // AND A, (HL)
        regularOperations[0xA7] = { alu.and(RegisterNames.A) } // AND A, A
        regularOperations[0xA8] = { alu.xor(RegisterNames.B) } // XOR A, B
        regularOperations[0xA9] = { alu.xor(RegisterNames.C) } // XOR A, C
        regularOperations[0xAA] = { alu.xor(RegisterNames.D) } // XOR A, D
        regularOperations[0xAB] = { alu.xor(RegisterNames.E) } // XOR A, E
        regularOperations[0xAC] = { alu.xor(RegisterNames.H) } // XOR A, H
        regularOperations[0xAD] = { alu.xor(RegisterNames.L) } // XOR A, L
        regularOperations[0xAE] = {
            alu.xorSpecial(cpu.CPURegisters.getHL(), true)
        } // XOR A, (HL)
        regularOperations[0xAF] = { alu.xor(RegisterNames.A) } // XOR A, A
        regularOperations[0xB0] = { alu.or(RegisterNames.B) } // OR A, B
        regularOperations[0xB1] = { alu.or(RegisterNames.C) } // OR A, C
        regularOperations[0xB2] = { alu.or(RegisterNames.D) } // OR A, D
        regularOperations[0xB3] = { alu.or(RegisterNames.E) } // OR A, E
        regularOperations[0xB4] = { alu.or(RegisterNames.H) } // OR A, H
        regularOperations[0xB5] = { alu.or(RegisterNames.L) } // OR A, L
        regularOperations[0xB6] = {
            alu.orSpecial(cpu.CPURegisters.getHL(), true)
        } // OR A, (HL)
        regularOperations[0xB7] = { alu.or(RegisterNames.A) } // OR A, A
        regularOperations[0xB8] = { alu.cp(RegisterNames.B) } // CP A, B
        regularOperations[0xB9] = { alu.cp(RegisterNames.C) } // CP A, C
        regularOperations[0xBA] = { alu.cp(RegisterNames.D) } // CP A, D
        regularOperations[0xBB] = { alu.cp(RegisterNames.E) } // CP A, E
        regularOperations[0xBC] = { alu.cp(RegisterNames.H) } // CP A, H
        regularOperations[0xBD] = { alu.cp(RegisterNames.L) } // CP A, L
        regularOperations[0xBE] = {
            alu.cpSpecial(cpu.CPURegisters.getHL(), true)
        } // CP A, (HL)
        regularOperations[0xBF] = { alu.cp(RegisterNames.A) } // CP A, A
        regularOperations[0xC0] = { jump.retCond(JumpConstants.NZ) } // RET NZ
        regularOperations[0xC1] = { load16Bit.pop(1) } // POP BC
        regularOperations[0xC2] = { jump.jpCond(JumpConstants.NZ) } // JP NZ, u16
        regularOperations[0xC3] = { jump.jp() } // JP u16
        regularOperations[0xC4] = { jump.callCond(JumpConstants.NZ) } // CALL NZ, u16
        regularOperations[0xC5] = { load16Bit.push(1) } // PUSH BC
        regularOperations[0xC6] =
            { alu.addSpecial(cpu.CPURegisters.getProgramCounter() + 1, false) } // ADD A, #
        regularOperations[0xC7] = { jump.rst(0x00) } // RST 00H
        regularOperations[0xC8] = { jump.retCond(JumpConstants.Z) } // RET Z
        regularOperations[0xC9] = { jump.ret() } // RET
        regularOperations[0xCA] = { jump.jpCond(JumpConstants.Z) } // JP Z, u16
        regularOperations[0xCB] = {
            cbInstruction = true
            cpu.CPURegisters.incrementProgramCounter(1)
            decode(bus.getValue(cpu.CPURegisters.getProgramCounter()).toInt())
        }
        regularOperations[0xCC] = { jump.callCond(JumpConstants.Z) } // CALL Z, nn
        regularOperations[0xCD] = { jump.call() } // CALL u16
        regularOperations[0xCE] =
            { alu.adcSpecial(cpu.CPURegisters.getProgramCounter() + 1, false) } // ADC A, #
        regularOperations[0xCF] = { jump.rst(0x08) } // RST 08H
        regularOperations[0xD0] = { jump.retCond(JumpConstants.NC) } // RET NC
        regularOperations[0xD1] = { load16Bit.pop(2) } // POP DE
        regularOperations[0xD2] = { jump.jpCond(JumpConstants.NC) } // JP NC,u16
        regularOperations[0xD4] = { jump.callCond(JumpConstants.NC) } // CALL NC,nn
        regularOperations[0xD5] = { load16Bit.push(2) } // PUSH DE
        regularOperations[0xD6] =
            { alu.subSpecial(cpu.CPURegisters.getProgramCounter() + 1, false) } // SUB A, #
        regularOperations[0xD7] = { jump.rst(0x10) } // RST 10H
        regularOperations[0xD8] = { jump.retCond(JumpConstants.C) } // RET C
        regularOperations[0xD9] = { jump.reti() } // RETI
        regularOperations[0xDA] = { jump.jpCond(JumpConstants.C) } // JP C, u16
        regularOperations[0xDC] = { jump.callCond(JumpConstants.C) } // CALL C, nn
        regularOperations[0xDE] =
            { alu.sbcSpecial(cpu.CPURegisters.getProgramCounter() + 1, false) } // SBC A, #
        regularOperations[0xDF] = { jump.rst(0x18) } // RST 18H
        regularOperations[0xE0] = { load8Bit.ldh(true) } // LD (FF00+u8), A
        regularOperations[0xE1] = { load16Bit.pop(3) } // POP (HL)
        regularOperations[0xE2] = { load8Bit.ldAC(true) } // LD (C), A
        regularOperations[0xE5] = { load16Bit.push(3) } // PUSH HL
        regularOperations[0xE6] =
            { alu.andSpecial(cpu.CPURegisters.getProgramCounter() + 1, false) } // AND #
        regularOperations[0xE7] = { jump.rst(0x20) } // RST 20H
        regularOperations[0xE8] = { alu.addSP(cpu.CPURegisters.getProgramCounter() + 1) } // ADD SP, n
        regularOperations[0xE9] = { jump.jpHL() } // JP (HL)
        regularOperations[0xEA] = { load8Bit.ldNN() } // LD (nn), A
        regularOperations[0xEE] =
            { alu.xorSpecial(cpu.CPURegisters.getProgramCounter() + 1, false) } // XOR #
        regularOperations[0xEF] = { jump.rst(0x28) } // RST 28H
        regularOperations[0xF0] = { load8Bit.ldh(false) } // LD A, (FF00+u8)
        regularOperations[0xE1] = { load16Bit.pop(0) } // POP AF
        regularOperations[0xF2] = { load8Bit.ldAC(false) } // LD A, (C)
        regularOperations[0xF3] = { control.di() } // DI
        regularOperations[0xF5] = { load16Bit.push(0) } // PUSH AF
        regularOperations[0xF6] =
            { alu.orSpecial(cpu.CPURegisters.getProgramCounter() + 1, false) } // OR #
        regularOperations[0xF7] = { jump.rst(0x30) } // RST 30H
        regularOperations[0xF8] = { load16Bit.ldHL() } // LDHL SP, n
        regularOperations[0xF9] = { load16Bit.ldSPHL() } // LD SP, HL
        regularOperations[0xFA] = { load8Bit.ldNNIntoA() } // LD A, (nn)
        regularOperations[0xFB] = { control.ei() } // EI
        regularOperations[0xFE] =
            { alu.cpSpecial(cpu.CPURegisters.getProgramCounter() + 1, false) } // CP A, u8
        regularOperations[0xFF] = { jump.rst(0x38) } // RST 38H

        // CB Operations assignment
        cbOperations[0x00] = { rotateShift.rlc(RegisterNames.B) } // RLC B
        cbOperations[0x01] = { rotateShift.rlc(RegisterNames.C) } // RLC C
        cbOperations[0x02] = { rotateShift.rlc(RegisterNames.D) } // RLC D
        cbOperations[0x03] = { rotateShift.rlc(RegisterNames.E) } // RLC E
        cbOperations[0x04] = { rotateShift.rlc(RegisterNames.H) } // RLC H
        cbOperations[0x05] = { rotateShift.rlc(RegisterNames.L) } // RLC L
        cbOperations[0x06] = { rotateShift.rlcHL(cpu.CPURegisters.getHL()) } // RLC (HL)
        cbOperations[0x07] = { rotateShift.rlc(RegisterNames.A) } // RLC A
        cbOperations[0x08] = { rotateShift.rrc(RegisterNames.B) } // RRC B
        cbOperations[0x09] = { rotateShift.rrc(RegisterNames.C) } // RRC C
        cbOperations[0x0A] = { rotateShift.rrc(RegisterNames.D) } // RRC D
        cbOperations[0x0B] = { rotateShift.rrc(RegisterNames.E) } // RRC E
        cbOperations[0x0C] = { rotateShift.rrc(RegisterNames.H) } // RRC H
        cbOperations[0x0D] = { rotateShift.rrc(RegisterNames.L) } // RRC L
        cbOperations[0x0E] = { rotateShift.rrcHL(cpu.CPURegisters.getHL()) } // RRC (HL)
        cbOperations[0x0F] = { rotateShift.rrc(RegisterNames.A) } // RRC A
        cbOperations[0x10] = { rotateShift.rl(RegisterNames.B) } // RL B
        cbOperations[0x11] = { rotateShift.rl(RegisterNames.C) } // RL C
        cbOperations[0x12] = { rotateShift.rl(RegisterNames.D) } // RL D
        cbOperations[0x13] = { rotateShift.rl(RegisterNames.E) } // RL E
        cbOperations[0x14] = { rotateShift.rl(RegisterNames.H) } // RL H
        cbOperations[0x15] = { rotateShift.rl(RegisterNames.L) } // RL L
        cbOperations[0x16] = { rotateShift.rlHL(cpu.CPURegisters.getHL()) } // RL (HL)
        cbOperations[0x17] = { rotateShift.rl(RegisterNames.A) } // RL A
        cbOperations[0x18] = { rotateShift.rr(RegisterNames.B) } // RR B
        cbOperations[0x19] = { rotateShift.rr(RegisterNames.C) } // RR C
        cbOperations[0x1A] = { rotateShift.rr(RegisterNames.D) } // RR D
        cbOperations[0x1B] = { rotateShift.rr(RegisterNames.E) } // RR E
        cbOperations[0x1C] = { rotateShift.rr(RegisterNames.H) } // RR H
        cbOperations[0x1D] = { rotateShift.rr(RegisterNames.L) } // RR L
        cbOperations[0x1E] = { rotateShift.rrHL(cpu.CPURegisters.getHL()) } // RR (HL)
        cbOperations[0x1F] = { rotateShift.rr(RegisterNames.A) } // RR A
        cbOperations[0x20] = { rotateShift.sla(RegisterNames.B) } // SLA B
        cbOperations[0x21] = { rotateShift.sla(RegisterNames.C) } // SLA C
        cbOperations[0x22] = { rotateShift.sla(RegisterNames.D) } // SLA D
        cbOperations[0x23] = { rotateShift.sla(RegisterNames.E) } // SLA E
        cbOperations[0x24] = { rotateShift.sla(RegisterNames.H) } // SLA H
        cbOperations[0x25] = { rotateShift.sla(RegisterNames.L) } // SLA L
        cbOperations[0x26] = { rotateShift.slaHL(cpu.CPURegisters.getHL()) } // SLA (HL)
        cbOperations[0x27] = { rotateShift.sla(RegisterNames.A) } // SLA A
        cbOperations[0x28] = { rotateShift.sra(RegisterNames.B) } // SRA B
        cbOperations[0x29] = { rotateShift.sra(RegisterNames.C) } // SRA C
        cbOperations[0x2A] = { rotateShift.sra(RegisterNames.D) } // SRA D
        cbOperations[0x2B] = { rotateShift.sra(RegisterNames.E) } // SRA E
        cbOperations[0x2C] = { rotateShift.sra(RegisterNames.H) } // SRA H
        cbOperations[0x2D] = { rotateShift.sra(RegisterNames.L) } // SRA L
        cbOperations[0x2E] = { rotateShift.sraHL(cpu.CPURegisters.getHL()) } // SRA (HL)
        cbOperations[0x2F] = { rotateShift.sra(RegisterNames.A) } // SRA A
        cbOperations[0x30] = { rotateShift.swap(RegisterNames.B) } // SWAP B
        cbOperations[0x31] = { rotateShift.swap(RegisterNames.C) } // SWAP C
        cbOperations[0x32] = { rotateShift.swap(RegisterNames.D) } // SWAP D
        cbOperations[0x33] = { rotateShift.swap(RegisterNames.E) } // SWAP E
        cbOperations[0x34] = { rotateShift.swap(RegisterNames.H) } // SWAP H
        cbOperations[0x35] = { rotateShift.swap(RegisterNames.L) } // SWAP L
        cbOperations[0x36] = { rotateShift.swapHL(cpu.CPURegisters.getHL()) } // SWAP (HL)
        cbOperations[0x37] = { rotateShift.swap(RegisterNames.A) } // SWAP A
        cbOperations[0x38] = { rotateShift.srl(RegisterNames.B) } // SRL B
        cbOperations[0x39] = { rotateShift.srl(RegisterNames.C) } // SRL C
        cbOperations[0x3A] = { rotateShift.srl(RegisterNames.D) } // SRL D
        cbOperations[0x3B] = { rotateShift.srl(RegisterNames.E) } // SRL E
        cbOperations[0x3C] = { rotateShift.srl(RegisterNames.H) } // SRL H
        cbOperations[0x3D] = { rotateShift.srl(RegisterNames.L) } // SRL L
        cbOperations[0x3E] = { rotateShift.srlHL(cpu.CPURegisters.getHL()) } // SRL (HL)
        cbOperations[0x3F] = { rotateShift.srl(RegisterNames.A) } // SRL A
        cbOperations[0x40] = { singleBit.bit(0, RegisterNames.B) } // BIT 0, B
        cbOperations[0x41] = { singleBit.bit(0, RegisterNames.C) } // BIT 0, C
        cbOperations[0x42] = { singleBit.bit(0, RegisterNames.D) } // BIT 0, D
        cbOperations[0x43] = { singleBit.bit(0, RegisterNames.E) } // BIT 0, E
        cbOperations[0x44] = { singleBit.bit(0, RegisterNames.H) } // BIT 0, H
        cbOperations[0x45] = { singleBit.bit(0, RegisterNames.L) } // BIT 0, L
        cbOperations[0x46] = { singleBit.bitHL(0, cpu.CPURegisters.getHL()) } // BIT 0, (HL)
        cbOperations[0x47] = { singleBit.bit(0, RegisterNames.A) } // BIT 0, A
        cbOperations[0x48] = { singleBit.bit(1, RegisterNames.B) } // BIT 1, B
        cbOperations[0x49] = { singleBit.bit(1, RegisterNames.C) } // BIT 1, C
        cbOperations[0x4A] = { singleBit.bit(1, RegisterNames.D) } // BIT 1, D
        cbOperations[0x4B] = { singleBit.bit(1, RegisterNames.E) } // BIT 1, E
        cbOperations[0x4C] = { singleBit.bit(1, RegisterNames.H) } // BIT 1, H
        cbOperations[0x4D] = { singleBit.bit(1, RegisterNames.L) } // BIT 1, L
        cbOperations[0x4E] = { singleBit.bitHL(1, cpu.CPURegisters.getHL()) } // BIT 1, (HL)
        cbOperations[0x4F] = { singleBit.bit(1, RegisterNames.A) } // BIT 1, A
        cbOperations[0x50] = { singleBit.bit(2, RegisterNames.B) } // BIT 2, B
        cbOperations[0x51] = { singleBit.bit(2, RegisterNames.C) } // BIT 2, C
        cbOperations[0x52] = { singleBit.bit(2, RegisterNames.D) } // BIT 2, D
        cbOperations[0x53] = { singleBit.bit(2, RegisterNames.E) } // BIT 2, E
        cbOperations[0x54] = { singleBit.bit(2, RegisterNames.H) } // BIT 2, H
        cbOperations[0x55] = { singleBit.bit(2, RegisterNames.L) } // BIT 2, L
        cbOperations[0x56] = { singleBit.bitHL(2, cpu.CPURegisters.getHL()) } // BIT 2, (HL)
        cbOperations[0x57] = { singleBit.bit(2, RegisterNames.A) } // BIT 2, A
        cbOperations[0x58] = { singleBit.bit(3, RegisterNames.B) } // BIT 3, B
        cbOperations[0x59] = { singleBit.bit(3, RegisterNames.C) } // BIT 3, C
        cbOperations[0x5A] = { singleBit.bit(3, RegisterNames.D) } // BIT 3, D
        cbOperations[0x5B] = { singleBit.bit(3, RegisterNames.E) } // BIT 3, E
        cbOperations[0x5C] = { singleBit.bit(3, RegisterNames.H) } // BIT 3, H
        cbOperations[0x5D] = { singleBit.bit(3, RegisterNames.L) } // BIT 3, L
        cbOperations[0x5E] = { singleBit.bitHL(3, cpu.CPURegisters.getHL()) } // BIT 3, (HL)
        cbOperations[0x5F] = { singleBit.bit(3, RegisterNames.A) } // BIT 3, A
        cbOperations[0x60] = { singleBit.bit(4, RegisterNames.B) } // BIT 4, B
        cbOperations[0x61] = { singleBit.bit(4, RegisterNames.C) } // BIT 4, C
        cbOperations[0x62] = { singleBit.bit(4, RegisterNames.D) } // BIT 4, D
        cbOperations[0x63] = { singleBit.bit(4, RegisterNames.E) } // BIT 4, E
        cbOperations[0x64] = { singleBit.bit(4, RegisterNames.H) } // BIT 4, H
        cbOperations[0x65] = { singleBit.bit(4, RegisterNames.L) } // BIT 4, L
        cbOperations[0x66] = { singleBit.bitHL(4, cpu.CPURegisters.getHL()) } // BIT 4, (HL)
        cbOperations[0x67] = { singleBit.bit(4, RegisterNames.A) } // BIT 4, A
        cbOperations[0x68] = { singleBit.bit(5, RegisterNames.B) } // BIT 5, B
        cbOperations[0x69] = { singleBit.bit(5, RegisterNames.C) } // BIT 5, C
        cbOperations[0x6A] = { singleBit.bit(5, RegisterNames.D) } // BIT 5, D
        cbOperations[0x6B] = { singleBit.bit(5, RegisterNames.E) } // BIT 5, E
        cbOperations[0x6C] = { singleBit.bit(5, RegisterNames.H) } // BIT 5, H
        cbOperations[0x6D] = { singleBit.bit(5, RegisterNames.L) } // BIT 5, L
        cbOperations[0x6E] = { singleBit.bitHL(5, cpu.CPURegisters.getHL()) } // BIT 5, (HL)
        cbOperations[0x6F] = { singleBit.bit(5, RegisterNames.A) } // BIT 5, A
        cbOperations[0x70] = { singleBit.bit(6, RegisterNames.B) } // BIT 6, B
        cbOperations[0x71] = { singleBit.bit(6, RegisterNames.C) } // BIT 6, C
        cbOperations[0x72] = { singleBit.bit(6, RegisterNames.D) } // BIT 6, D
        cbOperations[0x73] = { singleBit.bit(6, RegisterNames.E) } // BIT 6, E
        cbOperations[0x74] = { singleBit.bit(6, RegisterNames.H) } // BIT 6, H
        cbOperations[0x75] = { singleBit.bit(6, RegisterNames.L) } // BIT 6, L
        cbOperations[0x76] = { singleBit.bitHL(6, cpu.CPURegisters.getHL()) } // BIT 6, (HL)
        cbOperations[0x77] = { singleBit.bit(6, RegisterNames.A) } // BIT 6, A
        cbOperations[0x78] = { singleBit.bit(7, RegisterNames.B) } // BIT 7, B
        cbOperations[0x79] = { singleBit.bit(7, RegisterNames.C) } // BIT 7, C
        cbOperations[0x7A] = { singleBit.bit(7, RegisterNames.D) } // BIT 7, D
        cbOperations[0x7B] = { singleBit.bit(7, RegisterNames.E) } // BIT 7, E
        cbOperations[0x7C] = { singleBit.bit(7, RegisterNames.H) } // BIT 7, H
        cbOperations[0x7D] = { singleBit.bit(7, RegisterNames.L) } // BIT 7, L
        cbOperations[0x7E] = { singleBit.bitHL(7, cpu.CPURegisters.getHL()) } // BIT 7, (HL)
        cbOperations[0x7F] = { singleBit.bit(7, RegisterNames.A) } // BIT 7, A
        cbOperations[0x80] = { singleBit.res(0, RegisterNames.B) } // RES 0, B
        cbOperations[0x81] = { singleBit.res(0, RegisterNames.C) } // RES 0, C
        cbOperations[0x82] = { singleBit.res(0, RegisterNames.D) } // RES 0, D
        cbOperations[0x83] = { singleBit.res(0, RegisterNames.E) } // RES 0, E
        cbOperations[0x84] = { singleBit.res(0, RegisterNames.H) } // RES 0, H
        cbOperations[0x85] = { singleBit.res(0, RegisterNames.L) } // RES 0, L
        cbOperations[0x86] = { singleBit.resHL(0, cpu.CPURegisters.getHL()) } // RES 1, (HL)
        cbOperations[0x87] = { singleBit.res(0, RegisterNames.A) } // RES 0, A
        cbOperations[0x88] = { singleBit.res(1, RegisterNames.B) } // RES 1, B
        cbOperations[0x89] = { singleBit.res(1, RegisterNames.C) } // RES 1, C
        cbOperations[0x8A] = { singleBit.res(1, RegisterNames.D) } // RES 1, D
        cbOperations[0x8B] = { singleBit.res(1, RegisterNames.E) } // RES 1, E
        cbOperations[0x8C] = { singleBit.res(1, RegisterNames.H) } // RES 1, H
        cbOperations[0x8D] = { singleBit.res(1, RegisterNames.L) } // RES 1, L
        cbOperations[0x8E] = { singleBit.resHL(1, cpu.CPURegisters.getHL()) } // RES 1, (HL)
        cbOperations[0x8F] = { singleBit.res(1, RegisterNames.A) } // RES 1, A
        cbOperations[0x90] = { singleBit.res(2, RegisterNames.B) } // RES 2, B
        cbOperations[0x91] = { singleBit.res(2, RegisterNames.C) } // RES 2, C
        cbOperations[0x92] = { singleBit.res(2, RegisterNames.D) } // RES 2, D
        cbOperations[0x93] = { singleBit.res(2, RegisterNames.E) } // RES 2, E
        cbOperations[0x94] = { singleBit.res(2, RegisterNames.H) } // RES 2, H
        cbOperations[0x95] = { singleBit.res(2, RegisterNames.L) } // RES 2, L
        cbOperations[0x96] = { singleBit.resHL(2, cpu.CPURegisters.getHL()) } // RES 2, (HL)
        cbOperations[0x97] = { singleBit.res(2, RegisterNames.A) } // RES 2, A
        cbOperations[0x98] = { singleBit.res(3, RegisterNames.B) } // RES 3, B
        cbOperations[0x99] = { singleBit.res(3, RegisterNames.C) } // RES 3, C
        cbOperations[0x9A] = { singleBit.res(3, RegisterNames.D) } // RES 3, D
        cbOperations[0x9B] = { singleBit.res(3, RegisterNames.E) } // RES 3, E
        cbOperations[0x9C] = { singleBit.res(3, RegisterNames.H) } // RES 3, H
        cbOperations[0x9D] = { singleBit.res(3, RegisterNames.L) } // RES 3, L
        cbOperations[0x9E] = { singleBit.resHL(3, cpu.CPURegisters.getHL()) } // RES 3, (HL)
        cbOperations[0x9F] = { singleBit.res(3, RegisterNames.A) } // RES 3, A
        cbOperations[0xA0] = { singleBit.res(4, RegisterNames.B) } // RES 4, B
        cbOperations[0xA1] = { singleBit.res(4, RegisterNames.C) } // RES 4, C
        cbOperations[0xA2] = { singleBit.res(4, RegisterNames.D) } // RES 4, D
        cbOperations[0xA3] = { singleBit.res(4, RegisterNames.E) } // RES 4, E
        cbOperations[0xA4] = { singleBit.res(4, RegisterNames.H) } // RES 4, H
        cbOperations[0xA5] = { singleBit.res(4, RegisterNames.L) } // RES 4, L
        cbOperations[0xA6] = { singleBit.resHL(4, cpu.CPURegisters.getHL()) } // RES 4, (HL)
        cbOperations[0xA7] = { singleBit.res(4, RegisterNames.A) } // RES 4, A
        cbOperations[0xA8] = { singleBit.res(5, RegisterNames.B) } // RES 5, B
        cbOperations[0xA9] = { singleBit.res(5, RegisterNames.C) } // RES 5, C
        cbOperations[0xAA] = { singleBit.res(5, RegisterNames.D) } // RES 5, D
        cbOperations[0xAB] = { singleBit.res(5, RegisterNames.E) } // RES 5, E
        cbOperations[0xAC] = { singleBit.res(5, RegisterNames.H) } // RES 5, H
        cbOperations[0xAD] = { singleBit.res(5, RegisterNames.L) } // RES 5, L
        cbOperations[0xAE] = { singleBit.resHL(5, cpu.CPURegisters.getHL()) } // RES 5, (HL)
        cbOperations[0xAF] = { singleBit.res(5, RegisterNames.A) } // RES 5, A
        cbOperations[0xB0] = { singleBit.res(6, RegisterNames.B) } // RES 6, B
        cbOperations[0xB1] = { singleBit.res(6, RegisterNames.C) } // RES 6, C
        cbOperations[0xB2] = { singleBit.res(6, RegisterNames.D) } // RES 6, D
        cbOperations[0xB3] = { singleBit.res(6, RegisterNames.E) } // RES 6, E
        cbOperations[0xB4] = { singleBit.res(6, RegisterNames.H) } // RES 6, H
        cbOperations[0xB5] = { singleBit.res(6, RegisterNames.L) } // RES 6, L
        cbOperations[0xB6] = { singleBit.resHL(6, cpu.CPURegisters.getHL()) } // RES 6, (HL)
        cbOperations[0xB7] = { singleBit.res(6, RegisterNames.A) } // RES 6, A
        cbOperations[0xB8] = { singleBit.res(7, RegisterNames.B) } // RES 7, B
        cbOperations[0xB9] = { singleBit.res(7, RegisterNames.C) } // RES 7, C
        cbOperations[0xBA] = { singleBit.res(7, RegisterNames.D) } // RES 7, D
        cbOperations[0xBB] = { singleBit.res(7, RegisterNames.E) } // RES 7, E
        cbOperations[0xBC] = { singleBit.res(7, RegisterNames.H) } // RES 7, H
        cbOperations[0xBD] = { singleBit.res(7, RegisterNames.L) } // RES 7, L
        cbOperations[0xBE] = { singleBit.resHL(7, cpu.CPURegisters.getHL()) } // RES 7, (HL)
        cbOperations[0xBF] = { singleBit.res(7, RegisterNames.A) } // RES 7, A
        cbOperations[0xC0] = { singleBit.set(0, RegisterNames.B) } // SET 0, B
        cbOperations[0xC1] = { singleBit.set(0, RegisterNames.C) } // SET 0, C
        cbOperations[0xC2] = { singleBit.set(0, RegisterNames.D) } // SET 0, D
        cbOperations[0xC3] = { singleBit.set(0, RegisterNames.E) } // SET 0, E
        cbOperations[0xC4] = { singleBit.set(0, RegisterNames.H) } // SET 0, H
        cbOperations[0xC5] = { singleBit.set(0, RegisterNames.L) } // SET 0, L
        cbOperations[0xC6] = { singleBit.setHL(0, cpu.CPURegisters.getHL()) } // SET 0, (HL)
        cbOperations[0xC7] = { singleBit.set(0, RegisterNames.A) } // SET 0, A
        cbOperations[0xC8] = { singleBit.set(1, RegisterNames.B) } // SET 1, B
        cbOperations[0xC9] = { singleBit.set(1, RegisterNames.C) } // SET 1, C
        cbOperations[0xCA] = { singleBit.set(1, RegisterNames.D) } // SET 1, D
        cbOperations[0xCB] = { singleBit.set(1, RegisterNames.E) } // SET 1, E
        cbOperations[0xCC] = { singleBit.set(1, RegisterNames.H) } // SET 1, H
        cbOperations[0xCD] = { singleBit.set(1, RegisterNames.L) } // SET 1, L
        cbOperations[0xCE] = { singleBit.setHL(1, cpu.CPURegisters.getHL()) } // SET 1, (HL)
        cbOperations[0xCF] = { singleBit.set(1, RegisterNames.A) } // SET 1, A
        cbOperations[0xD0] = { singleBit.set(2, RegisterNames.B) } // SET 2, B
        cbOperations[0xD1] = { singleBit.set(2, RegisterNames.C) } // SET 2, C
        cbOperations[0xD2] = { singleBit.set(2, RegisterNames.D) } // SET 2, D
        cbOperations[0xD3] = { singleBit.set(2, RegisterNames.E) } // SET 2, E
        cbOperations[0xD4] = { singleBit.set(2, RegisterNames.H) } // SET 2, H
        cbOperations[0xD5] = { singleBit.set(2, RegisterNames.L) } // SET 2, L
        cbOperations[0xD6] = { singleBit.setHL(2, cpu.CPURegisters.getHL()) } // SET 2, (HL)
        cbOperations[0xD7] = { singleBit.set(2, RegisterNames.A) } // SET 2, A
        cbOperations[0xD8] = { singleBit.set(3, RegisterNames.B) } // SET 3, B
        cbOperations[0xD9] = { singleBit.set(3, RegisterNames.C) } // SET 3, C
        cbOperations[0xDA] = { singleBit.set(3, RegisterNames.D) } // SET 3, D
        cbOperations[0xDB] = { singleBit.set(3, RegisterNames.E) } // SET 3, E
        cbOperations[0xDC] = { singleBit.set(3, RegisterNames.H) } // SET 3, H
        cbOperations[0xDD] = { singleBit.set(3, RegisterNames.L) } // SET 3, L
        cbOperations[0xDE] = { singleBit.setHL(3, cpu.CPURegisters.getHL()) } // SET 3, (HL)
        cbOperations[0xDF] = { singleBit.set(3, RegisterNames.A) } // SET 3, A
        cbOperations[0xE0] = { singleBit.set(4, RegisterNames.B) } // SET 4, B
        cbOperations[0xE1] = { singleBit.set(4, RegisterNames.C) } // SET 4, C
        cbOperations[0xE2] = { singleBit.set(4, RegisterNames.D) } // SET 4, D
        cbOperations[0xE3] = { singleBit.set(4, RegisterNames.E) } // SET 4, E
        cbOperations[0xE4] = { singleBit.set(4, RegisterNames.H) } // SET 4, H
        cbOperations[0xE5] = { singleBit.set(4, RegisterNames.L) } // SET 4, L
        cbOperations[0xE6] = { singleBit.setHL(4, cpu.CPURegisters.getHL()) } // SET 4, (HL)
        cbOperations[0xE7] = { singleBit.set(4, RegisterNames.A) } // SET 4, A
        cbOperations[0xE8] = { singleBit.set(5, RegisterNames.B) } // SET 5, B
        cbOperations[0xE9] = { singleBit.set(5, RegisterNames.C) } // SET 5, C
        cbOperations[0xEA] = { singleBit.set(5, RegisterNames.D) } // SET 5, D
        cbOperations[0xEB] = { singleBit.set(5, RegisterNames.E) } // SET 5, E
        cbOperations[0xEC] = { singleBit.set(5, RegisterNames.H) } // SET 5, H
        cbOperations[0xED] = { singleBit.set(5, RegisterNames.L) } // SET 5, L
        cbOperations[0xEE] = { singleBit.setHL(5, cpu.CPURegisters.getHL()) } // SET 5, (HL)
        cbOperations[0xEF] = { singleBit.set(5, RegisterNames.A) } // SET 5, A
        cbOperations[0xF0] = { singleBit.set(6, RegisterNames.B) } // SET 6, B
        cbOperations[0xF1] = { singleBit.set(6, RegisterNames.C) } // SET 6, C
        cbOperations[0xF2] = { singleBit.set(6, RegisterNames.D) } // SET 6, D
        cbOperations[0xF3] = { singleBit.set(6, RegisterNames.E) } // SET 6, E
        cbOperations[0xF4] = { singleBit.set(6, RegisterNames.H) } // SET 6, H
        cbOperations[0xF5] = { singleBit.set(6, RegisterNames.L) } // SET 6, L
        cbOperations[0xF6] = { singleBit.setHL(6, cpu.CPURegisters.getHL()) } // SET 6, (HL)
        cbOperations[0xF7] = { singleBit.set(6, RegisterNames.A) } // SET 6, A
        cbOperations[0xF8] = { singleBit.set(7, RegisterNames.B) } // SET 7, B
        cbOperations[0xF9] = { singleBit.set(7, RegisterNames.C) } // SET 7, C
        cbOperations[0xFA] = { singleBit.set(7, RegisterNames.D) } // SET 7, D
        cbOperations[0xFB] = { singleBit.set(7, RegisterNames.E) } // SET 7, E
        cbOperations[0xFC] = { singleBit.set(7, RegisterNames.H) } // SET 7, H
        cbOperations[0xFD] = { singleBit.set(7, RegisterNames.L) } // SET 7, L
        cbOperations[0xFE] = { singleBit.setHL(7, cpu.CPURegisters.getHL()) } // SET 7, (HL)
        cbOperations[0xFF] = { singleBit.set(7, RegisterNames.A) } // SET 7, A
    }

    /**
     * Decodes operation code to be executed and handles the operation to the helper classes
     *
     * @param operationCode to be executed
     */
    fun decode(operationCode: Int) {
        cpu.timers.tick()

        if (!cbInstruction) {
            regularOperations[operationCode].invoke()
        } else {
            cbOperations[operationCode].invoke()
            cbInstruction = false
        }
    }
}
