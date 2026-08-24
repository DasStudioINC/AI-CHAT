package spidernetwork.com.daveai;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;



public class HelloApplication extends Application {

    BorderPane root;
    private VBox messageContainer;
    private OllamaClient ollamaClient;
    private Label statusLabel;
    private ComboBox<String> modelSelector; // Dropdown for switching models

    private String currentChat = null;

    // Tracks history for the current active chat session
    private List<ChatMessage> currentChatHistory;

    @Override
    public void init() {
        OllamaManager manager = new OllamaManager();
        if (!manager.isServerRunning()) {
            manager.startServerProcess();
        }
        manager.ensureModelReady("DAVEAIBOT");
        manager.ensureModelReady("phi3"); // Ensure default fallback model is ready too

        ollamaClient = new OllamaClient(); // Updated constructor
        currentChatHistory = new ArrayList<>();
        try{
            currentChat = FileManager.getFileNames().get(0);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Local AI Java Client (Ollama)");

        root = new BorderPane();
        VBox chats = buildChats();
        root.setLeft(chats);

        statusLabel = new Label("Status: Ready");
        statusLabel.setPadding(new Insets(5, 10, 5, 10));



        root.setTop(toolBar());




        root.setCenter(buildMainArea());
        TextField inputField = new TextField();
        inputField.setPromptText("Ask your local AI a question...");

        Button sendButton = new Button("Send");

        Runnable handleSend = () -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                appendMessage("You", text);
                inputField.clear();
                currentChatHistory.add(new ChatMessage("user", text));

                String selectedModel = modelSelector.getValue();

                // Add a temporary loading indicator
                Text loadingNode = new Text("AI (" + selectedModel + "): Thinking...\n\n");
                loadingNode.setStyle("-fx-fill: #888888; -fx-font-style: italic;");
                messageContainer.getChildren().add(loadingNode);

                new Thread(() -> {
                    String aiResponse = ollamaClient.chat(selectedModel, currentChatHistory);
                    currentChatHistory.add(new ChatMessage("assistant", aiResponse));

                    Platform.runLater(() -> {
                        // Remove the loading indicator and print the real response
                        messageContainer.getChildren().remove(loadingNode);
                        appendMessage("AI (" + selectedModel + ")", aiResponse);

                    });

                }).start();
            }
        };

        sendButton.setOnAction(e -> handleSend.run());
        inputField.setOnAction(e -> handleSend.run());

        VBox bottomBar = new VBox(5, inputField, sendButton);
        bottomBar.setPadding(new Insets(10));
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 650, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean chatsOpened = true;
    private VBox buildChats() throws Exception {
        VBox container = new VBox();

        HBox header = new HBox();
        Label headerLabel = new Label("Dave AI (CHATS)");

        Button close = new Button("⬅");
        header.getChildren().addAll(headerLabel, close);
        close.setOnAction(e -> {
            if(chatsOpened){
                close.setText("➡");
                container.getChildren().clear();
                container.getChildren().add(close);
                chatsOpened = false;
            }else if(!chatsOpened){
                container.getChildren().clear();
                chatsOpened = true;
                try {
                    root.setLeft(buildChats());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        container.getChildren().add(header);

        Button newChat = new Button("New Chat");
        newChat.setOnAction(e -> {
            try {
                String chatName = "Chat #" + FileManager.getFileNames().size();
                FileManager.createFile(chatName, "");
                currentChat = chatName;
                root.setLeft(buildChats());
                root.setCenter(buildMainArea());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        container.getChildren().addAll(new Separator(), newChat, new Separator());
        for(String name : FileManager.getFileNames()){
            Button chat = new Button(name);
            chat.setOnAction(e -> {
                currentChat = name;
                root.setCenter(buildMainArea());
            });
            container.getChildren().add(chat);
        }

        return container;
    }


    private BorderPane buildMainArea(){
        BorderPane ro = new BorderPane();
        messageContainer = new VBox(10);
        messageContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(messageContainer);
        scrollPane.setFitToWidth(true);
        ro.setCenter(scrollPane);


        VBox.setVgrow(messageContainer, Priority.ALWAYS);


        try{
            String[] chatBubbles = FileManager.readFile(currentChat).split("\\(-\\)");
            for(int i = 0; i < chatBubbles.length - 1; i++){
                messageContainer.getChildren().add(buildChatBubble(chatBubbles[i]));
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        scrollPane.setVvalue(1);
        return ro;
    }

    private HBox buildChatBubble(String chat){
        HBox bubble = new HBox();
        Label text = new Label(chat);
        text.setPrefWidth(300);
        text.setWrapText(true);
        bubble.getChildren().add(text);
        // Check if its ai or you;

        boolean isYou = chat.stripLeading().startsWith("You:");

        if(isYou){
            bubble.setAlignment(Pos.CENTER_RIGHT);
            text.setStyle("-fx-background-color: #497BFC; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 10px");
        }else{
            bubble.setAlignment(Pos.CENTER_LEFT);
            text.setStyle("-fx-background-color: #3D3D3D; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 10px");
        }
        return bubble;
    }



    private HBox toolBar(){
        // Model Selector Dropdown
        modelSelector = new ComboBox<>();
        modelSelector.getItems().addAll("DAVEAIBOT", "phi3", "llama3");
        modelSelector.setValue("DAVEAIBOT"); // Default active model
        modelSelector.setOnAction(e -> {
            statusLabel.setText("Active Model: " + modelSelector.getValue());
        });

        HBox topBar = new HBox(10, statusLabel, modelSelector);
        topBar.setPadding(new Insets(5));

        return topBar;
    }


    private void resetNewChat() {
        currentChatHistory.clear();
        messageContainer.getChildren().clear();
        appendMessage("System: Started a new blank chat session.", "");
    }

    private void appendMessage(String sender, String message) {
        Text textNode = new Text(sender + ":\n" + message + "\n\n");

        try{
            FileManager.appendFile(currentChat, textNode.getText() + "(-)\n");
            root.setCenter(buildMainArea());
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void stop() {
        OllamaManager.stopServerProcess();
    }

    public static void main(String[] args) {
        launch(args);
    }
}