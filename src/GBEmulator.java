import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;

public class GBEmulator extends Thread{

    private final CPU cpu;
    private final PPU ppu;
    public static final CountDownLatch latch = new CountDownLatch(1);

    public GBEmulator() throws FileNotFoundException, InterruptedException {
        cpu = new CPU();
        ppu = cpu.getPPU();
        latch.await();
    }

    private void gameLoop() throws InterruptedException {
        cpu.cycle();
        ppu.cycle();
        while (true) {
            try {
                int cpuCounter = cpu.getCounter();
                //Thread.sleep(10);
                if (!ppu.getLcdOn()) {
                    cpu.cycle();
                    ppu.readLCDControl();
                } else {
                    cpu.cycle();
                    for(int i = 0; i < (cpu.getCounter() - cpuCounter); i++) ppu.cycle();
                }
            } catch (InterruptedException e) {
                System.exit(-1);
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JBoyEmu");
        GBEmulator emulator = new GBEmulator();
        emulator.gameLoop();
    }
}

