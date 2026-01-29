package com.github.rodrigotimoteo.kboyemu.kotlin;

import androidx.compose.ui.input.key.KeyEvent;

import java.io.File;

public class DisplayFrame {

    private final Memory memory;
    private final CPU cpu;

//    private final JMenuBar menuBar;
//    private final JMenu file;

    private boolean gameLoaded = false;

    private final int HI_LO_INTERRUPT = 4;

    private static int WIDTH = 160;
    private static int HEIGHT = 144;

    private static int joypad = 0xff;

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
//        Dimension actualSize = getContentPane().getSize();
        int[] size = new int[2];
//        size[1] = actualSize.height;
//        size[0] = actualSize.width;

        return size;
    }

    public DisplayFrame(Memory memory, PPU ppu, CPU cpu) {

        DisplayPanel displayPanel = new DisplayPanel(memory, ppu, this);
        this.memory = memory;
        this.cpu = cpu;
        ppu.setDisplayFrame(this);
//
//        menuBar = new JMenuBar();
//        file = new JMenu("File");
//        menuBar.add(file);
//
//        JMenuItem open = new JMenuItem("Open", KeyEvent.VK_O + KeyEvent.VK_META);
//        file.add(open);
//        open.setActionCommand("open");
//        open.addActionListener(this);
//
//        setLocationRelativeTo(null);
//
//        setPreferredSize(new Dimension(WIDTH, HEIGHT));
//        setLayout(new BorderLayout());
//        pack();
//        add(displayPanel);
//        setResizable(true);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setTitle(ROM.getRomTitle());
//        setJMenuBar(menuBar);
//        pack();
//        setVisible(true);
//
//        addKeyListener(this);
    }

//    @Override
//    public void keyTyped(KeyEvent e) {
//
//    }

//    @Override
    public void keyPressed(KeyEvent e) {
        int keyPressed = getKeyValue(e);
        if(keyPressed == -1) return;

        joypad = joypad & (~(1 << keyPressed));
        cpu.setInterrupt(HI_LO_INTERRUPT);
    }

    public int getJoypad(char joypadInfo) {
        if ((joypadInfo & 0x10) == 0) {
            return (joypad & 0xf0) >> 4;
        } else if ((joypadInfo & 0x20) == 0) {
            return joypad & 0xf;
        }
        return 0;
    }

    public int getKeyValue(KeyEvent e) {
        int keyCode = e.getNativeKeyEvent().getKeyCode();

        return switch (keyCode) {
            case KeyEvent.VK_Z -> 0;  //A
            case KeyEvent.VK_X -> 1;  //B
            case KeyEvent.VK_1 -> 2;  //SELECT
            case KeyEvent.VK_2 -> 3;  //START
            case KeyEvent.VK_L -> 4;  //RIGHT
            case KeyEvent.VK_J -> 5;   //LEFT
            case KeyEvent.VK_I -> 6;     //UP
            case KeyEvent.VK_K -> 7;   //DOWN
            default -> -1;
        };
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyPressed = getKeyValue(e);
        if(keyPressed == -1) return;

        joypad = joypad | (1 << keyPressed);
    }

    public File chooseFile() {
        FileDialog fileChooser = new FileDialog(this, "Choose a main.kotlin.ROM");
        fileChooser.setVisible(true);
        memory.setGameFileName(fileChooser.getFile());
        return new File(fileChooser.getDirectory() + fileChooser.getFile());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if("open".equals(e.getActionCommand())) {
            if(gameLoaded) {
                //CPUInstructions.reset();
            }
            ROM.loadProgram(chooseFile(), memory);
            gameLoaded = true;
            Emulator.latch.countDown();
        }
    }

    public void saveState(int state) {

    }

    public void loadState(int state) {
//        cpu.loadState();
    }
}