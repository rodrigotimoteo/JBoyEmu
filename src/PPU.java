public class PPU {

    private final Memory memory;
    private final CPU cpu;
    private DisplayFrame displayFrame;
    private DisplayPanel display;

    private final int HBLANK = 0;
    private final int VBLANK = 1;
    private final int OAM = 2;
    private final int PIXEL_TRANSFER = 3;

    private final int TILE_DATA_0 = 0x8000;
    private final int TILE_DATA_1 = 0x8800;
    private final int TILE_DATA_2 = 0x9000;

    private final int TILE_MAP_0 = 0x9800;
    private final int TILE_MAP_1 = 0x9c00;

    private final int OAM_START = 0xfe00;
    private final int LCDC_CONTROL = 0xff40;
    private final int LCDC_STATUS = 0xff41;
    private final int SCROLL_Y_REGISTER = 0xff42;
    private final int SCROLL_X_REGISTER = 0xff43;
    private final int LY_REGISTER = 0xff44;
    private final int LYC_REGISTER = 0xff45;
    private final int WINDOW_Y_REGISTER = 0xff4a;
    private final int WINDOW_X_REGISTER = 0xff4b;

    private final int VBLANK_INTERRUPT = 0;
    private final int STAT_INTERRUPT = 1;

    private int counter;

    private int mode;
    private int currentLine;
    private int currentLineWindow;

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
    private boolean tileData;
    private boolean backgroundTileMap;
    private boolean spriteSize;
    private boolean spriteOn;


    private boolean negativeTiles;

    //Getters

    public boolean getLcdOn() {
        return lcdOn;
    }

    public int getCounter() {
        return counter;
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

    public void setScrolls() {
        this.scrollX = readScrollX();
        this.scrollY = readScrollY();
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
        memory.setLcdOn(lcdOn);

        //Read Window Tile Map (where window tiles are located if enabled)
        bit = (LCDControl & 0x40) >> 6;
        windowTileMap = bit == 1;

        //Read Window and Background Tile Data
        bit = (LCDControl & 0x10) >> 4;
        tileData = bit == 1;

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
        bit = LCD & 0x03;
        mode = bit;
        memory.setPpuMode(mode);
    }

    private void readWindow() {
        this.windowY = memory.getMemory(WINDOW_Y_REGISTER);
        this.windowX = memory.getMemory(WINDOW_X_REGISTER) - 7;
    }

    public char readScrollY() {
        return memory.getMemory(SCROLL_Y_REGISTER);
    }

    public char readScrollX() {
        return memory.getMemory(SCROLL_X_REGISTER);
    }

    public char readLY() {
        return memory.getMemory(LY_REGISTER);
    }

    private boolean treatLYC() {
        int lyc = memory.getMemory(LYC_REGISTER);

        if(currentLine == lyc) {
            memory.setBit(LCDC_STATUS, 2);
            return (memory.getMemory(LCDC_STATUS) & 0x40) != 0;
        } else {
            memory.resetBit(LCDC_STATUS, 2);
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
        //System.out.println(currentLine + "   " + Integer.toHexString(memory.getMemory(LYC_REGISTER)));
        boolean lycInterrupt = treatLYC();
        if(requestInterrupt || lycInterrupt) cpu.setInterrupt(STAT_INTERRUPT);
    }

    int lol = 0;

    private void draw() {
        counter++;

        currentLine = readLY();

        switch (mode) {
            case 0 -> { //H-BLANK
                if (counter == 114) {
                    counter = 0;
                    currentLine++;
                    memory.writePriv(LY_REGISTER, (char) currentLine);
                    if (currentLine > 143) {
                        display.drawImage(painting);
                        lol++;
                        requestRepaint();
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
                    memory.writePriv(LY_REGISTER, (char) currentLine);
                    if (currentLine > 153) {
                        currentLine = 0;
                        currentLineWindow = 0;
                        memory.writePriv(LY_REGISTER, (char) currentLine);
                        changeMode(OAM);
                        getToSleep = true;
                    }
                }
            }
            case 2 -> { //OAM Search
                if(getToSleep) getToSleep = false;
                if (counter == 20) {
                    changeMode(PIXEL_TRANSFER);
                }
            }
            case 3 -> { //Pixel Transfer
                if (counter == 63) {
                    int backgroundMapAddress, windowMapAddress;
                    windowMapAddress = windowTileMap ? TILE_MAP_1 : TILE_MAP_0;
                    backgroundMapAddress = backgroundTileMap ? TILE_MAP_1 : TILE_MAP_0;

                    int tileDataAddress = tileData ? TILE_DATA_0 : TILE_DATA_2;

                    if(tileDataAddress == TILE_DATA_2) negativeTiles = true;

                    //System.out.println(currentLine + "  " + Integer.toHexString(backgroundMapAddress));
//                    System.out.println(scrollX + " " + scrollY + "  " + cpu.getIsHalted());


                    setScrolls();
                    readWindow();
                    if(backgroundOn) drawBackground(backgroundMapAddress, tileDataAddress);
                    if(windowOn) drawWindow(windowMapAddress, tileDataAddress);
                    if(spriteOn) drawSprite();

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
            int tile = memory.getMemoryPriv(address + (tempX) / 8);

            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tile = tile & 0x7f;
                else tile = (tile & 0x7f) - 0x80;
            }

            int tileLine;
            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);
                else tileLine = TILE_DATA_1 + (((tile & 0xff) - 128) * 0x10) + ((currentLine % 8) * 2);
            } else tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);

            int offset = 7 - (tempX % 8);
            painting[x][currentLine] = (byte) (((memory.getMemoryPriv(tileLine) & (1 << offset)) >> offset) + (((memory.getMemoryPriv(tileLine + 1) & (1 << offset)) >> offset) * 2));
        }
    }

    private void drawWindow(int tileMapAddress, int tileDataAddress) {
        if (windowY < 0 || windowX > 166 || currentLine < windowY) {
            return;
        }

        int tempY = currentLineWindow;
        for(int x = 0; x < 160; x++) {
            int tempX;
            if(x < windowX) continue;
            else tempX = x - windowX;

            int address = tileMapAddress + ((tempY / 8) * 0x20);
            int tile = memory.getMemoryPriv(address + (tempX) / 8);

            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tile = tile & 0x7f;
                else tile = (tile & 0x7f) - 0x80;
            }

            int tileLine;
            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);
                else tileLine = TILE_DATA_1 + (((tile & 0xff) - 128) * 0x10) + ((currentLine % 8) * 2);
            } else tileLine = tileDataAddress + ((tile & 0xff) * 0x10) + ((currentLine % 8) * 2);

            int offset = 7 - (tempX % 8);
            painting[x][currentLine] = (byte) (((memory.getMemoryPriv(tileLine) & (1 << offset)) >> offset) + (((memory.getMemoryPriv(tileLine + 1) & (1 << offset)) >> offset) * 2));
        }

        currentLineWindow++;
    }

    private void drawSprite() {
        int tempY, tempX, spriteLocation, spriteAttributes;
        int drawnSprites = 0;
        for (int spriteNumber = 0; spriteNumber < 40; spriteNumber++) {

            tempY = memory.getMemoryPriv(OAM_START + (spriteNumber * 4)) - 16;
            tempX = memory.getMemoryPriv(OAM_START + (spriteNumber * 4) + 1) - 8;
            spriteLocation = memory.getMemoryPriv(OAM_START + (spriteNumber * 4) + 2);
            spriteAttributes = memory.getMemoryPriv(OAM_START + (spriteNumber * 4) + 3);

            boolean yFlipped = ((spriteAttributes & 0x40) >> 6) == 1;
            boolean xFlipped = ((spriteAttributes & 0x20) >> 5) == 1;

            int spriteSize = this.spriteSize ? 16 : 8;

            if(currentLine >= tempY && currentLine < (tempY + spriteSize) && drawnSprites < 10) {
                int offset;
                drawnSprites++;

                if (yFlipped) offset = 2 * (currentLine - 1);
                else offset = 2 * (currentLine - tempY);

                int pixelData0 = memory.getMemoryPriv((TILE_DATA_0 + spriteLocation * 16) + offset);
                int pixelData1 = memory.getMemoryPriv((TILE_DATA_0 + spriteLocation * 16) + offset + 1);

                for (int x = 0; x < 8; x++) {
                    int col_index = xFlipped ? x : 7 - x;
                    int color_num = ((pixelData0 & (1 << col_index)) >> col_index) + (((pixelData1 & (1 << col_index)) >> col_index) * 2);
                    if ((tempX + x < 160) && (tempX + x >= 0) && color_num != 0) {
                        painting[tempX + x][currentLine] = (byte) color_num;
                    }
                }
            }
        }
    }

    public void requestRepaint() {
        if(!lcdOn) display.drawBlankImage();
        display.repaint();
    }
}
