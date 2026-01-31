package com.github.rodrigotimoteo.kboyemucore

import androidx.compose.ui.input.key.KeyEvent.nativeKeyEvent
import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.memory.Memory
import com.github.rodrigotimoteo.kboyemucore.memory.rom.ROM
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU
import java.io.File

class DisplayFrame {

    private val memory: Memory? = null
    private val cpu: CPU? = null


    //    private final JMenuBar menuBar;
    //    private final JMenu file;
    private var gameLoaded = false

    private const val HI_LO_INTERRUPT = 4

    var WIDTH: Int = 160
    var HEIGHT: Int = 144

    var joypad: Int = 0xff

    fun getWidth(): Int {
        return WIDTH
    }

    fun getHeight(): Int {
        return HEIGHT
    }

    fun setWidth(width: Int) {
        WIDTH = width
    }

    fun setHeight(height: Int) {
        HEIGHT = height
    }

    fun getCurrentSize(): IntArray {
//        Dimension actualSize = getContentPane().getSize();
        val size = IntArray(2)

        //        size[1] = actualSize.height;
//        size[0] = actualSize.width;
        return size
    }

    fun DisplayFrame(memory: Memory, ppu: PPU, cpu: CPU) {
        val displayPanel: DisplayPanel = DisplayPanel(memory, ppu, this)
        this.memory = memory
        this.cpu = cpu
        ppu.setDisplayFrame(this)
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
    fun keyPressed(e: androidx.compose.ui.input.key.KeyEvent) {
        val keyPressed = getKeyValue(e)
        if (keyPressed == -1) return

        joypad = joypad and ((1 shl keyPressed).inv())
        cpu!!.setInterrupt(HI_LO_INTERRUPT)
    }

    fun getJoypad(joypadInfo: Char): Int {
        if ((joypadInfo.code and 0x10) == 0) {
            return (joypad and 0xf0) shr 4
        } else if ((joypadInfo.code and 0x20) == 0) {
            return joypad and 0xf
        }
        return 0
    }

    fun getKeyValue(e: androidx.compose.ui.input.key.KeyEvent): Int {
        val keyCode: Int = e.nativeKeyEvent.getKeyCode()

        return when (keyCode) {
            VK_Z -> 0
            VK_X -> 1
            VK_1 -> 2
            VK_2 -> 3
            VK_L -> 4
            VK_J -> 5
            VK_I -> 6
            VK_K -> 7
            else -> -1
        }
    }

    public override fun keyReleased(e: androidx.compose.ui.input.key.KeyEvent) {
        val keyPressed = getKeyValue(e)
        if (keyPressed == -1) return

        joypad = joypad or (1 shl keyPressed)
    }

    fun chooseFile(): File {
        val fileChooser: FileDialog = FileDialog(this, "Choose a main.kotlin.ROM")
        fileChooser.setVisible(true)
        memory!!.setGameFileName(fileChooser.getFile())
        return File(fileChooser.getDirectory() + fileChooser.getFile())
    }

    public override fun actionPerformed(e: ActionEvent) {
        if ("open" == e.getActionCommand()) {
            if (gameLoaded) {
                //CPUInstructions.reset();
            }
            ROM.loadProgram(chooseFile(), memory)
            gameLoaded = true
            Emulator.latch.countDown()
        }
    }

    fun saveState(state: Int) {
    }

    fun loadState(state: Int) {
//        cpu.loadState();
    }
}