public class PPU {

    private final CPU cpu;
    private final Memory memory;
    private Display display;

    private int counter;
    private int completedCycles;
    private int mode;
    private int currentLine;

    private char currentTileMapRow;
    private char currentTileLine;

    private boolean draw;

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

    public int getCurrentLine() {
        return currentLine;
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

    public boolean getDraw() {
        return draw;
    }

    //Seters

    public void setDisplay(Display display) {
        this.display = display;
    }

    public void setDraw(boolean state) {
        draw = state;
    }

    public PPU(CPU cpu, Memory memory) {
        this.cpu = cpu;
        this.memory = memory;

        counter = 0;
        lcdOn = false;
        windowDisplay = false;
    }

    public void cycle() {
        readLCDCStatus();
        readLCDStatus();
        draw();
    }

    public void readLCDCStatus() {
        int bit;

        char LCDC = memory.getMemory(0xFF40);
        //Read bit 7
        bit = (LCDC & 0x80) >> 7;
        lcdOn = bit == 1;
        //Read bit 6
        bit = (LCDC & 0x40) >> 6;
        windowTileMap = bit == 1;
        //Read bit 5
        bit = (LCDC & 0x20) >> 5;
        windowDisplay = bit == 1;
        //Read bit 4
        bit = (LCDC & 0x10) >> 4;
        windowTileData = bit == 1;
        //Read bit 3
        bit = (LCDC & 0x08) >> 3;
        tileMapDisplay = bit == 1;
        //Read bit 2
        bit = (LCDC & 0x04) >> 2;
        spriteSize = bit == 1;
        //Read bit 1
        bit = (LCDC & 0x02) >> 1;
        spriteOn = bit == 1;
        //Read bit 0
        bit = LCDC & 0x01;
        backAndWindowOn = bit == 1;

    }

    public void readLCDStatus() {
        int bit;

        char LCD = memory.getMemory(0xFF41);
        //Read bit 1 and 0
        bit = (LCD & 0x02) >> 1;
        mode = bit + bit;
        bit = LCD & 0x01;
        mode += bit;
    }

    public char readScrollY() {
        return (char) (memory.getMemory(0xFF42) & 0xff);
    }

    public char readScrollX() {
        return (char) (memory.getMemory(0xFF43) & 0xff);
    }

    private char readLY() {
        return (char) (memory.getMemory(0xFF44) & 0xFF);
    }

    private void draw() {
        if(lcdOn) counter += 2;
        else return;

        char currentLine = readLY();
        int tileMapAddress = getTileMapDisplay() ? 0x9c00 : 0x9800;
        int tileDataAddress = getWindowTileData() ? 0x8000 : 0x9000;

        switch(mode) {
            case 0: //H-BLANK
                //System.out.println(" Entering H-Blank " + counter);
                if(counter == 114) {
                    draw = true;
                    counter = 0;
                    currentLine++;
                    memory.setMemory(0xff42, currentLine);
                    memory.setMemory(0xff44, currentLine);
                    memory.setMemory(0xff0f, (char) ((memory.getMemory(0xff0f) & 0xff) + 2));
                    currentLine = readLY();
                if(currentLine == 144) memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                else memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 2));
            }
                break;

            case 1: //V-BLANK
                //System.out.println(" Entering V-Blank " + counter);
                if(counter == 114) {
                    counter = 0;
                    currentLine++;
                    memory.setMemory(0xff44, currentLine);
                    currentLine = readLY();
                } if(currentLine == 153) {
                    memory.setMemory(0xff0f, (char) ((memory.getMemory(0xff0f) & 0xff) + 1));
                    GBEmulator.setFrameCompleted(true);
                    memory.setMemory(0xff42, (char) 0);
                    memory.setMemory(0xff44, (char) 0);
                    memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                }
                break;

            case 2: //OAM Search
                //System.out.println(" Entering OAM " + counter);
                if(counter == 10) {
                    memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                    memory.setMemory(0xff0f, (char) ((memory.getMemory(0xff0f) & 0xff) + 2));
                }
                break;

            case 3: //Pixel Transfer
                //System.out.println(" Entering Pixel Transfer " + counter);
                if(counter == 34) {
                    for(int i = 0; i < 0x20; i++) display.setRowTiles(i, memory.getMemory(tileMapAddress + (currentLine * 0x20) + i) & 0xff);
                    for(int i = 0; i < 0x100; i++) {
                        for(int l = 0; l < 8; l++) {
                            display.setTiles(i, l, (((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x80) >> 7) * 2) + ((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x80) >> 7));
                            display.setTiles(i, l, (((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x40) >> 6) * 2) + ((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x40) >> 6));
                            display.setTiles(i, l, (((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x20) >> 5) * 2) + ((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x20) >> 5));
                            display.setTiles(i, l, (((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x10) >> 4) * 2) + ((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x10) >> 4));
                            display.setTiles(i, l, (((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x8) >> 3) * 2) + ((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x8) >> 3));
                            display.setTiles(i, l, (((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x4) >> 2) * 2) + ((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x4) >> 2));
                            display.setTiles(i, l, (((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x2) >> 1) * 2) + ((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x2) >> 1));
                            display.setTiles(i, l, ((memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x1) * 2) + (memory.getMemory(tileDataAddress + (i * 0x10) + (l + 1)) & 0x1));
                        }
                    }
                    memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) - 3));
                }
                break;
        }


    }

}
