public class CPUInstructions {

    private final static int DIVIDER_REGISTER = 0xff04;

    private static CPU cpu;
    private static Memory memory;

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

        switch(register) {
            case 0 -> address = (cpu.getRegister(1) << 8) + cpu.getRegister(2);
            case 1 -> address = (cpu.getRegister(3) << 8) + cpu.getRegister(4);
            case 2 -> {
                cpu.handleCPUTimers();
                int addressLower = memory.getMemory(cpu.getProgramCounter() + 1);
                cpu.handleCPUTimers();
                int addressUpper = memory.getMemory(cpu.getProgramCounter() + 2) << 8;;
                address = addressUpper + addressLower;

                status = 2;
            }
            default -> { return; }
        }

        cpu.handleCPUTimers();
        memory.setMemory(address, cpu.getRegister(0));

        if(status == 0) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(3);
    }

    //DONE
    public static void ldTwoRegistersIntoA(int register) {
        int status = 0, address;

        switch(register) {
            case 0 -> address = (cpu.getRegister(1) << 8) + cpu.getRegister(2);
            case 1 -> address = (cpu.getRegister(3) << 8) + cpu.getRegister(4);
            case 2 -> {
                cpu.handleCPUTimers();
                int addressLower = memory.getMemory(cpu.getProgramCounter() + 1);
                cpu.handleCPUTimers();
                int addressUpper = memory.getMemory(cpu.getProgramCounter() + 2) << 8;

                address = addressLower + addressUpper;
                status = 2;
            }
            case 3 -> {
                address = memory.getMemory(cpu.getProgramCounter() + 1); status = 1;
            }
            default -> { return; }
        }

        cpu.handleCPUTimers();
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
        int status = 0;

        if(registerIn < 8 && registerOut < 8) cpu.setRegister(registerIn, cpu.getRegister(registerOut));
        else if(registerIn < 8 && registerOut == 8) {
            registerOut = (cpu.getRegister(6) << 8) + cpu.getRegister(7);
            cpu.handleCPUTimers();
            cpu.setRegister(registerIn, (char) (memory.getMemory(registerOut) & 0xff));
        } else if(registerIn == 8 && registerOut < 8) {
            registerIn = (cpu.getRegister(6) << 8) + cpu.getRegister(7);
            cpu.handleCPUTimers();
            memory.setMemory(registerIn, cpu.getRegister(registerOut));
        } else if(registerIn < 8) {
            cpu.handleCPUTimers();
            cpu.setRegister(registerIn, memory.getMemory(cpu.getProgramCounter() + 1));
            status = 1;
        } else if(registerOut == 9) {
            registerIn = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            cpu.handleCPUTimers();
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
        int address = 0xff00 + cpu.getRegister(2);

        cpu.handleCPUTimers();
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

        if(type == 0) {
            ld(0, 8);
        }
        else {
            ld(8, 0);
        }

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

        if(type == 0) {
            ld(0, 8);
        }
        else {
            ld(8, 0);
        }

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
        cpu.handleCPUTimers();
        int address = (0xff00 + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));

        char a = cpu.getRegister(0);
        cpu.handleCPUTimers();
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
        int in1 = 1, in2 = 2;

        switch(type) {
            case 0, 3:
                break;
            case 1: in1 = 3; in2 = 4; break;
            case 2: in1 = 6; in2 = 7; break;
            default: return;
        }

        if(type < 3) {
            cpu.handleCPUTimers();
            cpu.setRegister(in2,  memory.getMemory(cpu.getProgramCounter() + 1));
            cpu.handleCPUTimers();
            cpu.setRegister(in1, memory.getMemory(cpu.getProgramCounter() + 2));
        } else {
            cpu.handleCPUTimers();
            cpu.handleCPUTimers();
            int temp = (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8);
            cpu.setStackPointer((char) temp);
        }

        cpu.increaseProgramCounter(3);
    }

    //DONE
    public static void ldSPHL() {
        int hl = (((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff));
        cpu.setStackPointer((char) hl);
        cpu.handleCPUTimers();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void LDHL() {
        cpu.handleCPUTimers();
        int temp = memory.getMemory(cpu.getProgramCounter() + 1);
        if(((temp & 0x80) >> 7) == 1) temp = (temp & 0x7f) - 0x80;

        int address = (cpu.getStackPointer() + temp) & 0xffff;

        cpu.setHalfCarryFlag((((cpu.getStackPointer() & 0xf) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf) & 0x10) == 0x10));
        cpu.setCarryFlag((((cpu.getStackPointer() & 0xff) + memory.getMemory(cpu.getProgramCounter() + 1)) & 0x100) == 0x100);

        cpu.setRegister(6, (char) ((address & 0xff00) >> 8));
        cpu.setRegister(7, (char) (address & 0x00ff));
        cpu.handleCPUTimers();

        cpu.setZeroFlag(false);
        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(2);
    }

    //DONE
    public static void LDnnSP() {
        cpu.handleCPUTimers();
        int addressLower = memory.getMemory(cpu.getProgramCounter() + 1);
        cpu.handleCPUTimers();
        int addressUpper = memory.getMemory(cpu.getProgramCounter() + 2) << 8;

        int address = addressLower + addressUpper;

        cpu.handleCPUTimers();
        memory.setMemory(address + 1, (char) ((cpu.getStackPointer() & 0xff00) >> 8));
        cpu.handleCPUTimers();
        memory.setMemory(address, (char) (cpu.getStackPointer() & 0x00ff));

        cpu.increaseProgramCounter(3);
    }

    //DONE
    public static void push(int register) {
        int in1, in2;

        cpu.handleCPUTimers();

        switch(register) {
            case 0: in1 = 0; in2 = 5; break;
            case 1: in1 = 1; in2 = 2; break;
            case 2: in1 = 3; in2 = 4; break;
            case 3: in1 = 6; in2 = 7; break;
            default: return;
        }

        cpu.handleCPUTimers();
        memory.setMemory(cpu.getStackPointer() - 1, cpu.getRegister(in1));
        cpu.handleCPUTimers();
        memory.setMemory(cpu.getStackPointer() - 2, cpu.getRegister(in2));

        cpu.increaseStackPointer(-2);
        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void pop(int register) {
        int in1, in2;

        switch(register) {
            case 0: in1 = 0; in2 = 5; break;
            case 1: in1 = 1; in2 = 2; break;
            case 2: in1 = 3; in2 = 4; break;
            case 3: in1 = 6; in2 = 7; break;
            default: return;
        }

        cpu.handleCPUTimers();
        int temp = memory.getMemory(cpu.getStackPointer() + 1);
        cpu.setRegister(in1, (char) temp);

        cpu.handleCPUTimers();
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
        int value, a;

        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
        } else {
            cpu.handleCPUTimers();
            value = memory.getMemory(cpu.getProgramCounter() + 1);
        }

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
        int value, a, carry;

        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
        } else {
            cpu.handleCPUTimers();
            value = memory.getMemory(cpu.getProgramCounter() + 1);
        }

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
        int value, a;

        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
        } else {
            cpu.handleCPUTimers();
            value = memory.getMemory(cpu.getProgramCounter() + 1);
        }

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
        int value, a, carry;

        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
        } else {
            cpu.handleCPUTimers();
            value = memory.getMemory(cpu.getProgramCounter() + 1);
        }

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
        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) & cpu.getRegister(register)));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            cpu.setRegister(0, (char) (cpu.getRegister(0) & memory.getMemory(temp)));
        }
        else {
            cpu.handleCPUTimers();
            cpu.setRegister(0, (char) (cpu.getRegister(0) & memory.getMemory(cpu.getProgramCounter() + 1)));
        }

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
        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) | (cpu.getRegister(register))));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            cpu.setRegister(0, (char) (cpu.getRegister(0) | (memory.getMemory(temp))));
        }
        else {
            cpu.handleCPUTimers();
            cpu.setRegister(0, (char) (cpu.getRegister(0) | (memory.getMemory(cpu.getProgramCounter() + 1))));
        }

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
        if(register < 8) cpu.setRegister(0, (char) ((cpu.getRegister(0) ^ cpu.getRegister(register)) & 0xff));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            cpu.setRegister(0, (char) ((cpu.getRegister(0) ^ memory.getMemory(temp)) & 0xff));
        } else {
            cpu.handleCPUTimers();
            cpu.setRegister(0, (char) ((cpu.getRegister(0) ^ memory.getMemory(cpu.getProgramCounter() + 1)) & 0xff));
        }

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
    public static void cp(int register) {
        int value, a;

        if(register < 8) value = cpu.getRegister(register);
        else if(register == 8) {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
        }
        else {
            cpu.handleCPUTimers();
            value = memory.getMemory(cpu.getProgramCounter() + 1);
        }

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
        int value;
        if(register < 8) {
            value = cpu.getRegister(register);
            cpu.setHalfCarryFlag((value & 0xf) == 0xf);

            value = (value + 1) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
            cpu.setHalfCarryFlag((value & 0xf) == 0xf);

            value = (value + 1) & 0xff;

            cpu.handleCPUTimers();
            memory.setMemory(address, (char) (value));
        }
        cpu.setZeroFlag(value == 0);

        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    /*Special Register
    8 - (HL)
     */
    public static void dec(int register) {
        int value;
        if(register < 8) {
            value = cpu.getRegister(register);
            cpu.setHalfCarryFlag((value & 0xf) == 0);

            value -= 1;
            cpu.setRegister(register, (char) (value & 0xff));
        }
        else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(temp);
            cpu.setHalfCarryFlag((value & 0xf) == 0);

            value -= 1;
            cpu.handleCPUTimers();
            memory.setMemory(temp, (char) (value & 0xff));
        }

        cpu.setZeroFlag(value == 0);
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
        int register1 = 0, register2 = 0;

        switch(register) {
            case 0: register1 = 1; register2 = 2; break;
            case 1: register1 = 3; register2 = 4; break;
            case 2: register1 = 6; register2 = 7; break;
            case 3: break;
            default: return;
        }

        int hl = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);

        cpu.handleCPUTimers();
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
        cpu.handleCPUTimers();
        int temp = memory.getMemory(cpu.getProgramCounter() + 1);
        if(((temp & 0x80) >> 7) == 1) temp = (temp & 0x7f) - 0x80;

        cpu.setHalfCarryFlag((((cpu.getStackPointer() & 0xf) + (1 & 0xf) & 0x10) == 0x10));
        cpu.setCarryFlag((((cpu.getStackPointer() & 0xff) + memory.getMemory(cpu.getProgramCounter() + 1)) & 0x100) == 0x100);


        temp = (cpu.getStackPointer() + temp) & 0xffff;

        cpu.handleCPUTimers();
        cpu.handleCPUTimers();
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
        int register1 = 0, register2 = 0;

        switch(register) {
            case 0: register1 = 1; register2 = 2;break;
            case 1: register1 = 3; register2 = 4;break;
            case 2: register1 = 6; register2 = 7;break;
            case 3: break;
            default: return;
        }

        cpu.handleCPUTimers();
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
        int register1 = 0, register2 = 0;

        switch(register) {
            case 0: register1 = 1; register2 = 2; break;
            case 1: register1 = 3; register2 = 4; break;
            case 2: register1 = 6; register2 = 7; break;
            case 3: break;
            default: return;
        }

        cpu.handleCPUTimers();
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
        int value;
        if(register < 8) {
            value = cpu.getRegister(register);
            int temp = (value & 0xf0) >> 4;
            int temp2 = (value & 0x0f) << 4;
            value = (temp | temp2) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
            int temp = (value & 0xf0) >> 4;
            int temp2 = (value & 0x0f) << 4;

            value = (temp | temp2) & 0xff;

            cpu.handleCPUTimers();
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
        cpu.setRegister(0, (char) (~cpu.getRegister(0) & 0xff));

        cpu.setSubtractFlag(true);
        cpu.setHalfCarryFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void ccf() {
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(!cpu.getCarryFlag());
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void scf() {
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void nop() {
        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void halt() {
        cpu.setIsHalted(true);
        cpu.setHaltCounter(cpu.getCounter());

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void stop() {
        cpu.setIsStopped(true);
        memory.setMemory(DIVIDER_REGISTER, (char) 0);

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void di() {
        cpu.setChangeInterrupt(true);
        cpu.setChangeTo(false);
        cpu.setInterruptCounter(cpu.getCounter());

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void ei() {
        cpu.setChangeInterrupt(true);
        cpu.setChangeTo(true);
        cpu.setInterruptCounter(cpu.getCounter());

        cpu.increaseProgramCounter(1);
    }





    //Rotates and Shifts !!DONE!!

    //DONE
    public static void rlca() {
        int carry;

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
        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);
            carry = (value & 0x80) >> 7;

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (((value << 1) & 0xff) | carry) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = (cpu.getRegister(6) << 8) + (cpu.getRegister(7));
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
            carry = (value & 0x80) >> 7;

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (((value << 1) & 0xff) | carry) & 0xff;

            cpu.handleCPUTimers();
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
        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);
            carry = cpu.getCarryFlag() ? 1 : 0;

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (((value << 1) & 0xff) | carry) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = (cpu.getRegister(6) << 8) + (cpu.getRegister(7));
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
            carry = cpu.getCarryFlag() ? 1 : 0;

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (((value << 1) & 0xff) | carry) & 0xff;

            cpu.handleCPUTimers();
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
        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);
            carry = (value & 0x01) << 7;

            cpu.setCarryFlag((value & 0x01) != 0);
            value = (((value >> 1) & 0xff) | carry) & 0xff;

            cpu.setRegister(register, (char) value);

        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
            carry = (value & 0x01) << 7;

            cpu.setCarryFlag(((value & 0x01) != 0));
            value = (((value >> 1) & 0xff) | carry) & 0xff;

            cpu.handleCPUTimers();
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
        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);
            carry = cpu.getCarryFlag() ? 1 : 0;

            cpu.setCarryFlag((value & 0x01) != 0);
            value = (((value >> 1) & 0xff) | (carry << 7)) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
            carry = cpu.getCarryFlag() ? 1 : 0;

            cpu.setCarryFlag(((value & 0x01) != 0));
            value = (((value >> 1) & 0xff) | (carry << 7)) & 0xff;

            cpu.handleCPUTimers();
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
        int value;
        if(register < 8) {
            value = cpu.getRegister(register);
            cpu.setCarryFlag((value & 0x80) != 0);
            value = (value << 1) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);

            cpu.setCarryFlag((value & 0x80) != 0);
            value = (value << 1) & 0xff;
            cpu.handleCPUTimers();
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
        int value, carry;
        if(register < 8) {
            value = cpu.getRegister(register);

            cpu.setCarryFlag((value & 0x01) != 0);
            carry = value & 0x80;
            value = ((value >> 1) | carry) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);

            cpu.setCarryFlag((value & 0x01) != 0);
            carry = value & 0x80;
            value = ((value >> 1) | carry) & 0xff;

            cpu.handleCPUTimers();
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
        int value;
        if(register < 8) {
            value = cpu.getRegister(register);

            cpu.setCarryFlag((value & 0x01) == 1);
            value = (value >> 1) & 0xff;

            cpu.setRegister(register, (char) value);
        } else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);

            cpu.setCarryFlag((value & 0x01) == 1);
            value = (value >> 1) & 0xff;

            cpu.handleCPUTimers();
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
        if(register != 8) register = cpu.getRegister(register);
        else {
            cpu.handleCPUTimers();
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
        if(register != 8) {
            cpu.setRegister(register, (char) (cpu.getRegister(register) | (1 << bit)));
        }
        else {
            int address = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.handleCPUTimers();
            register = memory.getMemory(address) & 0xff;
            cpu.handleCPUTimers();
            memory.setMemory(address, (char) (register | (1 << bit)));
        }

        cpu.increaseProgramCounter(1);
    }

    //DONE
    public static void res(int bit, int register) {
        int value;
        if (register < 8) {
            value = cpu.getRegister(register);
            cpu.setRegister(register, (char) (value & (~(1 << bit))));
        }
        else {
            int address = (cpu.getRegister(6) << 8) + cpu.getRegister(7);
            cpu.handleCPUTimers();
            value = memory.getMemory(address);
            cpu.handleCPUTimers();
            memory.setMemory(address, (char) (value & (~(1 << bit))));
        }

        cpu.increaseProgramCounter(1);
    }






    //Jumps !!DONE!!

    //DONE
    public static void jp() {
        cpu.handleCPUTimers();
        int addressLower = memory.getMemory(cpu.getProgramCounter() + 1);
        cpu.handleCPUTimers();
        int addressUpper = memory.getMemory(cpu.getProgramCounter() + 2) << 8;

        int address = addressLower + addressUpper;

        cpu.setProgramCounter((char) address);
        cpu.handleCPUTimers();
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

        switch(type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); break;
            case 1: booleanTemp = cpu.getZeroFlag(); break;
            case 2: booleanTemp = !cpu.getCarryFlag(); break;
            case 3: booleanTemp = cpu.getCarryFlag(); break;
            default: return;
        }

        cpu.handleCPUTimers();
        int addressLower = memory.getMemory(cpu.getProgramCounter() + 1);
        cpu.handleCPUTimers();
        int addressUpper = memory.getMemory(cpu.getProgramCounter() + 2) << 8;

        int address = addressLower + addressUpper;

        if(booleanTemp) {
            cpu.setProgramCounter((char) address);
            cpu.handleCPUTimers();
        }
        else cpu.increaseProgramCounter(3);
    }

    //DONE
    public static void jpHL() {
        int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
        cpu.setProgramCounter((char) temp);
    }

    //DONE
    public static void jr() {
        cpu.handleCPUTimers();
        int temp = memory.getMemory(cpu.getProgramCounter() + 1);

        if((temp >> 7) == 0) cpu.increaseProgramCounter(temp & 0x7f);
        else cpu.increaseProgramCounter((temp & 0x7f) - 128);
        cpu.handleCPUTimers();

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

        switch (type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); break;
            case 1: booleanTemp = cpu.getZeroFlag(); break;
            case 2: booleanTemp = !cpu.getCarryFlag(); break;
            case 3: booleanTemp = cpu.getCarryFlag(); break;
            default: return;
        }

        cpu.handleCPUTimers();
        int address = memory.getMemory(cpu.getProgramCounter() + 1);

        if((address >> 7) == 0 && booleanTemp) {
            cpu.increaseProgramCounter(address & 0x7f);
            cpu.handleCPUTimers();
        }
        else if(booleanTemp) {
            cpu.increaseProgramCounter((address & 0x7f) - 128);
            cpu.handleCPUTimers();
        }

        cpu.increaseProgramCounter(2);
    }





    //Calls !!DONE!!

    //DONE
    public static void call() {
        cpu.handleCPUTimers();
        int addressLower = memory.getMemory(cpu.getProgramCounter() + 1);
        cpu.handleCPUTimers();
        int addressUpper = memory.getMemory(cpu.getProgramCounter() + 2) << 8;

        int address = addressUpper + addressLower;
        int tempProgramCounter = cpu.getProgramCounter() + 3;

        cpu.handleCPUTimers();

        cpu.setProgramCounter((char) address);
        cpu.handleCPUTimers();
        memory.setMemory(cpu.getStackPointer() - 1, (char) (((tempProgramCounter) & 0xff00) >> 8));
        cpu.handleCPUTimers();
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

        switch(type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); break;
            case 1: booleanTemp = cpu.getZeroFlag(); break;
            case 2: booleanTemp = !cpu.getCarryFlag(); break;
            case 3: booleanTemp = cpu.getCarryFlag(); break;
            default: return;
        }

        cpu.handleCPUTimers();
        int addressLower = memory.getMemory(cpu.getProgramCounter() + 1);
        cpu.handleCPUTimers();
        int addressUpper = memory.getMemory(cpu.getProgramCounter() + 2) << 8;

        int address = addressLower + addressUpper;

        if(booleanTemp) {
            int tempProgramCounter = cpu.getProgramCounter() + 3;
            cpu.handleCPUTimers();

            cpu.setProgramCounter((char) address);
            cpu.handleCPUTimers();
            memory.setMemory(cpu.getStackPointer() - 1, (char) (((tempProgramCounter) & 0xff00) >> 8));
            cpu.handleCPUTimers();
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

        cpu.handleCPUTimers();

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

        int programCounter = cpu.getProgramCounter() + 1;
        cpu.handleCPUTimers();
        memory.setMemory(cpu.getStackPointer() - 1, (char) ((programCounter & 0xff00) >> 8));
        cpu.handleCPUTimers();
        memory.setMemory(cpu.getStackPointer() - 2, (char) (programCounter & 0xff));

        cpu.setProgramCounter((char) address);
        cpu.increaseStackPointer(-2);
    }





    //Returns !!DONE!!

    //DONE
    public static void ret() {
        cpu.handleCPUTimers();
        int addressLower = memory.getMemory(cpu.getStackPointer());
        cpu.handleCPUTimers();
        int addressUpper = memory.getMemory(cpu.getStackPointer() + 1) << 8;

        int address = addressLower + addressUpper;

        cpu.setProgramCounter((char) address);
        cpu.handleCPUTimers();
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
        boolean booleanTemp;
        int address;

        switch(type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); break;
            case 1: booleanTemp = cpu.getZeroFlag(); break;
            case 2: booleanTemp = !cpu.getCarryFlag(); break;
            case 3: booleanTemp = cpu.getCarryFlag(); break;
            default: return;
        }
        cpu.handleCPUTimers();

        if(booleanTemp) {
            cpu.handleCPUTimers();
            int addressLower = memory.getMemory(cpu.getStackPointer());
            cpu.handleCPUTimers();
            int addressUpper = memory.getMemory(cpu.getStackPointer() + 1) << 8;

            address = addressLower + addressUpper;

            cpu.setProgramCounter((char) address);
            cpu.handleCPUTimers();
            cpu.increaseStackPointer(2);
        } else {
            cpu.increaseProgramCounter(1);
        }
    }

    //DONE
    public static void reti() {
        int address;

        cpu.handleCPUTimers();
        int addressLower = memory.getMemory(cpu.getStackPointer());
        cpu.handleCPUTimers();
        int addressUpper = memory.getMemory(cpu.getStackPointer() + 1) << 8;

        address = addressLower + addressUpper;

        cpu.setProgramCounter((char) address);
        cpu.handleCPUTimers();

        cpu.setChangeInterrupt(true);
        cpu.setChangeTo(true);
        cpu.setInterruptCounter(cpu.getCounter());

        cpu.increaseStackPointer(2);
    }




    //Extras

    public static void cb() {
        cpu.increaseProgramCounter(1);
    }

    public static void readTAC() {
        //Timer Enabled
        cpu.setTimerEnabled(memory.testBit(0xff07, 2));
        //Timer Input Clock Select
        switch(memory.getMemory(0xff07) & 0x03) {
            case 1-> cpu.setTimerFrequency(4);
            case 2-> cpu.setTimerFrequency(16);
            case 3-> cpu.setTimerFrequency(64);
        }
    }
}
