public class GBEmulator extends Thread {

    private final CPU cpu;
    private final PPU ppu;
    private int cpuCounter;
    private int ppuCounter;
    private int completedCyles;

    public GBEmulator() {
        cpu = new CPU();
        ppu = cpu.getPPU();
    }

    public void run() {
        while(true) {
            try {
                //Thread.sleep(0, 952);
                cpuCounter = cpu.getCounter();
                ppuCounter = ppu.getCounter();
                completedCyles = ppu.getCompletedCycles();
                cpu.cycle();
                if(cpuCounter == (ppuCounter + (completedCyles * 456))) {
                    cpu.cycle();
                    ppu.cycle();
                } else if (cpuCounter < (ppuCounter + (completedCyles * 456))) {
                    cpu.cycle();
                } else {
                    ppu.cycle();
                }

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

