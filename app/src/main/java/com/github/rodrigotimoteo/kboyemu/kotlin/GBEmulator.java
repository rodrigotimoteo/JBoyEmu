package com.github.rodrigotimoteo.kboyemu.kotlin;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class GBEmulator extends Thread {

    private long oldTime;

    private CPU cpu;
    private PPU ppu;
    public static final CountDownLatch latch = new CountDownLatch(1);

//    private long getSleepTime(double oldTime) {
//        long newTime = System.nanoTime();
//        long sleepTime = (long) ((1000000000L / 60) - newTime - oldTime) / 1000000;
//        this.oldTime = newTime;
//        return sleepTime;
//    }

    public GBEmulator() throws IOException, InterruptedException {
        cpu = new CPU();
        ppu = cpu.getPPU();
        latch.await();
    }

    private void gameLoop() throws InterruptedException {
        cpu.cycle();
        ppu.cycle();
        oldTime = System.nanoTime();
        while (true) {
            try {
                int cpuCounter = cpu.getCounter();
//                if(ppu.isGetToSleep()) Thread.sleep(getSleepTime(oldTime));
                if (!ppu.getLcdOn()) {
                    cpu.cycle();
                    ppu.readLCDControl();
                } else {
                    cpu.cycle();
                    for (int i = 0; i < (cpu.getCounter() - cpuCounter); i++) ppu.cycle();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JBoyEmu");
        GBEmulator emulator = new GBEmulator();
        emulator.gameLoop();
    }
}

