import javax.swing.*;
import java.awt.*;

public class DisplayFrame extends JFrame {

    private CPU cpu;
    private Memory memory;
    private PPU ppu;

    private DisplayPanel displayPanel;

    private static final int WIDTH = 160;
    private static final int HEIGHT = 144;
    private static final int SCALE = 4;

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }

    public int getScale() {
        return SCALE;
    }

    public DisplayPanel getDisplayPanel() {
        return displayPanel;
    }

    public DisplayFrame(CPU cpu, Memory memory, PPU ppu) {
        this.cpu = cpu;
        this.memory = memory;
        this.ppu = ppu;

        displayPanel = new DisplayPanel(cpu, memory, ppu, this);
        ppu.setDisplayFrame(this);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        pack();
        setLayout(new BorderLayout());
        add(displayPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle(ROM.getRomTitle());
        pack();
        setVisible(true);
    }

}