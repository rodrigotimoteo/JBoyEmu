import javax.swing.*;
import java.awt.*;

public class DisplayFrame extends JFrame {

    public DisplayFrame(Memory memory) {
        DisplayPanel displayPanel = new DisplayPanel(memory);

        int WIDTH = displayPanel.getWIDTH();
        int HEIGHT = displayPanel.getHEIGHT();
        int SCALE = displayPanel.getSCALE();


        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        pack();
        setLayout(new BorderLayout());
        add(displayPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle("Gameboy Emulator");
        pack();
        setVisible(true);
    }

}
