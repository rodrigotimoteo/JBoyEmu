import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DisplayPanel extends JPanel {

    private Image tempImage;

    private final Memory memory;
    private final PPU ppu;
    private final DisplayFrame displayFrame;

    private final int BACK_WINDOW_PALLETE = 0xff47;

    private final int WIDTH;
    private final int HEIGHT;

    //Constructor

    public DisplayPanel(Memory memory, PPU ppu, DisplayFrame displayFrame) {
        this.memory = memory;
        this.ppu = ppu;
        this.displayFrame = displayFrame;
        ppu.setDisplayPanel(this);

        WIDTH = displayFrame.getWidth();
        HEIGHT = displayFrame.getHeight();
    }

    public void drawImage(byte[][] painting) {
        BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        int[] size = displayFrame.getCurrentSize();
        this.setSize(new Dimension(size[0], size[1]));
        displayFrame.setHeight(size[1] + 28);
        displayFrame.setWidth(size[0]);

        Graphics g = bufferedImage.getGraphics();

        for(int x = 0; x < 160; x++) {
            for(int y = 0; y < 144; y++) {
                Color c = getColor(painting[x][y]);
                g.setColor(c);
                g.fillRect(x, y, 1, 1);
            }
        }

        tempImage = bufferedImage;
    }

    public void drawBlankImage() {
        BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        int[] size = displayFrame.getCurrentSize();
        this.setSize(new Dimension(size[0], size[1]));
        displayFrame.setHeight(size[1] + 28);
        displayFrame.setWidth(size[0]);

        Graphics g = bufferedImage.getGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 160, 144);

        tempImage = bufferedImage;
    }


    public void paint(Graphics g) {
        int[] size = displayFrame.getCurrentSize();
        if(tempImage != null) {
            Image newImage = tempImage.getScaledInstance(size[0], size[1], Image.SCALE_DEFAULT);
            g.drawImage(newImage, 0, 0, this);
        }
    }

    private Color getColor(byte pixelNumber) {
        return switch (pixelNumber) {
            case 0 -> new Color(0xffffff);
            case 1 -> new Color(0xcccccc);
            case 2 -> new Color(0x777777);
            case 3 -> new Color(0x000000);
            default -> Color.RED;
        };
    }
}
