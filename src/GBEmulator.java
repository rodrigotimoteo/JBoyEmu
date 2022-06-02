import java.io.FileNotFoundException;

public class GBEmulator extends Thread{

    private final CPU cpu;
    private final PPU ppu;

    public GBEmulator() throws FileNotFoundException {
        cpu = new CPU();
        ppu = cpu.getPPU();

    }

    private void gameLoop() {
        ppu.setCounter(29);
        while (true) {
            try {
                //Thread.sleep(0, 90);
                int cpuCounter = cpu.getCounter();
                if(ppu.isGetToSleep()) Thread.sleep(1);
                if (!ppu.getLcdOn()) {
                    cpu.cycle();
                    ppu.readLCDCStatus();
                } else {
                    cpu.cycle();
                    for(int i = 0; i < (cpu.getCounter() - cpuCounter); i++) ppu.cycle();
                }
            } catch (InterruptedException e) {
                System.exit(-1);
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        GBEmulator emulator = new GBEmulator();
        emulator.gameLoop();
    }

}

