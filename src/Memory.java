import java.util.Arrays;

public class Memory {

    private char[] memory = new char[0x10000];

    private byte[] cartridge;

    CPU gbCPU;

    //Resets

    private void resetMemory() {
        Arrays.fill(memory, (char) 0);
    }

    //Seters

    public void setMemory(int index, char value) {
        memory[index] = value;
    }

    //Geters

    public char getMemory(int index) {
        return memory[index];
    }

    public byte getCartridgeMemory(int index) {
        return cartridge[index];
    }

    //Debug

    public void dumpMemory() {
        System.out.print("0 ");
        for(int i = 0; i < 0x10000; i++) {
            if(i % 16 == 0 && i != 0) { System.out.println(" "); System.out.print(Integer.toHexString(i ) + " "); System.out.print(Integer.toHexString(getMemory(i) & 0xff) + " ");  }
            else System.out.print(Integer.toHexString(getMemory(i) & 0xff) + " ");
        }
    }

    //Init

    private void init() {
       cartridge = ROM.loadProgram(gbCPU.getRomName(), this);
       gbCPU.setRegisters(0, 7, (char) 0, (char) 1);
       gbCPU.setRegister(7, (char) 0xB0);
    }

    /* Reserved Memory Locations
     * 0000 - restart $00 - restart address 0000
     * 0008 - restart $08 - restart address 0008
     * 0010 - restart $10 - restart address 0010
     * 0018 - restart $18 - restart address 0018
     * 0020 - restart $20 - restart address 0020
     * 0028 - restart $28 - restart address 0028
     * 0030 - restart $30 - restart address 0030
     * 0038 - restart $38 - restart address 0038
     * 0040 - vertical blank interrupt start address
     * 0048 - lcdc status interrupt start address
     * 0050 - timer overflow interrupt start address
     * 0058 - serial transfer completion interrupt
     * 0060 - high-to-low of p10-p13 interrupt
     *
    */

    public Memory(CPU gbCPU) {
        this.gbCPU = gbCPU;
        resetMemory();
        init();
    }


}
