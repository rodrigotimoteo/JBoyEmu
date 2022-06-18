public class CPUInstructions {

    private final static int DIVIDER_REGISTER = 0xff04;

    private static CPU cpu;
    private static Memory memory;

    private static boolean DEBUGMODE = false;

    public static void setCpu(CPU cpuIn) {
        cpu = cpuIn;
    }
    public static void setMem(Memory memoryIn) {
        memory = memoryIn;
    }

    //Reset

    public static void reset() {
        cpu.reset();
    }

    //Debugs

    public static void show() {
        System.out.print("SP: " + String.format("%04X", (int) cpu.getStackPointer()) + " ");
        System.out.print("PC: 00:" + String.format("%04X", (int) cpu.getProgramCounter()) + " ");
        System.out.println("(" + String.format("%02X", (int) memory.getMemory(cpu.getProgramCounter()))
        + " " + String.format("%02X", (int) memory.getMemory(cpu.getProgramCounter() + 1))
        + " " + String.format("%02X", (int) memory.getMemory(cpu.getProgramCounter() + 2))
        + " " + String.format("%02X", (int) memory.getMemory(cpu.getProgramCounter() + 3)) + ")");
    }

    public static void dumpRegisters() {
        System.out.print("A: " + String.format("%02X", (int) cpu.getRegister(0)) + " ");
        System.out.print("F: " + String.format("%02X", (int) cpu.getRegister(5)) + " ");
        System.out.print("B: " + String.format("%02X", (int) cpu.getRegister(1)) + " ");
        System.out.print("C: " + String.format("%02X", (int) cpu.getRegister(2)) + " ");
        System.out.print("D: " + String.format("%02X", (int) cpu.getRegister(3)) + " ");
        System.out.print("E: " + String.format("%02X", (int) cpu.getRegister(4)) + " ");
        System.out.print("H: " + String.format("%02X", (int) cpu.getRegister(6)) + " ");
        System.out.print("L: " + String.format("%02X", (int) cpu.getRegister(7)) + " ");
    }

    public static void dumpFlags() {
        int zeroFlagINT;
        int subtractFlagINT;
        int halfCarryFlagINT;
        int carryFlagINT;

        if(cpu.getZeroFlag())
            zeroFlagINT = 1;
        else
            zeroFlagINT = 0;

        if(cpu.getSubtractFlag())
            subtractFlagINT = 1;
        else
            subtractFlagINT = 0;

        if(cpu.getHalfCarryFlag())
            halfCarryFlagINT = 1;
        else
            halfCarryFlagINT = 0;

        if(cpu.getCarryFlag())
            carryFlagINT = 1;
        else
            carryFlagINT = 0;

        System.out.print(" Flags Z:" + zeroFlagINT + " N:" + subtractFlagINT + " H:" + halfCarryFlagINT + " C:" + carryFlagINT + "  ");
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
    public static void ldTwoRegisters(int register) {
        int status = 0, address;

        if(register != 2) cpu.increaseCounter(2);
        else cpu.increaseCounter(4);

        switch(register) {
            case 0 -> address = (cpu.getRegister(1) << 8) + cpu.getRegister(2);
            case 1 -> address = (cpu.getRegister(3) << 8) + cpu.getRegister(4);
            case 2 -> { address = (memory.getMemory(cpu.getProgramCounter() + 2) << 8) + memory.getMemory(cpu.getProgramCounter() + 1); status = 2; }
            default -> { return; }
        }


        if(DEBUGMODE) System.out.println("LD " + Integer.toHexString(address) + ", A");
        //else DEBUGMODE = true;

        memory.setMemory(address, cpu.getRegister(0));

        if(status == 0) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(3);
    }

    //DONE
    public static void ldTwoRegistersIntoA(int register) {
        int status = 0, address;

        if(register == 2) cpu.increaseCounter(4);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0 -> address = (cpu.getRegister(1) << 8) + cpu.getRegister(2);
            case 1 -> address = (cpu.getRegister(3) << 8) + cpu.getRegister(4);
            case 2 -> { address = (memory.getMemory(cpu.getProgramCounter() + 2) << 8) + memory.getMemory(cpu.getProgramCounter() + 1); status = 2; }
            case 3 -> { address = memory.getMemory(cpu.getProgramCounter() + 1); status = 1; }
            default -> { return; }
        }

        if(DEBUGMODE) System.out.println("LD A, " + Integer.toHexString(register));
        //else DEBUGMODE = true;

        if(register < 3) cpu.setRegister(0, memory.getMemory(address));
        else cpu.setRegister(0, (char) address);

        if(status == 0) cpu.increaseProgramCounter(1);
        else if(status == 1) cpu.increaseProgramCounter(2);
        else cpu.increaseProgramCounter(3);
    }

    //DONE
    /* registerIn and Out special types
        8 - (HL)
        9 - n
         */
    public static void ld(int registerIn, int registerOut) {
        String registerInChar;
        String registerOutChar;
        int status = 0;

        if(registerIn < 8 && registerOut < 8) cpu.increaseCounter(1);
        else if(registerIn < 8 && registerOut == 8) cpu.increaseCounter(2);
        else if(registerIn == 8 && registerOut < 8) cpu.increaseCounter(2);
        else if(registerIn < 8) cpu.increaseCounter(2);
        else if(registerOut == 9) cpu.increaseCounter(3);

        switch(registerIn) {
            case 0: registerInChar = "A"; break;
            case 1: registerInChar = "B"; break;
            case 2: registerInChar = "C"; break;
            case 3: registerInChar = "D"; break;
            case 4: registerInChar = "E"; break;
            case 6: registerInChar = "H"; break;
            case 7: registerInChar = "L"; break;
            case 8: registerInChar = Integer.toHexString(((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff)); break;
            default: return;
        }

        switch(registerOut) {
            case 0: registerOutChar = "A"; break;
            case 1: registerOutChar = "B"; break;
            case 2: registerOutChar = "C"; break;
            case 3: registerOutChar = "D"; break;
            case 4: registerOutChar = "E"; break;
            case 6: registerOutChar = "H"; break;
            case 7: registerOutChar = "L"; break;
            case 8: registerOutChar = Integer.toHexString(((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff)); break;
            case 9: registerOutChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1) & 0xff); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("LD " + registerInChar + ", " + registerOutChar);
        //else DEBUGMODE = true;

        if(registerIn < 8 && registerOut < 8) cpu.setRegister(registerIn, cpu.getRegister(registerOut));
        else if(registerIn < 8 && registerOut == 8) {
            registerOut = (cpu.getRegister(6) << 8) + cpu.getRegister(7);
            cpu.setRegister(registerIn, (char) (memory.getMemory(registerOut) & 0xff));
        } else if(registerIn == 8 && registerOut < 8) {
            registerIn = (cpu.getRegister(6) << 8) + cpu.getRegister(7);
            memory.setMemory(registerIn, cpu.getRegister(registerOut));
        } else if(registerIn < 8){
            cpu.setRegister(registerIn, memory.getMemory(cpu.getProgramCounter() + 1));
            status = 1;
        } else if(registerOut == 9) {
            registerIn = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            memory.setMemory(registerIn, (char) (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));
            status = 1;
        }

        if(status == 0) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE
    /* Type
        0 - LD A,(C)
        1 - LD (C), A
         */
    public static void ldAC(int type) {
            cpu.increaseCounter(2);

            int address = 0xff00 + cpu.getRegister(2);

            if(DEBUGMODE && type == 0) System.out.println("LD A, " + Integer.toHexString(address));
            if(DEBUGMODE && type == 1) System.out.println("LD " + Integer.toHexString(address) + ", A");

            if(type == 0) cpu.setRegister(0, memory.getMemory(address));
            else memory.setMemory(address, cpu.getRegister(0));

            cpu.increaseProgramCounter(1);
        }

    //DONE
    /* Type
        0 - LDD A,(HL)
        1 - LDD (HL),A
         */
    public static void ldd(int type) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);

            if(DEBUGMODE && type == 0) System.out.println("LDD A, " + Integer.toHexString(temp));
            if(DEBUGMODE && type == 1) System.out.println("LDD " + Integer.toHexString(temp) + ", A");
            if(DEBUGMODE) DEBUGMODE = false;

            if(type == 0) ld(0, 8);
            else ld(8, 0);

            temp = (temp - 1) & 0xffff;

            cpu.setRegister(6, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(7, (char) (temp & 0x00ff));
        }

    //DONE
    /*Type
        0 - LDI A, (HL)
        1 - LDI (HL), A
         */
    public static void ldi(int type) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);

            if(DEBUGMODE && type == 0) System.out.println("LDI A, " + Integer.toHexString(temp));
            else if(DEBUGMODE && type == 1) System.out.println("LDI " + Integer.toHexString(temp) + ", A");
            if(DEBUGMODE) DEBUGMODE = false;

            if(type == 0) ld(0, 8);
            else ld(8, 0);

            temp = (temp + 1) & 0xffff;

            cpu.setRegister(6, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(7, (char) (temp & 0x00ff));
        }

    //DONE
    /* Type
        0 - LDH (n), A
        1 - LDH A, (n)
         */
    public static void ldh(int type) {
        cpu.increaseCounter(3);
        int address = (0xff00 + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));

        if(DEBUGMODE && type == 0) System.out.println("LDH " + Integer.toHexString(address) + ", A");
        if(DEBUGMODE && type == 1) System.out.println("LDH A, " + Integer.toHexString(address));

        char a = cpu.getRegister(0);
        if(type == 0) {
            if(address == 0xff00) memory.setMemory(address, a);
            else memory.setMemory(address, cpu.getRegister(0));
        }
        else cpu.setRegister(0, (char) (memory.getMemory(address) & 0xff));

        cpu.increaseProgramCounter(2);
    }




    //16-Bit Loads !!DONE!!

    //DONE
    /*Types
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    public static void ld16bit(int type) {
        String registers;
        int in1, in2;

        cpu.increaseCounter(3);

        switch(type) {
            case 0: registers = "BC"; in1 = 1; in2 = 2; break;
            case 1: registers = "DE"; in1 = 3; in2 = 4; break;
            case 2: registers = "HL"; in1 = 6; in2 = 7; break;
            case 3: registers = "SP"; in1 = 1; in2 = 2; break;
            default: return;
        }
        if(DEBUGMODE) System.out.println("LD " + registers + ", " + Integer.toHexString(((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff)));

        if(type < 3) {
            cpu.setRegister(in2,  memory.getMemory(cpu.getProgramCounter() + 1));
            cpu.setRegister(in1, memory.getMemory(cpu.getProgramCounter() + 2));
        } else {
            int temp = (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8);
            cpu.setStackPointer((char) temp);
        }

        cpu.increaseProgramCounter(3);
    }

    //DONE
    public static void ldSPHL() {
        cpu.increaseCounter(2);

        if(DEBUGMODE) System.out.println("LD SP, HL");

        int hl = (((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff));
        cpu.setStackPointer((char) hl);

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void LDHL() {
        cpu.increaseCounter(3);

        int temp = memory.getMemory(cpu.getProgramCounter() + 1);
        if(((temp & 0x80) >> 7) == 1) temp = (temp & 0x7f) - 0x80;

        if(DEBUGMODE) System.out.println("LDHL SP, " + Integer.toHexString(temp));

        int address = (cpu.getStackPointer() + temp) & 0xffff;

        cpu.setHalfCarryFlag((((cpu.getStackPointer() & 0xf) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf) & 0x10) == 0x10));
        cpu.setCarryFlag((((cpu.getStackPointer() & 0xff) + memory.getMemory(cpu.getProgramCounter() + 1)) & 0x100) == 0x100);

        cpu.setRegister(6, (char) ((address & 0xff00) >> 8));
        cpu.setRegister(7, (char) (address & 0x00ff));

        cpu.setZeroFlag(false);
        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(2);
    }

    //DONE
    public static void LDnnSP() {
        cpu.increaseCounter(5);

        int address = (memory.getMemory(cpu.getProgramCounter() + 1)) + (memory.getMemory(cpu.getProgramCounter() + 2) << 8);

        if(DEBUGMODE) System.out.println("LD " + Integer.toHexString(address) + ", SP");

        memory.setMemory(address + 1, (char) ((cpu.getStackPointer() & 0xff00) >> 8));
        memory.setMemory(address, (char) (cpu.getStackPointer() & 0x00ff));

        cpu.increaseProgramCounter(3);
    }

    //DONE
    public static void push(int register) {
        String registers;
        int in1, in2;

        cpu.increaseCounter(4);

        switch(register) {
            case 0: registers = "AF"; in1 = 0; in2 = 5; break;
            case 1: registers = "BC"; in1 = 1; in2 = 2; break;
            case 2: registers = "DE"; in1 = 3; in2 = 4; break;
            case 3: registers = "HL"; in1 = 6; in2 = 7; break;
            default: return;
        }
        if(DEBUGMODE) System.out.println("PUSH " + registers);

        memory.setMemory(cpu.getStackPointer() - 1, cpu.getRegister(in1));
        memory.setMemory(cpu.getStackPointer() - 2, cpu.getRegister(in2));

        cpu.increaseStackPointer(-2);
        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void pop(int register) {
        String registers;
        int in1, in2;

        cpu.increaseCounter(3);

        switch(register) {
            case 0: registers = "AF"; in1 = 0; in2 = 5; break;
            case 1: registers = "BC"; in1 = 1; in2 = 2; break;
            case 2: registers = "DE"; in1 = 3; in2 = 4; break;
            case 3: registers = "HL"; in1 = 6; in2 = 7; break;
            default: return;
        }
        if(DEBUGMODE) System.out.println("POP " + registers);

        int temp = memory.getMemory(cpu.getStackPointer() + 1);
        cpu.setRegister(in1, (char) temp);

        if(register == 0) temp = memory.getMemory(cpu.getStackPointer()) & 0xf0;
        else temp = memory.getMemory(cpu.getStackPointer());
        cpu.setRegister(in2, (char) temp);

        if(register == 0) cpu.computeFlags();

        cpu.increaseStackPointer(2);
        cpu.increaseProgramCounter(1);
    }





    //8-Bit ALU !!DONE!!

    //DONE
    /*Special Register
8 - (HL)
9 - #
    */
    public static void add(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            case 9: registerChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("ADD A, " + registerChar);

        int value, a;
        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);
        } else value = memory.getMemory(cpu.getProgramCounter() + 1);

        a = cpu.getRegister(0);
        cpu.setHalfCarryFlag((((a & 0xf) + (value & 0xf)) & 0x10) == 0x10);
        value += a;

        cpu.setCarryFlag(value > 0xff);

        cpu.setRegister(0, (char) (value & 0xff));
        cpu.setZeroFlag((value & 0xff) == 0);
        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
    */
    public static void adc(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            case 9: registerChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("ADC A, " + registerChar);

        int value, a, carry;
        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);
        } else value = memory.getMemory(cpu.getProgramCounter() + 1);

        a = cpu.getRegister(0);
        carry = cpu.getCarryFlag() ? 1 : 0;
        cpu.setHalfCarryFlag((((a & 0xf) + (value & 0xf) + carry) & 0x10) == 0x10);
        value += (a + carry);

        cpu.setCarryFlag(value > 0xff);

        cpu.setRegister(0, (char) (value & 0xff));
        cpu.setZeroFlag((value & 0xff) == 0);
        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE
    /*Special Register
8 - (HL)
9 - #
*/
    public static void sub(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            case 9: registerChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("SUB A, " + registerChar);

        int value, a;
        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);
        } else value = memory.getMemory(cpu.getProgramCounter() + 1);

        a = cpu.getRegister(0);
        cpu.setCarryFlag(value > a);
        cpu.setHalfCarryFlag((value & 0x0f) > (a & 0x0f));
        value = (a - value) & 0xff;

        cpu.setRegister(0, (char) value);
        cpu.setZeroFlag(value == 0);
        cpu.setSubtractFlag(true);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
    */
    public static void sbc(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            case 9: registerChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("SBC A, " + registerChar);

        int value, a, carry;
        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);
        } else value = memory.getMemory(cpu.getProgramCounter() + 1);

        carry = cpu.getCarryFlag() ? 1 : 0;
        a = cpu.getRegister(0);
        cpu.setHalfCarryFlag(((a & 0xf) - (value & 0xf) - carry) < 0);
        value = (a - value - carry);
        cpu.setCarryFlag(value < 0);

        cpu.setRegister(0, (char) (value & 0xff));
        cpu.setZeroFlag((value & 0xff) == 0);
        cpu.setSubtractFlag(true);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
     */
    public static void and(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            case 9: registerChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("AND A, " + registerChar);

        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) & cpu.getRegister(register)));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) (cpu.getRegister(0) & memory.getMemory(temp)));
        }
        else cpu.setRegister(0, (char) (cpu.getRegister(0) & memory.getMemory(cpu.getProgramCounter() + 1)));

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(true);
        cpu.setCarryFlag(false);

        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
     */
    public static void or(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            case 9: registerChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("OR A, " + registerChar);

        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) | (cpu.getRegister(register))));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) (cpu.getRegister(0) | (memory.getMemory(temp))));
        }
        else cpu.setRegister(0, (char) (cpu.getRegister(0) | (memory.getMemory(cpu.getProgramCounter() + 1))));

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE
    /*Special Register
    8 - (HL)
    9 - #
     */
    public static void xor(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            case 9: registerChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("XOR A, " + registerChar);

        if(register < 8) cpu.setRegister(0, (char) ((cpu.getRegister(0) ^ cpu.getRegister(register)) & 0xff));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) ((cpu.getRegister(0) ^ memory.getMemory(temp)) & 0xff));
        } else cpu.setRegister(0, (char) ((cpu.getRegister(0) ^ memory.getMemory(cpu.getProgramCounter() + 1)) & 0xff));

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE lacks testing
    /*Special Register
    8 - (HL)
    9 - #
     */
    public static void cp(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            case 9: registerChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1) & 0xff); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("CP A, " + registerChar);

        int value, a;
        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);
        }
        else value = memory.getMemory(cpu.getProgramCounter() + 1);

        a = cpu.getRegister(0);

        cpu.setZeroFlag(a == value);
        cpu.setHalfCarryFlag((value & 0xf) > (a & 0xf));
        cpu.setCarryFlag(a < value);

        cpu.setSubtractFlag(true);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    //DONE
    /*Special Register
    8 - (HL)
     */
    public static void inc(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = Integer.toHexString(((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("INC " + registerChar);

        if(register < 8) {
            cpu.setHalfCarryFlag((cpu.getRegister(register) & 0xf) == 0xf);
            cpu.setRegister(register, (char) ((cpu.getRegister(register) + 1) & 0xff));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        } else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((memory.getMemory(temp) & 0xf) == 0xf);
            memory.setMemory(temp, (char) ((memory.getMemory(temp) + 1) & 0xff));
            cpu.setZeroFlag(memory.getMemory(temp) == 0);
        }

        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    /*Special Register
    8 - (HL)
     */
    public static void dec(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("DEC " + registerChar);

        int value;
        if(register < 8) {
            value = cpu.getRegister(register);
            cpu.setHalfCarryFlag((value & 0xf) == 0);

            value -= 1;
            cpu.setRegister(register, (char) (value & 0xff));
            cpu.setZeroFlag(value == 0);
        }
        else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(temp);
            cpu.setHalfCarryFlag((value & 0xf) == 0);

            value -= 1;
            memory.setMemory(temp, (char) (value & 0xff));
            cpu.setZeroFlag(value == 0);
        }

        cpu.setSubtractFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }




    //16-Bit ALU !!DONE!!

    //DONE
    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    public static void addHL(int register) {
        String registerChar;
        int register1 = 0, register2 = 0;

        cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "BC"; register1 = 1; register2 = 2; break;
            case 1: registerChar = "DE"; register1 = 3; register2 = 4; break;
            case 2: registerChar = "HL"; register1 = 6; register2 = 7; break;
            case 3: registerChar = "SP"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("ADD HL, " + registerChar);

        int hl = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);

        if(register < 3) {
            int temp = ((cpu.getRegister(register1) & 0xff) << 8) + (cpu.getRegister(register2) & 0xff);

            cpu.setHalfCarryFlag((((hl & 0xfff) + (temp & 0xfff) & 0x1000) == 0x1000));
            cpu.setCarryFlag((((hl & 0xffff) + (temp & 0xffff))) > 0xffff);

            temp = (((hl & 0xffff) + (temp & 0xffff)) & 0xffff);

            cpu.setRegister(6, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(7, (char) (temp & 0x00ff));
        } else {
            cpu.setHalfCarryFlag((((hl & 0xfff) + (cpu.getStackPointer() & 0xfff) & 0x1000) == 0x1000));
            cpu.setCarryFlag((((hl & 0xffff) + (cpu.getStackPointer() & 0xffff)) > 0xffff));

            int temp = (((hl & 0xffff) + (cpu.getStackPointer() & 0xffff)) & 0xffff);

            cpu.setRegister(6, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(7, (char) (temp & 0x00ff));
        }

        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void addSP() {
        cpu.increaseCounter(4);

        int temp = memory.getMemory(cpu.getProgramCounter() + 1);
        if(((temp & 0x80) >> 7) == 1) temp = (temp & 0x7f) - 0x80;

        if(DEBUGMODE) System.out.println("ADD SP, " + Integer.toHexString(temp));

        cpu.setHalfCarryFlag((((cpu.getStackPointer() & 0xf) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf) & 0x10) == 0x10));
        cpu.setCarryFlag((((cpu.getStackPointer() & 0xff) + memory.getMemory(cpu.getProgramCounter() + 1)) & 0x100) == 0x100);


        temp = (cpu.getStackPointer() + temp) & 0xffff;

        cpu.setStackPointer((char) temp);

        cpu.setZeroFlag(false);
        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(2);
    }

    //DONE
    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    public static void incR(int register) {
        String registerChar;
        int register1 = 0, register2 = 0;

        cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "BC"; register1 = 1; register2 = 2;break;
            case 1: registerChar = "DE"; register1 = 3; register2 = 4;break;
            case 2: registerChar = "HL"; register1 = 6; register2 = 7;break;
            case 3: registerChar = "SP"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("INC " + registerChar);

        if(register < 3) {
            int temp = ((cpu.getRegister(register1) & 0xff) << 8) + (cpu.getRegister(register2) & 0xff);

            temp = (temp + 1) & 0xffff;

            cpu.setRegister(register1, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(register2, (char) (temp & 0x00ff));
        } else {
            int temp = cpu.getStackPointer();

            temp = (temp + 1) & 0xffff;

            cpu.setStackPointer((char) temp);
        }

        cpu.increaseProgramCounter(1);
    }

    //DONE
    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    public static void decR(int register) {
        String registerChar;
        int register1 = 0, register2 = 0;

        cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "BC"; register1 = 1; register2 = 2; break;
            case 1: registerChar = "DE"; register1 = 3; register2 = 4; break;
            case 2: registerChar = "HL"; register1 = 6; register2 = 7; break;
            case 3: registerChar = "SP"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("DEC " + registerChar);

        if(register < 3) {
            int temp = ((cpu.getRegister(register1) & 0xff) << 8) + (cpu.getRegister(register2) & 0xff);

            temp = (temp - 1) & 0xffff;

            cpu.setRegister(register1, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(register2, (char) (temp & 0x00ff));
        } else {
            int temp = cpu.getStackPointer();

            temp = (temp - 1) & 0xffff;

            cpu.setStackPointer((char) temp);
        }

        cpu.increaseProgramCounter(1);
    }





    //Miscellaneous !!DONE!!

    //DONE
    public static void swap(int register) {
        String registerChar;

        if(register == 8) cpu.increaseCounter(3);
        else cpu.increaseCounter(1);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("SWAP " + registerChar);

        int value;
        if(register < 8) {
            value = cpu.getRegister(register);
            int temp = (value & 0xf0) >> 4;
            int temp2 = (value & 0x0f) << 4;
            value = (temp | temp2) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);
            int temp = (value & 0xf0) >> 4;
            int temp2 = (value & 0x0f) << 4;

            value = (temp | temp2) & 0xff;

            memory.setMemory(address, (char) value);
        }

        cpu.setZeroFlag(value == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void daa() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("DAA");

        if(cpu.getSubtractFlag()) {
            if(cpu.getCarryFlag()) cpu.setRegister(0, (char) ((cpu.getRegister(0) - 0x60) & 0xff));
            if(cpu.getHalfCarryFlag()) cpu.setRegister(0, (char) ((cpu.getRegister(0) - 0x06) & 0xff));
        } else {
            if(cpu.getCarryFlag() || cpu.getRegister(0) > 0x99) {
                cpu.setRegister(0, (char) ((cpu.getRegister(0) + 0x60) & 0xff));
                cpu.setCarryFlag(true);
            }
            if(cpu.getHalfCarryFlag() || (cpu.getRegister(0) & 0x0f) > 0x09) cpu.setRegister(0, (char) ((cpu.getRegister(0) + 0x6) & 0xff));
        }

        cpu.setZeroFlag((cpu.getRegister(0) & 0xff) == 0);
        cpu.setHalfCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void cpl() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("CPL");

        cpu.setRegister(0, (char) (~cpu.getRegister(0) & 0xff));

        cpu.setSubtractFlag(true);
        cpu.setHalfCarryFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void ccf() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("CCF");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(!cpu.getCarryFlag());
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void scf() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("SCF");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void nop() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("NOP");

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void halt() {
        cpu.increaseCounter(1);

        cpu.setIsHalted(true);

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void stop() {
        cpu.increaseCounter(1);

        cpu.setIsStopped(true);
        memory.setMemory(DIVIDER_REGISTER, (char) 0);

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void di() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("DI");

        cpu.setChangeInterrupt(true);
        cpu.setChangeTo(false);
        cpu.setInterruptCounter(cpu.getCounter());

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void ei() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("EI");

        cpu.setChangeInterrupt(true);
        cpu.setChangeTo(true);
        cpu.setInterruptCounter(cpu.getCounter());

        cpu.increaseProgramCounter(1);
    }





    //Rotates and Shifts !!DONE!!

    //DONE
    public static void rlca() {
        int carry;

        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("RLCA");

        int value = cpu.getRegister(0);
        cpu.setCarryFlag((value & 0x80) == 0x80);

        carry = (value & 0x80) >> 7;
        value = ((value << 1) & 0xff) | carry;

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setRegister(0, (char) value);
        cpu.setZeroFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void rla() {
        int carry;

        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("RLA");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);

        int value = cpu.getRegister(0);

        carry = cpu.getCarryFlag() ? 1 : 0;
        cpu.setCarryFlag((value & 0x80) == 0x80);

        value = ((value << 1) & 0xff) | carry;

        cpu.setRegister(0, (char) value);
        cpu.setZeroFlag(false);

        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void rrca() {
        int value, carry;

        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("RRCA");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);

        value = cpu.getRegister(0);
        carry = (value & 0x01) << 7;

        cpu.setCarryFlag((value & 0x01) == 1);
        value = ((value >> 1) & 0xff) | carry;

        cpu.setRegister(0, (char) value);

        cpu.setZeroFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void rra() {
        int carry, value;

        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("RRA");

        value = cpu.getRegister(0);
        carry = cpu.getCarryFlag() ? 1 : 0;

        cpu.setCarryFlag((value & 0x01) != 0);
        value = (((value >> 1) & 0xff) | (carry << 7)) & 0xff;

        cpu.setRegister(0, (char) value);

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setZeroFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void rlc(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("RLC " + registerChar);

        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);
            carry = (value & 0x80) >> 7;

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (((value << 1) & 0xff) | carry) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = (cpu.getRegister(6) << 8) + (cpu.getRegister(7));
            value = memory.getMemory(address);
            carry = (value & 0x80) >> 7;

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (((value << 1) & 0xff) | carry) & 0xff;

            memory.setMemory(address, (char) value);
        }

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setZeroFlag(value == 0);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void rl(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("RL " + registerChar);

        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);
            carry = cpu.getCarryFlag() ? 1 : 0;

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (((value << 1) & 0xff) | carry) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = (cpu.getRegister(6) << 8) + (cpu.getRegister(7));
            value = memory.getMemory(address);
            carry = cpu.getCarryFlag() ? 1 : 0;

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (((value << 1) & 0xff) | carry) & 0xff;

            memory.setMemory(address, (char) value);
        }

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setZeroFlag(value == 0);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void rrc(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("RRC " + registerChar);

        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);
            carry = (value & 0x01) << 7;

            cpu.setCarryFlag((value & 0x01) != 0);
            value = (((value >> 1) & 0xff) | carry) & 0xff;

            cpu.setRegister(register, (char) value);

        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);
            carry = (value & 0x01) << 7;

            cpu.setCarryFlag(((value & 0x01) != 0));
            value = (((value >> 1) & 0xff) | carry) & 0xff;

            memory.setMemory(address, (char) value);
        }

        cpu.setZeroFlag(value == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void rr(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("RR " + registerChar);

        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);
            carry = cpu.getCarryFlag() ? 1 : 0;

            cpu.setCarryFlag((value & 0x01) != 0);
            value = (((value >> 1) & 0xff) | (carry << 7)) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);
            carry = cpu.getCarryFlag() ? 1 : 0;

            cpu.setCarryFlag(((value & 0x01) != 0));
            value = (((value >> 1) & 0xff) | (carry << 7)) & 0xff;

            memory.setMemory(address, (char) value);
        }

        cpu.setZeroFlag(value == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void sla(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("SLA " + registerChar);

        int value;
        if(register < 8) {
            value = cpu.getRegister(register);
            cpu.setCarryFlag((value & 0x80) != 0);
            value = (value << 1) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (value << 1) & 0xff;
            memory.setMemory(address, (char) value);
        }

        cpu.setZeroFlag(value == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void sra(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("SRA " + registerChar);

        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);

            cpu.setCarryFlag((value & 0x01) != 0);
            carry = value & 0x80;
            value = ((value >> 1) | carry) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);

            cpu.setCarryFlag((value & 0x01) != 0);
            carry = value & 0x80;
            value = ((value >> 1) | carry) & 0xff;

            memory.setMemory(address, (char) value);
        }

        cpu.setZeroFlag(value == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void srl(int register) {
        String registerChar;

        if(register < 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("SRL " + registerChar);

        int value;
        if(register < 8) {
            value = cpu.getRegister(register);

            cpu.setCarryFlag((value & 0x01) == 1);
            value = (value >> 1) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            value = memory.getMemory(address);

            cpu.setCarryFlag((value & 0x01) == 1);
            value = (value >> 1) & 0xff;

            memory.setMemory(address, (char) value);
        }

        cpu.setZeroFlag(value == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }






    //Bit Opcodes !!DONE!!

    //DONE
    public static void bit(int bit, int register) {
        String registerChar;

        if(register != 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(2);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("BIT " + bit + ", " + registerChar);

        if(register != 8) register = cpu.getRegister(register);
        else {
            int temp = (cpu.getRegister(6) << 8) + (cpu.getRegister(7));
            register = memory.getMemory(temp);
        }


        boolean bitTest = (register & (1 << bit)) != 0;

        cpu.setZeroFlag(!bitTest);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void set(int bit, int register) {
        String registerChar;

        if(register != 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch(register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("SET " + bit + ", " + registerChar);

        if(register != 8) {
            cpu.setRegister(register, (char) (cpu.getRegister(register) | (1 << bit)));
        }
        else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            register = memory.getMemory(address) & 0xff;

            memory.setMemory(address, (char) (register | (1 << bit)));
        }


        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void res(int bit, int register) {
        String registerChar;

        if (register != 8) cpu.increaseCounter(1);
        else cpu.increaseCounter(3);

        switch (register) {
            case 0: registerChar = "A"; break;
            case 1: registerChar = "B"; break;
            case 2: registerChar = "C"; break;
            case 3: registerChar = "D"; break;
            case 4: registerChar = "E"; break;
            case 6: registerChar = "H"; break;
            case 7: registerChar = "L"; break;
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if (DEBUGMODE) System.out.println("RES " + bit + ", " + registerChar);

        int value;
        if (register < 8) {
            value = cpu.getRegister(register);
            cpu.setRegister(register, (char) (value & (~(1 << bit))));
        }
        else {
            int address = (cpu.getRegister(6) << 8) + cpu.getRegister(7);
            value = memory.getMemory(address);
            memory.setMemory(address, (char) (value & (~(1 << bit))));
        }

        cpu.increaseProgramCounter(1);

    }






    //Jumps !!DONE!!

    //DONE
    public static void jp() {
        int jump;

        cpu.increaseCounter(4);

        jump = (memory.getMemory(cpu.getProgramCounter() + 1)) + (memory.getMemory(cpu.getProgramCounter() + 2) << 8);
        if(DEBUGMODE) System.out.println("JP " + Integer.toHexString(jump));

        cpu.setProgramCounter((char) jump);
    }

    //DONE
    /* Type
   0 - RET NotZero
   1 - RET Zero
   2 - RET NoCarry
   3 - RET Carry
    */
    public static void jpCond(int type) {
        boolean booleanTemp;
        String condition;

        cpu.increaseCounter(3);

        switch(type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); condition = "NZ"; break;
            case 1: booleanTemp = cpu.getZeroFlag(); condition = "Z"; break;
            case 2: booleanTemp = !cpu.getCarryFlag(); condition = "NC"; break;
            case 3: booleanTemp = cpu.getCarryFlag(); condition = "C"; break;
            default: return;
        }

        int jump = (memory.getMemory(cpu.getProgramCounter() + 1)) + (memory.getMemory(cpu.getProgramCounter() + 2) << 8);

        if(DEBUGMODE) System.out.println("JP " + condition + ", " +  Integer.toHexString(jump));

        if(booleanTemp) {
            cpu.setProgramCounter((char) jump);
            cpu.increaseCounter(1);
        }
        else cpu.increaseProgramCounter(3);
    }

    //DONE
    public static void jpHL() {
        cpu.increaseCounter(1);
        if(DEBUGMODE) System.out.println("JP HL");

        int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
        cpu.setProgramCounter((char) temp);
    }

    //DONE
    public static void jr() {
        cpu.increaseCounter(3);

        int temp = memory.getMemory(cpu.getProgramCounter() + 1);
        if((temp >> 7) == 0) cpu.increaseProgramCounter(temp & 0x7f);
        else cpu.increaseProgramCounter((temp & 0x7f) - 128);

        if(DEBUGMODE) System.out.println("JR " + Integer.toHexString(cpu.getProgramCounter()));

        cpu.increaseProgramCounter(2);
    }

    //DONE
    /* Type
 0 - RET NotZero
 1 - RET Zero
 2 - RET NoCarry
 3 - RET Carry
  */
    public static void jrCond(int type) {
        boolean booleanTemp;
        String condition;

        cpu.increaseCounter(2);

        switch (type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); condition = "NZ"; break;
            case 1: booleanTemp = cpu.getZeroFlag(); condition = "Z"; break;
            case 2: booleanTemp = !cpu.getCarryFlag(); condition = "NC"; break;
            case 3: booleanTemp = cpu.getCarryFlag(); condition = "C"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("JR " + condition);

        int address = memory.getMemory(cpu.getProgramCounter() + 1);
        if((address >> 7) == 0 && booleanTemp) {
            cpu.increaseProgramCounter(address & 0x7f);
            cpu.increaseCounter(1);
        }
        else if(booleanTemp) {
            cpu.increaseProgramCounter((address & 0x7f) - 128);
            cpu.increaseCounter(1);
        }

        cpu.increaseProgramCounter(2);
    }





    //Calls !!DONE!!

    //DONE
    public static void call() {
        cpu.increaseCounter(6);

        int address = ((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8));
        if(DEBUGMODE) System.out.println("CALL " + Integer.toHexString(address));

        int tempProgramCounter = cpu.getProgramCounter() + 3;

        cpu.setProgramCounter((char) address);
        memory.setMemory(cpu.getStackPointer() - 1, (char) (((tempProgramCounter) & 0xff00) >> 8));
        memory.setMemory(cpu.getStackPointer() - 2, (char) ((tempProgramCounter) & 0xff));
        cpu.increaseStackPointer(-2);
    }

    //DONE
    /* Type
    0 - RET NotZero
    1 - RET Zero
    2 - RET NoCarry
    3 - RET Carry
     */
    public static void callCond(int type) {
        boolean booleanTemp;
        String condition;

        cpu.increaseCounter(3);

        switch(type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); condition = "NZ"; break;
            case 1: booleanTemp = cpu.getZeroFlag(); condition = "Z"; break;
            case 2: booleanTemp = !cpu.getCarryFlag(); condition = "NC"; break;
            case 3: booleanTemp = cpu.getCarryFlag(); condition = "C"; break;
            default: return;
        }

        int address = ((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8));

        if(DEBUGMODE) System.out.println("CALL " + condition + ", " + Integer.toHexString(address));


        if(booleanTemp) {
            int tempProgramCounter = cpu.getProgramCounter() + 3;

            cpu.setProgramCounter((char) address);
            memory.setMemory(cpu.getStackPointer() - 1, (char) (((tempProgramCounter) & 0xff00) >> 8));
            memory.setMemory(cpu.getStackPointer() - 2, (char) ((tempProgramCounter) & 0xff));
            cpu.increaseStackPointer(-2);
            cpu.increaseCounter(3);
        } else {
            cpu.increaseProgramCounter(3);
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
    public static void rst(int type) {
        int address = 0;

        cpu.increaseCounter(4);

        switch (type) {
            case 0: break;
            case 1: address = 0x8; break;
            case 2: address = 0x10; break;
            case 3: address = 0x18; break;
            case 4: address = 0x20; break;
            case 5: address = 0x28; break;
            case 6: address = 0x30; break;
            case 7: address = 0x38; break;
        }

        if(DEBUGMODE) System.out.println("RST " + Integer.toHexString(address));

        int programCounter = cpu.getProgramCounter() + 1;
        memory.setMemory(cpu.getStackPointer() - 1, (char) ((programCounter & 0xff00) >> 8));
        memory.setMemory(cpu.getStackPointer() - 2, (char) (programCounter & 0xff));
        cpu.setProgramCounter((char) address);
        cpu.increaseStackPointer(-2);
    }





    //Returns !!DONE!!

    //DONE
    public static void ret() {
        cpu.increaseCounter(4);

        int address = (memory.getMemory(cpu.getStackPointer()) & 0xff)  + ((memory.getMemory(cpu.getStackPointer() + 1) & 0xff) << 8);

        if(DEBUGMODE) System.out.println("RET " + Integer.toHexString(address));

        cpu.setProgramCounter((char) address);
        cpu.increaseStackPointer(2);
    }

    //DONE
    /* Type
    RET NotZero
    RET Zero
    RET NoCarry
    RET Carry
     */
    public static void retCond(int type) {
        cpu.increaseCounter(2);

        boolean booleanTemp;
        String condition;
        int address;

        switch(type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); condition = "NZ"; break;
            case 1: booleanTemp = cpu.getZeroFlag(); condition = "Z"; break;
            case 2: booleanTemp = !cpu.getCarryFlag(); condition = "NC"; break;
            case 3: booleanTemp = cpu.getCarryFlag(); condition = "C"; break;
            default: return;
        }

        address = (memory.getMemory(cpu.getStackPointer()) & 0xff) + ((memory.getMemory(cpu.getStackPointer() + 1) & 0xff) << 8);

        if(DEBUGMODE) System.out.println("RET " + condition + ", " + Integer.toHexString(address));

        if(booleanTemp) {
            cpu.setProgramCounter((char) address);
            cpu.increaseStackPointer(2);
            cpu.increaseCounter(3);
        } else {
            cpu.increaseProgramCounter(1);
        }
    }

    //DONE
    public static void reti() {
        cpu.increaseCounter(4);

        int address;

        address = (memory.getMemory(cpu.getStackPointer()) & 0xff) + ((memory.getMemory(cpu.getStackPointer() + 1) & 0xff) << 8);

        if(DEBUGMODE) System.out.println("RETI " + Integer.toHexString(address));

        cpu.setChangeInterrupt(true);
        cpu.setChangeTo(true);
        cpu.setInterruptCounter(cpu.getCounter());

        cpu.setProgramCounter((char) address);
        cpu.increaseStackPointer(2);
    }




    //Extras

    public static void cb() {
        cpu.increaseCounter(1);
        cpu.increaseProgramCounter(1);
        if(DEBUGMODE) System.out.print("CB PREFIX ");
    }

    public static int[] readTAC() {
        int[] tacStatus = new int[2]; //Record Bit 2 information and Bit0+1 Information in 0 and 1 index

        //Timer STOP
        tacStatus[0] = ((memory.getMemory(0xff07) & 0x04) >> 2);
        //Timer Input Clock Select
        tacStatus[1] = (memory.getMemory(0xff07) & 0x03);

        return  tacStatus;
    }
}
