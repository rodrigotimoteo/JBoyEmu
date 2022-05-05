public class GBEmulator extends Thread {

    private CPU cpu;
    private PPU ppu;
    private int cpuCounter;
    private int ppuCounter;
    private int completedCyles;

    public GBEmulator() {
        cpu = new CPU();
        ppu = cpu.getPPU();
    }

    public void run() { //Chip 8 runs at 60FPS
        while(true) {
            try {
                cpuCounter = cpu.getCounter();
                ppuCounter = ppu.getCounter();
                completedCyles = ppu.getCompletedCycles();
                cpu.cycle();
                //if(cpuCounter == (ppuCounter + (completedCyles * 456))) {
                 //   cpu.cycle();
                //    ppu.cycle();
                //} else if (cpuCounter < (ppuCounter + (completedCyles * 456))) {
                //    cpu.cycle();
                //} else {
                //    ppu.cycle();
                //}

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {
        GBEmulator emulator = new GBEmulator();
        emulator.start();
    }
}

