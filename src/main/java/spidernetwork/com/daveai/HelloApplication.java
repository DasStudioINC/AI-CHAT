package spidernetwork.com.daveai;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class HelloApplication extends Application {

    BorderPane root;
    VBox chatResponses;
    private OllamaClient ollamaClient;

    @Override
    public void init(){
        OllamaManager manager = new OllamaManager();

        // 1. Check if Ollama server is running
        if (!manager.isServerRunning()) {
            System.out.println("Ollama is not running. Attempting to start it...");
            boolean started = manager.startServerProcess();
            if (!started) {
                System.out.println("Warning: Could not auto-launch Ollama. Make sure Ollama is installed.");
            }
        }

        manager.ensureModelReady("phi3");
        ollamaClient = new OllamaClient("phi3");
    }

    @Override
    public void start(Stage stage) throws IOException {
        root = new BorderPane();
        chatResponses = new VBox(10);
        chatResponses.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(chatResponses);
        scroll.setFitToWidth(true);
        root.setCenter(scroll);

        TextField field = new TextField();
        field.setPromptText("Start Chatting");

        Button send = new Button("➡️");

        Runnable handleSend = () -> {
            String text = field.getText().trim();
            if (!text.isEmpty()) {
                appendMessage("You: " + text);
                field.clear();

                // Run network request in background thread to avoid freezing the UI window
                new Thread(() -> {
                    String aiResponse = ollamaClient.generateResponse(text);

                    Platform.runLater(() -> {
                        appendMessage("AI: " + aiResponse);
                    });
                }).start();
            }
        };

        send.setOnAction(e -> handleSend.run());

        HBox bt = new HBox(5, field, send);
        bt.setPadding(new Insets(10));
        root.setBottom(bt);

        Scene scene = new Scene(root, 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    private void appendMessage(String message) {
        Text textNode = new Text(message + "\n");
        chatResponses.getChildren().add(textNode);
    }
}
