import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DisplayPanel extends JPanel {

    private Image tempImage;

    private final Memory memory;
    private final PPU ppu;
    private final DisplayFrame displayFrame;

    private final int WIDTH;
    private final int HEIGHT;

    private long oldTime;

    //Constructor

    public DisplayPanel(Memory memory, PPU ppu, DisplayFrame displayFrame) {
        this.memory = memory;
        this.ppu = ppu;
        this.displayFrame = displayFrame;
        ppu.setDisplayPanel(this);

        WIDTH = displayFrame.getWidth();
        HEIGHT = displayFrame.getHeight();
        oldTime = System.nanoTime();
    }

    private double getFPS(double oldTime) {
        long newTime = System.nanoTime();
        long fps = (long) (1 / ((newTime - oldTime) / 1000000000));
        this.oldTime = newTime;
        return fps;
    }

    public void drawImage(byte[][] painting) {
        BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        int[] size = displayFrame.getCurrentSize();
        this.setSize(new Dimension(size[0], size[1]));
        displayFrame.setHeight(size[1] + 28);
        displayFrame.setWidth(size[0]);


        Graphics g = bufferedImage.getGraphics();
        int scrollX = ppu.readScrollX();
        int scrollY = ppu.readScrollY();

        for(int x = 0; x < 160; x++) {
            for(int y = 0; y < 144; y++) {
                Color c = getColor(painting[(x + scrollX) % 160][(y + scrollY) % 144]);
                g.setColor(c);
                g.fillRect(x, y, 1, 1);
            }
        }

        tempImage = bufferedImage;
    }



    public void paint(Graphics g) {
        int[] size = displayFrame.getCurrentSize();
//        System.out.println(size[0] + " " + size[1]);
        if(tempImage != null) {
            Image newImage = tempImage.getScaledInstance(size[0], size[1], Image.SCALE_DEFAULT);
            g.drawImage(newImage, 0, 0, this);
        }


    }

    private Color getColor(byte pixelNumber) {
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
