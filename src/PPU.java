public class PPU {

    private final Memory memory;
    private DisplayFrame displayFrame;
    private DisplayPanel display;

    private int counter;

    private int mode;
    private int currentLine;
    private int scrollY;
    private int scrollX;

    private final byte[][] painting = new byte[160][144];

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

    public void setScrolls(int scrollX, int scrollY) {
        this.scrollX = scrollX;
        this.scrollY = scrollY;
    }

    public void setCounter(int counter) {
        this.counter = counter;
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
        counter++;

        currentLine = readLY();
        int tileMapAddress = getTileMapDisplay() ? 0x9c00 : 0x9800;
        int tileDataAddress = getWindowTileData() ? 0x8000 : 0x9000;

        System.out.print(currentLine + " " + counter * 4 + " ");

        if(tileDataAddress == 0x9000) negativeTiles = true;

        switch (mode) {
            case 0 -> { //H-BLANK
//                System.out.println(" Entering H-Blank " + counter + " " + Integer.toHexString(currentLine));
                if (counter == 114) {
                    counter = 0;
                    displayFrame.repaint();
                    currentLine++;
                    memory.writePriv(0xff44, (char) currentLine);
                    currentLine = readLY();
                    if (currentLine == 144) {
                        display.drawImage(painting);
                        memory.writePriv(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                    } else memory.writePriv(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 2));
                }
            }
            case 1 -> { //V-BLANK
//                System.out.println(" Entering V-Blank " + counter + " " + Integer.toHexString(currentLine));
                if (currentLine == 153 && counter == 114) {
                    memory.writePriv(0xff0f, (char) ((memory.getMemory(0xff0f) & 0xff) | 0x01));
                    memory.writePriv(0xff44, (char) 0);
                    memory.writePriv(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                    setScrolls(readScrollX(), readScrollY());
                    getToSleep = true;
                    counter = 0;
                }
                if (counter == 114) {
                    counter = 0;
                    currentLine++;
                    memory.writePriv(0xff44, (char) currentLine);
                    currentLine = readLY();
                }
            }
            case 2 -> { //OAM Search
//                System.out.println(" Entering OAM " + counter + " " + Integer.toHexString(currentLine));
                if(getToSleep) getToSleep = false;
                if (counter == 10) {
                    memory.writePriv(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) + 1));
                }
            }
            case 3 -> { //Pixel Transfer
//                System.out.println(" Entering Pixel Transfer " + counter + " " + Integer.toHexString(currentLine));
                int tile;
                if (counter == 40) {
                    //System.out.println(Integer.toHexString(tileDataAddress));
                    int tempY = (currentLine + scrollY) % 0x100;
                    for (int i = 0; i < 20; i++) {
                        int tempX = (scrollX + i) % 0x20;
                        tile = memory.getMemory(tileMapAddress + ((tempY / 8) * 0x20) + tempX);
                        if(negativeTiles) {
                            tile = memory.getMemory(tileMapAddress + ((tempY / 8) * 0x20) + tempX);
                            if(((tile & 0x80) >> 7) == 0) tile = tile & 0x7f;
                            else tile = (tile & 0x7f) - 0x80;
                        } else {
                            tile = memory.getMemory(tileMapAddress + ((tempY / 8) * 0x20) + tempX);
                        }
                        writeTile(tileDataAddress, tile, i * 8);
                    }
                    memory.writePriv(0xff41, (char) ((memory.getMemory(0xff41) & 0xff) - 3));
                }
            }
        }
    }

    private void writeTile(int tileDataAddress, int tile, int x) {
        int tileLine;
        if(negativeTiles) {
            if(((tile & 0x80) >> 7) == 0) tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);
            else tileLine = 0x8800 + (((tile & 0xff) - 128) * 0x10) + ((currentLine % 8) * 2);
        } else tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);

//        System.out.println(Integer.toHexString(tile) + "  " + Integer.toHexString(tileLine) + "  " + Integer.toHexString(tileLine + 1));
        //System.out.println(currentLine + "  " + Integer.toHexString(tile));
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
