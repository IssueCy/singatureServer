package de.kolja.signatureserver;

import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.time.LocalDateTime;

public class SignatureServer {

    private static HttpServer server;

    public static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/", exchange -> {
                String response = "Signature Server running";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });

            server.createContext("/sign", exchange -> {
                String html = """
                        <!DOCTYPE html>
                        <html lang="de">
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                            <title>Unterschrift</title>
                            <style>
                                html, body {
                                    margin: 0;
                                    padding: 0;
                                    width: 100%;
                                    height: 100%;
                                    background: #f0f0f0;
                                    font-family: sans-serif;
                                }
                        
                                body {
                                    display: flex;
                                    flex-direction: column;
                                    align-items: center;
                                    justify-content: center;
                                }
                        
                                h2 {
                                    margin: 10px 0;
                                }
                        
                                #wrapper {
                                    width: 95vw;
                                    height: 60vh;
                                    max-width: 900px;
                                    background: white;
                                    border: 2px solid black;
                                }
                        
                                canvas {
                                    width: 100%;
                                    height: 100%;
                                    touch-action: none;
                                }
                        
                                button {
                                    margin: 12px;
                                    padding: 12px 26px;
                                    font-size: 16px;
                                }
                        
                                @media (orientation: landscape) {
                                    #wrapper {
                                        height: 75vh;
                                    }
                                }
                            </style>
                        </head>
                        <body>
                        
                            <h2>Bitte unterschreiben</h2>
                        
                            <div id="wrapper">
                                <canvas id="pad"></canvas>
                            </div>
                        
                            <button onclick="send()">Unterschrift senden</button>
                        
                            <script>
                                const canvas = document.getElementById("pad");
                                const ctx = canvas.getContext("2d");
                        
                                ctx.strokeStyle = "#000";
                                ctx.lineWidth = 3;
                                ctx.lineCap = "round";
                        
                                let drawing = false;
                        
                                function resizeCanvas() {
                                    const img = ctx.getImageData(0, 0, canvas.width, canvas.height);
                        
                                    const rect = canvas.getBoundingClientRect();
                                    canvas.width = rect.width;
                                    canvas.height = rect.height;
                        
                                    ctx.putImageData(img, 0, 0);
                                }
                        
                                window.addEventListener("resize", resizeCanvas);
                                resizeCanvas();
                        
                                function getPos(e) {
                                    const rect = canvas.getBoundingClientRect();
                                    const p = e.touches ? e.touches[0] : e;
                                    return {
                                        x: p.clientX - rect.left,
                                        y: p.clientY - rect.top
                                    };
                                }
                        
                                function start(e) {
                                    drawing = true;
                                    const pos = getPos(e);
                                    ctx.beginPath();
                                    ctx.moveTo(pos.x, pos.y);
                                }
                        
                                function move(e) {
                                    if (!drawing) return;
                                    e.preventDefault();
                                    const pos = getPos(e);
                                    ctx.lineTo(pos.x, pos.y);
                                    ctx.stroke();
                                }
                        
                                function end() {
                                    drawing = false;
                                }
                        
                                canvas.addEventListener("mousedown", start);
                                canvas.addEventListener("mousemove", move);
                                canvas.addEventListener("mouseup", end);
                                canvas.addEventListener("mouseleave", end);
                        
                                canvas.addEventListener("touchstart", start);
                                canvas.addEventListener("touchmove", move);
                                canvas.addEventListener("touchend", end);
                        
                                function send() {
                                    fetch("/upload", {
                                        method: "POST",
                                        body: canvas.toDataURL("image/png")
                                    }).then(() => alert("Unterschrift gesendet"));
                                }
                            </script>
                        
                        </body>
                        </html>
                        """;

                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });

            server.createContext("/upload", exchange -> {
                String data = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Unterschrift empfangen!");
                System.out.println(data.substring(0, Math.min(80, data.length())));

                String base64Image = data.split(",")[1];
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                String pngFile = "signature.png";
                try (FileOutputStream fos = new FileOutputStream(pngFile)) {
                    fos.write(imageBytes);
                }

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

                // PDF
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                String timestamp = LocalDateTime.now().format(formatter);

                File template = new File("C:/Users/kolja/OneDrive/Dokumente/JavaPDFsignatureTest.pdf");
                File output = new File("C:/Users/kolja/OneDrive/Dokumente/signed/signed_" + timestamp + ".pdf");

                try (PDDocument doc = PDDocument.load(template)) {

                    PDPage page = doc.getPage(0);

                    PDImageXObject signature =
                            PDImageXObject.createFromFile(pngFile, doc);

                    try (PDPageContentStream content =
                                 new PDPageContentStream(
                                         doc,
                                         page,
                                         PDPageContentStream.AppendMode.APPEND,
                                         true,
                                         true
                                 )) {

                        content.drawImage(signature,
                                75,   // X
                                120,   // Y
                                200,   // Breite
                                80     // Höhe
                        );
                    }

                    doc.save(output);
                    System.out.println("PDF erstellt: " + output.getAbsolutePath());

                    // PDF direkt öffnen
                    Desktop.getDesktop().open(output);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                exchange.sendResponseHeaders(200, 0);
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
