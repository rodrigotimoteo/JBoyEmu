public class PPU {

    private final CPU cpu;
    private final Memory memory;
    private final DisplayFrame displayFrame;

    private int counter;
    private int completedCycles;
    private int mode;
    private int currentLine;

    private boolean lcdOn;
    private boolean windowDisplay;
    private boolean windowTileMap;
    private boolean windowTileData;
    private boolean tileMapDisplay;
    private boolean spriteSize;
    private boolean spriteOn;
    private boolean backAndWindowOn;

    //Geters

    public int getCounter() {
        return counter;
    }

    public boolean getLcdOn() {
        return lcdOn;
    }

    public boolean getWindowDisplay() {
        return windowDisplay;
    }

    public boolean getWindowTileMap() {
        return windowTileMap;
    }

    public boolean getWindowTileData() {
        return windowTileData;
    }

    public boolean getTileMapDisplay() {
        return tileMapDisplay;
    }

    public boolean getSpriteSize() {
        return spriteSize;
    }

    public boolean getSpriteOn() {
        return spriteOn;
    }

    public boolean getBackAndWindowOn() {
        return backAndWindowOn;
    }

    public int getCompletedCycles() {
        return completedCycles;
    }

    public PPU(CPU cpu, Memory memory, DisplayFrame displayFrame) {
        this.cpu = cpu;
        this.memory = memory;
        this.displayFrame = displayFrame;

        counter = 0;
        lcdOn = false;
        windowDisplay = false;
    }

    public void cycle() {
        readLCDCStatus();
        readLCDStatus();
        draw();
    }

    private void readLCDCStatus() {
        int bit = 0;

        char LCDC = memory.getMemory(0xFF40);
        //Read bit 7
        bit = LCDC & 0x80 >> 7;
        lcdOn = bit == 1;
        //Read bit 6
        bit = LCDC & 0x40 >> 6;
        windowTileMap = LCDC == 1;
        //Read bit 5
        bit = LCDC & 0x20 >> 5;
        windowDisplay = bit == 1;
        //Read bit 4
        bit = LCDC & 0x10 >> 4;
        windowTileData = bit == 1;
        //Read bit 3
        bit = LCDC & 0x08 >> 3;
        tileMapDisplay = bit == 1;
        //Read bit 2
        bit = LCDC & 0x04 >> 2;
        spriteSize = bit == 1;
        //Read bit 1
        bit = LCDC & 0x02 >> 1;
        spriteOn = bit == 1;
        //Read bit 0
        bit = LCDC & 0x01;
        backAndWindowOn = bit == 1;

    }

    private void readLCDStatus() {
        int bit = 0;

        char LCD = memory.getMemory(0xFF41);
        //Read bit 1 and 0
        bit = LCD & 0x02 >> 1;
        mode += bit + bit;
        bit = LCD & 0x01;
        mode += bit;
    }

    private char readLY() {
        return (char) (memory.getMemory(0xFF44) & 0xFF);
    }

    private void draw() {
        if(lcdOn) counter++;
        else return;

        if(counter >= 456) { //Completed line
            completedCycles++;
            counter = 0;

            //memory.setMemory(0xFF44, (char) (memory.getMemory(0xFF44) + 1));
            char currentLine = readLY();

            //if (currentLine == 144) RequestInterupt(0) ;

            //else if (currentline > 153)
            //    m_Rom[0xFF44] = 0;

            //else if (currentline < 144)
            //    DrawScanLine();


        switch(mode) {
            case 0: //H-BLANK

                break;

            case 1: //V-BLANK

                break;

            case 2: //OAM Search

                break;

            case 3: //Pixel Transfer

                break;
        }

        displayFrame.repaint();
    }

    }
}
