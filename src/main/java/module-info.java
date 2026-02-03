module de.kolja.signatureserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires jdk.httpserver;
    requires org.apache.pdfbox;


    opens de.kolja.signatureserver to javafx.fxml;
    exports de.kolja.signatureserver;
}