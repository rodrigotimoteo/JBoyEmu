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

    public void writePriv(int index, char value) {
        memory[index] = value;
    }

    public void setMemory(int index, char value) {
        if(index == 0xff44 || index == 0xff04) memory[index] = 0;
        else if(index >= 0xc000 && index <= 0xde00) {
            memory[index] = (char) (value & 0xff);
            memory[index + 0x2000] = (char) (value & 0xff);
        }
        else if(index >= 0xe000 && index <= 0xfe00) {
            memory[index] = (char) (value & 0xff);
            memory[index - 0x2000] = (char) (value & 0xff);
        }
        else if(index > 0x8000) memory[index] = (char) (value & 0xff);
    }

    //Getters

    public char getMemory(int index) {
        return (char) (memory[index] & 0xff);
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

        setMemory(0xff44, (char) 0x99);
        setMemory(0xff41, (char) 0x01);
    }

    public Memory(CPU gbCPU) {
        this.gbCPU = gbCPU;
        resetMemory();
        init();
    }


}
