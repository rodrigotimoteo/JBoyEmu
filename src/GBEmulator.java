public class GBEmulator extends Thread{

    private final CPU cpu;
    private final PPU ppu;
    private final DisplayFrame displayFrame;
    private final DisplayPanel displayPanel;

    private int cpuCounter;
    private int ppuCounter;

    public GBEmulator() {
        cpu = new CPU();
        ppu = cpu.getPPU();
        displayFrame = cpu.getDisplayFrame();
        displayPanel = cpu.getDisplayPanel();
    }

    private void gameLoop() {
        while (true) {
            try {
                //Thread.sleep(0, 90);
                cpuCounter = cpu.getCounter();
                if(ppu.isGetToSleep()) Thread.sleep(10);
                if (!ppu.getLcdOn()) {
                    cpu.cycle();
                    ppu.readLCDCStatus();
                } else {
                    cpu.cycle();
                    for(int i = 0; i < ((cpu.getCounter() - cpuCounter) / 2); i++) ppu.cycle();
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

