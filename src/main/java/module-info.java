module de.kolja.signatureserver {
    requires javafx.controls;
    requires javafx.fxml;


    opens de.kolja.signatureserver to javafx.fxml;
    exports de.kolja.signatureserver;
}