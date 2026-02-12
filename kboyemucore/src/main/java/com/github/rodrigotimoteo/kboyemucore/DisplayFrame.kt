package com.github.rodrigotimoteo.kboyemucore

import com.github.rodrigotimoteo.kboyemucore.memory.Memory
import com.github.rodrigotimoteo.kboyemucore.memory.rom.ROM
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU
import java.io.File

class DisplayFrame {
//
//    private val memory: Memory? = null
//    private val cpu: OldCPU? = null
//
//
//    //    private final JMenuBar menuBar;
//    //    private final JMenu file;
//    private var gameLoaded = false
//
//    private const val HI_LO_INTERRUPT = 4
//
//    var WIDTH: Int = 160
//    var HEIGHT: Int = 144
//
//    var joypad: Int = 0xff
//
//    fun getWidth(): Int {
//        return WIDTH
//    }
//
//    fun getHeight(): Int {
//        return HEIGHT
//    }
//
//    fun setWidth(width: Int) {
//        WIDTH = width
//    }
//
//    fun setHeight(height: Int) {
//        HEIGHT = height
//    }
//
//    fun getCurrentSize(): IntArray {
////        Dimension actualSize = getContentPane().getSize();
//        val size = IntArray(2)
//
//        //        size[1] = actualSize.height;
////        size[0] = actualSize.width;
//        return size
//    }
//
//    fun DisplayFrame(memory: Memory, ppu: PPU, cpu: OldCPU) {
//        val displayPanel: DisplayPanel = DisplayPanel(memory, ppu, this)
//        this.memory = memory
//        this.cpu = cpu
//        ppu.setDisplayFrame(this)
////
////        menuBar = new JMenuBar();
////        file = new JMenu("File");
////        menuBar.add(file);
////
////        JMenuItem open = new JMenuItem("Open", KeyEvent.VK_O + KeyEvent.VK_META);
////        file.add(open);
////        open.setActionCommand("open");
////        open.addActionListener(this);
////
////        setLocationRelativeTo(null);
////
////        setPreferredSize(new Dimension(WIDTH, HEIGHT));
////        setLayout(new BorderLayout());
////        pack();
////        add(displayPanel);
////        setResizable(true);
////        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
////        setTitle(ROM.getRomTitle());
////        setJMenuBar(menuBar);
////        pack();
////        setVisible(true);
////
////        addKeyListener(this);
//    }
//
//
//    //    @Override
//    //    public void keyTyped(KeyEvent e) {
//    //
//    //    }
//    //    @Override

//
//    fun chooseFile(): File {
//        val fileChooser: FileDialog = FileDialog(this, "Choose a main.kotlin.ROM")
//        fileChooser.setVisible(true)
//        memory!!.setGameFileName(fileChooser.getFile())
//        return File(fileChooser.getDirectory() + fileChooser.getFile())
//    }
//
//    public override fun actionPerformed(e: ActionEvent) {
//        if ("open" == e.getActionCommand()) {
//            if (gameLoaded) {
//                //CPUInstructions.reset();
//            }
//            ROM.loadProgram(chooseFile(), memory)
//            gameLoaded = true
//            Emulator.latch.countDown()
//        }
//    }
//
//    fun saveState(state: Int) {
//    }
//
//    fun loadState(state: Int) {
////        cpu.loadState();
//    }
}
