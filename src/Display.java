import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Display extends Canvas{

    private static final int WIDTH = 160;
    private static final int HEIGHT = 144;

    private static final int SCALE = 4;

    private byte[] rowTiles = new byte[0x20];
    private byte[][] tiles = new byte[0x100][64];

    private GraphicsContext gc;
    private final CPU cpu;
    private final Memory memory;
    private final PPU ppu;

    //Geters

    public static int getWIDTH() {
        return WIDTH * SCALE;
    }

    public static int getHEIGHT() {
        return HEIGHT * SCALE;
    }

    //Seters

    public void setRowTiles(int index, int tile) {
        rowTiles[index] = (byte) tile;
    }

    public void setTiles(int tile, int index, int value) {
        tiles[tile][index] = (byte) value;
    }

    public Display(CPU cpu, Memory memory, PPU ppu) {
        super(WIDTH * SCALE, HEIGHT * SCALE);
        setFocusTraversable(true);

        gc = this.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH * SCALE, HEIGHT * SCALE);

        this.cpu = cpu;
        this.memory = memory;
        this.ppu = ppu;
    }

    public void renderDisplayTile(int currentLine) {
        int scrollX = (ppu.readScrollX() % 32);

        for(int x = 0; x < 20; x++) {
            drawPixel(rowTiles[x + scrollX], currentLine, ((x + scrollX) * 8));
        }

    }

    private void drawPixel(int tileNumber, int currentLine, int startX) {
        for(int y = 0; y < 8; y++) {
            for(int x = 0; x < 8; x++) {
                Color c = getColor(tiles[tileNumber][x + y * 8]);
                gc.setFill(c);
                gc.fillRect(((startX + x) * SCALE), (((currentLine * 8) + y) * SCALE), SCALE, SCALE);
            }
        }
    }

    private Color getColor(int pixelNumber) {
        int palette = (memory.getMemory(0xff47) & 0xff);
        int colorSelect;
        Color color;

        switch(pixelNumber) {
            case 0: colorSelect = (palette & 0x3);
                break;
            case 1: colorSelect = (palette >> 2) & 0x3;
                break;
            case 2: colorSelect = (palette >> 4) & 0x3;
                break;
            case 3: colorSelect = (palette >> 6) & 0x3;
                break;
            default:
                colorSelect = 0;
                break;
        }
        switch (colorSelect & 0x3) {
            case 1: color =  Color.web("0xcccccc");
                break;
            case 2: color =  Color.web("0x777777");
                break;
            case 3: color =  Color.web("0x000000");
                break;
            default: color =  Color.web("0xffffff");
        }
        return color;
    }
}
