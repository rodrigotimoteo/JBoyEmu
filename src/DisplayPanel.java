import javax.swing.*;
import java.awt.*;

public class DisplayPanel extends JPanel {

    private final int WIDTH = 160;
    private final int HEIGHT = 144;
    private final int SCALE = 4;

    private final Memory memory;
    private int SCROLLX;
    private int SCROLLY;

    public int getWIDTH() {
        return WIDTH;
    }

    public int getHEIGHT() {
        return HEIGHT;
    }

    public int getSCALE() {
        return SCALE;
    }

    public DisplayPanel(Memory memory) {
        this.memory = memory;
    }

    public void paint(Graphics g) { //Draws to the screen based on the contents of the Graphics array in memory
        //boolean[][] display = memory.getGraphics(); //Gets the content from the graphics array in memory
        for(int x = 0; x < WIDTH; x++) {
            for(int y = 0; y < HEIGHT; y++) {
                if(1 == 1)
                    g.setColor(Color.WHITE);
                else
                    g.setColor(Color.BLACK);
                g.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
            }
        }
    }
}

