package com.github.rodrigotimoteo.kboyemucore.cpu.instructions

import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.cpu.registers.RegisterNames
import kotlin.math.sin
import kotlin.system.exitProcess

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
        ::invalidOperation
    }

    /**
     * Holds all CB operations this makes it easier to access instead of using a giant when statement
     */
    private val cbOperations: Array<() -> Unit> = Array(256) {
        ::invalidOperation
    }

    init {
        regularOperations[0x00] = { control.nop() } // NOP
        regularOperations[0x01] = { load16Bit.ld16bit(0) } // LD BC, u16
        regularOperations[0x02] = { load8Bit.ldTwoRegisters(cpu.registers.getBC()) } // LD (BC), A
        regularOperations[0x03] = { alu.incR(cpu.registers.getBC()) }
//
//        0x03 ->  // INC BC
//        alu.incR(BusConstants.GET_BC, BusConstants.SET_BC)
//
//        0x04 ->  // INC B
//        alu.inc(RegisterNames.B)
//
//        0x05 ->  // DEC B
//        alu.dec(RegisterNames.B)
//
//        0x06 ->  // LD B,u8
//        load8Bit.ldNRegister(RegisterNames.B)
//
//        0x07 ->  // RLCA
//        rotateShift.rlca()
//
//        0x08 ->  // LD (u16),SP
//        load16Bit.ldNNSP()
//
//        0x09 ->  // ADD HL,BC
//        alu.addHL(BusConstants.GET_BC)
//
//        0x0A ->  // LD A,(BC)
//        load8Bit.ldTwoRegistersIntoA(BusConstants.GET_BC)
//
//        0x0B ->  // DEC BC
//        alu.decR(BusConstants.GET_BC, BusConstants.SET_BC)
//
//        0x0C ->  // INC C
//        alu.inc(RegisterNames.C)
//
//        0x0D ->  // DEC C
//        alu.dec(RegisterNames.C)
//
//        0x0E ->  // LD C,u8
//        load8Bit.ldNRegister(RegisterNames.C)
//
//        0x0F ->  // RRCA
//        rotateShift.rrca()

        regularOperations[0x10] = { control.stop() } // STOP

//        regularOperations[0x11] = { load16Bit.ld16bit(1) } // LD DE, u16
//
//        0x12 ->  // LD (DE),A
//        load8Bit.ldTwoRegisters(BusConstants.GET_DE)
//
//        0x13 ->  // INC DE
//        alu.incR(BusConstants.GET_DE, BusConstants.SET_DE)
//
//        0x14 ->  // INC D
//        alu.inc(RegisterNames.D)
//
//        0x15 ->  // DEC D
//        alu.dec(RegisterNames.D)
//
//        0x16 ->  // LD D,u8
//        load8Bit.ldNRegister(RegisterNames.D)
//
//        0x17 ->  // RLA
//        rotateShift.rla()
//
//        0x18 ->  // JR i8
//        jump.jr()
//
//        0x19 ->  // ADD HL,DE
//        alu.addHL(BusConstants.GET_DE)
//
//        0x1A ->  // LD A,(DE)
//        load8Bit.ldTwoRegistersIntoA(BusConstants.GET_DE)
//
//        0x1B ->  // DEC DE
//        alu.decR(BusConstants.GET_DE, BusConstants.SET_DE)
//
//        0x1C ->  // INC E
//        alu.inc(RegisterNames.E)
//
//        0x1D ->  // DEC E
//        alu.dec(RegisterNames.E)
//
//        0x1E ->  // LD E,u8
//        load8Bit.ldNRegister(RegisterNames.E)
//
//        0x1F ->  // RRA
//        rotateShift.rra()
//
//        0x20 ->  // JR NZ,i8
//        jump.jrCond(JumpConstants.NZ)
//
//        regularOperations[0x21] = { load16Bit.ld16bit(2) } // LD HL, u16
//
//        0x22 ->  // LDI (HL),A
//        load8Bit.ldi(true)
//
//        0x23 ->  // INC HL
//        alu.incR(BusConstants.GET_HL, BusConstants.SET_HL)
//
//        0x24 ->  // INC H
//        alu.inc(RegisterNames.H)
//
//        0x25 ->  // DEC H
//        alu.dec(RegisterNames.H)
//
//        0x26 ->  // LD H,u8
//        load8Bit.ldNRegister(RegisterNames.H)
//
//        0x27 ->  // DAA
//        alu.daa()
//
//        0x28 ->  // JR Z,u8
//        jump.jrCond(JumpConstants.Z)
//
//        0x29 ->  // ADD HL, HL
//        alu.addHL(BusConstants.GET_HL)
//
//        0x2A ->  // LDI A,(HL)
//        load8Bit.ldi(false)
//
//        0x2B ->  // DEC HL
//        alu.decR(BusConstants.GET_HL, BusConstants.SET_HL)
//
//        0x2C ->  // INC L
//        alu.inc(RegisterNames.L)
//
//        0x2D ->  // DEC L
//        alu.dec(RegisterNames.L)
//
//        0x2E ->  // LD L,u8
//        load8Bit.ldNRegister(RegisterNames.L)
//
//        0x2F ->  // CPL
//        alu.cpl()
//
//        0x30 ->  // JR NC,u8
//        jump.jrCond(JumpConstants.NC)
//
//        0x31 ->  // LD SP,u16
//        load16Bit.ldSPUU()
//
//        0x32 ->  // LDD (HL),A
//        load8Bit.ldd(true)
//
//        0x33 ->  // INC SP
//        alu.incSP()
//
//        0x34 ->  // INC (HL)
//        alu.incSpecial(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)
//
//        0x35 ->  // INC (HL)
//        alu.decSpecial(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)
//
//        0x36 ->  // LD (HL), n
//        load8Bit.ldNHL()

        regularOperations[0x37] = { control.scf() } // SCF

//        0x38 ->  // JR C,u8
//        jump.jrCond(JumpConstants.C)
//
//        0x39 ->  // ADD HL,SP
//        alu.addHLSP()
//
//        0x3A ->  // LDD A,(HL)
//        load8Bit.ldd(false)
//
//        0x3B ->  // DEC SP
//        alu.decSP()
//
//        0x3C ->  // INC A
//        alu.inc(RegisterNames.A)
//
//        0x3D ->  // DEC A
//        alu.dec(RegisterNames.A)
//
//        0x3E ->  // LD A,u8
//        load8Bit.ldNRegister(RegisterNames.A)

        regularOperations[0x3F] = { control.ccf() } // CCF

//        0x40 ->  // LD B,B
//        load8Bit.ld(RegisterNames.B, RegisterNames.B)
//
//        0x41 ->  // LD B,C
//        load8Bit.ld(RegisterNames.B, RegisterNames.C)
//
//        0x42 ->  // LD B,D
//        load8Bit.ld(RegisterNames.B, RegisterNames.D)
//
//        0x43 ->  // LD B,E
//        load8Bit.ld(RegisterNames.B, RegisterNames.E)
//
//        0x44 ->  // LD B,H
//        load8Bit.ld(RegisterNames.B, RegisterNames.H)
//
//        0x45 ->  // LD B,L
//        load8Bit.ld(RegisterNames.B, RegisterNames.L)
//
//        0x46 ->  // LD B,(HL)
//        load8Bit.ldHLtoRegister(RegisterNames.B)
//
//        0x47 ->  // LD B,A
//        load8Bit.ld(RegisterNames.B, RegisterNames.A)
//
//        0x48 ->  // LD C,B
//        load8Bit.ld(RegisterNames.C, RegisterNames.B)
//
//        0x49 ->  // LD C,C
//        load8Bit.ld(RegisterNames.C, RegisterNames.C)
//
//        0x4A ->  // LD C,D
//        load8Bit.ld(RegisterNames.C, RegisterNames.D)
//
//        0x4B ->  // LD C,E
//        load8Bit.ld(RegisterNames.C, RegisterNames.E)
//
//        0x4C ->  // LD C,H
//        load8Bit.ld(RegisterNames.C, RegisterNames.H)
//
//        0x4D ->  // LD C,L
//        load8Bit.ld(RegisterNames.C, RegisterNames.L)
//
//        0x4E ->  // LD C,(HL)
//        load8Bit.ldHLtoRegister(RegisterNames.C)
//
//        0x4F ->  // LD C,A
//        load8Bit.ld(RegisterNames.C, RegisterNames.A)
//
//        0x50 ->  // LD D,B
//        load8Bit.ld(RegisterNames.D, RegisterNames.B)
//
//        0x51 ->  // LD D,C
//        load8Bit.ld(RegisterNames.D, RegisterNames.C)
//
//        0x52 ->  // LD D,D
//        load8Bit.ld(RegisterNames.D, RegisterNames.D)
//
//        0x53 ->  // LD D,E
//        load8Bit.ld(RegisterNames.D, RegisterNames.E)
//
//        0x54 ->  // LD D,H
//        load8Bit.ld(RegisterNames.D, RegisterNames.H)
//
//        0x55 ->  // LD D,L
//        load8Bit.ld(RegisterNames.D, RegisterNames.L)
//
//        0x56 ->  // LD D,(HL)
//        load8Bit.ldHLtoRegister(RegisterNames.D)
//
//        0x57 ->  // LD D,A
//        load8Bit.ld(RegisterNames.D, RegisterNames.A)
//
//        0x58 ->  // LD E,B
//        load8Bit.ld(RegisterNames.E, RegisterNames.B)
//
//        0x59 ->  // LD E,C
//        load8Bit.ld(RegisterNames.E, RegisterNames.C)
//
//        0x5A ->  // LD E,D
//        load8Bit.ld(RegisterNames.E, RegisterNames.D)
//
//        0x5B ->  // LD E,E
//        load8Bit.ld(RegisterNames.E, RegisterNames.E)
//
//        0x5C ->  // LD E,H
//        load8Bit.ld(RegisterNames.E, RegisterNames.H)
//
//        0x5D ->  // LD E,L
//        load8Bit.ld(RegisterNames.E, RegisterNames.L)
//
//        0x5E ->  // LD E,(HL)
//        load8Bit.ldHLtoRegister(RegisterNames.E)
//
//        0x5F ->  // LD E,A
//        load8Bit.ld(RegisterNames.E, RegisterNames.A)
//
//        0x60 ->  // LD H,B
//        load8Bit.ld(RegisterNames.H, RegisterNames.B)
//
//        0x61 ->  // LD H,C
//        load8Bit.ld(RegisterNames.H, RegisterNames.C)
//
//        0x62 ->  // LD H,D
//        load8Bit.ld(RegisterNames.H, RegisterNames.D)
//
//        0x63 ->  // LD H,E
//        load8Bit.ld(RegisterNames.H, RegisterNames.E)
//
//        0x64 ->  // LD H,H
//        load8Bit.ld(RegisterNames.H, RegisterNames.H)
//
//        0x65 ->  // LD H,L
//        load8Bit.ld(RegisterNames.H, RegisterNames.L)
//
//        0x66 ->  // LD H,(HL)
//        load8Bit.ldHLtoRegister(RegisterNames.H)
//
//        0x67 ->  // LD H,A
//        load8Bit.ld(RegisterNames.H, RegisterNames.A)
//
//        0x68 ->  // LD L,B
//        load8Bit.ld(RegisterNames.L, RegisterNames.B)
//
//        0x69 ->  // LD L,C
//        load8Bit.ld(RegisterNames.L, RegisterNames.C)
//
//        0x6A ->  // LD L,D
//        load8Bit.ld(RegisterNames.L, RegisterNames.D)
//
//        0x6B ->  // LD L,E
//        load8Bit.ld(RegisterNames.L, RegisterNames.E)
//
//        0x6C ->  // LD L,H
//        load8Bit.ld(RegisterNames.L, RegisterNames.H)
//
//        0x6D ->  // LD L,L
//        load8Bit.ld(RegisterNames.L, RegisterNames.L)
//
//        0x6E ->  // LD L,(HL)
//        load8Bit.ldHLtoRegister(RegisterNames.L)
//
//        0x6F ->  // LD L,A
//        load8Bit.ld(RegisterNames.L, RegisterNames.A)
//
//        0x70 ->  // LD (HL),B
//        load8Bit.ldRtoHL(RegisterNames.B)
//
//        0x71 ->  // LD (HL),C
//        load8Bit.ldRtoHL(RegisterNames.C)
//
//        0x72 ->  // LD (HL),D
//        load8Bit.ldRtoHL(RegisterNames.D)
//
//        0x73 ->  // LD (HL),E
//        load8Bit.ldRtoHL(RegisterNames.E)
//
//        0x74 ->  // LD (HL),H
//        load8Bit.ldRtoHL(RegisterNames.H)
//
//        0x75 ->  // LD (HL),L
//        load8Bit.ldRtoHL(RegisterNames.L)

        regularOperations[0x76] = { control.halt() } // HALT

//
//        0x77 ->  // LD (HL),A
//        load8Bit.ldTwoRegisters(BusConstants.GET_HL)
//
//        0x78 ->  // LD A,B
//        load8Bit.ld(RegisterNames.A, RegisterNames.B)
//
//        0x79 ->  // LD A,C
//        load8Bit.ld(RegisterNames.A, RegisterNames.C)
//
//        0x7A ->  // LD A,D
//        load8Bit.ld(RegisterNames.A, RegisterNames.D)
//
//        0x7B ->  // LD A,E
//        load8Bit.ld(RegisterNames.A, RegisterNames.E)
//
//        0x7C ->  // LD A,H
//        load8Bit.ld(RegisterNames.A, RegisterNames.H)
//
//        0x7D ->  // LD A,L
//        load8Bit.ld(RegisterNames.A, RegisterNames.L)
//
//        0x7E ->  // LD A,(HL)
//        load8Bit.ldTwoRegistersIntoA(BusConstants.GET_HL)
//
//        0x7F ->  // LD A,A
//        load8Bit.ld(RegisterNames.A, RegisterNames.A)
//
//        0x80 ->  // ADD A,B
//        alu.add(RegisterNames.B)
//
//        0x81 ->  // ADD A,C
//        alu.add(RegisterNames.C)
//
//        0x82 ->  // ADD A,D
//        alu.add(RegisterNames.D)
//
//        0x83 ->  // ADD A,E
//        alu.add(RegisterNames.E)
//
//        0x84 ->  // ADD A, H
//        alu.add(RegisterNames.H)
//
//        0x85 ->  // ADD A,L
//        alu.add(RegisterNames.L)
//
//        0x86 ->  // ADD A,(HL)
//        alu.addSpecial(
//            (bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int),
//            true
//        )
//
//        0x87 ->  // ADD A,A
//        alu.add(RegisterNames.A)
//
//        0x88 ->  // ADC A,B
//        alu.adc(RegisterNames.B)
//
//        0x89 ->  // ADC A,C
//        alu.adc(RegisterNames.C)
//
//        0x8A ->  // ADC A,D
//        alu.adc(RegisterNames.D)
//
//        0x8B ->  // ADC A,E
//        alu.adc(RegisterNames.E)
//
//        0x8C ->  // ADC A,H
//        alu.adc(RegisterNames.H)
//
//        0x8D ->  // ADC A,L
//        alu.adc(RegisterNames.L)
//
//        0x8E ->  // ADC A,(HL)
//        alu.adcSpecial(
//            bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int,
//            true
//        )
//
//        0x8F ->  // ADC A,A
//        alu.adc(RegisterNames.A)
//
//        0x90 ->  // SUB A,B
//        alu.sub(RegisterNames.B)
//
//        0x91 ->  // SUB A,C
//        alu.sub(RegisterNames.C)
//
//        0x92 ->  // SUB A,D
//        alu.sub(RegisterNames.D)
//
//        0x93 ->  // SUB A,E
//        alu.sub(RegisterNames.E)
//
//        0x94 ->  // SUB A,H
//        alu.sub(RegisterNames.H)
//
//        0x95 ->  // SUB A,L
//        alu.sub(RegisterNames.L)
//
//        0x96 ->  // SUB A, (HL)
//        alu.subSpecial(
//            bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int,
//            true
//        )
//
//        0x97 ->  // SUB A,A
//        alu.sub(RegisterNames.A)
//
//        0x98 ->  // SBC A,B
//        alu.sbc(RegisterNames.B)
//
//        0x99 ->  // SBC A,C
//        alu.sbc(RegisterNames.C)
//
//        0x9A ->  // SBC A,D
//        alu.sbc(RegisterNames.D)
//
//        0x9B ->  // SBC A,E
//        alu.sbc(RegisterNames.E)
//
//        0x9C ->  // SBC A,H
//        alu.sbc(RegisterNames.H)
//
//        0x9D ->  // SBC A,L
//        alu.sbc(RegisterNames.L)
//
//        0x9E ->  // SBC A, (HL)
//        alu.sbcSpecial(
//            bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int,
//            true
//        )
//
//        0x9F ->  // SBC A,A
//        alu.sbc(RegisterNames.A)
//
//        0xA0 ->  // AND A,B
//        alu.and(RegisterNames.B)
//
//        0xA1 ->  // AND A,C
//        alu.and(RegisterNames.C)
//
//        0xA2 ->  // AND A,D
//        alu.and(RegisterNames.D)
//
//        0xA3 ->  // AND A,E
//        alu.and(RegisterNames.E)
//
//        0xA4 ->  // AND A,H
//        alu.and(RegisterNames.H)
//
//        0xA5 ->  // AND A,L
//        alu.and(RegisterNames.L)
//
//        0xA6 ->  // AND A,(HL)
//        alu.andSpecial(
//            bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int,
//            true
//        )
//
//        0xA7 ->  // AND A,A
//        alu.and(RegisterNames.A)
//
//        0xA8 ->  // XOR A,B
//        alu.xor(RegisterNames.B)
//
//        0xA9 ->  // XOR A,C
//        alu.xor(RegisterNames.C)
//
//        0xAA ->  // XOR A,D
//        alu.xor(RegisterNames.D)
//
//        0xAB ->  // XOR A,E
//        alu.xor(RegisterNames.E)
//
//        0xAC ->  // XOR A,H
//        alu.xor(RegisterNames.H)
//
//        0xAD ->  // XOR A,L
//        alu.xor(RegisterNames.L)
//
//        0xAE ->  // XOR A,(HL)
//        alu.xorSpecial(
//            bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int,
//            true
//        )
//
//        0xAF ->  // XOR A,A
//        alu.xor(RegisterNames.A)
//
//        0xB0 ->  // OR A,B
//        alu.or(RegisterNames.B)
//
//        0xB1 ->  // OR A,C
//        alu.or(RegisterNames.C)
//
//        0xB2 ->  // OR A,D
//        alu.or(RegisterNames.D)
//
//        0xB3 ->  // OR A,E
//        alu.or(RegisterNames.E)
//
//        0xB4 ->  // OR A,H
//        alu.or(RegisterNames.H)
//
//        0xB5 ->  // OR A,L
//        alu.or(RegisterNames.L)
//
//        0xB6 ->  // OR A,(HL)
//        alu.orSpecial(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int, true)
//
//        0xB7 ->  // OR A,A
//        alu.or(RegisterNames.A)
//
//        0xB8 ->  // CP A,B
//        alu.cp(RegisterNames.B)
//
//        0xB9 ->  // CP A,C
//        alu.cp(RegisterNames.C)
//
//        0xBA ->  // CP A,D
//        alu.cp(RegisterNames.D)
//
//        0xBB ->  // CP A,E
//        alu.cp(RegisterNames.E)
//
//        0xBC ->  // CP A,H
//        alu.cp(RegisterNames.H)
//
//        0xBD ->  // CP A,L
//        alu.cp(RegisterNames.L)
//
//        0xBE ->  // CP A,(HL)
//        alu.cpSpecial(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int, true)
//
//        0xBF ->  // CP A,A
//        alu.cp(RegisterNames.A)
//
//        0xC0 ->  // RET NZ
//        jump.retCond(JumpConstants.NZ)
//
//        0xC1 ->  // POP BC
//        load16Bit.pop(1)
//
//        0xC2 ->  // JP NZ,u16
//        jump.jpCond(JumpConstants.NZ)
//
//        0xC3 ->  // JP u16
//        jump.jp()
//
//        0xC4 ->  // CALL NZ, nn
//        jump.callCond(JumpConstants.NZ)
//
//        0xC5 ->  // PUSH BC
//        load16Bit.push(1)
//
//        0xC6 ->  // ADD A,#
//        alu.addSpecial(
//            bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1,
//            false
//        )
//
//        0xC7 ->  // RST 00H
//        jump.rst(0x00)
//
//        0xC8 ->  // RET Z
//        jump.retCond(JumpConstants.Z)
//
//        0xC9 ->  // RET
//        jump.ret()
//
//        0xCA ->  // JP Z,u16
//        jump.jpCond(JumpConstants.Z)
//
//        0xCB -> {
//            cbInstruction = true
//            bus.executeFromCPU(BusConstants.INCR_PC, arrayOf<String>("1"))
//            decode(
//                bus.getValue(
//                    (bus.getFromCPU(
//                        BusConstants.GET_PC,
//                        Bus.EMPTY_ARGUMENTS
//                    ) as Int)
//                )
//            )
//        }
//
//        0xCC ->  // CALL Z,nn
//        jump.callCond(JumpConstants.Z)
//
//        0xCD ->  // CALL u16
//        jump.call()
//
//        0xCE ->  // ADC A,#
//        alu.adcSpecial(
//            bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1,
//            false
//        )
//
//        0xCF ->  // RST 08H
//        jump.rst(0x08)
//
//        0xD0 ->  // RET NC
//        jump.retCond(JumpConstants.NC)
//
//        0xD1 ->  // POP DE
//        load16Bit.pop(2)
//
//        0xD2 ->  // JP NC,u16
//        jump.jpCond(JumpConstants.NC)
//
//        0xD4 ->  // CALL NC,nn
//        jump.callCond(JumpConstants.NC)
//
//        0xD5 ->  // PUSH DE
//        load16Bit.push(2)
//
//        0xD6 ->  // SUB A, #
//        alu.subSpecial(
//            bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1,
//            false
//        )
//
//        0xD7 ->  // RST 10H
//        jump.rst(0x10)
//
//        0xD8 ->  // RET C
//        jump.retCond(JumpConstants.C)
//
//        0xD9 ->  // RETI
//        jump.reti()
//
//        0xDA ->  // JP C,u16
//        jump.jpCond(JumpConstants.C)
//
//        0xDC ->  // CALL C,nn
//        jump.callCond(JumpConstants.C)
//
//        0xDE ->  // SBC A,#
//        alu.sbcSpecial(
//            bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1,
//            false
//        )
//
//        0xDF ->  // RST 18H
//        jump.rst(0x18)
//
//        0xE0 ->  // LD (FF00+u8),A
//        load8Bit.ldh(true)
//
//        0xE1 ->  // POP (HL)
//        load16Bit.pop(3)
//
//        0xE2 ->  // LD (C), A
//        load8Bit.ldAC(true)
//
//        0xE5 ->  // PUSH HL
//        load16Bit.push(3)
//
//        0xE6 ->  // AND #
//        alu.andSpecial(
//            bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1,
//            false
//        )
//
//        0xE7 ->  // RST 20H
//        jump.rst(0x20)
//
//        0xE8 ->  // ADD SP,n
//        alu.addSP(bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1)
//
//        0xE9 ->  // JP (HL)
//        jump.jpHL()
//
//        0xEA ->  // LD (nn),A
//        load8Bit.ldNN()
//
//        0xEE ->  // XOR #
//        alu.xorSpecial(
//            bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1,
//            false
//        )
//
//        0xEF ->  // RST 28H
//        jump.rst(0x28)
//
//        0xF0 ->  // LD A,(FF00+u8)
//        load8Bit.ldh(false)
//
//        0xF1 ->  // POP AF
//        load16Bit.pop(0)
//
//        0xF2 ->  // LD A,(C)
//        load8Bit.ldAC(false)

        regularOperations[0xF3] = { control.di() } // DI

//        0xF5 ->  // PUSH AF
//        load16Bit.push(0)
//
//        0xF6 ->  // OR #
//        alu.orSpecial(
//            bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1,
//            false
//        )
//
//        0xF7 ->  // RST 30H
//        jump.rst(0x30)
//
//        0xF8 ->  // LDHL SP,n
//        load16Bit.ldHL()
//
//        0xF9 ->  // LD SP,HL
//        load16Bit.ldSPHL()
//
//        0xFA ->  // LD A,(nn)
//        load8Bit.ldNNIntoA()

        regularOperations[0xFB] = { control.ei() } // EI

//        0xFE ->  // CP A,u8
//        alu.cpSpecial(
//            bus.getFromCPU(BusConstants.GET_PC, Bus.EMPTY_ARGUMENTS) as Int + 1,
//            false
//        )
//
//        0xFF ->  // RST 38H
//        jump.rst(0x38)
//
//        else -> {
//            println("No OPCode or Lacks Implementation")
//            exitProcess(0)
//        }

        0x00 ->  // RLC B
        rotateShift.rlc(RegisterNames.B)

        0x01 ->  // RLC C
        rotateShift.rlc(RegisterNames.C)

        0x02 ->  // RLC D
        rotateShift.rlc(RegisterNames.D)

        0x03 ->  // RLC E
        rotateShift.rlc(RegisterNames.E)

        0x04 ->  // RLC H
        rotateShift.rlc(RegisterNames.H)

        0x05 ->  // RLC L
        rotateShift.rlc(RegisterNames.L)

        0x06 ->  // RLC HL
        rotateShift.rlcHL(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)

        0x07 ->  // RLC A
        rotateShift.rlc(RegisterNames.A)

        0x08 ->  // RRC B
        rotateShift.rrc(RegisterNames.B)

        0x09 ->  // RRC C
        rotateShift.rrc(RegisterNames.C)

        0x0A ->  // RRC D
        rotateShift.rrc(RegisterNames.D)

        0x0B ->  // RRC E
        rotateShift.rrc(RegisterNames.E)

        0x0C ->  // RRC H
        rotateShift.rrc(RegisterNames.H)

        0x0D ->  // RRC L
        rotateShift.rrc(RegisterNames.L)

        0x0E ->  // RRC (HL)
        rotateShift.rrcHL(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)

        0x0F ->  // RRC A
        rotateShift.rrc(RegisterNames.A)

        0x10 ->  // RL B
        rotateShift.rl(RegisterNames.B)

        0x11 ->  // RL C
        rotateShift.rl(RegisterNames.C)

        0x12 ->  // RL D
        rotateShift.rl(RegisterNames.D)

        0x13 ->  // RL E
        rotateShift.rl(RegisterNames.E)

        0x14 ->  // RL H
        rotateShift.rl(RegisterNames.H)

        0x15 ->  // RL L
        rotateShift.rl(RegisterNames.L)

        0x16 ->  // RL (HL)
        rotateShift.rlHL(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)

        0x17 ->  // RL A
        rotateShift.rl(RegisterNames.A)

        0x18 ->  // RR B
        rotateShift.rr(RegisterNames.B)

        0x19 ->  // RR C
        rotateShift.rr(RegisterNames.C)

        0x1A ->  // RR D
        rotateShift.rr(RegisterNames.D)

        0x1B ->  // RR E
        rotateShift.rr(RegisterNames.E)

        0x1C ->  // RR H
        rotateShift.rr(RegisterNames.H)

        0x1D ->  // RR L
        rotateShift.rr(RegisterNames.L)

        0x1E ->  // RR (HL)
        rotateShift.rrHL(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)

        0x1F ->  // RR A
        rotateShift.rr(RegisterNames.A)

        0x20 ->  // SLA B
        rotateShift.sla(RegisterNames.B)

        0x21 ->  // SLA C
        rotateShift.sla(RegisterNames.C)

        0x22 ->  // SLA D
        rotateShift.sla(RegisterNames.D)

        0x23 ->  // SLA E
        rotateShift.sla(RegisterNames.E)

        0x24 ->  // SLA H
        rotateShift.sla(RegisterNames.H)

        0x25 ->  // SLA L
        rotateShift.sla(RegisterNames.L)

        0x26 ->  // SLA (HL)
        rotateShift.slaHL(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)

        0x27 ->  // SLA A
        rotateShift.sla(RegisterNames.A)

        0x28 ->  // SRA B
        rotateShift.sra(RegisterNames.B)

        0x29 ->  // SRA C
        rotateShift.sra(RegisterNames.C)

        0x2A ->  // SRA D
        rotateShift.sra(RegisterNames.D)

        0x2B ->  // SRA E
        rotateShift.sra(RegisterNames.E)

        0x2C ->  // SRA H
        rotateShift.sra(RegisterNames.H)

        0x2D ->  // SRA L
        rotateShift.sra(RegisterNames.L)

        0x2E ->  // SRA (HL)
        rotateShift.sraHL(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)

        0x2F ->  // SRA A
        rotateShift.sra(RegisterNames.A)

        0x30 ->  // SWAP B
        rotateShift.swap(RegisterNames.B)

        0x31 ->  // SWAP C
        rotateShift.swap(RegisterNames.C)

        0x32 ->  // SWAP D
        rotateShift.swap(RegisterNames.D)

        0x33 ->  // SWAP E
        rotateShift.swap(RegisterNames.E)

        0x34 ->  // SWAP H
        rotateShift.swap(RegisterNames.H)

        0x35 ->  // SWAP L
        rotateShift.swap(RegisterNames.L)

        0x36 ->  // SWAP (HL)
        rotateShift.swapHL(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)

        0x37 ->  // SWAP A
        rotateShift.swap(RegisterNames.A)

        0x38 ->  // SRL B
        rotateShift.srl(RegisterNames.B)

        0x39 ->  // SRL C
        rotateShift.srl(RegisterNames.C)

        0x3A ->  // SRL D
        rotateShift.srl(RegisterNames.D)

        0x3B ->  // SRL E
        rotateShift.srl(RegisterNames.E)

        0x3C ->  // SRL H
        rotateShift.srl(RegisterNames.H)

        0x3D ->  // SRL L
        rotateShift.srl(RegisterNames.L)

        0x3E ->  // SRL (HL)
        rotateShift.srlHL(bus.getFromCPU(BusConstants.GET_HL, Bus.EMPTY_ARGUMENTS) as Int)

        0x3F ->  // SRL A
        rotateShift.srl(RegisterNames.A)

        cbOperations[0x40] = { singleBit.bit(0, RegisterNames.B) } // BIT 0, B
        cbOperations[0x41] = { singleBit.bit(0, RegisterNames.C) } // BIT 0, C
        cbOperations[0x42] = { singleBit.bit(0, RegisterNames.D) } // BIT 0, D
        cbOperations[0x43] = { singleBit.bit(0, RegisterNames.E) } // BIT 0, E
        cbOperations[0x44] = { singleBit.bit(0, RegisterNames.H) } // BIT 0, H
        cbOperations[0x45] = { singleBit.bit(0, RegisterNames.L) } // BIT 0, L
        cbOperations[0x46] = { singleBit.bitHL(0, cpu.registers.getHL()) } // BIT 0, (HL)
        cbOperations[0x47] = { singleBit.bit(0, RegisterNames.A) } // BIT 0, A
        cbOperations[0x48] = { singleBit.bit(1, RegisterNames.B) } // BIT 1, B
        cbOperations[0x49] = { singleBit.bit(1, RegisterNames.C) } // BIT 1, C
        cbOperations[0x4A] = { singleBit.bit(1, RegisterNames.D) } // BIT 1, D
        cbOperations[0x4B] = { singleBit.bit(1, RegisterNames.E) } // BIT 1, E
        cbOperations[0x4C] = { singleBit.bit(1, RegisterNames.H) } // BIT 1, H
        cbOperations[0x4D] = { singleBit.bit(1, RegisterNames.L) } // BIT 1, L
        cbOperations[0x4E] = { singleBit.bitHL(1, cpu.registers.getHL()) } // BIT 1, (HL)
        cbOperations[0x4F] = { singleBit.bit(1, RegisterNames.A) } // BIT 1, A
        cbOperations[0x50] = { singleBit.bit(2, RegisterNames.B) } // BIT 2, B
        cbOperations[0x51] = { singleBit.bit(2, RegisterNames.C) } // BIT 2, C
        cbOperations[0x52] = { singleBit.bit(2, RegisterNames.D) } // BIT 2, D
        cbOperations[0x53] = { singleBit.bit(2, RegisterNames.E) } // BIT 2, E
        cbOperations[0x54] = { singleBit.bit(2, RegisterNames.H) } // BIT 2, H
        cbOperations[0x55] = { singleBit.bit(2, RegisterNames.L) } // BIT 2, L
        cbOperations[0x56] = { singleBit.bitHL(2, cpu.registers.getHL()) } // BIT 2, (HL)
        cbOperations[0x57] = { singleBit.bit(2, RegisterNames.A) } // BIT 2, A
        cbOperations[0x58] = { singleBit.bit(3, RegisterNames.B) } // BIT 3, B
        cbOperations[0x59] = { singleBit.bit(3, RegisterNames.C) } // BIT 3, C
        cbOperations[0x5A] = { singleBit.bit(3, RegisterNames.D) } // BIT 3, D
        cbOperations[0x5B] = { singleBit.bit(3, RegisterNames.E) } // BIT 3, E
        cbOperations[0x5C] = { singleBit.bit(3, RegisterNames.H) } // BIT 3, H
        cbOperations[0x5D] = { singleBit.bit(3, RegisterNames.L) } // BIT 3, L
        cbOperations[0x5E] = { singleBit.bitHL(3, cpu.registers.getHL()) } // BIT 3, (HL)
        cbOperations[0x5F] = { singleBit.bit(3, RegisterNames.A) } // BIT 3, A
        cbOperations[0x60] = { singleBit.bit(4, RegisterNames.B) } // BIT 4, B
        cbOperations[0x61] = { singleBit.bit(4, RegisterNames.C) } // BIT 4, C
        cbOperations[0x62] = { singleBit.bit(4, RegisterNames.D) } // BIT 4, D
        cbOperations[0x63] = { singleBit.bit(4, RegisterNames.E) } // BIT 4, E
        cbOperations[0x64] = { singleBit.bit(4, RegisterNames.H) } // BIT 4, H
        cbOperations[0x65] = { singleBit.bit(4, RegisterNames.L) } // BIT 4, L
        cbOperations[0x66] = { singleBit.bitHL(4, cpu.registers.getHL()) } // BIT 4, (HL)
        cbOperations[0x67] = { singleBit.bit(4, RegisterNames.A) } // BIT 4, A
        cbOperations[0x68] = { singleBit.bit(5, RegisterNames.B) } // BIT 5, B
        cbOperations[0x69] = { singleBit.bit(5, RegisterNames.C) } // BIT 5, C
        cbOperations[0x6A] = { singleBit.bit(5, RegisterNames.D) } // BIT 5, D
        cbOperations[0x6B] = { singleBit.bit(5, RegisterNames.E) } // BIT 5, E
        cbOperations[0x6C] = { singleBit.bit(5, RegisterNames.H) } // BIT 5, H
        cbOperations[0x6D] = { singleBit.bit(5, RegisterNames.L) } // BIT 5, L
        cbOperations[0x6E] = { singleBit.bitHL(5, cpu.registers.getHL()) } // BIT 5, (HL)
        cbOperations[0x6F] = { singleBit.bit(5, RegisterNames.A) } // BIT 5, A
        cbOperations[0x70] = { singleBit.bit(6, RegisterNames.B) } // BIT 6, B
        cbOperations[0x71] = { singleBit.bit(6, RegisterNames.C) } // BIT 6, C
        cbOperations[0x72] = { singleBit.bit(6, RegisterNames.D) } // BIT 6, D
        cbOperations[0x73] = { singleBit.bit(6, RegisterNames.E) } // BIT 6, E
        cbOperations[0x74] = { singleBit.bit(6, RegisterNames.H) } // BIT 6, H
        cbOperations[0x75] = { singleBit.bit(6, RegisterNames.L) } // BIT 6, L
        cbOperations[0x76] = { singleBit.bitHL(6, cpu.registers.getHL()) } // BIT 6, (HL)
        cbOperations[0x77] = { singleBit.bit(6, RegisterNames.A) } // BIT 6, A
        cbOperations[0x78] = { singleBit.bit(7, RegisterNames.B) } // BIT 7, B
        cbOperations[0x79] = { singleBit.bit(7, RegisterNames.C) } // BIT 7, C
        cbOperations[0x7A] = { singleBit.bit(7, RegisterNames.D) } // BIT 7, D
        cbOperations[0x7B] = { singleBit.bit(7, RegisterNames.E) } // BIT 7, E
        cbOperations[0x7C] = { singleBit.bit(7, RegisterNames.H) } // BIT 7, H
        cbOperations[0x7D] = { singleBit.bit(7, RegisterNames.L) } // BIT 7, L
        cbOperations[0x7E] = { singleBit.bitHL(7, cpu.registers.getHL()) } // BIT 7, (HL)
        cbOperations[0x7F] = { singleBit.bit(7, RegisterNames.A) } // BIT 7, A
        cbOperations[0x80] = { singleBit.res(0, RegisterNames.B) } // RES 0, B
        cbOperations[0x81] = { singleBit.res(0, RegisterNames.C) } // RES 0, C
        cbOperations[0x82] = { singleBit.res(0, RegisterNames.D) } // RES 0, D
        cbOperations[0x83] = { singleBit.res(0, RegisterNames.E) } // RES 0, E
        cbOperations[0x84] = { singleBit.res(0, RegisterNames.H) } // RES 0, H
        cbOperations[0x85] = { singleBit.res(0, RegisterNames.L) } // RES 0, L
        cbOperations[0x86] = { singleBit.resHL(0, cpu.registers.getHL()) } // RES 1, (HL)
        cbOperations[0x87] = { singleBit.res(0, RegisterNames.A) } // RES 0, A
        cbOperations[0x88] = { singleBit.res(1, RegisterNames.B) } // RES 1, B
        cbOperations[0x89] = { singleBit.res(1, RegisterNames.C) } // RES 1, C
        cbOperations[0x8A] = { singleBit.res(1, RegisterNames.D) } // RES 1, D
        cbOperations[0x8B] = { singleBit.res(1, RegisterNames.E) } // RES 1, E
        cbOperations[0x8C] = { singleBit.res(1, RegisterNames.H) } // RES 1, H
        cbOperations[0x8D] = { singleBit.res(1, RegisterNames.L) } // RES 1, L
        cbOperations[0x8E] = { singleBit.resHL(1, cpu.registers.getHL()) } // RES 1, (HL)
        cbOperations[0x8F] = { singleBit.res(1, RegisterNames.A) } // RES 1, A
        cbOperations[0x90] = { singleBit.res(2, RegisterNames.B) } // RES 2, B
        cbOperations[0x91] = { singleBit.res(2, RegisterNames.C) } // RES 2, C
        cbOperations[0x92] = { singleBit.res(2, RegisterNames.D) } // RES 2, D
        cbOperations[0x93] = { singleBit.res(2, RegisterNames.E) } // RES 2, E
        cbOperations[0x94] = { singleBit.res(2, RegisterNames.H) } // RES 2, H
        cbOperations[0x95] = { singleBit.res(2, RegisterNames.L) } // RES 2, L
        cbOperations[0x96] = { singleBit.resHL(2, cpu.registers.getHL()) } // RES 2, (HL)
        cbOperations[0x97] = { singleBit.res(2, RegisterNames.A) } // RES 2, A
        cbOperations[0x98] = { singleBit.res(3, RegisterNames.B) } // RES 3, B
        cbOperations[0x99] = { singleBit.res(3, RegisterNames.C) } // RES 3, C
        cbOperations[0x9A] = { singleBit.res(3, RegisterNames.D) } // RES 3, D
        cbOperations[0x9B] = { singleBit.res(3, RegisterNames.E) } // RES 3, E
        cbOperations[0x9C] = { singleBit.res(3, RegisterNames.H) } // RES 3, H
        cbOperations[0x9D] = { singleBit.res(3, RegisterNames.L) } // RES 3, L
        cbOperations[0x9E] = { singleBit.resHL(3, cpu.registers.getHL()) } // RES 3, (HL)
        cbOperations[0x9F] = { singleBit.res(3, RegisterNames.A) } // RES 3, A
        cbOperations[0xA0] = { singleBit.res(4, RegisterNames.B) } // RES 4, B
        cbOperations[0xA1] = { singleBit.res(4, RegisterNames.C) } // RES 4, C
        cbOperations[0xA2] = { singleBit.res(4, RegisterNames.D) } // RES 4, D
        cbOperations[0xA3] = { singleBit.res(4, RegisterNames.E) } // RES 4, E
        cbOperations[0xA4] = { singleBit.res(4, RegisterNames.H) } // RES 4, H
        cbOperations[0xA5] = { singleBit.res(4, RegisterNames.L) } // RES 4, L
        cbOperations[0xA6] = { singleBit.resHL(4, cpu.registers.getHL()) } // RES 4, (HL)
        cbOperations[0xA7] = { singleBit.res(4, RegisterNames.A) } // RES 4, A
        cbOperations[0xA8] = { singleBit.res(5, RegisterNames.B) } // RES 5, B
        cbOperations[0xA9] = { singleBit.res(5, RegisterNames.C) } // RES 5, C
        cbOperations[0xAA] = { singleBit.res(5, RegisterNames.D) } // RES 5, D
        cbOperations[0xAB] = { singleBit.res(5, RegisterNames.E) } // RES 5, E
        cbOperations[0xAC] = { singleBit.res(5, RegisterNames.H) } // RES 5, H
        cbOperations[0xAD] = { singleBit.res(5, RegisterNames.L) } // RES 5, L
        cbOperations[0xAE] = { singleBit.resHL(5, cpu.registers.getHL()) } // RES 5, (HL)
        cbOperations[0xAF] = { singleBit.res(5, RegisterNames.A) } // RES 5, A
        cbOperations[0xB0] = { singleBit.res(6, RegisterNames.B) } // RES 6, B
        cbOperations[0xB1] = { singleBit.res(6, RegisterNames.C) } // RES 6, C
        cbOperations[0xB2] = { singleBit.res(6, RegisterNames.D) } // RES 6, D
        cbOperations[0xB3] = { singleBit.res(6, RegisterNames.E) } // RES 6, E
        cbOperations[0xB4] = { singleBit.res(6, RegisterNames.H) } // RES 6, H
        cbOperations[0xB5] = { singleBit.res(6, RegisterNames.L) } // RES 6, L
        cbOperations[0xB6] = { singleBit.resHL(6, cpu.registers.getHL()) } // RES 6, (HL)
        cbOperations[0xB7] = { singleBit.res(6, RegisterNames.A) } // RES 6, A
        cbOperations[0xB8] = { singleBit.res(7, RegisterNames.B) } // RES 7, B
        cbOperations[0xB9] = { singleBit.res(7, RegisterNames.C) } // RES 7, C
        cbOperations[0xBA] = { singleBit.res(7, RegisterNames.D) } // RES 7, D
        cbOperations[0xBB] = { singleBit.res(7, RegisterNames.E) } // RES 7, E
        cbOperations[0xBC] = { singleBit.res(7, RegisterNames.H) } // RES 7, H
        cbOperations[0xBD] = { singleBit.res(7, RegisterNames.L) } // RES 7, L
        cbOperations[0xBE] = { singleBit.resHL(7, cpu.registers.getHL()) } // RES 7, (HL)
        cbOperations[0xBF] = { singleBit.res(7, RegisterNames.A) } // RES 7, A
        cbOperations[0xC0] = { singleBit.set(0, RegisterNames.B) } // SET 0, B
        cbOperations[0xC1] = { singleBit.set(0, RegisterNames.C) } // SET 0, C
        cbOperations[0xC2] = { singleBit.set(0, RegisterNames.D) } // SET 0, D
        cbOperations[0xC3] = { singleBit.set(0, RegisterNames.E) } // SET 0, E
        cbOperations[0xC4] = { singleBit.set(0, RegisterNames.H) } // SET 0, H
        cbOperations[0xC5] = { singleBit.set(0, RegisterNames.L) } // SET 0, L
        cbOperations[0xC6] = { singleBit.setHL(0, cpu.registers.getHL()) } // SET 0, (HL)
        cbOperations[0xC7] = { singleBit.set(0, RegisterNames.A) } // SET 0, A
        cbOperations[0xC8] = { singleBit.set(1, RegisterNames.B) } // SET 1, B
        cbOperations[0xC9] = { singleBit.set(1, RegisterNames.C) } // SET 1, C
        cbOperations[0xCA] = { singleBit.set(1, RegisterNames.D) } // SET 1, D
        cbOperations[0xCB] = { singleBit.set(1, RegisterNames.E) } // SET 1, E
        cbOperations[0xCC] = { singleBit.set(1, RegisterNames.H) } // SET 1, H
        cbOperations[0xCD] = { singleBit.set(1, RegisterNames.L) } // SET 1, L
        cbOperations[0xCE] = { singleBit.setHL(1, cpu.registers.getHL()) } // SET 1, (HL)
        cbOperations[0xCF] = { singleBit.set(1, RegisterNames.A) } // SET 1, A
        cbOperations[0xD0] = { singleBit.set(2, RegisterNames.B) } // SET 2, B
        cbOperations[0xD1] = { singleBit.set(2, RegisterNames.C) } // SET 2, C
        cbOperations[0xD2] = { singleBit.set(2, RegisterNames.D) } // SET 2, D
        cbOperations[0xD3] = { singleBit.set(2, RegisterNames.E) } // SET 2, E
        cbOperations[0xD4] = { singleBit.set(2, RegisterNames.H) } // SET 2, H
        cbOperations[0xD5] = { singleBit.set(2, RegisterNames.L) } // SET 2, L
        cbOperations[0xD6] = { singleBit.setHL(2, cpu.registers.getHL()) } // SET 2, (HL)
        cbOperations[0xD7] = { singleBit.set(2, RegisterNames.A) } // SET 2, A
        cbOperations[0xD8] = { singleBit.set(3, RegisterNames.B) } // SET 3, B
        cbOperations[0xD9] = { singleBit.set(3, RegisterNames.C) } // SET 3, C
        cbOperations[0xDA] = { singleBit.set(3, RegisterNames.D) } // SET 3, D
        cbOperations[0xDB] = { singleBit.set(3, RegisterNames.E) } // SET 3, E
        cbOperations[0xDC] = { singleBit.set(3, RegisterNames.H) } // SET 3, H
        cbOperations[0xDD] = { singleBit.set(3, RegisterNames.L) } // SET 3, L
        cbOperations[0xDE] = { singleBit.setHL(3, cpu.registers.getHL()) } // SET 3, (HL)
        cbOperations[0xDF] = { singleBit.set(3, RegisterNames.A) } // SET 3, A
        cbOperations[0xE0] = { singleBit.set(4, RegisterNames.B) } // SET 4, B
        cbOperations[0xE1] = { singleBit.set(4, RegisterNames.C) } // SET 4, C
        cbOperations[0xE2] = { singleBit.set(4, RegisterNames.D) } // SET 4, D
        cbOperations[0xE3] = { singleBit.set(4, RegisterNames.E) } // SET 4, E
        cbOperations[0xE4] = { singleBit.set(4, RegisterNames.H) } // SET 4, H
        cbOperations[0xE5] = { singleBit.set(4, RegisterNames.L) } // SET 4, L
        cbOperations[0xE6] = { singleBit.setHL(4, cpu.registers.getHL()) } // SET 4, (HL)
        cbOperations[0xE7] = { singleBit.set(4, RegisterNames.A) } // SET 4, A
        cbOperations[0xE8] = { singleBit.set(5, RegisterNames.B) } // SET 5, B
        cbOperations[0xE9] = { singleBit.set(5, RegisterNames.C) } // SET 5, C
        cbOperations[0xEA] = { singleBit.set(5, RegisterNames.D) } // SET 5, D
        cbOperations[0xEB] = { singleBit.set(5, RegisterNames.E) } // SET 5, E
        cbOperations[0xEC] = { singleBit.set(5, RegisterNames.H) } // SET 5, H
        cbOperations[0xED] = { singleBit.set(5, RegisterNames.L) } // SET 5, L
        cbOperations[0xEE] = { singleBit.setHL(5, cpu.registers.getHL()) } // SET 5, (HL)
        cbOperations[0xEF] = { singleBit.set(5, RegisterNames.A) } // SET 5, A
        cbOperations[0xF0] = { singleBit.set(6, RegisterNames.B) } // SET 6, B
        cbOperations[0xF1] = { singleBit.set(6, RegisterNames.C) } // SET 6, C
        cbOperations[0xF2] = { singleBit.set(6, RegisterNames.D) } // SET 6, D
        cbOperations[0xF3] = { singleBit.set(6, RegisterNames.E) } // SET 6, E
        cbOperations[0xF4] = { singleBit.set(6, RegisterNames.H) } // SET 6, H
        cbOperations[0xF5] = { singleBit.set(6, RegisterNames.L) } // SET 6, L
        cbOperations[0xF6] = { singleBit.setHL(6, cpu.registers.getHL()) } // SET 6, (HL)
        cbOperations[0xF7] = { singleBit.set(6, RegisterNames.A) } // SET 6, A
        cbOperations[0xF8] = { singleBit.set(7, RegisterNames.B) } // SET 7, B
        cbOperations[0xF9] = { singleBit.set(7, RegisterNames.C) } // SET 7, C
        cbOperations[0xFA] = { singleBit.set(7, RegisterNames.D) } // SET 7, D
        cbOperations[0xFB] = { singleBit.set(7, RegisterNames.E) } // SET 7, E
        cbOperations[0xFC] = { singleBit.set(7, RegisterNames.H) } // SET 7, H
        cbOperations[0xFD] = { singleBit.set(7, RegisterNames.L) } // SET 7, L
        cbOperations[0xFE] = { singleBit.setHL(7, cpu.registers.getHL()) } // SET 7, (HL)
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

    /**
     * This method exists as a default value for all instructions therefore if its not overridden
     * then the instruction is invalid
     *
     * @throws IllegalArgumentException when executed (should not be executed default value only)
     */
    private fun invalidOperation() {
        throw IllegalArgumentException("This instruction doesn't exist")
    }
}
