package com.github.rodrigotimoteo.kboyemu.kotlin

import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

class Emulator {

    private var oldTime: Long = 0

    private val cpu: CPU = CPU()
    private val ppu: PPU = cpu.ppu

    init {
        latch.await()
        gameLoop()
    }

    //    private long getSleepTime(double oldTime) {
    //        long newTime = System.nanoTime();
    //        long sleepTime = (long) ((1000000000L / 60) - newTime - oldTime) / 1000000;
    //        this.oldTime = newTime;
    //        return sleepTime;
    //    }

    @Throws(InterruptedException::class)
    private fun gameLoop() {
        cpu.cycle()
        ppu.cycle()
        oldTime = System.nanoTime()
        while (true) {
            try {
                val cpuCounter = cpu.getCounter()
                //                if(ppu.isGetToSleep()) Thread.sleep(getSleepTime(oldTime));
                if (!ppu.getLcdOn()) {
                    cpu.cycle()
                    ppu.readLCDControl()
                } else {
                    cpu.cycle()
                    for (i in 0..<(cpu.getCounter() - cpuCounter)) ppu.cycle()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                exitProcess(-1)
            }
        }
    }

    companion object {
        @JvmField
        var latch: CountDownLatch = CountDownLatch(1)
    }
}

fun main() {
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JBoyEmu")
    Emulator()
}
