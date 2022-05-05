public class CPUInstructions {

    private static CPU cpu;
    private static Memory memory;

    private static final boolean DEBUGMODE = true;

    public static void setCpu(CPU cpuIn) {
        cpu = cpuIn;
    }
    public static void setMem(Memory memoryIn) {
        memory = memoryIn;
    }

    //Debugs

    public static void show() {
        if(DEBUGMODE) {
            System.out.print(Integer.toHexString(cpu.getProgramCounter()) + "  ");
            System.out.print(Integer.toHexString(cpu.getOperationCode()) + "  " + cpu.getCounter() + "  ");
        }
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

        if(register == 3) cpu.increaseCounter(4);
        else cpu.increaseCounter(2);

        if(operation == 0)  {
            registerInChar = "A";
            switch(register) {
                case 0: registerOutChar = "(BC)"; break;
                case 1: registerOutChar = "(DE)"; break;
                case 2: registerOutChar = "(nn)"; break;
                case 3: registerOutChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1) << 8 + memory.getCartridgeMemory(cpu.getProgramCounter() + 2)); break;
                default: return;
            }
        } else {
            registerOutChar = "A";
            switch(register) {
                case 0: registerInChar = "(BC)"; break;
                case 1: registerInChar = "(DE)"; break;
                case 2: registerInChar = "(nn)"; break;
                case 3: registerInChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1) << 8 + memory.getCartridgeMemory(cpu.getProgramCounter() + 2)); break;
                default: return;
            }
        }

        if(DEBUGMODE) System.out.println("LD " + registerInChar + ", " + registerOutChar);

        if(operation == 0) {
            switch(register) {
                case 0:
                    register =  (cpu.getRegister(1) & 0xff << 8) + (cpu.getRegister(2) & 0xff);
                    cpu.setRegister(0, (char) (memory.getMemory(register) & 0xff));
                    break;
                case 1:
                    register =  (cpu.getRegister(3) & 0xff << 8) + (cpu.getRegister(4) & 0xff);
                    cpu.setRegister(0, (char) (memory.getMemory(register) & 0xff));
                    break;
                case 2:
                    register =  (memory.getCartridgeMemory(cpu.getProgramCounter() + 1) << 8 + memory.getCartridgeMemory(cpu.getProgramCounter()) + 2);
                    cpu.setRegister(0, (char) (memory.getMemory(register) & 0xff));
                    status = 2;
                    break;
                case 3:
                    register =  memory.getCartridgeMemory(cpu.getProgramCounter()) + 1;
                    cpu.setRegister(0, (char) (register));
                    status = 1;
                    break;
            }
        } else {
            switch(register) {
                case 0:
                    register = (cpu.getRegister(1) & 0xff << 8) + (cpu.getRegister(2) & 0xff);
                    memory.setMemory(register, cpu.getRegister(0));
                    break;
                case 1:
                    register = (cpu.getRegister(3) & 0xff << 8) + (cpu.getRegister(4) & 0xff);
                    memory.setMemory(register, cpu.getRegister(0));
                    break;
                case 2:
                    register =  (memory.getCartridgeMemory(cpu.getProgramCounter() + 1) << 8 + memory.getCartridgeMemory(cpu.getProgramCounter()) + 2);
                    memory.setMemory(register, cpu.getRegister(0));
                    status = 2;
                    break;
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
            case 8: registerInChar = "(HL)"; break;
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
            case 8: registerOutChar = "(HL)"; break;
            case 9: registerOutChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("LD " + registerInChar + ", " + registerOutChar);

        if(registerIn < 8 && registerOut < 8) {
            cpu.setRegister(registerIn, (char) (cpu.getRegister(registerOut) & 0xff));
        } else if(registerIn < 8 && registerOut == 8) {
            registerOut = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(registerIn, (char) (memory.getMemory(registerOut) & 0xff));
        } else if(registerIn == 8 && registerOut < 8) {
            registerIn = registerOut;
            registerOut =  (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            memory.setMemory(registerOut, (char) (cpu.getRegister(registerIn) & 0xff));
        } else if(registerIn < 8){
            cpu.setRegister(registerIn, (char) memory.getCartridgeMemory(cpu.getProgramCounter() + 1));
            status = 1;
        } else if(registerOut == 9) {
            registerOut =  (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            memory.setMemory(registerOut, (char) memory.getCartridgeMemory(cpu.getProgramCounter() + 1));
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

        if(DEBUGMODE && type == 0) System.out.println("LD A, (C)");
        if(DEBUGMODE && type == 1) System.out.println("LD (C), A");

        if(type == 0) cpu.setRegister(0, memory.getMemory(0xFF00 + (cpu.getRegister(2) & 0xff)));
        else memory.setMemory(0xFF00 + (cpu.getRegister(2) & 0xff), (char) (cpu.getRegister(0) & 0xff));

        cpu.increaseProgramCounter(1);
    }

    /* Type
    0 - LDD A,(HL)
    1 - LDD (HL),A
     */
    public static void ldd(int type) {
        cpu.increaseCounter(2);

        if(DEBUGMODE && type == 0) System.out.println("LDD A, (HL)");
        if(DEBUGMODE && type == 1) System.out.println("LDD (HL), A");

        if(type == 0) {
            ld(0, 8);
            cpu.setRegister(7, (char) (cpu.getRegister(7) - 1));
            //LACKS HL--
        }
        else {
            ld(8, 0);
            cpu.setRegister(7, (char) (cpu.getRegister(7) - 1));
            //LACKS HL--
        }

        cpu.increaseProgramCounter(1);
    }

    /*Type
    0 - LDI A, (HL)
    1 - LDI (HL), A
     */
    public static void ldi(int type) {
        cpu.increaseCounter(2);

        if(DEBUGMODE && type == 0) System.out.println("LDI A, (HL)");
        if(DEBUGMODE && type == 1) System.out.println("LDI (HL), A");

        if(type == 0) {
            ld(0, 8);
            cpu.setRegister(7, (char) (cpu.getRegister(7) + 1));
            //LACKS HL++
        }
        else {
            ld(8, 0);
            cpu.setRegister(7, (char) (cpu.getRegister(7) + 1));
            //LACKS HL++
        }

        cpu.increaseProgramCounter(1);
    }

    /* Type
    0 - LDH (n), A
    1 - LDH A, (n)
     */
    public static void ldh(int type) {
        cpu.increaseCounter(3);

        if(DEBUGMODE && type == 0) System.out.println("LDH (n), A");
        if(DEBUGMODE && type == 1) System.out.println("LDH A, (n)");

        if(type == 0) {
            memory.setMemory(0xFF00 + memory.getCartridgeMemory(cpu.getProgramCounter() + 1), (char) (cpu.getRegister(0) & 0xff));
        }
        else {
            cpu.setRegister(0, (char) (memory.getMemory(0xFF00 + memory.getCartridgeMemory(cpu.getProgramCounter()) + 1) & 0xff));
        }

        cpu.increaseProgramCounter(2);
    }

    //16-Bit Loads




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
            case 9: registerChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("ADD A, " + registerChar);

        if(register < 8) {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + (cpu.getRegister(register) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff) > 255);
            if((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff) >= 255) cpu.setRegister(0, (char) (0x1FF - (cpu.getRegister(0) & 0xff) - (cpu.getRegister(register) & 0xff)));
            else cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff)));
            cpu.setZeroFlag(cpu.getRegister(0) == 0);
        } else if(register == 8) {
            int temp = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((((memory.getMemory(temp) & 0xf) + 0x01) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff) > 255);
            if((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff) >= 255) cpu.setRegister(0, (char) (0x1FF - (cpu.getRegister(0) & 0xff) - (memory.getMemory(temp) & 0xff)));
            else cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff)));
            cpu.setZeroFlag(cpu.getRegister(0) == 0);
        } else {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + (memory.getCartridgeMemory(cpu.getProgramCounter() + 1) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + memory.getCartridgeMemory(cpu.getProgramCounter() + 1) > 255);
            if((memory.getCartridgeMemory(cpu.getProgramCounter() + 1) & 0xff) + (cpu.getRegister(register) & 0xff) >= 255) cpu.setRegister(0, (char) (0x1FE - (memory.getCartridgeMemory(cpu.getProgramCounter() + 1) & 0xff) - (cpu.getRegister(register) & 0xff)));
            else cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff)));
            cpu.setZeroFlag(cpu.getRegister(0) == 0);
        }

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
            case 9: registerChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("ADD A, " + registerChar);

        int carry = cpu.getCarryFlag() ? 1 : 0;

        if(register < 8) {
            cpu.setHalfCarryFlag(((((cpu.getRegister(0) + carry) & 0xf) + (cpu.getRegister(register) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) + carry & 0xff) + (cpu.getRegister(register) & 0xff) + carry > 255);
            if((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff + carry) >= 255) cpu.setRegister(0, (char) (0x1FF - (cpu.getRegister(0) & 0xff) - (cpu.getRegister(register) & 0xff) - carry));
            else cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff) + carry));
            cpu.setZeroFlag(cpu.getRegister(0) == 0);
        } else if(register == 8) {
            int temp = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag(((((memory.getMemory(temp) + carry) & 0xf) + 0x01) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff) + carry > 255);
            if((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff) + carry >= 255) cpu.setRegister(0, (char) (0x1FF - (cpu.getRegister(0) & 0xff) - (memory.getMemory(temp) & 0xff) - carry));
            else cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) + (memory.getMemory(temp) & 0xff) + carry));
            cpu.setZeroFlag(cpu.getRegister(0) == 0);
        } else {
            cpu.setHalfCarryFlag(((((cpu.getRegister(0) + carry) & 0xf) + (memory.getCartridgeMemory(cpu.getProgramCounter() + 1) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) + memory.getCartridgeMemory(cpu.getProgramCounter() + 1) + carry > 255);
            if((memory.getCartridgeMemory(cpu.getProgramCounter() + 1) & 0xff) + (cpu.getRegister(register) & 0xff) >= 255) cpu.setRegister(0, (char) (0x1FF - (memory.getCartridgeMemory(cpu.getProgramCounter() + 1) & 0xff) - (cpu.getRegister(register) & 0xff) - carry));
            else cpu.setRegister(0, (char) ((cpu.getRegister(0) & 0xff) + (cpu.getRegister(register) & 0xff) + carry));
            cpu.setZeroFlag(cpu.getRegister(0) == 0);
        }

        cpu.setSubtractFlag(false);
        cpu.computeFRegister();

        if(register < 9) cpu.increaseProgramCounter(1);
        else cpu.increaseProgramCounter(2);
    }

    /*Special Register
8 - (HL)
9 - #
*/
    //Lacks Implementation
    public static void sub(int register) {

    }

    /*Special Register
    8 - (HL)
    9 - #
    */
    //Lacks Implementation
    public static void sbc(int register) {

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
            case 9: registerChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("AND A, " + registerChar);

        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) & cpu.getRegister(1) & 0xff));
        else if(register == 8) {
            int temp = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) (cpu.getRegister(0) & memory.getMemory(temp) & 0xff));
        } else cpu.setRegister(0, (char) (cpu.getRegister(0) & memory.getCartridgeMemory(cpu.getProgramCounter() + 1)));

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
            case 9: registerChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("OR A, " + registerChar);

        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) | cpu.getRegister(1) & 0xff));
        else if(register == 8) {
            int temp = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) (cpu.getRegister(0) | memory.getMemory(temp) & 0xff));
        } else cpu.setRegister(0, (char) (cpu.getRegister(0) | memory.getCartridgeMemory(cpu.getProgramCounter() + 1)));

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
            case 9: registerChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("XOR A, " + registerChar);

        if(register < 8) cpu.setRegister(0, (char) (cpu.getRegister(0) ^ cpu.getRegister(1) & 0xff));
        else if(register == 8) {
            int temp = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setRegister(0, (char) (cpu.getRegister(0) ^ memory.getMemory(temp) & 0xff));
        } else cpu.setRegister(0, (char) (cpu.getRegister(0) ^ memory.getCartridgeMemory(cpu.getProgramCounter() + 1)));

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
            case 9: registerChar = Integer.toHexString(memory.getCartridgeMemory(cpu.getProgramCounter() + 1)); break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("CP A, " + registerChar);

        if(register < 8) {
            cpu.setZeroFlag(cpu.getRegister(0) == cpu.getRegister(register));
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + (cpu.getRegister(register) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag((cpu.getRegister(0) & 0xff) < (cpu.getRegister(register) & 0xff));
        }
        else if(register == 8) {
            int temp = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setZeroFlag((cpu.getRegister(0) & 0xff) == (memory.getMemory(temp) & 0xff));
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + (memory.getMemory(temp) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag(cpu.getRegister(0) < (memory.getMemory(temp) & 0xff));
        }
        else {
            cpu.setZeroFlag((cpu.getRegister(0) & 0xff) == memory.getCartridgeMemory(cpu.getProgramCounter() + 1));
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + (memory.getCartridgeMemory(cpu.getProgramCounter() + 1) & 0xf)) & 0x10) == 0x10);
            cpu.setCarryFlag(cpu.getRegister(0) < memory.getCartridgeMemory(cpu.getProgramCounter() + 1));
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
            case 8: registerChar = "(HL)"; break;
            default: return;
        }

        if(DEBUGMODE) System.out.println("INC " + registerChar);

        if(register < 8) {
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) + 0x01) & 0x10) == 0x10);
            if((cpu.getRegister(register) & 0xff) == 0xff) cpu.setRegister(register, (char) 0);
            else cpu.setRegister(register, (char) (cpu.getRegister(register) & 0xff + 1));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        }
        else {
            int temp = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((((memory.getMemory(temp) & 0xf) + 0x01) & 0x10) == 0x10);
            if((memory.getMemory(temp) & 0xff) == 0xff) memory.setMemory(temp, (char) 0);
            else memory.setMemory(temp, (char) (memory.getMemory(temp) & 0xff + 1));
            cpu.setZeroFlag((memory.getCartridgeMemory(temp) & 0xff) == 0);
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
            cpu.setHalfCarryFlag((((cpu.getRegister(0) & 0xf) - 0x01) & 0x10) == 0x10);
            if((cpu.getRegister(register) & 0xff) == 0x00) cpu.setRegister(register, (char) 0xff);
            else cpu.setRegister(register, (char) (cpu.getRegister(register) & 0xff - 1));
            cpu.setZeroFlag(cpu.getRegister(register) == 0);
        }
        else {
            int temp = (cpu.getRegister(6) & 0xff << 8) + (cpu.getRegister(7) & 0xff);
            cpu.setHalfCarryFlag((((memory.getMemory(temp) & 0xf) - 0x01) & 0x10) == 0x10);
            if((memory.getMemory(temp) & 0xff) == 0xff) memory.setMemory(temp, (char) 0);
            else memory.setMemory(temp, (char) (memory.getMemory(temp) & 0xff + 1));
            cpu.setZeroFlag((memory.getCartridgeMemory(temp) & 0xff) == 0);
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
    //Lacks Implementation
    public static void addHL(int register) {

    }

    //Lacks Implementation
    public static void addSP() {

    }

    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    //Lacks Implementation
    public static void incR(int register) {

    }

    /*Registers
    0 - BC
    1 - DE
    2 - HL
    3 - SP
     */
    //Lacks Implementation
    public static void decR(int register) {

    }

    //Miscellaneous

    //Missing Implementation
    public static void swap(int register) {
        String registerChar;

        if(register == 8) cpu.increaseCounter(4);
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

        if(DEBUGMODE) System.out.println("SWAP " + registerChar);

        //

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

    //Lacks Implementation
    public static void halt() {
        //Such Empty
    }

    //Lacks Implementation
    public static void stop() {
        //Such Empty
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



    //Bit Opcodes

    public static void bit(int bit, int register) {
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

        if(DEBUGMODE) System.out.println("BIT " + bit + ", " + registerChar);

        if(register != 8) register = cpu.getRegister(register);
        else register = cpu.getRegister(7);

        switch(bit) {
                case 0: bit = register & 0x01; break;
                case 1: bit = register & 0x02 >> 1; break;
                case 2: bit = register & 0x04 >> 2; break;
                case 3: bit = register & 0x08 >> 3; break;
                case 4: bit = register & 0x10 >> 4; break;
                case 5: bit = register & 0x20 >> 5; break;
                case 6: bit = register & 0x40 >> 6; break;
                case 7: bit = register & 0x80 >> 7; break;
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
        else register = cpu.getRegister(7);

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
        else register = cpu.getRegister(7);

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



    //Calls



    //Restarts



    //Returns


}
