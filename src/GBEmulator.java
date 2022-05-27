public class GBEmulator extends Thread{

    private final CPU cpu;
    private final PPU ppu;

    public GBEmulator() {
        cpu = new CPU();
        ppu = cpu.getPPU();

    }

    private void gameLoop() {
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
                    for(int i = 0; i < ((cpu.getCounter() - cpuCounter) * 2); i++) ppu.cycle();
                }
            } catch (InterruptedException e) {
                System.exit(-1);
            }
        }
    }

    public static void main(String[] args) {
        GBEmulator emulator = new GBEmulator();
        emulator.gameLoop();
    }

}

