public class GBEmulator extends Thread {

    private CPU cpu;

    public GBEmulator() {
        cpu = new CPU();
    }

    public void run() { //Chip 8 runs at 60FPS
        while(true) {
            try {
                cpu.cycle();
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

