package de.kolja.signatureserver;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class GUI {

    public void start(Stage stage) {

        Label statusLabel = new Label("Signature Server läuft.\nWarte auf Unterschrift...");
        statusLabel.setStyle("-fx-font-size: 16px;");

        BorderPane root = new BorderPane();
        root.setCenter(statusLabel);

        Scene scene = new Scene(root, 400, 200);

        stage.setTitle("Signature Server");
        stage.setScene(scene);
        stage.show();
    }
}