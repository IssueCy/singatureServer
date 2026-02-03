package de.kolja.signatureserver;

import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class SignatureServer {

    private static HttpServer server;

    public static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);

            // Statusseite
            server.createContext("/", exchange -> {
                String response = "Signature Server running";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });

            // Signatur HTML
            server.createContext("/sign", exchange -> {
                String html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Unterschrift</title>
                            <style>
                                body { font-family:sans-serif; text-align:center; background:#f0f0f0;}
                                canvas { border:2px solid black; background:white; touch-action:none;}
                                button { margin-top:10px; padding:10px 20px; font-size:16px;}
                            </style>
                        </head>
                        <body>
                            <h2>Bitte unterschreiben</h2>
                            <canvas id="pad" width="300" height="150"></canvas><br>
                            <button onclick="send()">Unterschrift senden</button>

                            <script>
                                const canvas = document.getElementById('pad');
                                const ctx = canvas.getContext('2d');
                                ctx.strokeStyle = "#000";
                                ctx.lineWidth = 3;
                                ctx.lineCap = "round";
                                let drawing=false;

                                function getPos(e){
                                    const rect=canvas.getBoundingClientRect();
                                    if(e.touches){ return {x:e.touches[0].clientX-rect.left, y:e.touches[0].clientY-rect.top};}
                                    else { return {x:e.clientX-rect.left, y:e.clientY-rect.top};}
                                }

                                canvas.addEventListener("touchstart", e=>{drawing=true; const pos=getPos(e); ctx.beginPath(); ctx.moveTo(pos.x,pos.y);});
                                canvas.addEventListener("touchmove", e=>{if(!drawing)return; e.preventDefault(); const pos=getPos(e); ctx.lineTo(pos.x,pos.y); ctx.stroke();});
                                canvas.addEventListener("touchend", ()=>drawing=false);
                                canvas.addEventListener("mousedown", e=>{drawing=true; const pos=getPos(e); ctx.beginPath(); ctx.moveTo(pos.x,pos.y);});
                                canvas.addEventListener("mousemove", e=>{if(!drawing)return; const pos=getPos(e); ctx.lineTo(pos.x,pos.y); ctx.stroke();});
                                canvas.addEventListener("mouseup", ()=>drawing=false);

                                function send(){
                                    fetch('/upload',{method:'POST',body:canvas.toDataURL("image/png")}).then(()=>alert("Unterschrift gesendet"));
                                }
                            </script>
                        </body>
                        </html>
                        """;

                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type","text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200,bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });

            // Upload endpoint
            server.createContext("/upload", exchange -> {
                String data = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Unterschrift empfangen!");
                System.out.println(data.substring(0, Math.min(80, data.length())));

                // Base64 PNG extrahieren
                String base64Image = data.split(",")[1]; // entfernt data:image/png;base64,
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                // 1️⃣ PNG speichern
                String pngFile = "signature.png";
                try (FileOutputStream fos = new FileOutputStream(pngFile)) {
                    fos.write(imageBytes);
                }

                // 2️⃣ PNG im neuen JavaFX-Fenster anzeigen
                Platform.runLater(() -> {
                    Stage stage = new Stage();
                    Image img = new Image("file:" + new File(pngFile).getAbsolutePath());
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(400);
                    iv.setPreserveRatio(true);
                    StackPane root = new StackPane(iv);
                    stage.setTitle("Unterschrift Vorschau");
                    stage.setScene(new Scene(root, 420, 250));
                    stage.show();
                });

                // 3️⃣ PDF Test (Platzhalter PDF)
                try (PDDocument doc = new PDDocument()) {
                    PDPage page = new PDPage();
                    doc.addPage(page);

                    PDImageXObject pdImage = PDImageXObject.createFromFile(pngFile, doc);
                    try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                        content.drawImage(pdImage, 50, 500, 300, 150); // Position + Größe
                    }

                    doc.save("signature_test.pdf");
                    System.out.println("Test PDF erstellt: signature_test.pdf");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                exchange.sendResponseHeaders(200,0);
                exchange.close();
            });

            server.start();
            System.out.println("Server started on port 8080");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Server stopped");
        }
    }
}
