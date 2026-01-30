package com.github.rodrigotimoteo.kboyemucore

class CPUInstructions(
    private val cpu: CPU,
    private val memory: Memory
) {

    val DIVIDER_REGISTER: Int = 0xff04


    //Reset
    fun reset() {
        cpu!!.reset()
    }


    //Debugs
    fun show() {
        print("SP: " + String.format("%04X", cpu!!.getStackPointer() as Int) + " ")
        print("PC: 00:" + String.format("%04X", cpu!!.getProgramCounter() as Int) + " ")
        println(
            ("(" + String.format(
                "%02X",
                memory!!.getMemory(cpu!!.getProgramCounter()).code
            ) + " " + String.format(
                "%02X", memory!!.getMemory(
                    cpu!!.getProgramCounter() + 1
                ).code
            ) + " " + String.format(
                "%02X",
                memory!!.getMemory(cpu!!.getProgramCounter() + 2).code
            ) + " " + String.format(
                "%02X", memory!!.getMemory(
                    cpu!!.getProgramCounter() + 3
                ).code
            ) + ")")
        )
    }

    fun dumpRegisters() {
        print("A: " + String.format("%02X", cpu!!.getRegister(0) as Int) + " ")
        print("F: " + String.format("%02X", cpu!!.getRegister(5) as Int) + " ")
        print("B: " + String.format("%02X", cpu!!.getRegister(1) as Int) + " ")
        print("C: " + String.format("%02X", cpu!!.getRegister(2) as Int) + " ")
        print("D: " + String.format("%02X", cpu!!.getRegister(3) as Int) + " ")
        print("E: " + String.format("%02X", cpu!!.getRegister(4) as Int) + " ")
        print("H: " + String.format("%02X", cpu!!.getRegister(6) as Int) + " ")
        print("L: " + String.format("%02X", cpu!!.getRegister(7) as Int) + " ")
    }

    fun dumpFlags() {
        val zeroFlagINT: Int
        val subtractFlagINT: Int
        val halfCarryFlagINT: Int
        val carryFlagINT: Int

        if (cpu!!.getZeroFlag()) zeroFlagINT = 1
        else zeroFlagINT = 0

        if (cpu!!.getSubtractFlag()) subtractFlagINT = 1
        else subtractFlagINT = 0

        if (cpu!!.getHalfCarryFlag()) halfCarryFlagINT = 1
        else halfCarryFlagINT = 0

        if (cpu!!.getCarryFlag()) carryFlagINT = 1
        else carryFlagINT = 0

        print(" Flags Z:" + zeroFlagINT + " N:" + subtractFlagINT + " H:" + halfCarryFlagINT + " C:" + carryFlagINT + "  ")
    }


    //8-Bit Loads !!DONE!!
    //DONE
    /* register special types
        0 - (BC)
        1 - (DE)
        2 - (nn)
        Operation
        using A
         */
    fun ldTwoRegisters(register: Int) {
        var status = 0
        val address: Int

        when (register) {
            0 -> address = (cpu!!.getRegister(1) shl 8) + cpu!!.getRegister(2)
            1 -> address = (cpu!!.getRegister(3) shl 8) + cpu!!.getRegister(4)
            2 -> {
                cpu!!.handleCPUTimers()
                val addressLower = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
                cpu!!.handleCPUTimers()
                val addressUpper = memory!!.getMemory(cpu!!.getProgramCounter() + 2).code shl 8

                address = addressUpper + addressLower

                status = 2
            }

            else -> {
                return
            }
        }

        cpu!!.handleCPUTimers()
        memory!!.setMemory(address, cpu!!.getRegister(0))

        if (status == 0) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(3)
    }

    //DONE
    fun ldTwoRegistersIntoA(register: Int) {
        var status = 0
        val address: Int

        when (register) {
            0 -> address = (cpu!!.getRegister(1) shl 8) + cpu!!.getRegister(2)
            1 -> address = (cpu!!.getRegister(3) shl 8) + cpu!!.getRegister(4)
            2 -> {
                cpu!!.handleCPUTimers()
                val addressLower = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
                cpu!!.handleCPUTimers()
                val addressUpper = memory!!.getMemory(cpu!!.getProgramCounter() + 2).code shl 8

                address = addressLower + addressUpper
                status = 2
            }

            3 -> {
                address = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
                status = 1
            }

            else -> {
                return
            }
        }

        cpu!!.handleCPUTimers()
        if (register < 3) cpu!!.setRegister(0, memory!!.getMemory(address))
        else cpu!!.setRegister(0, address.toChar())

        if (status == 0) cpu!!.increaseProgramCounter(1)
        else if (status == 1) cpu!!.increaseProgramCounter(2)
        else cpu!!.increaseProgramCounter(3)
    }

    //DONE
    /* registerIn and Out special types
        8 - (HL)
        9 - n
         */
    fun ld(registerIn: Int, registerOut: Int) {
        var registerIn = registerIn
        var registerOut = registerOut
        var status = 0

        if (registerIn < 8 && registerOut < 8) cpu!!.setRegister(
            registerIn,
            cpu!!.getRegister(registerOut)
        )
        else if (registerIn < 8 && registerOut == 8) {
            registerOut = (cpu!!.getRegister(6) shl 8) + cpu!!.getRegister(7)
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(registerIn, (memory!!.getMemory(registerOut).code and 0xff).toChar())
        } else if (registerIn == 8 && registerOut < 8) {
            registerIn = (cpu!!.getRegister(6) shl 8) + cpu!!.getRegister(7)
            cpu!!.handleCPUTimers()
            memory!!.setMemory(registerIn, cpu!!.getRegister(registerOut))
        } else if (registerIn < 8) {
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(registerIn, memory!!.getMemory(cpu!!.getProgramCounter() + 1))
            status = 1
        } else if (registerOut == 9) {
            registerIn = ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            cpu!!.handleCPUTimers()
            memory!!.setMemory(
                registerIn,
                (memory!!.getMemory(cpu!!.getProgramCounter() + 1).code and 0xff).toChar()
            )
            status = 1
        }

        if (status == 0) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /* Type
        0 - LD A,(C)
        1 - LD (C), A
         */
    fun ldAC(type: Int) {
        val address: Int = 0xff00 + cpu!!.getRegister(2)

        cpu!!.handleCPUTimers()
        if (type == 0) cpu!!.setRegister(0, memory!!.getMemory(address))
        else memory!!.setMemory(address, cpu!!.getRegister(0))

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    /* Type
        0 - LDD A,(HL)
        1 - LDD (HL),A
         */
    fun ldd(type: Int) {
        var temp: Int = ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)

        if (type == 0) {
            ld(0, 8)
        } else {
            ld(8, 0)
        }

        temp = (temp - 1) and 0xffff

        cpu!!.setRegister(6, ((temp and 0xff00) shr 8).toChar())
        cpu!!.setRegister(7, (temp and 0x00ff).toChar())
    }

    //DONE
    /*Type
        0 - LDI A, (HL)
        1 - LDI (HL), A
         */
    fun ldi(type: Int) {
        var temp: Int = ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)

        if (type == 0) {
            ld(0, 8)
        } else {
            ld(8, 0)
        }

        temp = (temp + 1) and 0xffff

        cpu!!.setRegister(6, ((temp and 0xff00) shr 8).toChar())
        cpu!!.setRegister(7, (temp and 0x00ff).toChar())
    }

    //DONE
    /* Type
        0 - LDH (n), A
        1 - LDH A, (n)
         */
    fun ldh(type: Int) {
        cpu!!.handleCPUTimers()
        val address = (0xff00 + (memory!!.getMemory(cpu!!.getProgramCounter() + 1).code and 0xff))

        val a = cpu!!.getRegister(0)
        cpu!!.handleCPUTimers()
        if (type == 0) {
            if (address == 0xff00) memory!!.setMemory(address, a)
            else memory!!.setMemory(address, cpu!!.getRegister(0))
        } else cpu!!.setRegister(0, (memory!!.getMemory(address).code and 0xff).toChar())

        cpu!!.increaseProgramCounter(2)
    }


    //16-Bit Loads !!DONE!!
    //DONE
    /*Types
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    fun ld16bit(type: Int) {
        var in1 = 1
        var in2 = 2

        when (type) {
            0, 3 -> {}
            1 -> {
                in1 = 3
                in2 = 4
            }

            2 -> {
                in1 = 6
                in2 = 7
            }

            else -> return
        }

        if (type < 3) {
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(in2, memory!!.getMemory(cpu!!.getProgramCounter() + 1))
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(in1, memory!!.getMemory(cpu!!.getProgramCounter() + 2))
        } else {
            cpu!!.handleCPUTimers()
            cpu!!.handleCPUTimers()
            val temp =
                (memory!!.getMemory(cpu!!.getProgramCounter() + 1).code and 0xff) + ((memory!!.getMemory(
                    cpu!!.getProgramCounter() + 2
                ).code and 0xff) shl 8)
            cpu!!.setStackPointer(temp.toChar())
        }

        cpu!!.increaseProgramCounter(3)
    }

    //DONE
    fun ldSPHL() {
        val hl: Int = (((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff))
        cpu!!.setStackPointer(hl.toChar())
        cpu!!.handleCPUTimers()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun LDHL() {
        cpu!!.handleCPUTimers()
        var temp = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        if (((temp and 0x80) shr 7) == 1) temp = (temp and 0x7f) - 0x80

        val address: Int = (cpu!!.getStackPointer() + temp) and 0xffff

        cpu!!.setHalfCarryFlag((((cpu!!.getStackPointer() and 0xf) + (memory!!.getMemory(cpu!!.getProgramCounter() + 1).code and 0xf) and 0x10) === 0x10))
        cpu!!.setCarryFlag((((cpu!!.getStackPointer() and 0xff) + memory!!.getMemory(cpu!!.getProgramCounter() + 1)) and 0x100) === 0x100)

        cpu!!.setRegister(6, ((address and 0xff00) shr 8).toChar())
        cpu!!.setRegister(7, (address and 0x00ff).toChar())
        cpu!!.handleCPUTimers()

        cpu!!.setZeroFlag(false)
        cpu!!.setSubtractFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(2)
    }

    //DONE
    fun LDnnSP() {
        cpu!!.handleCPUTimers()
        val addressLower = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        cpu!!.handleCPUTimers()
        val addressUpper = memory!!.getMemory(cpu!!.getProgramCounter() + 2).code shl 8

        val address = addressLower + addressUpper

        cpu!!.handleCPUTimers()
        memory!!.setMemory(address + 1, ((cpu!!.getStackPointer() and 0xff00) shr 8) as Char)
        cpu!!.handleCPUTimers()
        memory!!.setMemory(address, (cpu!!.getStackPointer() and 0x00ff) as Char)

        cpu!!.increaseProgramCounter(3)
    }

    //DONE
    fun push(register: Int) {
        val in1: Int
        val in2: Int

        cpu!!.handleCPUTimers()

        when (register) {
            0 -> {
                in1 = 0
                in2 = 5
            }

            1 -> {
                in1 = 1
                in2 = 2
            }

            2 -> {
                in1 = 3
                in2 = 4
            }

            3 -> {
                in1 = 6
                in2 = 7
            }

            else -> return
        }

        cpu!!.handleCPUTimers()
        memory!!.setMemory(cpu!!.getStackPointer() - 1, cpu!!.getRegister(in1))
        cpu!!.handleCPUTimers()
        memory!!.setMemory(cpu!!.getStackPointer() - 2, cpu!!.getRegister(in2))

        cpu!!.increaseStackPointer(-2)
        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun pop(register: Int) {
        val in1: Int
        val in2: Int

        when (register) {
            0 -> {
                in1 = 0
                in2 = 5
            }

            1 -> {
                in1 = 1
                in2 = 2
            }

            2 -> {
                in1 = 3
                in2 = 4
            }

            3 -> {
                in1 = 6
                in2 = 7
            }

            else -> return
        }

        cpu!!.handleCPUTimers()
        var temp = memory!!.getMemory(cpu!!.getStackPointer() + 1).code
        cpu!!.setRegister(in1, temp.toChar())

        cpu!!.handleCPUTimers()
        if (register == 0) temp = memory!!.getMemory(cpu!!.getStackPointer()).code and 0xf0
        else temp = memory!!.getMemory(cpu!!.getStackPointer()).code
        cpu!!.setRegister(in2, temp.toChar())

        if (register == 0) cpu!!.computeFlags()

        cpu!!.increaseStackPointer(2)
        cpu!!.increaseProgramCounter(1)
    }


    //8-Bit ALU !!DONE!!
    //DONE
    /*Special Register
8 - (HL)
9 - #
    */
    fun add(register: Int) {
        var value: Int
        val a: Int

        if (register < 8) value = cpu!!.getRegister(register)
        else if (register == 8) {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
        } else {
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        }

        a = cpu!!.getRegister(0)
        cpu!!.setHalfCarryFlag((((a and 0xf) + (value and 0xf)) and 0x10) == 0x10)
        value += a

        cpu!!.setCarryFlag(value > 0xff)

        cpu!!.setRegister(0, (value and 0xff).toChar())
        cpu!!.setZeroFlag((value and 0xff) == 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.computeFRegister()

        if (register < 9) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
    */
    fun adc(register: Int) {
        var value: Int
        val a: Int
        val carry: Int

        if (register < 8) value = cpu!!.getRegister(register)
        else if (register == 8) {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
        } else {
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        }

        a = cpu!!.getRegister(0)
        carry = if (cpu!!.getCarryFlag()) 1 else 0
        cpu!!.setHalfCarryFlag((((a and 0xf) + (value and 0xf) + carry) and 0x10) == 0x10)
        value += (a + carry)

        cpu!!.setCarryFlag(value > 0xff)

        cpu!!.setRegister(0, (value and 0xff).toChar())
        cpu!!.setZeroFlag((value and 0xff) == 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.computeFRegister()

        if (register < 9) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Special Register
8 - (HL)
9 - #
*/
    fun sub(register: Int) {
        var value: Int
        val a: Int

        if (register < 8) value = cpu!!.getRegister(register)
        else if (register == 8) {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
        } else {
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        }

        a = cpu!!.getRegister(0)
        cpu!!.setCarryFlag(value > a)
        cpu!!.setHalfCarryFlag((value and 0x0f) > (a and 0x0f))
        value = (a - value) and 0xff

        cpu!!.setRegister(0, value.toChar())
        cpu!!.setZeroFlag(value == 0)
        cpu!!.setSubtractFlag(true)
        cpu!!.computeFRegister()

        if (register < 9) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
    */
    fun sbc(register: Int) {
        var value: Int
        val a: Int
        val carry: Int

        if (register < 8) value = cpu!!.getRegister(register)
        else if (register == 8) {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
        } else {
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        }

        carry = if (cpu!!.getCarryFlag()) 1 else 0
        a = cpu!!.getRegister(0)
        cpu!!.setHalfCarryFlag(((a and 0xf) - (value and 0xf) - carry) < 0)
        value = (a - value - carry)
        cpu!!.setCarryFlag(value < 0)

        cpu!!.setRegister(0, (value and 0xff).toChar())
        cpu!!.setZeroFlag((value and 0xff) == 0)
        cpu!!.setSubtractFlag(true)
        cpu!!.computeFRegister()

        if (register < 9) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
     */
    fun and(register: Int) {
        if (register < 8) cpu!!.setRegister(
            0,
            (cpu!!.getRegister(0) and cpu!!.getRegister(register)) as Char
        )
        else if (register == 8) {
            val temp: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(0, (cpu!!.getRegister(0) and memory!!.getMemory(temp)) as Char)
        } else {
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(
                0,
                (cpu!!.getRegister(0) and memory!!.getMemory(cpu!!.getProgramCounter() + 1)) as Char
            )
        }

        cpu!!.setZeroFlag(cpu!!.getRegister(0) === 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(true)
        cpu!!.setCarryFlag(false)

        cpu!!.computeFRegister()

        if (register < 9) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
     */
    fun or(register: Int) {
        if (register < 8) cpu!!.setRegister(
            0,
            (cpu!!.getRegister(0) or (cpu!!.getRegister(register))) as Char
        )
        else if (register == 8) {
            val temp: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(0, (cpu!!.getRegister(0) or (memory!!.getMemory(temp))) as Char)
        } else {
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(
                0,
                (cpu!!.getRegister(0) or (memory!!.getMemory(cpu!!.getProgramCounter() + 1))) as Char
            )
        }

        cpu!!.setZeroFlag(cpu!!.getRegister(0) === 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setCarryFlag(false)
        cpu!!.computeFRegister()

        if (register < 9) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
     */
    fun xor(register: Int) {
        if (register < 8) cpu!!.setRegister(
            0,
            ((cpu!!.getRegister(0) xor cpu!!.getRegister(register)) and 0xff) as Char
        )
        else if (register == 8) {
            val temp: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(
                0,
                ((cpu!!.getRegister(0) xor memory!!.getMemory(temp)) and 0xff) as Char
            )
        } else {
            cpu!!.handleCPUTimers()
            cpu!!.setRegister(
                0,
                ((cpu!!.getRegister(0) xor memory!!.getMemory(cpu!!.getProgramCounter() + 1)) and 0xff) as Char
            )
        }

        cpu!!.setZeroFlag(cpu!!.getRegister(0) === 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setCarryFlag(false)
        cpu!!.computeFRegister()

        if (register < 9) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
     */
    fun cp(register: Int) {
        val value: Int
        val a: Int

        if (register < 8) value = cpu!!.getRegister(register)
        else if (register == 8) {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
        } else {
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        }

        a = cpu!!.getRegister(0)

        cpu!!.setZeroFlag(a == value)
        cpu!!.setHalfCarryFlag((value and 0xf) > (a and 0xf))
        cpu!!.setCarryFlag(a < value)

        cpu!!.setSubtractFlag(true)
        cpu!!.computeFRegister()

        if (register < 9) cpu!!.increaseProgramCounter(1)
        else cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Special Register
    8 - (HL)
     */
    fun inc(register: Int) {
        var value: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            cpu!!.setHalfCarryFlag((value and 0xf) == 0xf)

            value = (value + 1) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
            cpu!!.setHalfCarryFlag((value and 0xf) == 0xf)

            value = (value + 1) and 0xff

            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, (value).toChar())
        }
        cpu!!.setZeroFlag(value == 0)

        cpu!!.setSubtractFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    /*Special Register
    8 - (HL)
     */
    fun dec(register: Int) {
        var value: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            cpu!!.setHalfCarryFlag((value and 0xf) == 0)

            value -= 1
            cpu!!.setRegister(register, (value and 0xff).toChar())
        } else {
            val temp: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(temp).code
            cpu!!.setHalfCarryFlag((value and 0xf) == 0)

            value -= 1
            cpu!!.handleCPUTimers()
            memory!!.setMemory(temp, (value and 0xff).toChar())
        }

        cpu!!.setZeroFlag(value == 0)
        cpu!!.setSubtractFlag(true)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }


    //16-Bit ALU !!DONE!!
    //DONE
    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    fun addHL(register: Int) {
        var register1 = 0
        var register2 = 0

        when (register) {
            0 -> {
                register1 = 1
                register2 = 2
            }

            1 -> {
                register1 = 3
                register2 = 4
            }

            2 -> {
                register1 = 6
                register2 = 7
            }

            3 -> {}
            else -> return
        }

        val hl: Int = ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)

        cpu!!.handleCPUTimers()
        if (register < 3) {
            var temp: Int =
                ((cpu!!.getRegister(register1) and 0xff) shl 8) + (cpu!!.getRegister(register2) and 0xff)

            cpu!!.setHalfCarryFlag((((hl and 0xfff) + (temp and 0xfff) and 0x1000) == 0x1000))
            cpu!!.setCarryFlag((((hl and 0xffff) + (temp and 0xffff))) > 0xffff)

            temp = (((hl and 0xffff) + (temp and 0xffff)) and 0xffff)

            cpu!!.setRegister(6, ((temp and 0xff00) shr 8).toChar())
            cpu!!.setRegister(7, (temp and 0x00ff).toChar())
        } else {
            cpu!!.setHalfCarryFlag((((hl and 0xfff) + (cpu!!.getStackPointer() and 0xfff) and 0x1000) === 0x1000))
            cpu!!.setCarryFlag((((hl and 0xffff) + (cpu!!.getStackPointer() and 0xffff)) > 0xffff))

            val temp: Int = (((hl and 0xffff) + (cpu!!.getStackPointer() and 0xffff)) and 0xffff)

            cpu!!.setRegister(6, ((temp and 0xff00) shr 8).toChar())
            cpu!!.setRegister(7, (temp and 0x00ff).toChar())
        }

        cpu!!.setSubtractFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun addSP() {
        cpu!!.handleCPUTimers()
        var temp = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        if (((temp and 0x80) shr 7) == 1) temp = (temp and 0x7f) - 0x80

        cpu!!.setHalfCarryFlag((((cpu!!.getStackPointer() and 0xf) + (1 and 0xf) and 0x10) === 0x10))
        cpu!!.setCarryFlag((((cpu!!.getStackPointer() and 0xff) + memory!!.getMemory(cpu!!.getProgramCounter() + 1)) and 0x100) === 0x100)


        temp = (cpu!!.getStackPointer() + temp) and 0xffff

        cpu!!.handleCPUTimers()
        cpu!!.handleCPUTimers()
        cpu!!.setStackPointer(temp.toChar())
        cpu!!.setZeroFlag(false)
        cpu!!.setSubtractFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    fun incR(register: Int) {
        var register1 = 0
        var register2 = 0

        when (register) {
            0 -> {
                register1 = 1
                register2 = 2
            }

            1 -> {
                register1 = 3
                register2 = 4
            }

            2 -> {
                register1 = 6
                register2 = 7
            }

            3 -> {}
            else -> return
        }

        cpu!!.handleCPUTimers()
        if (register < 3) {
            var temp: Int =
                ((cpu!!.getRegister(register1) and 0xff) shl 8) + (cpu!!.getRegister(register2) and 0xff)

            temp = (temp + 1) and 0xffff

            cpu!!.setRegister(register1, ((temp and 0xff00) shr 8).toChar())
            cpu!!.setRegister(register2, (temp and 0x00ff).toChar())
        } else {
            var temp: Int = cpu!!.getStackPointer()

            temp = (temp + 1) and 0xffff

            cpu!!.setStackPointer(temp.toChar())
        }


        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    fun decR(register: Int) {
        var register1 = 0
        var register2 = 0

        when (register) {
            0 -> {
                register1 = 1
                register2 = 2
            }

            1 -> {
                register1 = 3
                register2 = 4
            }

            2 -> {
                register1 = 6
                register2 = 7
            }

            3 -> {}
            else -> return
        }

        cpu!!.handleCPUTimers()
        if (register < 3) {
            var temp: Int =
                ((cpu!!.getRegister(register1) and 0xff) shl 8) + (cpu!!.getRegister(register2) and 0xff)

            temp = (temp - 1) and 0xffff

            cpu!!.setRegister(register1, ((temp and 0xff00) shr 8).toChar())
            cpu!!.setRegister(register2, (temp and 0x00ff).toChar())
        } else {
            var temp: Int = cpu!!.getStackPointer()

            temp = (temp - 1) and 0xffff

            cpu!!.setStackPointer(temp.toChar())
        }

        cpu!!.increaseProgramCounter(1)
    }


    //Miscellaneous !!DONE!!
    //DONE
    fun swap(register: Int) {
        var value: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            val temp = (value and 0xf0) shr 4
            val temp2 = (value and 0x0f) shl 4
            value = (temp or temp2) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
            val temp = (value and 0xf0) shr 4
            val temp2 = (value and 0x0f) shl 4

            value = (temp or temp2) and 0xff

            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, value.toChar())
        }

        cpu!!.setZeroFlag(value == 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setCarryFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun daa() {
        if (cpu!!.getSubtractFlag()) {
            if (cpu!!.getCarryFlag()) cpu!!.setRegister(
                0,
                ((cpu!!.getRegister(0) - 0x60) and 0xff) as Char
            )
            if (cpu!!.getHalfCarryFlag()) cpu!!.setRegister(
                0,
                ((cpu!!.getRegister(0) - 0x06) and 0xff) as Char
            )
        } else {
            if (cpu!!.getCarryFlag() || cpu!!.getRegister(0) > 0x99) {
                cpu!!.setRegister(0, ((cpu!!.getRegister(0) + 0x60) and 0xff) as Char)
                cpu!!.setCarryFlag(true)
            }
            if (cpu!!.getHalfCarryFlag() || (cpu!!.getRegister(0) and 0x0f) > 0x09) cpu!!.setRegister(
                0,
                ((cpu!!.getRegister(0) + 0x6) and 0xff) as Char
            )
        }

        cpu!!.setZeroFlag((cpu!!.getRegister(0) and 0xff) === 0)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun cpl() {
        cpu!!.setRegister(0, (cpu!!.getRegister(0).inv() and 0xff) as Char)

        cpu!!.setSubtractFlag(true)
        cpu!!.setHalfCarryFlag(true)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun ccf() {
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setCarryFlag(!cpu!!.getCarryFlag())
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun scf() {
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setCarryFlag(true)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun nop() {
        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun halt() {
        cpu!!.setIsHalted(true)
        cpu!!.setHaltCounter(cpu!!.getCounter())

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun stop() {
        cpu!!.setIsStopped(true)
        memory!!.setMemory(DIVIDER_REGISTER, 0.toChar())

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun di() {
        cpu!!.setChangeInterrupt(true)
        cpu!!.setChangeTo(false)
        cpu!!.setInterruptCounter(cpu!!.getCounter())

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun ei() {
        cpu!!.setChangeInterrupt(true)
        cpu!!.setChangeTo(true)
        cpu!!.setInterruptCounter(cpu!!.getCounter())

        cpu!!.increaseProgramCounter(1)
    }


    //Rotates and Shifts !!DONE!!
    //DONE
    fun rlca() {
        val carry: Int

        var value: Int = cpu!!.getRegister(0)
        cpu!!.setCarryFlag((value and 0x80) == 0x80)

        carry = (value and 0x80) shr 7
        value = ((value shl 1) and 0xff) or carry

        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setRegister(0, value.toChar())
        cpu!!.setZeroFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun rla() {
        val carry: Int

        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)

        var value: Int = cpu!!.getRegister(0)

        carry = if (cpu!!.getCarryFlag()) 1 else 0
        cpu!!.setCarryFlag((value and 0x80) == 0x80)

        value = ((value shl 1) and 0xff) or carry

        cpu!!.setRegister(0, value.toChar())
        cpu!!.setZeroFlag(false)

        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun rrca() {
        var value: Int
        val carry: Int

        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)

        value = cpu!!.getRegister(0)
        carry = (value and 0x01) shl 7

        cpu!!.setCarryFlag((value and 0x01) == 1)
        value = ((value shr 1) and 0xff) or carry

        cpu!!.setRegister(0, value.toChar())

        cpu!!.setZeroFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun rra() {
        val carry: Int
        var value: Int

        value = cpu!!.getRegister(0)
        carry = if (cpu!!.getCarryFlag()) 1 else 0

        cpu!!.setCarryFlag((value and 0x01) != 0)
        value = (((value shr 1) and 0xff) or (carry shl 7)) and 0xff

        cpu!!.setRegister(0, value.toChar())

        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setZeroFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun rlc(register: Int) {
        var value: Int
        val carry: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            carry = (value and 0x80) shr 7

            cpu!!.setCarryFlag((value and 0x80) != 0)
            value = (((value shl 1) and 0xff) or carry) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int = (cpu!!.getRegister(6) shl 8) + (cpu!!.getRegister(7))
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
            carry = (value and 0x80) shr 7

            cpu!!.setCarryFlag((value and 0x80) != 0)
            value = (((value shl 1) and 0xff) or carry) and 0xff

            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, value.toChar())
        }

        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setZeroFlag(value == 0)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun rl(register: Int) {
        var value: Int
        val carry: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            carry = if (cpu!!.getCarryFlag()) 1 else 0

            cpu!!.setCarryFlag((value and 0x80) != 0)
            value = (((value shl 1) and 0xff) or carry) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int = (cpu!!.getRegister(6) shl 8) + (cpu!!.getRegister(7))
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
            carry = if (cpu!!.getCarryFlag()) 1 else 0

            cpu!!.setCarryFlag((value and 0x80) != 0)
            value = (((value shl 1) and 0xff) or carry) and 0xff

            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, value.toChar())
        }

        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.setZeroFlag(value == 0)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun rrc(register: Int) {
        var value: Int
        val carry: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            carry = (value and 0x01) shl 7

            cpu!!.setCarryFlag((value and 0x01) != 0)
            value = (((value shr 1) and 0xff) or carry) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
            carry = (value and 0x01) shl 7

            cpu!!.setCarryFlag(((value and 0x01) != 0))
            value = (((value shr 1) and 0xff) or carry) and 0xff

            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, value.toChar())
        }

        cpu!!.setZeroFlag(value == 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun rr(register: Int) {
        var value: Int
        val carry: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            carry = if (cpu!!.getCarryFlag()) 1 else 0

            cpu!!.setCarryFlag((value and 0x01) != 0)
            value = (((value shr 1) and 0xff) or (carry shl 7)) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
            carry = if (cpu!!.getCarryFlag()) 1 else 0

            cpu!!.setCarryFlag(((value and 0x01) != 0))
            value = (((value shr 1) and 0xff) or (carry shl 7)) and 0xff

            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, value.toChar())
        }

        cpu!!.setZeroFlag(value == 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun sla(register: Int) {
        var value: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            cpu!!.setCarryFlag((value and 0x80) != 0)
            value = (value shl 1) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code

            cpu!!.setCarryFlag((value and 0x80) != 0)
            value = (value shl 1) and 0xff
            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, value.toChar())
        }

        cpu!!.setZeroFlag(value == 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun sra(register: Int) {
        var value: Int
        val carry: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)

            cpu!!.setCarryFlag((value and 0x01) != 0)
            carry = value and 0x80
            value = ((value shr 1) or carry) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code

            cpu!!.setCarryFlag((value and 0x01) != 0)
            carry = value and 0x80
            value = ((value shr 1) or carry) and 0xff

            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, value.toChar())
        }

        cpu!!.setZeroFlag(value == 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun srl(register: Int) {
        var value: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)

            cpu!!.setCarryFlag((value and 0x01) == 1)
            value = (value shr 1) and 0xff

            cpu!!.setRegister(register, value.toChar())
        } else {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code

            cpu!!.setCarryFlag((value and 0x01) == 1)
            value = (value shr 1) and 0xff

            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, value.toChar())
        }

        cpu!!.setZeroFlag(value == 0)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(false)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }


    //Bit Opcodes !!DONE!!
    //DONE
    fun bit(bit: Int, register: Int) {
        var register = register
        if (register != 8) register = cpu!!.getRegister(register)
        else {
            cpu!!.handleCPUTimers()
            val temp: Int = (cpu!!.getRegister(6) shl 8) + (cpu!!.getRegister(7))
            register = memory!!.getMemory(temp).code
        }

        val bitTest = (register and (1 shl bit)) != 0

        cpu!!.setZeroFlag(!bitTest)
        cpu!!.setSubtractFlag(false)
        cpu!!.setHalfCarryFlag(true)
        cpu!!.computeFRegister()

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun set(bit: Int, register: Int) {
        var register = register
        if (register != 8) {
            cpu!!.setRegister(register, (cpu!!.getRegister(register) or (1 shl bit)) as Char)
        } else {
            val address: Int =
                ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
            cpu!!.handleCPUTimers()
            register = memory!!.getMemory(address).code and 0xff
            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, (register or (1 shl bit)).toChar())
        }

        cpu!!.increaseProgramCounter(1)
    }

    //DONE
    fun res(bit: Int, register: Int) {
        val value: Int
        if (register < 8) {
            value = cpu!!.getRegister(register)
            cpu!!.setRegister(register, (value and ((1 shl bit).inv())).toChar())
        } else {
            val address: Int = (cpu!!.getRegister(6) shl 8) + cpu!!.getRegister(7)
            cpu!!.handleCPUTimers()
            value = memory!!.getMemory(address).code
            cpu!!.handleCPUTimers()
            memory!!.setMemory(address, (value and ((1 shl bit).inv())).toChar())
        }

        cpu!!.increaseProgramCounter(1)
    }


    //Jumps !!DONE!!
    //DONE
    fun jp() {
        cpu!!.handleCPUTimers()
        val addressLower = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        cpu!!.handleCPUTimers()
        val addressUpper = memory!!.getMemory(cpu!!.getProgramCounter() + 2).code shl 8

        val address = addressLower + addressUpper

        cpu!!.setProgramCounter(address.toChar())
        cpu!!.handleCPUTimers()
    }

    //DONE
    /* Type
   0 - RET NotZero
   1 - RET Zero
   2 - RET NoCarry
   3 - RET Carry
    */
    fun jpCond(type: Int) {
        val booleanTemp: Boolean

        when (type) {
            0 -> booleanTemp = !cpu!!.getZeroFlag()
            1 -> booleanTemp = cpu!!.getZeroFlag()
            2 -> booleanTemp = !cpu!!.getCarryFlag()
            3 -> booleanTemp = cpu!!.getCarryFlag()
            else -> return
        }

        cpu!!.handleCPUTimers()
        val addressLower = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        cpu!!.handleCPUTimers()
        val addressUpper = memory!!.getMemory(cpu!!.getProgramCounter() + 2).code shl 8

        val address = addressLower + addressUpper

        if (booleanTemp) {
            cpu!!.setProgramCounter(address.toChar())
            cpu!!.handleCPUTimers()
        } else cpu!!.increaseProgramCounter(3)
    }

    //DONE
    fun jpHL() {
        val temp: Int = ((cpu!!.getRegister(6) and 0xff) shl 8) + (cpu!!.getRegister(7) and 0xff)
        cpu!!.setProgramCounter(temp.toChar())
    }

    //DONE
    fun jr() {
        cpu!!.handleCPUTimers()
        val temp = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code

        if ((temp shr 7) == 0) cpu!!.increaseProgramCounter(temp and 0x7f)
        else cpu!!.increaseProgramCounter((temp and 0x7f) - 128)
        cpu!!.handleCPUTimers()

        cpu!!.increaseProgramCounter(2)
    }

    //DONE
    /* Type
 0 - RET NotZero
 1 - RET Zero
 2 - RET NoCarry
 3 - RET Carry
  */
    fun jrCond(type: Int) {
        val booleanTemp: Boolean

        when (type) {
            0 -> booleanTemp = !cpu!!.getZeroFlag()
            1 -> booleanTemp = cpu!!.getZeroFlag()
            2 -> booleanTemp = !cpu!!.getCarryFlag()
            3 -> booleanTemp = cpu!!.getCarryFlag()
            else -> return
        }

        cpu!!.handleCPUTimers()
        val address = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code

        if ((address shr 7) == 0 && booleanTemp) {
            cpu!!.increaseProgramCounter(address and 0x7f)
            cpu!!.handleCPUTimers()
        } else if (booleanTemp) {
            cpu!!.increaseProgramCounter((address and 0x7f) - 128)
            cpu!!.handleCPUTimers()
        }

        cpu!!.increaseProgramCounter(2)
    }


    //Calls !!DONE!!
    //DONE
    fun call() {
        cpu!!.handleCPUTimers()
        val addressLower = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        cpu!!.handleCPUTimers()
        val addressUpper = memory!!.getMemory(cpu!!.getProgramCounter() + 2).code shl 8

        val address = addressUpper + addressLower
        val tempProgramCounter: Int = cpu!!.getProgramCounter() + 3

        cpu!!.handleCPUTimers()

        cpu!!.setProgramCounter(address.toChar())
        cpu!!.handleCPUTimers()
        memory!!.setMemory(
            cpu!!.getStackPointer() - 1,
            (((tempProgramCounter) and 0xff00) shr 8).toChar()
        )
        cpu!!.handleCPUTimers()
        memory!!.setMemory(cpu!!.getStackPointer() - 2, ((tempProgramCounter) and 0xff).toChar())

        cpu!!.increaseStackPointer(-2)
    }

    //DONE
    /* Type
    0 - RET NotZero
    1 - RET Zero
    2 - RET NoCarry
    3 - RET Carry
     */
    fun callCond(type: Int) {
        val booleanTemp: Boolean

        when (type) {
            0 -> booleanTemp = !cpu!!.getZeroFlag()
            1 -> booleanTemp = cpu!!.getZeroFlag()
            2 -> booleanTemp = !cpu!!.getCarryFlag()
            3 -> booleanTemp = cpu!!.getCarryFlag()
            else -> return
        }

        cpu!!.handleCPUTimers()
        val addressLower = memory!!.getMemory(cpu!!.getProgramCounter() + 1).code
        cpu!!.handleCPUTimers()
        val addressUpper = memory!!.getMemory(cpu!!.getProgramCounter() + 2).code shl 8

        val address = addressLower + addressUpper

        if (booleanTemp) {
            val tempProgramCounter: Int = cpu!!.getProgramCounter() + 3
            cpu!!.handleCPUTimers()

            cpu!!.setProgramCounter(address.toChar())
            cpu!!.handleCPUTimers()
            memory!!.setMemory(
                cpu!!.getStackPointer() - 1,
                (((tempProgramCounter) and 0xff00) shr 8).toChar()
            )
            cpu!!.handleCPUTimers()
            memory!!.setMemory(
                cpu!!.getStackPointer() - 2,
                ((tempProgramCounter) and 0xff).toChar()
            )

            cpu!!.increaseStackPointer(-2)
            cpu!!.increaseCounter(3)
        } else {
            cpu!!.increaseProgramCounter(3)
        }
    }


    //Restarts !!DONE!!
    //DONE
    /*Type
    0 - 00H
    1 - 08H
    2 - 10H
    3 - 18H
    4 - 20H
    5 - 28H
    6 - 30H
    7 - 38H
     */
    fun rst(type: Int) {
        var address = 0

        cpu!!.handleCPUTimers()

        when (type) {
            0 -> {}
            1 -> address = 0x8
            2 -> address = 0x10
            3 -> address = 0x18
            4 -> address = 0x20
            5 -> address = 0x28
            6 -> address = 0x30
            7 -> address = 0x38
        }

        val programCounter: Int = cpu!!.getProgramCounter() + 1
        cpu!!.handleCPUTimers()
        memory!!.setMemory(
            cpu!!.getStackPointer() - 1,
            ((programCounter and 0xff00) shr 8).toChar()
        )
        cpu!!.handleCPUTimers()
        memory!!.setMemory(cpu!!.getStackPointer() - 2, (programCounter and 0xff).toChar())

        cpu!!.setProgramCounter(address.toChar())
        cpu!!.increaseStackPointer(-2)
    }


    //Returns !!DONE!!
    //DONE
    fun ret() {
        cpu!!.handleCPUTimers()
        val addressLower = memory!!.getMemory(cpu!!.getStackPointer()).code
        cpu!!.handleCPUTimers()
        val addressUpper = memory!!.getMemory(cpu!!.getStackPointer() + 1).code shl 8

        val address = addressLower + addressUpper

        cpu!!.setProgramCounter(address.toChar())
        cpu!!.handleCPUTimers()
        cpu!!.increaseStackPointer(2)
    }

    //DONE
    /* Type
    RET NotZero
    RET Zero
    RET NoCarry
    RET Carry
     */
    fun retCond(type: Int) {
        val booleanTemp: Boolean
        val address: Int

        when (type) {
            0 -> booleanTemp = !cpu!!.getZeroFlag()
            1 -> booleanTemp = cpu!!.getZeroFlag()
            2 -> booleanTemp = !cpu!!.getCarryFlag()
            3 -> booleanTemp = cpu!!.getCarryFlag()
            else -> return
        }
        cpu!!.handleCPUTimers()

        if (booleanTemp) {
            cpu!!.handleCPUTimers()
            val addressLower = memory!!.getMemory(cpu!!.getStackPointer()).code
            cpu!!.handleCPUTimers()
            val addressUpper = memory!!.getMemory(cpu!!.getStackPointer() + 1).code shl 8

            address = addressLower + addressUpper

            cpu!!.setProgramCounter(address.toChar())
            cpu!!.handleCPUTimers()
            cpu!!.increaseStackPointer(2)
        } else {
            cpu!!.increaseProgramCounter(1)
        }
    }

    //DONE
    fun reti() {
        val address: Int

        cpu!!.handleCPUTimers()
        val addressLower = memory!!.getMemory(cpu!!.getStackPointer()).code
        cpu!!.handleCPUTimers()
        val addressUpper = memory!!.getMemory(cpu!!.getStackPointer() + 1).code shl 8

        address = addressLower + addressUpper

        cpu!!.setProgramCounter(address.toChar())
        cpu!!.handleCPUTimers()

        cpu!!.setChangeInterrupt(true)
        cpu!!.setChangeTo(true)
        cpu!!.setInterruptCounter(cpu!!.getCounter())

        cpu!!.increaseStackPointer(2)
    }


    //Extras
    fun cb() {
        cpu!!.increaseProgramCounter(1)
    }

    fun readTAC() {
        //Timer Enabled
        cpu!!.setTimerEnabled(memory!!.testBit(0xff07, 2))
        //Timer Input Clock Select
        when (memory!!.getMemory(0xff07).code and 0x03) {
            1 -> cpu!!.setTimerFrequency(4)
            2 -> cpu!!.setTimerFrequency(16)
            3 -> cpu!!.setTimerFrequency(64)
        }
    }
}