package de.kolja.signatureserver;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main  extends Application {
    @Override
    public void start(Stage stage) {
        SignatureServer.start();

        GUI gui = new GUI();
        gui.start(stage);
    }

    @Override
    public void stop() {
        SignatureServer.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
