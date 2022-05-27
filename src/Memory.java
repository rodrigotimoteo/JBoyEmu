import java.util.Arrays;

public class Memory {

    private final char[] memory = new char[0x10000];

    private byte[] cartridge;

    CPU gbCPU;

    //Resets

    private void resetMemory() {
        Arrays.fill(memory, (char) 0);
    }

    //Setters

    public void setMemory(int index, char value) {
        memory[index] = (char) (value & 0xff);
    }

    //Getters

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

        //TEMPORARY
        setMemory(0xff00, (char) 0xcf);

        setMemory(0xff10, (char) 0x80);
        setMemory(0xff11, (char) 0xbf);
        setMemory(0xff12, (char) 0xf3);
        setMemory(0xff14, (char) 0xbf);
        setMemory(0xff16, (char) 0x3f);
        setMemory(0xff19, (char) 0xbf);
        setMemory(0xff1a, (char) 0x7f);
        setMemory(0xff1b, (char) 0xff);
        setMemory(0xff1c, (char) 0x9f);
        setMemory(0xff1e, (char) 0xbf);
        setMemory(0xff20, (char) 0xff);
        setMemory(0xff23, (char) 0xbf);
        setMemory(0xff24, (char) 0x77);
        setMemory(0xff25, (char) 0xf3);
        setMemory(0xff26, (char) 0xf1);
        setMemory(0xff40, (char) 0x91);
        setMemory(0xff47, (char) 0xfc);
        setMemory(0xff48, (char) 0xff);
        setMemory(0xff49, (char) 0xff);
    }

    /* Reserved Memory Locations
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
