package com.github.rodrigotimoteo.kboyemu.kotlin;

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
    private final int BG_PALETTE = 0xff47;
    private final int OBJECT_PALETTE_0 = 0xff48;
    private final int OBJECT_PALETTE_1 = 0xff49;
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
    private boolean cgb = false;


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

    public void setCgbMode() {
        cgb = true;
        display.setCgbMode();
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

        //Read LCD and main.kotlin.PPU enabled bit
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
        int tempY = (currentLine + scrollY) & 0xff;
        //System.out.println(currentLine + "  " + tempY);

        for (int x = 0; x < 160; x++) {
            int tempX = (scrollX + x) % 0x100;

            int address = tileMapAddress + ((tempY / 8) * 0x20);
            int tile = memory.getMemoryPriv(address + (tempX) / 8);

            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tile = tile & 0x7f;
                else tile = (tile & 0x7f) - 0x80;
            }

            int tileLine;
            int i = tileDataAddress + ((tile & 0xff) * 0x10) + ((tempY % 8) * 2);
            if(negativeTiles) {
                if(((tile & 0x80) >> 7) == 0) tileLine = i;
                else tileLine = TILE_DATA_1 + (((tile & 0xff) - 128) * 0x10) + ((tempY % 8) * 2);
            } else tileLine = i;

            int offset = 7 - (tempX % 8);
            int color_num = (byte) (((memory.getMemoryPriv(tileLine) & (1 << offset)) >> offset) + (((memory.getMemoryPriv(tileLine + 1) & (1 << offset)) >> offset) * 2));
            byte color = decodeColor(color_num, memory.getMemoryPriv(BG_PALETTE));
            painting[x][currentLine] = color;
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
            int color_num = (byte) (((memory.getMemoryPriv(tileLine) & (1 << offset)) >> offset) + (((memory.getMemoryPriv(tileLine + 1) & (1 << offset)) >> offset) * 2));
            byte color = decodeColor(color_num, memory.getMemoryPriv(BG_PALETTE));
            painting[x][currentLine] = color;
        }

        currentLineWindow++;
    }

    private void drawSprite() {
        int tempY, tempX, tile, attributesAddress;
        int[] drawnX = new int[10];

        int drawnSprites = 0;
        int spriteOffset = this.spriteSize ? 16 : 8;

        for (int spriteNumber = 0; spriteNumber < 40 && drawnSprites < 10; spriteNumber++) {
            tempY = memory.getMemoryPriv(OAM_START + (spriteNumber * 4)) - 16;
            tempX = memory.getMemoryPriv(OAM_START + (spriteNumber * 4) + 1) - 8;
            tile = memory.getMemoryPriv(OAM_START + (spriteNumber * 4) + 2);

            boolean quit = false;
            if(drawnSprites > 1) for (int x : drawnX) if (x == tempX) quit = true;
            if(quit) continue;

            if(spriteOffset == 16) tile &= 0xfe;

            attributesAddress = OAM_START + (spriteNumber * 4) + 3;

            if((currentLine >= tempY) && (currentLine < (tempY + spriteOffset))) {
                boolean priority = memory.testBit(attributesAddress,7);
                boolean yFlipped = memory.testBit(attributesAddress,6);
                boolean xFlipped = memory.testBit(attributesAddress,5);
                int paletteAddress = memory.testBit(attributesAddress, 4) ? OBJECT_PALETTE_1 : OBJECT_PALETTE_0;
                int palette = memory.getMemoryPriv(paletteAddress);

                int tileLine = spriteOffset - (currentLine - tempY);

                int offset;
                if (!yFlipped) offset = 2 * (currentLine - tempY);
                else offset = 2 * (tileLine - 1);

                int pixelDataAddress = TILE_DATA_0 + tile * 16 + offset;

                for (int pixelPrinted = 0; pixelPrinted < 8; pixelPrinted++) {
                    if(tempX + pixelPrinted < 0 || tempX + pixelPrinted >= 160) continue;
                    if(priority && painting[tempX + pixelPrinted][currentLine] > 0) continue;

                    int x = xFlipped ? pixelPrinted : 7 - pixelPrinted;
                    int color_num = ((memory.getMemoryPriv(pixelDataAddress) & (1 << x)) >> x) + (((memory.getMemoryPriv(pixelDataAddress + 1) & (1 << x)) >> x) * 2);
                    byte color = decodeColor(color_num, palette);

                    if ((tempX + pixelPrinted < 160) && (tempX + pixelPrinted >= 0) && (color_num != 0) ) {
                        painting[tempX + pixelPrinted][currentLine] = color;
                    }
                }

                drawnX[drawnSprites] = tempX;
                drawnSprites++;
            }
        }
    }

    private byte decodeColor(int index, int palette) {
        byte[] colors = new byte[4];
        colors[3] = (byte) (((palette & 0x80) >> 6) + ((palette & 0x40) >> 6));
        colors[2] = (byte) (((palette & 0x20) >> 4) + ((palette & 0x10) >> 4));
        colors[1] = (byte) (((palette & 0x08) >> 2) + ((palette & 0x04) >> 2));
        colors[0] = (byte) ((palette & 0x02) + (palette & 0x01));

        return colors[index];
    }


    public void requestRepaint() {
        if(!lcdOn) display.drawBlankImage();
        display.repaint();
    }
}
