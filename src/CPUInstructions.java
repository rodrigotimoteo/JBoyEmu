public class CPUInstructions {

    private static CPU cpu;
    private static Memory memory;

    private static boolean DEBUGMODE = false;

    public static void setCpu(CPU cpuIn) {
        cpu = cpuIn;
    }
    public static void setMem(Memory memoryIn) {
        memory = memoryIn;
    }

    //Debugs

    public static void show() {
        System.out.print(Integer.toHexString(cpu.getProgramCounter()) + "  ");
        System.out.print(Integer.toHexString(cpu.getOperationCode()) + "  " + cpu.getCounter() + "  " + Integer.toHexString(cpu.getStackPointer()) + " ");
    }

    public static void dumpRegisters() {
        for(int i = 0; i < 8; i++)
            System.out.print(i + ":" + Integer.toHexString((cpu.getRegister(i)) & 0xff) + " ");
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

    //8-Bit Loads

    /* register special types
    0 - (BC)
    1 - (DE)
    2 - (nn)
    3 - #
    Operation
    0 - into A
    1 - using A
     */
    public static void ldTwoRegisters(int operation, int register) {
        String registerInChar;
        String registerOutChar;
        int status = 0;

        if(register == 2) cpu.increaseCounter(4);
        else cpu.increaseCounter(2);

        if(operation == 0)  {
            registerInChar = "A";
            switch(register) {
                case 0: registerOutChar = Integer.toHexString(((cpu.getRegister(1) & 0xff) << 8) + (cpu.getRegister(2) & 0xff)); break;
                case 1: registerOutChar = Integer.toHexString(((cpu.getRegister(3) & 0xff) << 8) + (cpu.getRegister(4) & 0xff)); break;
                case 2: registerOutChar = Integer.toHexString(((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff)); break;
                case 3: registerOutChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1) & 0xff); break;
                default: return;
            }
        } else {
            registerOutChar = "A";
            switch(register) {
                case 0: registerInChar = Integer.toHexString(((cpu.getRegister(1) & 0xff) << 8) + (cpu.getRegister(2) & 0xff)); break;
                case 1: registerInChar = Integer.toHexString(((cpu.getRegister(3) & 0xff) << 8) + (cpu.getRegister(4) & 0xff)); break;
                case 2: registerInChar = Integer.toHexString(((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff)); break;
                case 3: registerInChar = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1) & 0xff); break;
                default: return;
            }
        }

        if(DEBUGMODE) System.out.println("LD " + registerInChar + ", " + registerOutChar);
        //else DEBUGMODE = true;

        if(operation == 0) {
            switch (register) {
                case 0 -> {
                    register = ((cpu.getRegister(1) & 0xff) << 8) + (cpu.getRegister(2) & 0xff);
                    cpu.setRegister(0, (char) (memory.getMemory(register) & 0xff));
                }
                case 1 -> {
                    register = ((cpu.getRegister(3) & 0xff) << 8) + (cpu.getRegister(4) & 0xff);
                    cpu.setRegister(0, (char) (memory.getMemory(register) & 0xff));
                }
                case 2 -> {
                    register = ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff);
                    cpu.setRegister(0, (char) (memory.getMemory(register) & 0xff));
                    status = 2;
                }
                case 3 -> {
                    register = (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff);
                    cpu.setRegister(0, (char) (register));
                    status = 1;
                }
            }
        } else {
            switch (register) {
                case 0 -> {
                    register = ((cpu.getRegister(1) & 0xff) << 8) + (cpu.getRegister(2) & 0xff);
                    if (register == 0xff44 || register == 0xff04) {
                        memory.setMemory(register, (char) 0);
                    } else if (register >= 0xc000 && register <= 0xde00) {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                        memory.setMemory(register + 0x2000, (char) (cpu.getRegister(0) & 0xff));
                    } else if (register >= 0xe000 && register <= 0xfe00) {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                        memory.setMemory(register - 0x2000, (char) (cpu.getRegister(0) & 0xff));
                    } else if(register < 0x8000) {
                        cpu.increaseProgramCounter(1);
                        return;
                    } else {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                    }
                }
                case 1 -> {
                    register = ((cpu.getRegister(3) & 0xff) << 8) + (cpu.getRegister(4) & 0xff);
                    if (register == 0xff44 || register == 0xff04) {
                        memory.setMemory(register, (char) 0);
                    } else if (register >= 0xc000 && register <= 0xde00) {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                        memory.setMemory(register + 0x2000, (char) (cpu.getRegister(0) & 0xff));
                    } else if (register >= 0xe000 && register <= 0xfe00) {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                        memory.setMemory(register - 0x2000, (char) (cpu.getRegister(0) & 0xff));
                    } else if(register < 0x8000) {
                        cpu.increaseProgramCounter(1);
                        return;
                    } else {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                    }
                }
                case 2 -> {
                    register = ((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) << 8) + (memory.getMemory(cpu.getProgramCounter() + 2) & 0xff);
                    if (register == 0xff44 || register == 0xff04) {
                        memory.setMemory(register, (char) 0);
                    } else if (register >= 0xc000 && register <= 0xde00) {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                        memory.setMemory(register + 0x2000, (char) (cpu.getRegister(0) & 0xff));
                    } else if (register >= 0xe000 && register <= 0xfe00) {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                        memory.setMemory(register - 0x2000, (char) (cpu.getRegister(0) & 0xff));
                    } else if(register < 0x8000) {
                        cpu.increaseProgramCounter(3);
                        return;
                    } else {
                        memory.setMemory(register, (char) (cpu.getRegister(0) & 0xff));
                    }
                    status = 2;
                }
            }
        }

        if(status == 0) cpu.increaseProgramCounter(1);
        else if(status == 1) cpu.increaseProgramCounter(2);
        else cpu.increaseProgramCounter(3);

    }

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

        if(registerIn < 8 && registerOut < 8) {
            cpu.setRegister(registerIn, (char) (cpu.getRegister(registerOut) & 0xff));
        } else if(registerIn < 8 && registerOut == 8) {
            registerOut = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(registerIn, (char) (memory.getMemory(registerOut) & 0xff));
        } else if(registerIn == 8 && registerOut < 8) {
            registerIn = registerOut;
            registerOut =  ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);

            if(registerOut == 0xff44 || registerOut == 0xff04) {
                memory.setMemory(registerOut, (char) 0);
            }
            else if(registerOut >= 0xc000 && registerOut <= 0xde00) {
                memory.setMemory(registerOut, (char) (cpu.getRegister(registerIn) & 0xff));
                memory.setMemory(registerOut + 0x2000, (char) (cpu.getRegister(registerIn) & 0xff));
            }
            else if(registerOut >= 0xe000 && registerOut <= 0xfe00) {
                memory.setMemory(registerOut, (char) (cpu.getRegister(registerIn) & 0xff));
                memory.setMemory(registerOut - 0x2000, (char) (cpu.getRegister(registerIn) & 0xff));
            } else if(registerOut < 0x8000) {
                cpu.increaseProgramCounter(1);
                return;
            } else {
                memory.setMemory(registerOut, (char) (cpu.getRegister(registerIn) & 0xff));
            }
        } else if(registerIn < 8){
            cpu.setRegister(registerIn, (char) (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));
            status = 1;
        } else if(registerOut == 9) {
            registerOut = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            if(registerOut == 0xff44 || registerOut == 0xff04) memory.setMemory(registerOut, (char) 0);
            else memory.setMemory(registerOut, (char) (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));
            status = 1;
        }

        if(status == 0) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    /* Type
    0 - LD A,(C)
    1 - LD (C), A
     */
    public static void ldAC(int type) {
        cpu.increaseCounter(2);

        int address = 0xff00 + (cpu.getRegister(2) & 0xff);

        if(DEBUGMODE && type == 0) System.out.println("LD A, " + Integer.toHexString(address));
        if(DEBUGMODE && type == 1) System.out.println("LD " + Integer.toHexString(address) + ", A");

        if(type == 0) cpu.setRegister(0, (char) (memory.getMemory(address) & 0xff));
        else memory.setMemory(address, (char) (cpu.getRegister(0) & 0xff));

        cpu.increaseProgramCounter(1);
    }

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

        if((temp & 0xffff) == 0) temp = 0xffff;
        else temp = (temp & 0xffff) - 1;
        cpu.setRegister(6, (char) ((temp & 0xff00) >> 8));
        cpu.setRegister(7, (char) (temp & 0x00ff));
    }

    /*Type
    0 - LDI A, (HL)
    1 - LDI (HL), A
     */
    public static void ldi(int type) {
        int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);

        if(DEBUGMODE && type == 0) System.out.println("LDI A, " + Integer.toHexString(temp));
        if(DEBUGMODE && type == 1) System.out.println("LDI " + Integer.toHexString(temp) + ", A");
        if(DEBUGMODE) DEBUGMODE = false;

        if(type == 0) ld(0, 8);
        else ld(8, 0);

        if((temp & 0xffff) == 0xffff) temp = 0x0;
        else temp++;

        if(temp == 0x9800) { memory.dumpMemory(); System.exit(-1);}

        cpu.setRegister(6, (char) ((temp & 0xff00) >> 8));
        cpu.setRegister(7, (char) (temp & 0x00ff));
    }

    /* Type
    0 - LDH (n), A
    1 - LDH A, (n)
     */
    public static void ldh(int type) {
        cpu.increaseCounter(3);

        int address = (0xff00 + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));

        if(DEBUGMODE && type == 0) System.out.println("LDH " + Integer.toHexString(address) + ", A");
        if(DEBUGMODE && type == 1) System.out.println("LDH A, " + Integer.toHexString(address));

        if(type == 0) {
            if(address == 0xff00) {
                if((cpu.getRegister(0) & 0xff) == 0x10) memory.setMemory(address, (char) (0xc0 + (memory.getMemory(0xff00) & 0xf) + 0x10));
                else if((cpu.getRegister(0) & 0xff) == 0x20) memory.setMemory(address, (char) (0xc0 + (memory.getMemory(0xff00)& 0xf) + 0x20));
                else if((cpu.getRegister(0) & 0xff) == 0x30) memory.setMemory(address, (char) (0xc0 + (memory.getMemory(0xff00)& 0xf)));
            } else memory.setMemory(address, (char) (cpu.getRegister(0) & 0xff));
        }
        else cpu.setRegister(0, (char) (memory.getMemory(address) & 0xff));

        cpu.increaseProgramCounter(2);
    }

    //16-Bit Loads

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

    public static void ldSPHL() {
        cpu.increaseCounter(2);

        if(DEBUGMODE) System.out.println("LD SP, HL");

        cpu.setStackPointer((char) (((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff)));

        cpu.increaseProgramCounter(1);
    }

    //Don't understand the flags
    public static void LDHL() {
        cpu.increaseCounter(3);

        if(DEBUGMODE) System.out.println("LDHL SP, " + Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));

        int address = (cpu.getStackPointer() & 0xff) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff);
        cpu.setRegister(6, (char) ((address & 0xff00) >> 8));
        cpu.setRegister(7, (char) (address & 0x00ff));

        cpu.setZeroFlag(false);
        cpu.setSubtractFlag(false);
        //cpu.setHalfCarryFlag();
        //cpu.setCarryFlag();
        cpu.computeFRegister();

        cpu.increaseProgramCounter(2);
    }

    public static void LDnnSP() {
        cpu.increaseCounter(5);

        if(DEBUGMODE) System.out.println("LD " + Integer.toHexString(((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8) + (memory.getMemory(cpu.getProgramCounter() + 1))) + ", SP");

        int address = ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8) + (memory.getMemory(cpu.getProgramCounter() + 1));
        memory.setMemory(address, cpu.getStackPointer());

        cpu.increaseProgramCounter(3);
    }

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

        cpu.setRegister(in1, memory.getMemory(cpu.getStackPointer() + 1));
        cpu.setRegister(in2, memory.getMemory(cpu.getStackPointer()));

        cpu.increaseStackPointer(2);
        cpu.increaseProgramCounter(1);
    }

    //8-Bit ALU

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

        if(register < 8) {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + (cpu.getRegister(register) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff) > 255);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff)) & 0xff));
        } else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((((memory.getMemory(temp) & 0xf) + (cpu.getRegister(0) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff) > 255);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff)) & 0xff));
        } else {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) > 255);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff)) & 0xff));
        }

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

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

        int carry = cpu.getCarryFlag() ? 1 : 0;

        if(register < 8) {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + (cpu.getRegister(register) & 0xf) + carry) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) + carry & 0xff) + (cpu.getRegister(register) & 0xff) + carry > 255);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff) + carry) & 0xff));
        } else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag(((((memory.getMemory(temp)) & 0xf) + (cpu.getRegister(0) & 0xf) + carry) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff) + carry > 255);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff) + carry) & 0xff));
        } else {
            cpu.setHalfCarryFlag(((((cpu.getRegister(0)) & 0xf) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf) + carry) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + memory.getMemory(cpu.getProgramCounter() + 1) + carry > 255);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff) + carry) & 0xff));
        }

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

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

        if(register < 8) {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (cpu.getRegister(register) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) - (cpu.getRegister(register) & 0xff) < 0);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) - (cpu.getRegister(register) & 0xff)) & 0xff));

        } else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (memory.getMemory(temp) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) - (memory.getMemory(temp) & 0xff) < 0);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) - (memory.getMemory(temp) & 0xff)) & 0xff));
        } else {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) - (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) < 0);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) - (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff)) & 0xff));
        }

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(true);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    /*Special Register
    8 - (HL)
    9 - #
    */
    //Lacks Implementation
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

        int carry = cpu.getCarryFlag() ? 1 : 0;

        if(register < 8) {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (cpu.getRegister(register) & 0xf) - carry) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) - (cpu.getRegister(register) & 0xff) - carry < 0);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) - (cpu.getRegister(register) & 0xff) - carry) & 0xff));

        } else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (memory.getMemory(temp) & 0xf) - carry) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) - (memory.getMemory(temp) & 0xff) - carry < 0);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) - (memory.getMemory(temp) & 0xff) - carry) & 0xff));
        } else {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf) - carry) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) - (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) - carry < 0);
            cpu.setRegister(0, (char) (((cpu.getRegister(0) & 0xff) - (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) - carry) & 0xff));
        }

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(true);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

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

        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) & cpu.getRegister(register) & 0xff));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) (cpu.getRegister(0) & memory.getMemory(temp) & 0xff));
        } else cpu.setRegister(0, (char) (cpu.getRegister(0) & memory.getMemory(cpu.getProgramCounter() + 1)));

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(true);
        cpu.setCarryFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

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

        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) | (cpu.getRegister(register) & 0xff)));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) (cpu.getRegister(0) | (memory.getMemory(temp) & 0xff)));
        } else cpu.setRegister(0, (char) (cpu.getRegister(0) | (memory.getMemory(cpu.getProgramCounter() + 1))));

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

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

        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) ^ cpu.getRegister(register) & 0xff));
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) (cpu.getRegister(0) ^ memory.getMemory(temp) & 0xff));
        } else cpu.setRegister(0, (char) (cpu.getRegister(0) ^ memory.getMemory(cpu.getProgramCounter() + 1)));

        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

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

        if(register < 8) {
            cpu.setZeroFlag(cpu.getRegister(0) == cpu.getRegister(register));
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (cpu.getRegister(register) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) < (cpu.getRegister(register) & 0xff));
        }
        else if(register == 8) {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setZeroFlag((cpu.getRegister(0) & 0xff) == (memory.getMemory(temp) & 0xff));
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (memory.getMemory(temp) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag(cpu.getRegister(0) < (memory.getMemory(temp) & 0xff));
        }
        else {
            cpu.setZeroFlag((cpu.getRegister(0) & 0xff) == (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag(cpu.getRegister(0) < (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff));
        }

        cpu.setSubtractFlag(true);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

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
            if((cpu.getRegister(register) & 0xff) == 0xff) cpu.setRegister(register, (char) 0);
            else cpu.setRegister(register, (char) ((cpu.getRegister(register) & 0xff) + 1));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        }
        else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((memory.getMemory(temp) & 0xf) == 0xf);
            memory.setMemory(temp, (char) (((memory.getMemory(temp) & 0xff) + 1) & 0xff));
            cpu.setZeroFlag((memory.getMemory(temp) & 0xff) == 0);
        }

        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

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

        if(register < 8) {
            cpu.setHalfCarryFlag((cpu.getRegister(register) & 0xf) == 0);
            if((cpu.getRegister(register) & 0xff) == 0x00) cpu.setRegister(register, (char) 0xff);
            else cpu.setRegister(register, (char) ((cpu.getRegister(register) & 0xff) - 1));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        }
        else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((((memory.getMemory(temp) & 0xf) - 1) & 0x10) == 0);
            memory.setMemory(temp, (char) (((memory.getMemory(temp) & 0xff) - 1) & 0xff));
            cpu.setZeroFlag((memory.getMemory(temp) & 0xff) == 0);
        }

        cpu.setSubtractFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //16-Bit ALU

    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    /* Register
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
            case 0: registerChar = "BC"; register1 = 1; register2 = 2;break;
            case 1: registerChar = "DE"; register1 = 3; register2 = 4;break;
            case 2: registerChar = "HL"; register1 = 6; register2 = 7;break;
            case 3: registerChar = "SP"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("ADD HL, " + registerChar);

        int hl = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);

        if(register < 3) {
            int temp = ((cpu.getRegister(register1) & 0xff) << 8) + (cpu.getRegister(register2) & 0xff);

            cpu.setHalfCarryFlag((((hl & 0xfff) + (temp & 0xfff) & 0x1000) == 0x1000));
            cpu.setCarryFlag((((hl & 0xffff) + (temp & 0xffff)) & 0xffff) < ((hl & 0xffff) + (temp & 0xffff)));

            temp = (((hl & 0xffff) + (temp & 0xffff)) & 0xffff);

            cpu.setRegister(6, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(7, (char) (temp & 0x00ff));
        } else {
            cpu.setHalfCarryFlag((((hl & 0xfff) + (cpu.getStackPointer() & 0xfff) & 0x1000) == 0x1000));
            cpu.setCarryFlag((((hl & 0xffff) + (cpu.getStackPointer() & 0xffff)) & 0xffff) < ((hl & 0xffff) + (cpu.getStackPointer() & 0xffff)));

            int temp = (((hl & 0xffff) + (cpu.getStackPointer() & 0xffff)) & 0xffff);

            cpu.setRegister(6, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(7, (char) (temp & 0x00ff));
        }

        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void addSP() {
        cpu.increaseCounter(4);

        if(DEBUGMODE) System.out.println("ADD SP, " + Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1)));

        cpu.setHalfCarryFlag((((cpu.getStackPointer() & 0xf) + (memory.getMemory(cpu.getProgramCounter() + 1) & 0xf) & 0x10) == 0x10));
        cpu.setCarryFlag((((cpu.getStackPointer() & 0xffff) + memory.getMemory(cpu.getProgramCounter() + 1)) & 0x10000) == 0x10000);
        if((cpu.getStackPointer() & 0xffff) + memory.getMemory(cpu.getProgramCounter() + 1) >= 0x10000) {
            cpu.setStackPointer((char) (0x1ffff - ((cpu.getStackPointer() & 0xffff) + memory.getMemory(cpu.getProgramCounter() + 1))));
        } else {
            cpu.setStackPointer((char) ((cpu.getStackPointer() & 0xffff) + memory.getMemory(cpu.getProgramCounter() + 1)));
        }
        cpu.setZeroFlag(false);
        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(2);
    }

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

            if((temp & 0xffff) == 0xffff) temp = 0;
            else temp = (temp & 0xffff) + 1;

            cpu.setRegister(register1, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(register2, (char) (temp & 0x00ff));
        } else {
            if((cpu.getStackPointer() & 0xffff) == 0xffff) cpu.setStackPointer((char) 0);
            else cpu.setStackPointer((char) ((cpu.getStackPointer() & 0xffff) + 1));
        }

        cpu.increaseProgramCounter(1);
    }

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

            if((temp & 0xffff) == 0) temp = 0xffff;
            else temp = (temp & 0xffff) - 1;

            cpu.setRegister(register1, (char) ((temp & 0xff00) >> 8));
            cpu.setRegister(register2, (char) (temp & 0x00ff));
        } else {
            if((cpu.getStackPointer() & 0xffff) == 0) cpu.setStackPointer((char) 0xffff);
            else cpu.setStackPointer((char) ((cpu.getStackPointer() & 0xffff) - 1));
        }

        cpu.increaseProgramCounter(1);
    }

    //Miscellaneous

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

        int temp = (cpu.getRegister(register) & 0xf0) >> 4;
        int temp2 = (cpu.getRegister(register) & 0x0f) << 4;

        cpu.setRegister(register, (char) (temp + temp2));

        cpu.setZeroFlag((cpu.getRegister(0) & 0xff) == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //Missing Implementation
    public static void daa() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("DAA");

        cpu.setRegister(0, (char) ~cpu.getRegister(0));

        cpu.setZeroFlag((cpu.getRegister(0) & 0xff) == 0);
        cpu.setHalfCarryFlag(false);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void cpl() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("CPL");

        cpu.setRegister(0, (char) ~cpu.getRegister(0));

        cpu.setSubtractFlag(true);
        cpu.setHalfCarryFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void ccf() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("CCF");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(!cpu.getCarryFlag());
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void scf() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("SCF");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void nop() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("NOP");

        cpu.increaseProgramCounter(1);
    }

    public static void halt() {
        cpu.increaseCounter(1);

        cpu.setIsHalted(true);

        cpu.increaseProgramCounter(1);
    }

    public static void stop() {
        cpu.increaseCounter(1);

        cpu.setIsStopped(true);

        cpu.increaseProgramCounter(1);
    }

    public static void di() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("DI");

        cpu.setInterruptMasterEnable(false);

        cpu.increaseProgramCounter(1);
    }

    public static void ei() {
        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("EI");

        cpu.setInterruptMasterEnable(true);

        cpu.increaseProgramCounter(1);
    }


    //Rotates and Shifts

    public static void rlca() {
        int carry;

        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("RLCA");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        carry = (cpu.getRegister(0) & 0xff) >> 7;
        cpu.setCarryFlag(carry != 0);
        cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) << 1));
        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void rla() {
        int carry, carry1;

        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("RLA");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        carry = (cpu.getRegister(0) & 0xff) >> 7;
        carry1 = cpu.getCarryFlag() ? 1 : 0;
        cpu.setCarryFlag(carry != 0);
        cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) << 1));
        cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) + carry1));
        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void rrca() {
        int carry;

        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("RRCA");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        carry = (cpu.getRegister(0) & 0x01);
        cpu.setCarryFlag(carry != 0);
        cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) >> 1));
        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void rra() {
        int carry, carry1;

        cpu.increaseCounter(1);

        if(DEBUGMODE) System.out.println("RRA");

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);
        carry = cpu.getRegister(0) & 0x01;
        carry1 = cpu.getCarryFlag() ? 1 : 0;
        cpu.setCarryFlag(carry != 0);
        cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) >> 1));
        cpu.setRegister(0, (char) (cpu.getRegister(0) + (carry1 << 7)));
        cpu.setZeroFlag(cpu.getRegister(0) == 0);
        cpu.computeFRegister();

        cpu.increaseCounter(1);
    }

    public static void rlc(int register) {
        String registerChar;
        int carry;

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

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);

        if(register < 8) {
            carry = (cpu.getRegister(register) & 0xff) >> 7;
            cpu.setCarryFlag(carry != 0);
            cpu.setRegister(register, (char) ((cpu.getRegister(register) & 0xff) << 1));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        } else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            carry = (memory.getMemory(temp) & 0xff) >> 7;
            cpu.setCarryFlag(carry != 0);
            memory.setMemory(temp, (char) ((memory.getMemory(temp) & 0xff) << 1));
            cpu.setZeroFlag(memory.getMemory(temp) == 0);
        }

        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void rl(int register) {
        String registerChar;
        int carry, carry1;

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

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);

        if(register < 8) {
            carry = (cpu.getRegister(register) & 0xff) >> 7;
            carry1 = cpu.getCarryFlag() ? 1 : 0;
            cpu.setCarryFlag(carry != 0);
            cpu.setRegister(register, (char) ((cpu.getRegister(register) & 0xff) << 1));
            cpu.setRegister(register, (char) ((cpu.getRegister(register) & 0xff) + carry1));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        } else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            carry = (memory.getMemory(temp) & 0xff) >> 7;
            carry1 = cpu.getCarryFlag() ? 1 : 0;
            cpu.setCarryFlag(carry != 0);
            memory.setMemory(temp, (char) ((memory.getMemory(temp) & 0xff) << 1));
            memory.setMemory(temp, (char) ((memory.getMemory(temp) & 0xff) + carry1));
            cpu.setZeroFlag(memory.getMemory(temp) == 0);
        }

        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void rrc(int register) {
        String registerChar;
        int carry;

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

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);

        if (register < 8) {
            carry = (cpu.getRegister(register) & 0x01);
            cpu.setCarryFlag(carry != 0);
            cpu.setRegister(register, (char) ((cpu.getRegister(register) & 0xff) >> 1));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        } else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            carry = (memory.getMemory(temp) & 0x01);
            cpu.setCarryFlag(carry != 0);
            memory.setMemory(temp, (char) ((memory.getMemory(temp) & 0xff) >> 1));
            cpu.setZeroFlag(memory.getMemory(temp) == 0);
        }

        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    public static void rr(int register) {
        String registerChar;
        int carry, carry1;

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

        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(false);

        if(register < 8) {
            carry = cpu.getRegister(register) & 0x01;
            carry1 = cpu.getCarryFlag() ? 1 : 0;
            cpu.setCarryFlag(carry != 0);
            cpu.setRegister(register, (char) ((cpu.getRegister(register) & 0xff) >> 1));
            cpu.setRegister(register, (char) (cpu.getRegister(register) + (carry1 << 7)));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        } else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            carry = memory.getMemory(temp) & 0x01;
            carry1 = cpu.getCarryFlag() ? 1 : 0;
            cpu.setCarryFlag(carry != 0);
            memory.setMemory(temp, (char) ((memory.getMemory(temp) & 0xff) >> 1));
            memory.setMemory(temp, (char) ((memory.getMemory(temp) & 0xff) + (carry1 << 7)));
            cpu.setZeroFlag(memory.getMemory(temp) == 0);
        }

        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

    //Lacks Implementation
    public static void sla(int register) {

        if(DEBUGMODE) System.out.println("SLA B");


    }

    //Lacks Implementation
    public static void sra(int register) {

        if(DEBUGMODE) System.out.println("SRA B");


    }

    //Lacks Implementation
    public static void srl(int register) {

        if(DEBUGMODE) System.out.println("SRL B");


    }

    //Bit Opcodes

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
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            register = memory.getMemory(temp) & 0xff;
        }

        switch (bit) {
            case 0 -> bit = register & 0x01;
            case 1 -> bit = register & 0x02 >> 1;
            case 2 -> bit = register & 0x04 >> 2;
            case 3 -> bit = register & 0x08 >> 3;
            case 4 -> bit = register & 0x10 >> 4;
            case 5 -> bit = register & 0x20 >> 5;
            case 6 -> bit = register & 0x40 >> 6;
            case 7 -> bit = register & 0x80 >> 7;
        }

        cpu.setZeroFlag(bit == 0);
        cpu.setSubtractFlag(false);
        cpu.setHalfCarryFlag(true);
        cpu.computeFRegister();

        cpu.increaseProgramCounter(1);
    }

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

        if(register != 8) register = cpu.getRegister(register);
        else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            register = memory.getMemory(temp) & 0xff;
        }

        switch(bit) {
            case 0: if((register & 0x01) == 0) { cpu.setRegister(bit, (char) (register + 1)); } break;
            case 1: if((register & 0x02 >> 1) == 0) { cpu.setRegister(bit, (char) (register + 2)); } break;
            case 2: if((register & 0x04 >> 2) == 0) { cpu.setRegister(bit, (char) (register + 4)); } break;
            case 3: if((register & 0x08 >> 3) == 0) { cpu.setRegister(bit, (char) (register + 8)); } break;
            case 4: if((register & 0x10 >> 4) == 0) { cpu.setRegister(bit, (char) (register + 16)); } break;
            case 5: if((register & 0x20 >> 5) == 0) { cpu.setRegister(bit, (char) (register + 32)); } break;
            case 6: if((register & 0x40 >> 6) == 0) { cpu.setRegister(bit, (char) (register + 64)); } break;
            case 7: if((register & 0x80 >> 7) == 0) { cpu.setRegister(bit, (char) (register + 128)); } break;
        }

        cpu.increaseProgramCounter(1);
    }

    public static void res(int bit, int register) {
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

        if(DEBUGMODE) System.out.println("RES " + bit + ", " + registerChar);

        if(register != 8) register = cpu.getRegister(register);
        else {
            int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
            register = memory.getMemory(temp) & 0xff;
        }

        switch(bit) {
            case 0: if((register & 0x01) == 1) { cpu.setRegister(bit, (char) (register - 1)); } break;
            case 1: if((register & 0x02 >> 1) == 1) { cpu.setRegister(bit, (char) (register - 2)); } break;
            case 2: if((register & 0x04 >> 2) == 1) { cpu.setRegister(bit, (char) (register - 4)); } break;
            case 3: if((register & 0x08 >> 3) == 1) { cpu.setRegister(bit, (char) (register - 8)); } break;
            case 4: if((register & 0x10 >> 4) == 1) { cpu.setRegister(bit, (char) (register - 16)); } break;
            case 5: if((register & 0x20 >> 5) == 1) { cpu.setRegister(bit, (char) (register - 32)); } break;
            case 6: if((register & 0x40 >> 6) == 1) { cpu.setRegister(bit, (char) (register - 64)); } break;
            case 7: if((register & 0x80 >> 7) == 1) { cpu.setRegister(bit, (char) (register - 128)); } break;
        }

        cpu.increaseProgramCounter(1);

    }

    //Jumps

    public static void jp() {
        int jump;

        cpu.increaseCounter(4);

        jump = (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8);
        if(DEBUGMODE) System.out.println("JP " + Integer.toHexString(jump));

        cpu.setProgramCounter((char) jump);
    }

    /* Type
   0 - RET NotZero
   1 - RET Zero
   2 - RET NoCarry
   3 - RET Carry
    */
    public static void jpCond(int type) {
        boolean booleanTemp;
        String condition;
        String jumpAddress;

        cpu.increaseCounter(3);

        switch(type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); condition = "NZ"; break;
            case 1: booleanTemp = cpu.getZeroFlag(); condition = "Z"; break;
            case 2: booleanTemp = !cpu.getCarryFlag(); condition = "NC"; break;
            case 3: booleanTemp = cpu.getCarryFlag(); condition = "C"; break;
            default: return;
        }

        if(type == 1) jumpAddress = Integer.toHexString(((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + (memory.getMemory(cpu.getProgramCounter() + 2) & 0xff << 8)));
        else jumpAddress = Integer.toHexString(memory.getMemory(cpu.getProgramCounter()) + 1);

        if(DEBUGMODE && type == 1) System.out.println("JP " + condition + ", " +  jumpAddress);
        else if(DEBUGMODE) System.out.println("JP" + jumpAddress);

        if(booleanTemp) {
            cpu.setProgramCounter((char) ((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8)));
            cpu.increaseCounter(1);
        }
        else cpu.increaseProgramCounter(3);
    }

    public static void jpHL() {
        cpu.increaseCounter(1);
        if(DEBUGMODE) System.out.println("JP HL");

        int temp = ((cpu.getRegister(6) & 0xff) << 8) + (cpu.getRegister(7) & 0xff);
        cpu.setProgramCounter((char) temp);
    }

    /* Type
 0 - RET NotZero
 1 - RET Zero
 2 - RET NoCarry
 3 - RET Carry
  */
    public static void jr(int type, int type1) {
        boolean booleanTemp = false;
        String condition;
        String callAddress;

        cpu.increaseCounter(2);

        switch (type1) {
            case 0 -> {
                booleanTemp = !cpu.getZeroFlag();
                condition = "NZ";
            }
            case 1 -> {
                booleanTemp = cpu.getZeroFlag();
                condition = "Z";
            }
            case 2 -> {
                booleanTemp = !cpu.getCarryFlag();
                condition = "NC";
            }
            case 3 -> {
                booleanTemp = cpu.getCarryFlag();
                condition = "C";
            }
            default -> condition = "ERROR";
        }

        if(type == 1) callAddress = Integer.toHexString(((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + (memory.getMemory(cpu.getProgramCounter() + 2) & 0xff << 8)));
        else callAddress = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1));

        if(DEBUGMODE && type == 1) System.out.println("JR " + condition + ", " +  callAddress);
        else if(DEBUGMODE) System.out.println("JR" + callAddress);

        int address = (memory.getMemory(cpu.getProgramCounter() + 1) & 0xff);
        if(type == 0) {
            if((address >> 7) == 0) cpu.increaseProgramCounter(address & 0x7f);
            else cpu.increaseProgramCounter((address & 0x7f) - 128);
            cpu.increaseCounter(1);
        } else {
            if((address >> 7) == 0 && booleanTemp) {
                cpu.increaseProgramCounter(address & 0x7f);
                cpu.increaseCounter(1);
            }
            else if(booleanTemp) { cpu.increaseProgramCounter((address & 0x7f) - 128); cpu.increaseCounter(1); }
        }
        cpu.increaseProgramCounter(2);
    }


    //Calls

    public static void call() {
        cpu.increaseCounter(6);

        String calledString = Integer.toHexString(memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8);
        if(DEBUGMODE) System.out.println("CALL " + calledString);

        int tempProgramCounter = cpu.getProgramCounter() + 3;

        cpu.setProgramCounter((char) ((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8)));
        memory.setMemory(cpu.getStackPointer() - 1, (char) (((tempProgramCounter) & 0xff00) >> 8));
        memory.setMemory(cpu.getStackPointer() - 2, (char) ((tempProgramCounter) & 0xff));
        cpu.increaseStackPointer(-2);
    }

    /* Type
    0 - RET NotZero
    1 - RET Zero
    2 - RET NoCarry
    3 - RET Carry
     */
    public static void callCond(int type) {
        boolean booleanTemp;
        String condition;
        String callAddress;

        cpu.increaseCounter(3);

        switch(type) {
            case 0: booleanTemp = !cpu.getZeroFlag(); condition = "NZ"; break;
            case 1: booleanTemp = cpu.getZeroFlag(); condition = "Z"; break;
            case 2: booleanTemp = !cpu.getCarryFlag(); condition = "NC"; break;
            case 3: booleanTemp = cpu.getCarryFlag(); condition = "C"; break;
            default: return;
        }

        callAddress = Integer.toHexString(((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + ((memory.getMemory(cpu.getProgramCounter() + 2) & 0xff) << 8)));

        if(DEBUGMODE) System.out.println("CALL " + condition + ", " + callAddress);


        if(booleanTemp) {
            int tempProgramCounter = cpu.getProgramCounter() + 3;

            cpu.setProgramCounter((char) ((memory.getMemory(cpu.getProgramCounter() + 1) & 0xff) + (memory.getMemory(cpu.getProgramCounter() + 2) & 0xff << 8)));
            memory.setMemory(cpu.getStackPointer() - 1, (char) (((tempProgramCounter) & 0xff00) >> 8));
            memory.setMemory(cpu.getStackPointer() - 2, (char) ((tempProgramCounter) & 0xff));
            cpu.increaseStackPointer(-2);
            cpu.increaseCounter(3);
        } else {
            cpu.increaseProgramCounter(3);
        }
    }

    //Restarts

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

    //Returns

    public static void ret() {
        cpu.increaseCounter(4);

        int address = (memory.getMemory(cpu.getStackPointer()) & 0xff)  + ((memory.getMemory(cpu.getStackPointer() + 1) & 0xff) << 8);

        if(DEBUGMODE) System.out.println("RET " + Integer.toHexString(address));

        cpu.setProgramCounter((char) address);
        cpu.increaseStackPointer(2);
    }

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

    public static void reti() {
        cpu.increaseCounter(4);

        int address;

        address = (memory.getMemory(cpu.getStackPointer()) & 0xff) + ((memory.getMemory(cpu.getStackPointer() + 1) & 0xff) << 8);

        if(DEBUGMODE) System.out.println("RETI " + Integer.toHexString(address));

        cpu.setInterruptMasterEnable(true);

        cpu.setProgramCounter((char) address);
        cpu.increaseStackPointer(2);
    }

    //Extras

    public static void cb() {
        cpu.increaseCounter(1);
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
