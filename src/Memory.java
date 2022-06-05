import java.util.Arrays;

public class Memory {

    private final char[] memory = new char[0x10000];

    CPU cpu;
    DisplayFrame displayFrame;

    private int cartridgeType;

    //Resets

    private void resetMemory() {
        Arrays.fill(memory, (char) 0xff);
    }

    //Setters

    public void setCartridgeType(int cartridgeType) {
        this.cartridgeType = cartridgeType;
    }

    public void writePriv(int index, char value) {
        memory[index] = value;
    }

    public void setMemory(int index, char value) {
        switch(cartridgeType) {
            case 1 -> setMemoryMBC0(index, value);
            case 2 -> setMemoryMBC1(index, value);
        }
    }

    public void setMemoryMBC0(int index, char value) {
        if(index == 0xff44 || index == 0xff04) {
            memory[index] = 0; //Check timer and currentLine
        }
        else if(index == 0xff26) { //Check sound enable/disable
            if(value != 0) memory[index] = 0xff;
            else memory[index] = 0;
        }
        else if(index == 0xff14 && ((value & 0xff) >> 7) != 0) { //Check Sound Channel 1
            memory[index] = value;
        }
        else if(index >= 0xc000 && index <= 0xde00) { //Check Ram Echo
            memory[index] = (char) (value & 0xff);
            memory[index + 0x2000] = (char) (value & 0xff);
        }
        else if(index >= 0xe000 && index <= 0xfe00) { //Check Ram Echo
            memory[index] = (char) (value & 0xff);
            memory[index - 0x2000] = (char) (value & 0xff);
        }
        else if(index == 0xff00) {
            if(value == 0x10) setMemory(index, (char) (0xc0 + (memory[0xff00] & 0xf) + 0x10));
            else if(value == 0x20) setMemory(index, (char) (0xc0 + (memory[0xff00] & 0xf) + 0x20));
            else if(value == 0x30) setMemory(index, (char) (0xc0 + (memory[0xff00] & 0xf)));
        }
        else if(index > 0x8000) {
            memory[index] = (char) (value & 0xff);
        }
    }

    public void setMemoryMBC1(int index, char value) {

    }

    public void setDisplayFrame(DisplayFrame displayFrame) {
        this.displayFrame = displayFrame;
    }

    //Getters

    public char getMemory(int index) {
        if(index == 0xff00) {
            return (char) displayFrame.getJoypad((char) (memory[index] & 0xff));
        }
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
        writePriv(0xff00, (char) 0xcf);
        writePriv(0xff10, (char) 0x80);
        writePriv(0xff11, (char) 0xbf);
        writePriv(0xff12, (char) 0xf3);
        writePriv(0xff14, (char) 0xbf);
        writePriv(0xff16, (char) 0x3f);
        writePriv(0xff19, (char) 0xbf);
        writePriv(0xff1a, (char) 0x7f);
        writePriv(0xff1b, (char) 0xff);
        writePriv(0xff1c, (char) 0x9f);
        writePriv(0xff1e, (char) 0xbf);
        writePriv(0xff20, (char) 0xff);
        writePriv(0xff23, (char) 0xbf);
        writePriv(0xff24, (char) 0x77);
        writePriv(0xff25, (char) 0xf3);
        writePriv(0xff26, (char) 0xf1);
        writePriv(0xff40, (char) 0x91);
        writePriv(0xff47, (char) 0xfc);
        writePriv(0xff48, (char) 0xff);
        writePriv(0xff49, (char) 0xff);
        writePriv(0xff4d, (char) 0xff);
        writePriv(0xff44, (char) 0x00);
    }

    public Memory(CPU cpu) {
        this.cpu = cpu;
        resetMemory();
        init();
    }


}
