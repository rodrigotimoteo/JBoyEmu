package com.github.rodrigotimoteo.kboyemucore

import com.github.rodrigotimoteo.kboyemucore.cpu.CPU
import com.github.rodrigotimoteo.kboyemucore.ppu.PPU

class KBEmu {
    class GBEmulator : Thread() {

        private var oldTime: Long = 0

        private val cpu: CPU
        private val ppu: PPU
        //    private long getSleepTime(double oldTime) {
        //        long newTime = System.nanoTime();
        //        long sleepTime = (long) ((1000000000L / 60) - newTime - oldTime) / 1000000;
        //        this.oldTime = newTime;
        //        return sleepTime;
        //    }
        init {
            cpu = CPU()
            ppu = cpu.getPPU()
            latch.await()
        }

        @Throws(InterruptedException::class)
        private fun gameLoop() {
            cpu.cycle()
            ppu.cycle()
            oldTime = System.nanoTime()
            while (true) {
                try {
                    val cpuCounter: Int = cpu.getCounter()
                    //                if(ppu.isGetToSleep()) Thread.sleep(getSleepTime(oldTime));
                    if (!ppu.getLcdOn()) {
                        cpu.cycle()
                        ppu.readLCDControl()
                    } else {
                        cpu.cycle()
                        for (i in 0 ..< (cpu.getCounter() - cpuCounter)) ppu.cycle()
                    }
                }catch (e: InterruptedException) {
                    e.printStackTrace()
                    System.exit(-1)
                }
            }
        }

        companion object {
            val latch: java.util.concurrent.CountDownLatch = java.util.concurrent.CountDownLatch(1)

            @Throws(java.io.IOException::class, InterruptedException::class) @JvmStatic fun main(args: Array<String>) {
                System.setProperty("apple.laf.useScreenMenuBar", "true")
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JBoyEmu")
                val emulator: GBEmulator = GBEmulator()
                emulator.gameLoop()
            }
        }}
}