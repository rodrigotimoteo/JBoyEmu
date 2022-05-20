import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DisplayPanel extends JPanel {

    private byte[] rowTiles = new byte[0x20];
    private byte[][] tiles = new byte[0x100][64];

    private Image tempImage;

    private final CPU cpu;
    private final Memory memory;
    private final PPU ppu;
    private final DisplayFrame displayFrame;

    private final int WIDTH;
    private final int HEIGHT;
    private final int SCALE;

    //Seters

    public void setRowTiles(int index, int tile) {
        rowTiles[index] = (byte) tile;
    }

    public void setTiles(int tile, int index, int value) {
        tiles[tile][index] = (byte) value;
    }

    //Debug

    private void dumpRowTiles() {
        System.out.print("0 ");
        for (int i = 0; i < 0x20; i++) {
            if (i % 16 == 0 && i != 0) {
                System.out.println(" ");
                System.out.print(Integer.toHexString(i) + " ");
                System.out.print(Integer.toHexString(rowTiles[i] & 0xff) + " ");
            } else System.out.print(Integer.toHexString(rowTiles[i] & 0xff) + " ");
        }
    }

    //Constructor

    public DisplayPanel(CPU cpu, Memory memory, PPU ppu, DisplayFrame displayFrame) {
        this.cpu = cpu;
        this.memory = memory;
        this.ppu = ppu;
        this.displayFrame = displayFrame;
        ppu.setDisplayPanel(this);

        WIDTH = displayFrame.getWidth();
        HEIGHT = displayFrame.getHeight();
        SCALE = displayFrame.getScale();
    }

    public void drawImage() {
        int currentLine = ((ppu.readScrollY() + ppu.readLY()) % 256);
        if(currentLine == ppu.readScrollY()) tempImage = null;

        Image temp = createImage(tempImage);
        tempImage = temp;
    }

    public void paint(Graphics g) {
        Image tempImg = createImage(tempImage);
        g.drawImage(tempImg, 0, 0, this);
    }

    private Image createImage(Image img) {
        int tileNumber;

        BufferedImage bufferedImage = new BufferedImage(WIDTH * SCALE, HEIGHT * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics g = bufferedImage.getGraphics();

        if(img != null) g.drawImage(img, 0, 0, this);

        int currentLine = ppu.readLY();
        int scrollX = (ppu.readScrollX() % 32);

        for(int i = 0; i < 20; i++) {
            tileNumber = rowTiles[(scrollX + i) % 32];
            int startX = ((i + scrollX) * 8) % 32;
            for(int x = 0; x < 8; x++) {
                Color c = getColor(tiles[tileNumber][x + (((currentLine + ppu.readScrollY()) % 8) * 8)]);
                g.setColor(c);
                g.fillRect(x + (i * 8), ppu.readLY(), 1, 1);
            }
        }

        tempImage = bufferedImage;
        return bufferedImage;
    }

    private Color getColor(int pixelNumber) {
        int palette = (memory.getMemory(0xff47) & 0xff);
        int colorSelect;
        Color color;

        colorSelect = switch (pixelNumber) {
            case 0 -> (palette & 0x3);
            case 1 -> (palette >> 2) & 0x3;
            case 2 -> (palette >> 4) & 0x3;
            case 3 -> (palette >> 6) & 0x3;
            default -> 0;
        };
        color = switch (colorSelect & 0x3) {
            case 1 -> new Color(0xcccccc);
            case 2 -> new Color(0x777777);
            case 3 -> new Color(0x000000);
            default -> new Color(0xffffff);
        };
        return color;
    }
}
