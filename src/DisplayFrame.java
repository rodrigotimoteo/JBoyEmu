import javax.swing.*;
import java.awt.*;

public class DisplayFrame extends JFrame {

    private static int WIDTH = 160;
    private static int HEIGHT = 144;

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }

    public void setWidth(int width) {
        WIDTH = width;
    }

    public void setHeight(int height) {
        HEIGHT = height;
    }

    public int[] getCurrentSize() {
        Dimension actualSize = getContentPane().getSize();
        int[] size = new int[2];
        size[1] = actualSize.height;
        size[0] = actualSize.width;

        return size;
    }

    public DisplayFrame(Memory memory, PPU ppu) {

        DisplayPanel displayPanel = new DisplayPanel(memory, ppu, this);
        ppu.setDisplayFrame(this);

        setLocationRelativeTo(null);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setLayout(new BorderLayout());
        pack();
        add(displayPanel);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(ROM.getRomTitle());
        pack();
        setVisible(true);

    }

}