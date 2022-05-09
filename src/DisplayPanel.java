import javax.swing.*;
import java.awt.*;

public class DisplayPanel extends JPanel {

    private final int WIDTH = 160;
    private final int HEIGHT = 144;
    private final int SCALE = 4;

    private final Memory memory;
    private int currentX;
    private int currentY;

    public int getWIDTH() {
        return WIDTH;
    }

    public int getHEIGHT() {
        return HEIGHT;
    }

    public int getSCALE() {
        return SCALE;
    }

    //Seters

    public void setCurrentX(int currentX) {
        this.currentX = currentX;
    }

    public void setCurrentY(int currentY) {
        this.currentY = currentY;
    }

    //Constructor

    public DisplayPanel(Memory memory) {
        this.memory = memory;
    }

    /*
    0  White
    1  Light gray
    2  Dark gray
    3  Black
     */

    public void paint(Graphics g) {
        setCurrentY(PPU.getCurrentY());
        setCurrentX(PPU.getCurrentX());

        if(currentY % 2 == 1) g.setColor(Color.WHITE);
        else g.setColor(Color.BLACK);
        g.fillRect(currentX * SCALE, currentY * SCALE, SCALE, SCALE);
    }
}

