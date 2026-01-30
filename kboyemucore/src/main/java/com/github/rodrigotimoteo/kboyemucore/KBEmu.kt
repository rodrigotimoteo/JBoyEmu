package com.github.rodrigotimoteo.kboyemucore

class KBEmu {
    class GBEmulator : java.lang.Thread() {

        private var oldTime: kotlin.Long = 0

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
            GBEmulator.Companion.latch.await()
        }

        @kotlin.Throws(java.lang.InterruptedException::class) private fun gameLoop() {
            cpu.cycle()
            ppu.cycle()
            oldTime = java.lang.System.nanoTime()
            while (true) {
                try {
                    val cpuCounter: kotlin.Int = cpu.getCounter()
                    //                if(ppu.isGetToSleep()) Thread.sleep(getSleepTime(oldTime));
                    if (!ppu.getLcdOn()) {
                        cpu.cycle()
                        ppu.readLCDControl()
                    } else {
                        cpu.cycle()
                        for (i in 0 ..< (cpu.getCounter() - cpuCounter)) ppu.cycle()
                    }
                }catch (e: java.lang.InterruptedException) {
                    e.printStackTrace()
                    java.lang.System.exit(-1)
                }
            }
        }

        companion object {
            val latch: java.util.concurrent.CountDownLatch = java.util.concurrent.CountDownLatch(1)

            @kotlin.Throws(java.io.IOException::class, java.lang.InterruptedException::class) @kotlin.jvm.JvmStatic fun main(args: kotlin.Array<kotlin.String>) {
                java.lang.System.setProperty("apple.laf.useScreenMenuBar", "true")
                java.lang.System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JBoyEmu")
                val emulator: GBEmulator = GBEmulator()
                emulator.gameLoop()
            }
        }}


}