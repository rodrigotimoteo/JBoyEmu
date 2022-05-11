import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TimelineBuilder;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GBEmulator extends Application {

    private CPU cpu;
    private Memory memory;
    private PPU ppu;
    private Display display;

    private static Stage mainStage;

    private int cpuCounter;
    private int ppuCounter;
    private int completedCyles;

    private static boolean frameCompleted;

    public static void setTitle(String title) {
        mainStage.setTitle(title);
    }

    public static void setFrameCompleted(boolean state) {
        frameCompleted = state;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;
        beginEmulation();
    }

    public void beginEmulation() {
        cpu = new CPU();
        memory = cpu.getMemory();
        ppu = cpu.getPPU();
        display = cpu.getDisplay();
        ppu.setDisplay(display);

        //MenuBar menuBar = new MenuBar();
        //Menu menuFile = new Menu("File");
        //MenuItem loadRomItem = new MenuItem("Load ROM");
        //menuBar.getMenus().add(menuFile);



        VBox root = new VBox();
        //root.getChildren().add(menuBar);
        root.getChildren().add(display);

        Scene mainScene = new Scene(root);



        final Duration oneFrameDuration = Duration.millis(25);
        final KeyFrame oneFrame = new KeyFrame(oneFrameDuration, event -> {
            gameLoop();
            drawFrame();
        });

        final Timeline timeLine = TimelineBuilder.create().cycleCount(Animation.INDEFINITE).keyFrames(oneFrame).build();

        mainStage.setScene(mainScene);
        mainStage.setMaxWidth(Display.getWIDTH());
        mainStage.setMaxHeight(Display.getHEIGHT());
        mainStage.setMinWidth(Display.getWIDTH());
        mainStage.setMinHeight(Display.getHEIGHT());
        mainStage.setResizable(false);

        timeLine.play();

        mainStage.show();
    }

    private void drawFrame() {
        if(ppu.getDraw()) { display.renderDisplayTile((memory.getMemory(0xff44) & 0xff) - 1); ppu.setDraw(false); }
    }


    private void gameLoop() {
        while (!ppu.getDraw()) {
            try {
                cpuCounter = cpu.getCounter();
                completedCyles = ppu.getCompletedCycles();
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
        launch(args);
    }

}

