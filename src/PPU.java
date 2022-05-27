public class PPU {

    private final Memory memory;
    private DisplayFrame displayFrame;
    private DisplayPanel display;

    private int counter;

    private int mode;
    private int currentLine;
    private int scrollY;

    private final byte[][] painting = new byte[256][153];

    private char currentTileMapRow;
    private char currentTileLine;

    private boolean getToSleep;

    private boolean lcdOn;
    private boolean windowDisplay;
    private boolean windowTileMap;
    private boolean windowTileData;
    private boolean tileMapDisplay;
    private boolean spriteSize;
    private boolean spriteOn;
    private boolean backAndWindowOn;

    private boolean negativeTiles;

    //Getters

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

    public boolean isGetToSleep() {
        return getToSleep;
    }

    //Setters

    public void setDisplayFrame(DisplayFrame display) {
        displayFrame = display;
    }

    public void setDisplayPanel(DisplayPanel display) {
        this.display = display;
    }

    public PPU(Memory memory) {
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
        return (char) (memory.getMemory(0xff42) & 0xff);
    }

    public char readScrollX() {
        return (char) (memory.getMemory(0xff43) & 0xff);
    }

    public char readLY() {
        return (char) (memory.getMemory(0xff44) & 0xff);
    }

    private void draw() {
        if(lcdOn) counter += 2;
        else return;

        if(currentLine == 0 && counter == 2) { scrollY = readScrollY(); }
        currentLine = readLY();
        int tileMapAddress = getTileMapDisplay() ? 0x9c00 : 0x9800;
        int tileDataAddress = getWindowTileData() ? 0x8000 : 0x9000;

        if(tileDataAddress == 0x9000) negativeTiles = true;

        switch (mode) {
            case 0 -> { //H-BLANK
                //System.out.println(" Entering H-Blank " + counter + " " + Integer.toHexString(currentLine));
                if (counter == 114) {
                    counter = 0;
                    currentLine++;
                    memory.setMemory(0xff44, (char) currentLine);
                    memory.setMemory(0xff0f, (char) ((memory.getMemory(0xff0f) & 0xff) + 2));
                    currentLine = readLY();
                    if (currentLine == 144) {
                        display.drawImage(painting);
                        displayFrame.repaint();
                        memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                    } else memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 2));
                }
            }
            case 1 -> { //V-BLANK
                //System.out.println(" Entering V-Blank " + counter + " " + Integer.toHexString(currentLine));
                if (counter == 114) {
                    counter = 0;
                    currentLine++;
                    memory.setMemory(0xff44, (char) currentLine);
                    currentLine = readLY();
                }
                if (currentLine == 153) {
                    memory.setMemory(0xff0f, (char) ((memory.getMemory(0xff0f) & 0xff) + 1));
                    memory.setMemory(0xff44, (char) 0);
                    memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                    getToSleep = true;
                }
            }
            case 2 -> { //OAM Search
                //System.out.println(" Entering OAM " + counter + " " + Integer.toHexString(currentLine));
                if(getToSleep) getToSleep = false;
                if (counter == 10) {
                    memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                    memory.setMemory(0xff0f, (char) ((memory.getMemory(0xff0f) & 0xff) + 2));
                }
            }
            case 3 -> { //Pixel Transfer
                //System.out.println(" Entering Pixel Transfer " + counter + " " + Integer.toHexString(currentLine));
                int tile;
                if (counter == 34) {
                    int temp = (currentLine + scrollY) % 255;
                    for (int i = 0; i < 0x20; i++) {
                        tile = memory.getMemory(tileMapAddress + ((temp / 8) * 0x20) + i);
                        if(negativeTiles) {
                            if((tile >> 7) == 0) tile = tile & 0x7f;
                            else tile = (tile & 0x7f) - 128;
                        } else {
                            tile = tile & 0xff;
                        }
                        writeTile(tileDataAddress, tile, i * 8);
                    }
                    memory.setMemory(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) - 3));
                }
            }
        }
    }

    private void writeTile(int tileDataAddress, int tile, int x) {
        int tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);
//        System.out.println(Integer.toHexString(tile) + "  " + Integer.toHexString(tileLine) + "  " + Integer.toHexString(tileLine + 1));

        painting[x][currentLine] = (byte) (((memory.getMemory(tileLine) & 0x80) >> 7) + (((memory.getMemory(tileLine + 1) & 0x80) >> 7) * 2));
        painting[x + 1][currentLine] = (byte) (((memory.getMemory(tileLine) & 0x40) >> 6) + (((memory.getMemory(tileLine + 1) & 0x40) >> 6) * 2));
        painting[x + 2][currentLine] = (byte) (((memory.getMemory(tileLine) & 0x20) >> 5) + (((memory.getMemory(tileLine + 1) & 0x20) >> 5) * 2));
        painting[x + 3][currentLine] = (byte) (((memory.getMemory(tileLine) & 0x10) >> 4) + (((memory.getMemory(tileLine + 1) & 0x10) >> 4) * 2));
        painting[x + 4][currentLine] = (byte) (((memory.getMemory(tileLine) & 0x8) >> 3) + (((memory.getMemory(tileLine + 1) & 0x8) >> 3) * 2));
        painting[x + 5][currentLine] = (byte) (((memory.getMemory(tileLine) & 0x4) >> 2) + (((memory.getMemory(tileLine + 1) & 0x4) >> 2) * 2));
        painting[x + 6][currentLine] = (byte) (((memory.getMemory(tileLine) & 0x2) >> 1) + (((memory.getMemory(tileLine + 1) & 0x2) >> 1) * 2));
        painting[x + 7][currentLine] = (byte) ((memory.getMemory(tileLine) & 0x1) + ((memory.getMemory(tileLine + 1) & 0x1) * 2));
    }

}
