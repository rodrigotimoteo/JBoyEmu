public class PPU {

    private final CPU cpu;
    private final Memory memory;
    private final DisplayFrame displayFrame;

    private boolean lcdStatus = true;
    private int scanLineCounter;

    public void setLCDStatus(boolean status) {
        lcdStatus = status;
    }

    public PPU(CPU cpu, Memory memory, DisplayFrame displayFrame) {
        this.cpu = cpu;
        this.memory = memory;
        this.displayFrame = displayFrame;

        scanLineCounter = 456;
    }

    public void draw(int cycles, char mem) {
        if(mem != memory.getMemory(0xFF44)) memory.setMemory(0xFF44, (char) 0);

        if (lcdStatus) scanLineCounter -= cycles;
        else return;

        if (scanLineCounter <= 0)
        {
            // time to move onto next scanline
            memory.setMemory(0xFF44, (char) (memory.getMemory(0xFF44) + 1));
            int currentline = memory.getMemory(0xFF44);

            scanLineCounter = 456 ;

            if (currentline == 144) System.out.println("OI");
            else if (currentline > 153) memory.setMemory(0xFF44, (char) 0);
            else if (currentline < 144) displayFrame.repaint();
        }
    }

}
