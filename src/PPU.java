public class PPU {

    private final Memory memory;
    private final CPU cpu;
    private DisplayFrame displayFrame;
    private DisplayPanel display;

    private final int HBLANK = 0;
    private final int VBLANK = 1;
    private final int OAM = 2;
    private final int PIXEL_TRANSFER = 3;

    private final int LCDC_CONTROL = 0xff40;
    private final int LCDC_STATUS = 0xff41;

    private final int VBLANK_INTERRUPT = 0;
    private final int STAT_INTERRUPT = 1;

    private int counter;

    private int mode;
    private int currentLine;

    private int scrollY;
    private int scrollX;

    private int windowX;
    private int windowY;

    private final byte[][] painting = new byte[160][144];
    private boolean getToSleep;

    private boolean lcdOn;
    private boolean windowOn;
    private boolean backgroundOn;
    private boolean windowTileMap;
    private boolean windowTileData;
    private boolean backgroundTileMap;
    private boolean spriteSize;
    private boolean spriteOn;


    private boolean negativeTiles;

    //Getters

    public boolean getLcdOn() {
        return lcdOn;
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

    public void reset() {
        counter = 0;
    }

    public PPU(Memory memory, CPU cpu) {
        this.memory = memory;
        this.cpu = cpu;

        counter = 0;
        lcdOn = false;
        windowOn = false;
    }

    public void cycle() {
        readLCDControl();
        readLCDStatus();
        draw();
    }

    public void readLCDControl() {
        int bit;
        char LCDControl = memory.getMemory(LCDC_CONTROL);

        //Read LCD and PPU enabled bit
        bit = (LCDControl & 0x80) >> 7;
        lcdOn = bit == 1;

        //Read Window Tile Map (where window tiles are located if enabled)
        bit = (LCDControl & 0x40) >> 6;
        windowTileMap = bit == 1;

        //Read Window and Background Tile Data
        bit = (LCDControl & 0x10) >> 4;
        windowTileData = bit == 1;

        //Read Background tile Area
        bit = (LCDControl & 0x08) >> 3;
        backgroundTileMap = bit == 1;

        //Read Sprite Size (0 = 8x8, 1 = 8x16)
        bit = (LCDControl & 0x04) >> 2;
        spriteSize = bit == 1;

        //Read Sprite Enabled Status
        bit = (LCDControl & 0x02) >> 1;
        spriteOn = bit == 1;

        //Read Background and window Enabled Status
        bit = LCDControl & 0x01;
        backgroundOn = bit == 1;
        windowOn = bit == 1;

        //Read Window Enabled state
        bit = (LCDControl & 0x20) >> 5;
        windowOn = windowOn && bit == 1;
    }

    private void readLCDStatus() {
        int bit;

        char LCD = memory.getMemory(LCDC_STATUS);
        //Read bit 1 and 0
        bit = (LCD & 0x02) >> 1;
        mode = bit + bit;
        bit = LCD & 0x01;
        mode += bit;
    }

    private void readWindow() {
        windowY = memory.getMemory(0xff4a);
        windowX = memory.getMemory(0xff4b);
    }

    public char readScrollY() {
        return memory.getMemory(0xff42);
    }

    public char readScrollX() {
        return memory.getMemory(0xff43);
    }

    public char readLY() {
        return memory.getMemory(0xff44);
    }

    private boolean treatLYC() {
        int lyc = memory.getMemory(0xff45);

        if(currentLine == lyc) {
            memory.setBit(LCDC_CONTROL, 2);
            return (memory.getMemory(LCDC_CONTROL) & 0x40) != 0;
        } else {
            memory.resetBit(LCDC_CONTROL, 2);
        }

        return false;
    }

    private void changeMode(int mode) {
        int lcdStatus = memory.getMemory(LCDC_STATUS) & 0xfc;
        boolean requestInterrupt = false;

        switch(mode) {
            case HBLANK -> {
                if((lcdStatus & 0x08) != 0) requestInterrupt = true;
            }
            case VBLANK -> {
                lcdStatus |= 0x01;
                if((lcdStatus & 0x10) != 0) requestInterrupt = true;
            }
            case OAM -> {
                lcdStatus |= 0x02;
                if((lcdStatus & 0x20) != 0) requestInterrupt = true;
            }
            case PIXEL_TRANSFER -> {
                lcdStatus |= 0x03;
            }
        }
        memory.writePriv(LCDC_STATUS, (char) lcdStatus);
        boolean lycInterrupt = treatLYC();

        if(requestInterrupt || lycInterrupt) cpu.setInterrupt(STAT_INTERRUPT);
    }

    private void draw() {
//        if(counter == 0) {
//            setScrolls(readScrollX() & 0x7, scrollY);
//        }

        counter++;

        currentLine = readLY();
        int tileMapAddress;
        if(windowOn) tileMapAddress = windowTileMap ? 0x9c00 : 0x9800;
        else tileMapAddress = backgroundTileMap ? 0x9c00 : 0x9800;

        int tileDataAddress = windowTileData ? 0x8000 : 0x9000;

        if(tileDataAddress == 0x9000) negativeTiles = true;

        switch (mode) {
            case 0 -> { //H-BLANK
                if (counter == 114) {
                    counter = 0;
                    currentLine++;
                    //memory.writePriv(0xff44, (char) currentLine);
                    if (currentLine >= 144) {
                        display.drawImage(painting);
                        displayFrame.repaint();
                        changeMode(VBLANK);
                        cpu.setInterrupt(VBLANK_INTERRUPT);
                    }
                    else changeMode(OAM);
                }
            }
            case 1 -> { //V-BLANK
                if (counter == 114) {
                    counter = 0;
                    currentLine++;
                    //memory.writePriv(0xff44, (char) currentLine);
                }
                if (currentLine > 153) {
                    currentLine = 0;
                    //memory.writePriv(0xff44, (char) currentLine);
                    changeMode(OAM);
                    getToSleep = true;
                    counter = 0;
                }
            }
            case 2 -> { //OAM Search
                if(getToSleep) getToSleep = false;
                if (counter == 10) {
                    changeMode(PIXEL_TRANSFER);
                }
            }
            case 3 -> { //Pixel Transfer
                if (counter == 40) {
                    setScrolls(readScrollX(), readScrollY());
                    if(backgroundOn) drawBackground(tileMapAddress, tileDataAddress);
                    //if(windowOn) drawWindow(tileMapAddress, tileDataAddress);
                    //if(spriteOn) drawSprite();
                    changeMode(HBLANK);
                }
            }
        }
    }

    private void drawBackground(int tileMapAddress, int tileDataAddress) {
        int tempY = (currentLine + scrollY) % 0x100;

        for (int x = 0; x < 160; x++) {
            int tempX = (scrollX + x) % 0x100;

            int address = tileMapAddress + ((tempY / 8) * 0x20);
            int tile = memory.getMemory(address + (tempX) / 8);

            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tile = tile & 0x7f;
                else tile = (tile & 0x7f) - 0x80;
            }

            int tileLine;
            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);
                else tileLine = 0x8800 + (((tile & 0xff) - 128) * 0x10) + ((currentLine % 8) * 2);
            } else tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);

            int offset = 7 - (tempX % 8);
            painting[x][currentLine] = (byte) (((memory.getMemory(tileLine) & (1 << offset)) >> offset) + (((memory.getMemory(tileLine + 1) & (1 << offset)) >> offset) * 2));
        }
    }

    private void drawWindow(int tileMapAddress, int tileDataAddress) {
        if (windowY > 143 || windowX > 166 || windowY > currentLine) {
            return;
        }

        for(int x = 0; x < 160; x++) {
            int tempX = (windowX + x) % 0x100;

            int address = tileMapAddress + ((currentLine / 8) * 0x20);
            int tile = memory.getMemory(address + (tempX) / 8);

            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tile = tile & 0x7f;
                else tile = (tile & 0x7f) - 0x80;
            }

            int tileLine;
            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);
                else tileLine = 0x8800 + (((tile & 0xff) - 128) * 0x10) + ((currentLine % 8) * 2);
            } else tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);

            int offset = 7 - (tempX % 8);
            painting[x][currentLine] = (byte) (((memory.getMemory(tileLine) & (1 << offset)) >> offset) + (((memory.getMemory(tileLine + 1) & (1 << offset)) >> offset) * 2));
        }
    }

    private void drawSprite() {
        int tempY, tempX, spriteLocation, spriteAttributes;
        for (int i = 0; i < 40; i++) {
            tempY = memory.getMemory(0xfe00 + (i * 4)) - 16;
            tempX = memory.getMemory(0xfe00 + (i * 4) + 1) - 8;

            spriteLocation = memory.getMemory(0xfe00 + (i * 4) + 2);
            spriteAttributes = memory.getMemory(0xfe00 + (i * 4) + 3);

            boolean yFlipped = ((spriteAttributes & 0x40) >> 6) == 1;
            boolean xFlipped = ((spriteAttributes & 0x20) >> 5) == 1;

            int spriteSize = this.spriteSize ? 16 : 8;

//            if(currentLine >= tempY) {
//                writeTileSprite(spriteLocation, tile, tempX, yFlipped, xFlipped);
//            }

        }
    }
}
