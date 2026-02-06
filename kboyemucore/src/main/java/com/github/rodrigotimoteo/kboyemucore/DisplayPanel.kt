package com.github.rodrigotimoteo.kboyemucore

import com.github.rodrigotimoteo.kboyemucore.memory.Memory
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU

class DisplayPanel {
//
//    private var tempImage: android.media.Image? = null
//
//    private val memory: Memory? = null
//    private val ppu: PPU? = null
//    private val displayFrame: com.github.rodrigotimoteo.kboyemu.kotlin.DisplayFrame? = null
//
//    private const val BACK_WINDOW_PALLETE = 0xff47
//
//    private const val WIDTH = 0
//    private const val HEIGHT = 0
//
//    private var cgb = false
//
//
//    //Constructor
//    fun DisplayPanel(
//        memory: Memory?,
//        ppu: PPU,
//        displayFrame: DisplayFrame
//    ) {
//        this.memory = memory
//        this.ppu = ppu
//        this.displayFrame = displayFrame
//        ppu.setDisplayPanel(this)
//
//        WIDTH = displayFrame.getWidth()
//        HEIGHT = displayFrame.getHeight()
//    }
//
//    fun setCgbMode() {
//        cgb = true
//    }
//
//    fun drawImage(painting: Array<ByteArray?>) {
//        val bufferedImage: BufferedImage = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
//
//        val size: IntArray = displayFrame.getCurrentSize()
//        this.setSize(Dimension(size[0], size[1]))
//        displayFrame.setHeight(size[1] + 28)
//        displayFrame.setWidth(size[0])
//
//        val g: Graphics = bufferedImage.getGraphics()
//
//        for (x in 0..159) {
//            for (y in 0..143) {
//                val c: androidx.compose.ui.graphics.Color? = getColor(painting[x]!![y])
//                g.setColor(c)
//                g.fillRect(x, y, 1, 1)
//            }
//        }
//
//        tempImage = bufferedImage
//    }
//
//    fun drawBlankImage() {
//        val bufferedImage: BufferedImage = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
//
//        val size: IntArray = displayFrame.getCurrentSize()
//        this.setSize(Dimension(size[0], size[1]))
//        displayFrame.setHeight(size[1] + 28)
//        displayFrame.setWidth(size[0])
//
//        val g: Graphics = bufferedImage.getGraphics()
//
//        g.setColor(WHITE)
//        g.fillRect(0, 0, 160, 144)
//
//        tempImage = bufferedImage
//    }
//
//
//    fun paint(g: Graphics) {
//        val size: IntArray = displayFrame.getCurrentSize()
//        if (tempImage != null) {
//            val newImage: android.media.Image? =
//                tempImage.getScaledInstance(size[0], size[1], android.media.Image.SCALE_DEFAULT)
//            g.drawImage(newImage, 0, 0, this)
//        }
//    }
//
//    private fun getColor(pixelNumber: Byte): androidx.compose.ui.graphics.Color? {
//        if (cgb) {
//        } else {
//            return when (pixelNumber) {
//                0 -> Color(0xffffff)
//                1 -> Color(0xcccccc)
//                2 -> Color(0x777777)
//                3 -> Color(0x000000)
//                else -> RED
//            }
//        }
//        return null
//    }
}